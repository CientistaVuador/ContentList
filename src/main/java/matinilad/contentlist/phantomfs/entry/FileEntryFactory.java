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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import matinilad.contentlist.phantomfs.PhantomPath;

/**
 *
 * @author Cien
 */
public class FileEntryFactory {

    private boolean sha256Enabled = true;
    private int sampleSize = 32;

    public FileEntryFactory() {

    }

    public boolean isSha256Enabled() {
        return sha256Enabled;
    }

    public void setSha256Enabled(boolean sha256Enabled) {
        this.sha256Enabled = sha256Enabled;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        if (sampleSize < 0) {
            throw new IllegalArgumentException("sample size is negative");
        }
        this.sampleSize = sampleSize;
    }

    protected boolean onShouldInterrupt() {
        return Thread.interrupted();
    }

    protected void onFileProgress(Path path, long currentCount, long totalBytes) {

    }

    public FileEntry newFileEntry(Path root, Path path) throws IOException, InterruptedException {
        if (onShouldInterrupt()) {
            throw new InterruptedException("interrupted");
        }

        Objects.requireNonNull(root, "root is null");
        Objects.requireNonNull(path, "path is null");

        if (!Files.exists(path)) {
            throw new IOException("file does not exists: " + path);
        }

        Path relative = root.relativize(path);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < relative.getNameCount(); i++) {
            names.add(relative.getName(i).toString());
        }

        FileEntryType type;
        if (Files.isRegularFile(path)) {
            type = FileEntryType.FILE;
        } else if (Files.isDirectory(path)) {
            type = FileEntryType.DIRECTORY;
        } else {
            throw new IOException("unknown file type: " + path);
        }

        FileEntry entry = new FileEntry(PhantomPath.of(names.toArray(String[]::new), false), type);

        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);

            entry.setCreated(attributes.creationTime().toMillis());
            entry.setModified(attributes.lastModifiedTime().toMillis());
            entry.setAccess(attributes.lastAccessTime().toMillis());
        } catch (UnsupportedOperationException ex) {
            //ignored
        }
        
        if (entry.getType().equals(FileEntryType.FILE)) {
            long size = Files.size(path);
            entry.setSize(size);

            try (InputStream in = Files.newInputStream(path)) {
                long count = 0;
                onFileProgress(path, count, size);

                MessageDigest digest;
                try {
                    digest = MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException ex) {
                    throw new IOException(ex);
                }

                if (getSampleSize() > 0) {
                    byte[] sample = new byte[getSampleSize()];

                    for (int i = 0; i < sample.length; i++) {
                        int b = in.read();
                        if (b == -1) {
                            break;
                        }

                        sample[i] = (byte) b;
                        count++;

                        onFileProgress(path, count, size);
                        
                        if (onShouldInterrupt()) {
                            throw new InterruptedException("interrupted");
                        }
                    }

                    sample = Arrays.copyOf(sample, (int) count);
                    digest.update(sample, 0, sample.length);

                    entry.setSample(sample);
                }

                if (isSha256Enabled()) {
                    byte[] buffer = new byte[1 * 1024 * 1024];
                    int r;
                    while ((r = in.read(buffer, 0, buffer.length)) != -1) {
                        count += r;
                        digest.update(buffer, 0, r);
                        
                        onFileProgress(path, count, size);
                        
                        if (onShouldInterrupt()) {
                            throw new InterruptedException("interrupted");
                        }
                    }

                    entry.setSha256(digest.digest());
                }
            }
        }

        return entry;
    }

}
