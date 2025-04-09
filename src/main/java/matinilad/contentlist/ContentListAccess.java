package matinilad.contentlist;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Cien
 */
public class ContentListAccess {

    public static interface ContentListAccessCallbacks {

        public void onStart() throws IOException, InterruptedException;

        public void onReadProgressUpdate(long current, long total) throws IOException, InterruptedException;

        public void onContentEntryRead(ContentEntry entry, int index) throws IOException, InterruptedException;

        public void onFinish() throws IOException, InterruptedException;
    }

    private static class SeekableInputStream extends InputStream {

        private final RandomAccessFile file;

        private final byte[] buffer = new byte[524288];
        private long bufferPointer = 0;
        private int bufferSize = 0;
        private int bufferIndex = 0;
        private boolean eof = false;

        public SeekableInputStream(RandomAccessFile file) {
            this.file = file;
        }

        public RandomAccessFile getFile() {
            return file;
        }

        public long pointer() {
            return this.bufferPointer + this.bufferIndex;
        }

        private void fillBuffer() throws IOException {
            this.bufferPointer = this.file.getFilePointer();

            int index = 0;
            int r;
            while ((r = this.file.read(this.buffer, index, this.buffer.length - index)) != -1) {
                index += r;
                if (index >= this.buffer.length) {
                    break;
                }
            }

            this.bufferSize = index;
            this.bufferIndex = 0;

            if (r == -1) {
                this.eof = true;
            }
        }

        public void seek(long pointer) throws IOException {
            long index = pointer - this.bufferPointer;
            if (index >= 0 && index < this.bufferSize) {
                this.bufferIndex = (int) index;
                return;
            }
            this.file.seek(pointer);
            this.eof = false;
            fillBuffer();
        }

        @Override
        public int read() throws IOException {
            if (available() == 0) {
                if (this.eof) {
                    return -1;
                }
                fillBuffer();
            }
            int b = this.buffer[this.bufferIndex] & 0xFF;
            this.bufferIndex++;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, b.length);
            if (available() == 0) {
                if (this.eof) {
                    return -1;
                }
                fillBuffer();
            }
            if (len == 0) {
                return 0;
            }
            int minSize = Math.min(available(), len);
            System.arraycopy(this.buffer, this.bufferIndex, b, off, minSize);
            this.bufferIndex += minSize;
            return minSize;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int available() throws IOException {
            return this.bufferSize - this.bufferIndex;
        }

        @Override
        public void close() throws IOException {

        }
    }

    private static int getHeaderIndex(Map<String, Integer> map, String name)
            throws IOException {
        Integer i = map.get(name);
        if (i == null) {
            throw new IOException("Header field '" + name + "' not found.");
        }
        return i;
    }

    private final RandomAccessFile file;
    private final SeekableInputStream stream;
    private final long[] entries;

    private final int headerSize;
    private final int pathIndex;
    private final int typeIndex;
    private final int createdIndex;
    private final int modifiedIndex;
    private final int sizeIndex;
    private final int filesIndex;
    private final int directoriesIndex;
    private final int sha256Index;
    private final int sampleIndex;

    public ContentListAccess(
            RandomAccessFile file, ContentListAccessCallbacks callbacks) throws IOException, InterruptedException {
        Objects.requireNonNull(file, "file is null");
        this.file = file;

        if (callbacks != null) {
            callbacks.onStart();
        }

        this.stream = new SeekableInputStream(file);
        this.stream.seek(0);

        if (callbacks != null) {
            callbacks.onReadProgressUpdate(this.stream.pointer(), this.file.length());
        }

        String[] header = readRecord();
        if (header == null) {
            throw new IOException("Header not found.");
        }
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            headerIndex.put(header[i], i);
        }
        this.headerSize = header.length;
        this.pathIndex = getHeaderIndex(headerIndex, "path");
        this.typeIndex = getHeaderIndex(headerIndex, "type");
        this.createdIndex = getHeaderIndex(headerIndex, "created");
        this.modifiedIndex = getHeaderIndex(headerIndex, "modified");
        this.sizeIndex = getHeaderIndex(headerIndex, "size");
        this.filesIndex = getHeaderIndex(headerIndex, "files");
        this.directoriesIndex = getHeaderIndex(headerIndex, "directories");
        this.sha256Index = getHeaderIndex(headerIndex, "sha256");
        this.sampleIndex = getHeaderIndex(headerIndex, "sample");

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        if (callbacks != null) {
            callbacks.onReadProgressUpdate(this.stream.pointer(), this.file.length());
        }

        long[] entriesArray = new long[64];
        int entriesIndex = 0;

        try {
            long currentPointer = this.stream.pointer();
            ContentEntry entry;
            while ((entry = readContentEntry()) != null) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                if (callbacks != null) {
                    callbacks.onContentEntryRead(entry, entriesIndex);
                    callbacks.onReadProgressUpdate(this.stream.pointer(), this.file.length());
                }
                if (entriesIndex >= entriesArray.length) {
                    entriesArray = Arrays.copyOf(entriesArray, (entriesArray.length * 2) + 1);
                }
                entriesArray[entriesIndex] = currentPointer;
                entriesIndex++;
                
                currentPointer = this.stream.pointer();
            }
        } catch (IOException ex) {
            throw new IOException("Error at record " + entriesIndex, ex);
        }

        if (callbacks != null) {
            callbacks.onReadProgressUpdate(this.stream.pointer(), this.file.length());
        }

        this.entries = Arrays.copyOf(entriesArray, entriesIndex);

        if (callbacks != null) {
            callbacks.onFinish();
        }
    }

    public RandomAccessFile getFile() {
        return file;
    }

    private String[] readRecord() throws IOException {
        List<String> fields = new ArrayList<>();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean quotesOpen = false;
        boolean quotesClosed = false;

        while (true) {
            int current = this.stream.read();
            int next = this.stream.read();
            if (next != -1) {
                this.stream.seek(this.stream.pointer() - 1);
            }

            if (current == -1) {
                if (out.size() == 0 && fields.isEmpty()) {
                    return null;
                }
                if (quotesOpen) {
                    throw new IOException("Quotes not closed.");
                }
                fields.add(out.toString(StandardCharsets.UTF_8));
                break;
            }

            if (quotesClosed && current != ',' && current != '\n' && current != '\r') {
                throw new IOException("Expected ',' or '\\n' or '\\r'");
            }

            if (quotesOpen) {
                if (current == '"') {
                    if (next == '"') {
                        this.stream.read();
                    } else {
                        quotesOpen = false;
                        quotesClosed = true;
                        continue;
                    }
                }
                out.write(current);
                continue;
            }

            if (current == '"') {
                if (out.size() != 0) {
                    throw new IOException("Field does not start with quotes.");
                }
                quotesOpen = true;
                continue;
            }

            if (current == ',') {
                fields.add(out.toString(StandardCharsets.UTF_8));
                out.reset();
                quotesClosed = false;
                continue;
            }

            if (current == '\n' || current == '\r') {
                fields.add(out.toString(StandardCharsets.UTF_8));
                if (current == '\r' && next == '\n') {
                    this.stream.read();
                }
                break;
            }

            out.write(current);
        }

        return fields.toArray(String[]::new);
    }

    private ContentEntry readContentEntry() throws IOException {
        String[] record = readRecord();
        if (record == null) {
            return null;
        }
        if (record.length != this.headerSize) {
            throw new IOException(
                    "Invalid record size! expected "
                    + this.headerSize
                    + ", found " + record.length);
        }
        ContentEntry entry = new ContentEntry(
                ContentPath.of(record[this.pathIndex]),
                ContentType.valueOf(record[this.typeIndex]),
                Long.parseLong(record[this.createdIndex]),
                Long.parseLong(record[this.modifiedIndex]),
                Long.parseLong(record[this.sizeIndex]),
                Integer.parseInt(record[this.filesIndex]),
                Integer.parseInt(record[this.directoriesIndex]),
                ContentListUtils.readHexString(record[this.sha256Index]),
                ContentListUtils.readHexString(record[this.sampleIndex])
        );
        return entry;
    }

    public int getNumberOfEntries() {
        return this.entries.length;
    }

    public ContentEntry getEntry(int index) {
        ContentEntry entry;
        try {
            this.stream.seek(this.entries[index]);
            entry = readContentEntry();
        } catch (IOException ex) {
            throw new UncheckedIOException("Error at record " + index, ex);
        }
        return entry;
    }

}
