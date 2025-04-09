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
package matinilad.contentlist;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Cien
 */
public class ContentListValidator {

    public static enum ValidatorReason {
        EXISTS,
        TYPE,
        SIZE,
        SAMPLE,
        HASH
    }

    public static interface ContentListValidatorCallbacks {

        public void onStart() throws IOException, InterruptedException;

        public void onEntryStart(ContentEntry entry, Path path) throws IOException, InterruptedException;

        public void onEntryAccepted(ValidatorReason reason, Object expected, Object found) throws IOException, InterruptedException;

        public void onEntryRefused(ValidatorReason reason, Object expected, Object found) throws IOException, InterruptedException;

        public void onEntryProgressUpdate(long current, long total) throws IOException, InterruptedException;

        public void onEntryFinish() throws IOException, InterruptedException;

        public void onFinish() throws IOException, InterruptedException;
    }

    public static boolean validate(
            Path inputFile, Path baseDirectory,
            ContentListValidatorCallbacks callbacks
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(inputFile, "input file is null");
        Objects.requireNonNull(baseDirectory, "base directory is null");
        if (!Files.exists(inputFile)) {
            throw new IOException("Input file does not exists.");
        }
        if (!Files.exists(baseDirectory)) {
            throw new IOException("Base directory does not exists");
        }
        if (!Files.isDirectory(baseDirectory)) {
            throw new IOException("Base directory is not a directory");
        }

        AtomicBoolean status = new AtomicBoolean(true);
        new ContentListAccess(
                new RandomAccessFile(inputFile.toFile(), "r"),
                new ContentListAccess.ContentListAccessCallbacks() {
            @Override
            public void onStart() throws IOException, InterruptedException {
                if (callbacks != null) {
                    callbacks.onStart();
                }
            }

            @Override
            public void onReadProgressUpdate(long current, long total) throws IOException, InterruptedException {

            }

            private void validateEntry(ContentEntry entry, Path fileToValidate) throws IOException, InterruptedException {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                
                if (!Files.exists(fileToValidate)) {
                    status.set(false);
                    if (callbacks != null) {
                        callbacks.onEntryRefused(ValidatorReason.EXISTS, true, false);
                    }
                    return;
                }
                if (callbacks != null) {
                    callbacks.onEntryAccepted(ValidatorReason.EXISTS, true, true);
                }

                ContentType type = ContentListUtils.typeOf(fileToValidate);

                if (!entry.getType().equals(type)) {
                    status.set(false);
                    if (callbacks != null) {
                        callbacks.onEntryRefused(ValidatorReason.TYPE, entry.getType(), type);
                    }
                    return;
                }
                if (callbacks != null) {
                    callbacks.onEntryAccepted(ValidatorReason.TYPE, entry.getType(), type);
                }

                if (type.equals(ContentType.FILE)) {
                    long fileSize = Files.size(fileToValidate);
                    if (fileSize != entry.getSize()) {
                        status.set(false);
                        if (callbacks != null) {
                            callbacks.onEntryRefused(ValidatorReason.SIZE, entry.getSize(), fileSize);
                        }
                        return;
                    }
                    if (callbacks != null) {
                        callbacks.onEntryAccepted(ValidatorReason.SIZE, entry.getSize(), fileSize);
                    }

                    MessageDigest digest;
                    try {
                        digest = MessageDigest.getInstance("SHA-256");
                    } catch (NoSuchAlgorithmException ex) {
                        throw new RuntimeException(ex);
                    }

                    if (callbacks != null) {
                        callbacks.onEntryProgressUpdate(0, fileSize);
                    }

                    try (InputStream in = Files.newInputStream(fileToValidate)) {
                        long count = 0;

                        byte[] entrySample = entry.getSample();
                        if (entrySample != null) {
                            byte[] sample = new byte[entrySample.length];
                            int realSampleSize = 0;

                            for (int i = 0; i < sample.length; i++) {
                                int b = in.read();
                                if (b == -1) {
                                    break;
                                }
                                sample[i] = (byte) b;
                                realSampleSize++;
                                count++;
                                if (callbacks != null) {
                                    callbacks.onEntryProgressUpdate(count, fileSize);
                                }
                                if (Thread.interrupted()) {
                                    throw new InterruptedException();
                                }
                            }
                            sample = Arrays.copyOf(sample, realSampleSize);
                            digest.update(sample, 0, sample.length);
                            
                            if (!Arrays.equals(entrySample, sample)) {
                                status.set(false);
                                if (callbacks != null) {
                                    callbacks.onEntryRefused(ValidatorReason.SAMPLE, entrySample.clone(), sample.clone());
                                }
                                return;
                            }
                            if (callbacks != null) {
                                callbacks.onEntryAccepted(ValidatorReason.SAMPLE, entrySample.clone(), sample.clone());
                            }
                        }

                        byte[] entryHash = entry.getSha256();
                        if (entryHash != null) {
                            byte[] buffer = new byte[1048576];
                            int r;
                            while ((r = in.read(buffer, 0, buffer.length)) != -1) {
                                count += r;
                                digest.update(buffer, 0, r);
                                if (callbacks != null) {
                                    callbacks.onEntryProgressUpdate(count, fileSize);
                                }
                                if (Thread.interrupted()) {
                                    throw new InterruptedException();
                                }
                            }

                            byte[] hash = digest.digest();

                            if (!Arrays.equals(entryHash, hash)) {
                                status.set(false);
                                if (callbacks != null) {
                                    callbacks.onEntryRefused(ValidatorReason.HASH, entryHash.clone(), hash);
                                }
                                return;
                            }
                            if (callbacks != null) {
                                callbacks.onEntryAccepted(ValidatorReason.HASH, entryHash.clone(), hash);
                            }
                        }
                    }
                }
            }

            @Override
            public void onContentEntryRead(ContentEntry entry, int index) throws IOException, InterruptedException {
                Path fileToValidate = entry.getPath().resolveToPath(baseDirectory);
                if (callbacks != null) {
                    callbacks.onEntryStart(entry, fileToValidate);
                    callbacks.onEntryProgressUpdate(0, 0);
                }
                validateEntry(entry, fileToValidate);
                if (callbacks != null) {
                    callbacks.onEntryFinish();
                }
            }

            @Override
            public void onFinish() throws IOException, InterruptedException {
                if (callbacks != null) {
                    callbacks.onFinish();
                }
            }
        });
        return status.get();
    }

    private ContentListValidator() {

    }

}
