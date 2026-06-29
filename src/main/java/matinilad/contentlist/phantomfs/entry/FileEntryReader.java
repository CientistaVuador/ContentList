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
    
    private final Reader in;

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
    
    public FileEntryReader(Reader in) {
        Objects.requireNonNull(in, "in is null");
        this.in = in;
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
            entry.getMetadata().load(metadata);
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
    
    @Override
    public void close() throws IOException {
        this.in.close();
    }

}
