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
import matinilad.contentlist.phantomfs.PhantomPath;

/**
 *
 * @author Cien
 */
public class FileEntryCreator {

    public static interface Factory {

        public FileEntryCreator newFileEntryCreator();
    }

    private boolean sha256Enabled = true;
    private int sampleSize = 24;

    public FileEntryCreator() {

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

    protected boolean onShouldInterrupt() throws IOException, InterruptedException {
        return Thread.interrupted();
    }

    protected void onEntryCreated(FileEntry entry) throws IOException, InterruptedException {

    }

    protected void onEntryProgress(FileEntry entry, long bytes) throws IOException, InterruptedException {

    }

    private void checkInterrupt() throws IOException, InterruptedException {
        if (onShouldInterrupt()) {
            throw new InterruptedException();
        }
    }

    private MessageDigest createDigest() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        return digest;
    }

    private PhantomPath createPath(Path file, int depth) {
        int start = (file.getNameCount() - 1) + depth;
        if (start < 0) {
            start = 0;
        }
        
        List<String> names = new ArrayList<>();
        for (int i = start; i < file.getNameCount(); i++) {
            names.add(file.getName(i).toString());
        }
        return PhantomPath.of(names.toArray(String[]::new), false);
    }

    public FileEntry create(Path file, int depth) throws IOException, InterruptedException {
        if (file == null || (file = file.toRealPath()).getFileName() == null) {
            checkInterrupt();
            
            FileEntry root = new FileEntry(PhantomPath.of("/"), FileEntryType.DIRECTORY);
            onEntryCreated(root);
            return root;
        }
        
        PhantomPath path = createPath(file, depth);
        FileEntry entry = new FileEntry(path, FileEntryType.typeOf(file));
        onEntryCreated(entry);
        
        try {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);

            entry.setCreated(attributes.creationTime().toMillis());
            entry.setModified(attributes.lastModifiedTime().toMillis());
        } catch (UnsupportedOperationException ex) {}
        
        checkInterrupt();

        if (entry.getType().equals(FileEntryType.FILE)) {
            entry.setSize(Files.size(file));

            try (InputStream in = Files.newInputStream(file)) {
                long count = 0;
                onEntryProgress(entry, 0);

                MessageDigest digest = createDigest();

                if (getSampleSize() > 0) {
                    byte[] sample = new byte[getSampleSize()];

                    for (int i = 0; i < sample.length; i++) {
                        int b = in.read();
                        if (b == -1) {
                            break;
                        }

                        sample[i] = (byte) b;
                        count++;

                        onEntryProgress(entry, count);
                        checkInterrupt();
                    }

                    sample = Arrays.copyOf(sample, (int) count);
                    digest.update(sample, 0, sample.length);

                    entry.setSample(sample);
                }

                if (isSha256Enabled()) {
                    byte[] buffer = new byte[1048576];
                    int r;
                    while ((r = in.read(buffer, 0, buffer.length)) != -1) {
                        count += r;
                        digest.update(buffer, 0, r);

                        onEntryProgress(entry, count);
                        checkInterrupt();
                    }

                    entry.setSha256(digest.digest());
                }
            }
        }

        return entry;
    }
}
