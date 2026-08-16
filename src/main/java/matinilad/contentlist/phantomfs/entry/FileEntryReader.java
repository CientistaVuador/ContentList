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
import java.io.Reader;
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

    //todo: add line count
    private final Reader in;

    private int peekChar = -1;
    private boolean hasPeek = false;

    private boolean endOfFileFound = false;

    private final Map<String, Integer> indices = new HashMap<>();
    private boolean indicesPopulated = false;

    public FileEntryReader(Reader in) {
        Objects.requireNonNull(in, "in is null");
        this.in = in;
    }

    private String getFieldFromRecord(String[] record, String name) {
        Integer index = this.indices.get(name);
        if (index == null) {
            return null;
        }
        int i = index;
        if (i >= record.length) {
            return null;
        }
        return record[index];
    }

    public FileEntry readEntry() throws IOException, IllegalArgumentException, NumberFormatException {
        if (this.endOfFileFound) {
            return null;
        }

        String[] record = null;

        if (!this.indicesPopulated) {
            record = readRecord();
            if (record == null) {
                this.endOfFileFound = true;
                return null;
            }
            if (!(record.length == 0 || (record.length == 1 && !record[0].equalsIgnoreCase("path")))) {
                for (int i = 0; i < record.length; i++) {
                    this.indices.put(record[i].toLowerCase(), i);
                }
                record = null;
            }
            this.indicesPopulated = true;
        }

        if (record == null) {
            record = readRecord();
        }
        
        if (record == null) {
            this.endOfFileFound = true;
            return null;
        }
        
        String path = getFieldFromRecord(record, "path");
        if (path == null && record.length >= 1) {
            path = record[0];
        }
        String type = getFieldFromRecord(record, "type");
        if (path != null && type == null) {
            type = (path.endsWith("/") ? FileEntryType.DIRECTORY.name() : FileEntryType.FILE.name());
        }
        String created = getFieldFromRecord(record, "created");
        String modified = getFieldFromRecord(record, "modified");
        String access = getFieldFromRecord(record, "access");
        String size = getFieldFromRecord(record, "size");
        String files = getFieldFromRecord(record, "files");
        String directories = getFieldFromRecord(record, "directories");
        String sha256 = getFieldFromRecord(record, "sha256");
        String sample = getFieldFromRecord(record, "sample");
        String meta = getFieldFromRecord(record, "meta");
        
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path not found");
        }

        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("type not found");
        }

        FileEntry entry = new FileEntry(
                PhantomPath.of(path).toAbsolute().normalize(),
                FileEntryType.valueOf(type)
        );
        
        if (created != null && !created.isEmpty()) {
            entry.setCreated(Long.parseLong(created));
        }

        if (modified != null && !modified.isEmpty()) {
            entry.setModified(Long.parseLong(modified));
        }
        
        if (access != null && !access.isEmpty()) {
            entry.setAccess(Long.parseLong(access));
        }

        if (size != null && !size.isEmpty()) {
            entry.setSize(Long.parseLong(size));
        }

        if (files != null && !files.isEmpty()) {
            entry.setFiles(Integer.parseInt(files));
        }

        if (directories != null && !directories.isEmpty()) {
            entry.setDirectories(Integer.parseInt(directories));
        }
        
        if (sha256 != null || sample != null) {
            HexFormat hex = HexFormat.of();

            if (sha256 != null && !sha256.isEmpty()) {
                entry.setSha256(hex.parseHex(sha256));
            }

            if (sample != null && !sample.isEmpty()) {
                entry.setSample(hex.parseHex(sample));
            }
        }

        if (meta != null && !meta.isEmpty()) {
            entry.getMetadata().load(meta);
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
                    throw new IOException("Quotes not closed.");
                }
                fields.add(out.toString());
                break;
            }

            if (quotesClosed && current != ',' && current != '\n' && current != '\r') {
                throw new IOException("Expected ',' or '\\n' or '\\r'");
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
                    throw new IOException("Field does not start with quotes.");
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

    @Override
    public void close() throws IOException {
        this.in.close();
    }

}
