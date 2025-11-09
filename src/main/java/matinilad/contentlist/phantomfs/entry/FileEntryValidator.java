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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author Cien
 */
public class FileEntryValidator {

    public static interface Factory {
        public FileEntryValidator newFileEntryValidator(Path rootDirectory, FileEntry entry);
    }
    
    private final Path rootDirectory;
    private final FileEntry entry;
    private final Path path;

    public FileEntryValidator(Path rootDirectory, FileEntry entry) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory is null");
        this.entry = Objects.requireNonNull(entry, "entry is null");
        
        //resolve the entry path to the root directory
        this.path = this.entry.getPath().resolveToPath(this.rootDirectory);
    }
    
    public Path getRootDirectory() {
        return rootDirectory;
    }

    public FileEntry getEntry() {
        return entry;
    }
    
    public Path getPath() {
        return path;
    }
    
    protected boolean onShouldInterrupt() throws IOException, InterruptedException {
        return Thread.interrupted();
    }
    
    protected void onProgressUpdate(long bytes) throws IOException, InterruptedException {
        
    }
    
    protected void onEntryAccepted(FileEntryValidatorReason reason) throws IOException, InterruptedException {
        
    }
    
    private void checkInterrupt() throws IOException, InterruptedException {
        if (onShouldInterrupt()) {
            throw new InterruptedException();
        }
    }

    private MessageDigest createSHA256Digest() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        return digest;
    }
    
    public FileEntryValidatorResult validate() throws IOException, InterruptedException {
        FileEntry e = getEntry();
        Path f = getPath();
        
        checkInterrupt();

        //check if the file exists
        if (!Files.exists(f)) {
            return new FileEntryValidatorResult(this, FileEntryValidatorReason.EXISTENCE, true, false);
        }
        onEntryAccepted(FileEntryValidatorReason.EXISTENCE);
        
        checkInterrupt();

        //check file type
        FileEntryType otherType = FileEntryType.typeOf(f);
        if (!e.getType().equals(otherType)) {
            return new FileEntryValidatorResult(this, FileEntryValidatorReason.TYPE, e.getType(), otherType);
        }
        onEntryAccepted(FileEntryValidatorReason.TYPE);
        
        checkInterrupt();

        if (otherType.equals(FileEntryType.FILE)) {
            //check file size
            long otherSize = Files.size(f);
            if (otherSize != e.getSize()) {
                return new FileEntryValidatorResult(this, FileEntryValidatorReason.SIZE, e.getSize(), otherSize);
            }
            onEntryAccepted(FileEntryValidatorReason.SIZE);
            
            checkInterrupt();
            
            //check file sample and hash
            MessageDigest digest = createSHA256Digest();
            onProgressUpdate(0);
            try (InputStream in = Files.newInputStream(f)) {
                checkInterrupt();
                long count = 0;
                
                //check sample
                byte[] sample = e.getSample();
                if (sample != null) {
                    byte[] otherSample = new byte[sample.length];
                    int realSampleSize = 0;
                    
                    for (int i = 0; i < otherSample.length; i++) {
                        checkInterrupt();
                        
                        int b = in.read();
                        if (b == -1) {
                            break;
                        }
                        otherSample[i] = (byte) b;
                        realSampleSize++;
                        count++;
                        
                        onProgressUpdate(count);
                    }
                    otherSample = Arrays.copyOf(otherSample, realSampleSize);
                    digest.update(otherSample, 0, otherSample.length);
                    
                    if (!Arrays.equals(sample, otherSample)) {
                        return new FileEntryValidatorResult(this, FileEntryValidatorReason.SAMPLE, sample, otherSample);
                    }
                    onEntryAccepted(FileEntryValidatorReason.SAMPLE);
                    
                    checkInterrupt();
                }
                
                //check hash
                byte[] hash = e.getSha256();
                if (hash != null) {
                    byte[] buffer = new byte[1048576];
                    int r;
                    while ((r = in.read(buffer, 0, buffer.length)) != -1) {
                        checkInterrupt();
                        
                        count += r;
                        digest.update(buffer, 0, r);
                        
                        onProgressUpdate(count);
                    }
                    
                    byte[] otherHash = digest.digest();
                    if (!Arrays.equals(hash, otherHash)) {
                        return new FileEntryValidatorResult(this, FileEntryValidatorReason.HASH, hash, otherHash);
                    }
                    onEntryAccepted(FileEntryValidatorReason.HASH);
                    
                    checkInterrupt();
                }
            }
        }
        
        return new FileEntryValidatorResult(this, FileEntryValidatorReason.SUCCESS, null, null);
    }

}
