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
import java.io.Writer;
import java.util.HexFormat;
import java.util.Objects;

/**
 *
 * @author Cien
 */
public class FileEntryWriter implements Closeable {

    public static class Flags {

        private boolean allDisabled = false;

        private boolean typeEnabled = true;
        private boolean timestampsEnabled = true;
        private boolean sizeEnabled = true;
        private boolean filesAndDirectoriesEnabled = true;
        private boolean sha256Enabled = true;
        private boolean sampleEnabled = true;
        private boolean metadataEnabled = true;

        public Flags() {

        }

        public boolean isAllDisabled() {
            return allDisabled;
        }

        public void setAllDisabled(boolean allDisabled) {
            this.allDisabled = allDisabled;
        }

        public void setTypeEnabled(boolean typeEnabled) {
            this.typeEnabled = typeEnabled;
        }

        public boolean isTypeEnabled() {
            if (isAllDisabled()) {
                return false;
            }
            return typeEnabled;
        }

        public void setTimestampsEnabled(boolean timestampsEnabled) {
            this.timestampsEnabled = timestampsEnabled;
        }

        public boolean isTimestampsEnabled() {
            if (isAllDisabled()) {
                return false;
            }
            return timestampsEnabled;
        }

        public void setSizeEnabled(boolean sizeEnabled) {
            this.sizeEnabled = sizeEnabled;
        }

        public boolean isSizeEnabled() {
            if (isAllDisabled()) {
                return false;
            }
            return sizeEnabled;
        }

        public void setFilesAndDirectoriesEnabled(boolean filesAndDirectoriesEnabled) {
            this.filesAndDirectoriesEnabled = filesAndDirectoriesEnabled;
        }

        public boolean isFilesAndDirectoriesEnabled() {
            if (isAllDisabled()) {
                return false;
            }
            return filesAndDirectoriesEnabled;
        }

        public void setSha256Enabled(boolean sha256Enabled) {
            this.sha256Enabled = sha256Enabled;
        }

        public boolean isSha256Enabled() {
            if (isAllDisabled()) {
                return false;
            }
            return sha256Enabled;
        }

        public void setSampleEnabled(boolean sampleEnabled) {
            this.sampleEnabled = sampleEnabled;
        }

        public boolean isSampleEnabled() {
            if (isAllDisabled()) {
                return false;
            }
            return sampleEnabled;
        }

        public boolean isMetadataEnabled() {
            if (isAllDisabled()) {
                return false;
            }
            return metadataEnabled;
        }

        public void setMetadataEnabled(boolean metadataEnabled) {
            this.metadataEnabled = metadataEnabled;
        }

    }

    private final Writer out;
    private final Flags flags;

    private boolean firstLineWritten = false;
    private boolean headerWritten = false;

    public FileEntryWriter(Writer out, Flags flags) {
        Objects.requireNonNull(out, "out is null");
        this.out = out;
        this.flags = Objects.requireNonNull(flags, "flags is null");
    }

    public Flags getFlags() {
        return flags;
    }

    public void writeHeader() throws IOException {
        if (this.headerWritten || this.flags.isAllDisabled()) {
            return;
        }

        StringBuilder b = new StringBuilder();
        b.append("path");
        if (this.flags.isTypeEnabled()) {
            b.append(",type");
        }
        if (this.flags.isTimestampsEnabled()) {
            b.append(",created,modified,access");
        }
        if (this.flags.isSizeEnabled()) {
            b.append(",size");
        }
        if (this.flags.isFilesAndDirectoriesEnabled()) {
            b.append(",files,directories");
        }
        if (this.flags.isSha256Enabled()) {
            b.append(",sha256");
        }
        if (this.flags.isSampleEnabled()) {
            b.append(",sample");
        }
        if (this.flags.isMetadataEnabled()) {
            b.append(",meta");
        }
        this.out.write(b.toString());

        this.headerWritten = true;
        this.firstLineWritten = true;
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

    public void writeFileEntry(FileEntry entry) throws IOException {
        writeHeader();
        
        if (this.flags.isAllDisabled() && entry.getPath().isRoot()) {
            return;
        }
        
        if (this.firstLineWritten) {
            this.out.write(System.lineSeparator());
        }
        this.headerWritten = true;
        this.firstLineWritten = true;
        
        String text;
        if (!this.flags.isAllDisabled()) {
            StringBuilder b = new StringBuilder();
            HexFormat hex = HexFormat.of();
            String path = entry.getPath().toString();
            
            b.append(path);
            if (this.flags.isTypeEnabled()) {
                b.append(",").append(escapeField(entry.getType().name()));
            } else if (entry.getType().equals(FileEntryType.DIRECTORY) && !path.endsWith("/")) {
                b.append("/");
            }
            
            if (this.flags.isTimestampsEnabled()) {
                b.append(",").append(escapeField(Long.toString(entry.getCreated())));
                b.append(",").append(escapeField(Long.toString(entry.getModified())));
                b.append(",").append(escapeField(Long.toString(entry.getAccess())));
            }
            if (this.flags.isSizeEnabled()) {
                b.append(",").append(escapeField(Long.toString(entry.getSize())));
            }
            if (this.flags.isFilesAndDirectoriesEnabled()) {
                b.append(",").append(escapeField(Integer.toString(entry.getFiles())));
                b.append(",").append(escapeField(Integer.toString(entry.getDirectories())));
            }
            if (this.flags.isSha256Enabled()) {
                b.append(",");

                byte[] sha256 = entry.getSha256();
                if (sha256 != null && sha256.length > 0) {
                    b.append(escapeField(hex.formatHex(sha256)));
                }
            }
            if (this.flags.isSampleEnabled()) {
                b.append(",");

                byte[] sample = entry.getSample();
                if (sample != null && sample.length > 0) {
                    b.append(escapeField(hex.formatHex(sample)));
                }
            }
            if (this.flags.isMetadataEnabled()) {
                b.append(",");
                b.append(escapeField(entry.getMetadata().save()));
            }
            text = b.toString();
        } else {
            text = entry.getPath().toString().substring(1);
            if (entry.getType().equals(FileEntryType.DIRECTORY)) {
                text += "/";
            }
        }
        this.out.write(text);
    }

    @Override
    public void close() throws IOException {
        this.out.close();
    }

}
