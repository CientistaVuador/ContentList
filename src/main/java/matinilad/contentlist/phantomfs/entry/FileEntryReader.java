/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */
package matinilad.contentlist.phantomfs.entry;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import matinilad.contentlist.phantomfs.PhantomPath;

/**
 *
 * @author Cien
 */
public class FileEntryReader implements Closeable {

    @Deprecated
    public static interface ContentListAccessCallbacks {

        public void onStart() throws IOException, InterruptedException;

        public void onReadProgressUpdate(long current, long total) throws IOException, InterruptedException;

        public void onContentEntryRead(FileEntry entry, int index) throws IOException, InterruptedException;

        public void onFinish() throws IOException, InterruptedException;
    }

    private final InputStreamReader in;

    private int peekChar = -1;
    private boolean hasPeek = false;

    private boolean endOfFileFound = false;
    private boolean indicesPopulated = false;
    private int pathIndex;
    private int typeIndex;
    private int createdIndex;
    private int modifiedIndex;
    private int sizeIndex;
    private int filesIndex;
    private int directoriesIndex;
    private int sha256Index;
    private int sampleIndex;
    private int metaIndex;

    @Deprecated
    private RandomAccessFile file = null;
    @Deprecated
    private FileEntry[] entries = null;

    public FileEntryReader(InputStreamReader in) {
        Objects.requireNonNull(in, "in is null");
        this.in = in;
    }

    @Deprecated
    public FileEntryReader(RandomAccessFile file, FileEntryReader.ContentListAccessCallbacks callbacks) throws IOException, InterruptedException {
        Objects.requireNonNull(file, "file is null");
        this.file = file;

        if (callbacks != null) {
            callbacks.onStart();
            callbacks.onReadProgressUpdate(0, file.length());
        }

        file.seek(0);
        FileEntryReader reader = new FileEntryReader(new InputStreamReader(new InputStream() {
            long count = 0;

            @Override
            public int read() throws IOException {
                if (Thread.interrupted()) {
                    throw new RuntimeException(new InterruptedException());
                }

                int r = file.read();
                if (r != -1) {
                    this.count++;
                    if (callbacks != null) {
                        try {
                            callbacks.onReadProgressUpdate(count, file.length());
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
                return r;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (Thread.interrupted()) {
                    throw new RuntimeException(new InterruptedException());
                }

                int r = file.read(b, off, len);
                if (r != -1) {
                    this.count += r;
                    if (callbacks != null) {
                        try {
                            callbacks.onReadProgressUpdate(this.count, file.length());
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
                return r;
            }
        }, StandardCharsets.UTF_8));

        try {
            List<FileEntry> list = new ArrayList<>();
            FileEntry entry;
            while ((entry = reader.readEntry()) != null) {
                if (callbacks != null) {
                    callbacks.onContentEntryRead(entry, list.size());
                }
                list.add(entry);
            }
            this.entries = list.toArray(FileEntry[]::new);
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof InterruptedException e) {
                throw e;
            }
            throw ex;
        }
        
        if (callbacks != null) {
            callbacks.onFinish();
        }

        file.seek(0);
        this.in = new InputStreamReader(new InputStream() {
            @Override
            public int read() throws IOException {
                return file.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return file.read(b, off, len);
            }
        }, StandardCharsets.UTF_8);
    }

    private int getHeaderIndex(Map<String, Integer> map, String name) {
        Integer i = map.get(name);
        if (i == null) {
            return -1;
        }
        return i;
    }

    private String getFieldFromRecord(String[] record, int index) {
        if (index < 0 || index >= record.length) {
            return null;
        }
        return record[index];
    }
    
    private void parseMeta(FileEntry entry, String data) {
        StringBuilder b = new StringBuilder();
        String key = null;
        
        boolean quotes = false;
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            
            if (quotes) {
                if (c == '\'') {
                    char peek = '\0';
                    if ((i + 1) < data.length()) {
                        peek = data.charAt(i + 1);
                    }
                    if (peek != '\'') {
                        quotes = false;
                        continue;
                    }
                    i++;
                }
                b.append(c);
                continue;
            }
            
            if (c == '\'') {
                quotes = true;
                continue;
            }
            
            if (Character.isWhitespace(c)) {
                continue;
            }
            
            if (c == '=') {
                key = b.toString();
                b.setLength(0);
                continue;
            }
            
            if (c == ';' && key != null) {
                entry.setMetadata(key, b.toString());
                b.setLength(0);
                key = null;
                continue;
            }
            
            b.append(c);
        }
        if (key != null) {
            entry.setMetadata(key, b.toString());
        }
    }

    public FileEntry readEntry() throws IOException, IllegalArgumentException, NumberFormatException {
        if (this.endOfFileFound) {
            return null;
        }

        if (!this.indicesPopulated) {
            String[] header = readRecord();
            if (header == null) {
                this.endOfFileFound = true;
                return null;
            }
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                headerIndex.put(header[i], i);
            }
            this.pathIndex = getHeaderIndex(headerIndex, "path");
            this.typeIndex = getHeaderIndex(headerIndex, "type");
            this.createdIndex = getHeaderIndex(headerIndex, "created");
            this.modifiedIndex = getHeaderIndex(headerIndex, "modified");
            this.sizeIndex = getHeaderIndex(headerIndex, "size");
            this.filesIndex = getHeaderIndex(headerIndex, "files");
            this.directoriesIndex = getHeaderIndex(headerIndex, "directories");
            this.sha256Index = getHeaderIndex(headerIndex, "sha256");
            this.sampleIndex = getHeaderIndex(headerIndex, "sample");
            this.metaIndex = getHeaderIndex(headerIndex, "meta");
            this.indicesPopulated = true;
            
            if (this.pathIndex < 0) {
                throw new IOException("path column not found.");
            }
            if (this.typeIndex < 0) {
                throw new IOException("type column not found.");
            }
        }
        
        String[] record = readRecord();
        
        if (record == null) {
            this.endOfFileFound = true;
            return null;
        }
        
        String path = getFieldFromRecord(record, this.pathIndex);
        String type = getFieldFromRecord(record, this.typeIndex);
        
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path not found");
        }
        
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("type not found");
        }
        
        FileEntry entry = new FileEntry(PhantomPath.of(path), FileEntryType.valueOf(type));
        
        String created = getFieldFromRecord(record, this.createdIndex);
        String modified = getFieldFromRecord(record, this.modifiedIndex);
        String size = getFieldFromRecord(record, this.sizeIndex);
        
        if (created != null && !created.isEmpty()) {
            entry.setCreated(Long.parseLong(created));
        }
        
        if (modified != null && !modified.isEmpty()) {
            entry.setModified(Long.parseLong(modified));
        }
        
        if (size != null && !size.isEmpty()) {
            entry.setSize(Long.parseLong(size));
        }
        
        String files = getFieldFromRecord(record, this.filesIndex);
        String directories = getFieldFromRecord(record, this.directoriesIndex);
        
        if (files != null && !files.isEmpty()) {
            entry.setFiles(Integer.parseInt(files));
        }
        
        if (directories != null && !directories.isEmpty()) {
            entry.setDirectories(Integer.parseInt(directories));
        }
        
        String sha256 = getFieldFromRecord(record, this.sha256Index);
        String sample = getFieldFromRecord(record, this.sampleIndex);
        
        if (sha256 != null || sample != null) {
            HexFormat hex = HexFormat.of();
            
            if (sha256 != null && !sha256.isEmpty()) {
                entry.setSha256(hex.parseHex(sha256));
            }
            
            if (sample != null && !sample.isEmpty()) {
                entry.setSample(hex.parseHex(sample));
            }
        }
        
        String metadata = getFieldFromRecord(record, this.metaIndex);
        
        if (metadata != null && !metadata.isEmpty()) {
            parseMeta(entry, metadata);
        }
        
        return entry;
    }

    private int read() throws IOException {
        if (this.hasPeek) {
            this.hasPeek = false;
            return this.peekChar;
        }
        return this.in.read();
    }

    private int peek() throws IOException {
        if (this.hasPeek) {
            return this.peekChar;
        }
        this.peekChar = this.in.read();
        this.hasPeek = true;
        return this.peekChar;
    }

    private String[] readRecord() throws IOException {
        List<String> fields = new ArrayList<>();

        StringBuilder out = new StringBuilder();

        boolean quotesOpen = false;
        boolean quotesClosed = false;

        while (true) {
            int current = read();
            int next = peek();

            if (current == -1) {
                if (out.isEmpty() && fields.isEmpty()) {
                    return null;
                }
                if (quotesOpen) {
                    //throw new IOException("Quotes not closed.");
                }
                fields.add(out.toString());
                break;
            }

            if (quotesClosed && current != ',' && current != '\n' && current != '\r') {
                //throw new IOException("Expected ',' or '\\n' or '\\r'");
            }

            if (quotesOpen) {
                if (current == '"') {
                    if (next == '"') {
                        read();
                    } else {
                        quotesOpen = false;
                        quotesClosed = true;
                        continue;
                    }
                }
                out.append((char) current);
                continue;
            }

            if (current == '"') {
                if (!out.isEmpty()) {
                    //throw new IOException("Field does not start with quotes.");
                }
                quotesOpen = true;
                continue;
            }

            if (current == ',') {
                fields.add(out.toString());
                out.setLength(0);
                quotesClosed = false;
                continue;
            }

            if (current == '\n' || current == '\r') {
                fields.add(out.toString());
                if (current == '\r' && next == '\n') {
                    read();
                }
                break;
            }

            out.append((char) current);
        }

        return fields.toArray(String[]::new);
    }

    @Deprecated
    public RandomAccessFile getFile() {
        return file;
    }

    @Deprecated
    public int getNumberOfEntries() {
        return this.entries.length;
    }

    @Deprecated
    public FileEntry getEntry(int index) {
        return this.entries[index];
    }
    
    @Override
    public void close() throws IOException {
        this.in.close();
    }

}
