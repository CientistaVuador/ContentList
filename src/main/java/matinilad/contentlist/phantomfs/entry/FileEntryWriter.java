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
import java.io.OutputStreamWriter;
import java.util.HexFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 *
 * @author Cien
 */
public class FileEntryWriter implements Closeable {

    public static final int FLAG_NO_FILES_AND_DIRECTORIES = 0b1;
    public static final int FLAG_NO_SHA256 = 0b10;
    public static final int FLAG_NO_SAMPLE = 0b100;
    public static final int FLAG_NO_METADATA = 0b1000;
    
    private final OutputStreamWriter out;
    private final int flags;
    
    private boolean headerWritten = false;
    
    public FileEntryWriter(OutputStreamWriter out, int flags) {
        Objects.requireNonNull(out, "out is null");
        this.out = out;
        this.flags = flags;
    }
    
    public int getFlags() {
        return flags;
    }
    
    private boolean writeFilesAndDirectories() {
        return (this.flags & FLAG_NO_FILES_AND_DIRECTORIES) == 0;
    }
    
    private boolean writeSha256() {
        return (this.flags & FLAG_NO_SHA256) == 0;
    }
    
    private boolean writeSample() {
        return (this.flags & FLAG_NO_SAMPLE) == 0;
    }
    
    private boolean writeMetadata() {
        return (this.flags & FLAG_NO_METADATA) == 0;
    }
    
    public void writeHeader() throws IOException {
        if (this.headerWritten) {
            return;
        }
        
        StringBuilder b = new StringBuilder();
        b.append("path,type,created,modified,size");
        if (writeFilesAndDirectories()) {
            b.append(",files,directories");
        }
        if (writeSha256()) {
            b.append(",sha256");
        }
        if (writeSample()) {
            b.append(",sample");
        }
        if (writeMetadata()) {
            b.append(",meta");
        }
        this.out.write(b.toString());
        
        this.headerWritten = true;
    }
    
    private String escapeField(String s) {
        StringBuilder b = new StringBuilder();
        boolean quotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\n' || c == '\r' || c == ',') {
                quotes = true;
            }
            if (c == '"') {
                b.append('"');
            }
            b.append(c);
        }
        String result = b.toString();
        if (quotes) {
            return '"' + result + '"';
        }
        return result;
    }
    
    private String escapeMetadataField(String s) {
        StringBuilder b = new StringBuilder();
        b.append('\'');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                b.append('\'');
            }
            b.append(c);
        }
        b.append('\'');
        return b.toString();
    }
    
    private String writeMetadata(Map<String, String> map) {
        StringBuilder b = new StringBuilder();
        for (Entry<String, String> e:map.entrySet()) {
            b
                    .append(escapeMetadataField(e.getKey()))
                    .append('=')
                    .append(escapeMetadataField(e.getValue()))
                    .append(';');
        }
        if (!b.isEmpty()) {
            b.setLength(b.length() - 1);
        }
        return b.toString();
    }
    
    public void writeFileEntry(FileEntry entry) throws IOException {
        writeHeader();
        
        this.out.write(System.lineSeparator());
        
        StringBuilder b = new StringBuilder();
        
        b
                .append(escapeField(entry.getPath().toString())).append(",")
                .append(escapeField(entry.getType().name())).append(",")
                .append(escapeField(Long.toString(entry.getCreated()))).append(",")
                .append(escapeField(Long.toString(entry.getModified()))).append(",")
                .append(escapeField(Long.toString(entry.getSize())))
                ;
        
        if (writeFilesAndDirectories()) {
            b.append(",");
            
            b
                    .append(escapeField(Integer.toString(entry.getFiles()))).append(",")
                    .append(escapeField(Integer.toString(entry.getDirectories())))
                    ;
        }
        
        HexFormat hex = HexFormat.of();
        
        if (writeSha256()) {
            b.append(",");
            
            byte[] sha256 = entry.getSha256();
            if (sha256 != null && sha256.length > 0) {
                b.append(escapeField(hex.formatHex(sha256)));
            }
        }
        
        if (writeSample()) {
            b.append(",");
            
            byte[] sample = entry.getSample();
            if (sample != null && sample.length > 0) {
                b.append(escapeField(hex.formatHex(sample)));
            }
        }
        
        if (writeMetadata()) {
            b.append(",");
            
            b.append(escapeField(writeMetadata(entry.getMetadata())));
        }
        
        this.out.write(b.toString());
    }
    
    @Override
    public void close() throws IOException {
        this.out.close();
    }
    
}
