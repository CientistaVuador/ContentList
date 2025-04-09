package matinilad.contentlist;

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
public class ContentEntry {

    public static enum ValidationReason {
        EXISTS,
        TYPE,
        SIZE,
        SAMPLE,
        HASH
    }

    public static interface ValidationCallbacks {

        public void setEntry(ContentEntry entry) throws IOException, InterruptedException;

        public void setPath(Path path) throws IOException, InterruptedException;

        public void setSize(long size) throws IOException, InterruptedException;

        public void progressUpdate(long bytes) throws IOException, InterruptedException;

        public void accepted(ValidationReason reason) throws IOException, InterruptedException;

        public void refused(ValidationReason reason, Object foundValue) throws IOException, InterruptedException;
    }

    public static String csvHeader() {
        return "path,type,created,modified,size,files,directories,sha256,sample";
    }

    private final ContentPath path;
    private final ContentType type;
    private final long created;
    private final long modified;
    private final long size;
    private final int files;
    private final int directories;
    private final byte[] sha256;
    private final byte[] sample;

    public ContentEntry(
            ContentPath path,
            ContentType type,
            long created, long modified, long size,
            int files, int directories,
            byte[] sha256, byte[] sample
    ) {
        Objects.requireNonNull(path, "path is null");
        Objects.requireNonNull(type, "type is null");
        if (size < 0) {
            throw new IllegalArgumentException("size is negative");
        }
        if (files < 0) {
            throw new IllegalArgumentException("files is negative");
        }
        if (sha256 != null && sha256.length != 32) {
            throw new IllegalArgumentException("Invalid sha256 length! " + sha256.length + " found, but 32 is required.");
        }
        if (path.hasSpecialLinks()) {
            throw new IllegalArgumentException("path must not contain special links!");
        }
        if (path.isRelative()) {
            throw new IllegalArgumentException("path must be absolute!");
        }
        this.path = path;
        this.type = type;
        this.modified = modified;
        this.size = size;
        this.files = files;
        this.directories = directories;
        this.sha256 = (sha256 == null ? null : sha256.clone());
        this.sample = (sample == null ? null : sample.clone());
        this.created = created;
    }

    public ContentPath getPath() {
        return path;
    }

    public ContentType getType() {
        return type;
    }

    public long getCreated() {
        return created;
    }

    public long getModified() {
        return modified;
    }

    public long getSize() {
        return size;
    }

    public int getFiles() {
        return files;
    }

    public int getDirectories() {
        return directories;
    }

    public byte[] getSha256() {
        return sha256;
    }

    public byte[] getSample() {
        return sample;
    }

    public boolean validate(Path baseDirectory, ValidationCallbacks callbacks)
            throws IOException, InterruptedException {
        Objects.requireNonNull(baseDirectory, "baseDirectory is null");
        if (callbacks != null) {
            callbacks.setEntry(this);
        }

        Path fileToValidate = getPath().resolveToPath(baseDirectory);

        if (callbacks != null) {
            callbacks.setPath(fileToValidate);
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!Files.exists(fileToValidate)) {
            if (callbacks != null) {
                callbacks.refused(ValidationReason.EXISTS, false);
            }
            return false;
        }
        if (callbacks != null) {
            callbacks.accepted(ValidationReason.EXISTS);
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        ContentType otherType = ContentListUtils.typeOf(fileToValidate);
        if (!getType().equals(otherType)) {
            if (callbacks != null) {
                callbacks.refused(ValidationReason.TYPE, otherType);
            }
            return false;
        }
        if (callbacks != null) {
            callbacks.accepted(ValidationReason.TYPE);
        }

        if (otherType.equals(ContentType.FILE)) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            long fileSize = Files.size(fileToValidate);
            if (fileSize != getSize()) {
                if (callbacks != null) {
                    callbacks.refused(ValidationReason.SIZE, fileSize);
                }
                return false;
            }
            if (callbacks != null) {
                callbacks.accepted(ValidationReason.SIZE);
                callbacks.setSize(fileSize);
            }

            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex);
            }

            if (callbacks != null) {
                callbacks.progressUpdate(0);
            }

            try (InputStream in = Files.newInputStream(fileToValidate)) {
                long count = 0;

                byte[] entrySample = getSample();
                if (entrySample != null) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    byte[] otherSample = new byte[entrySample.length];
                    int realSampleSize = 0;

                    for (int i = 0; i < otherSample.length; i++) {
                        if (Thread.interrupted()) {
                            throw new InterruptedException();
                        }
                        int b = in.read();
                        if (b == -1) {
                            break;
                        }
                        otherSample[i] = (byte) b;
                        realSampleSize++;
                        count++;
                        if (callbacks != null) {
                            callbacks.progressUpdate(count);
                        }
                    }
                    otherSample = Arrays.copyOf(otherSample, realSampleSize);
                    digest.update(otherSample, 0, otherSample.length);

                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    if (!Arrays.equals(entrySample, otherSample)) {
                        if (callbacks != null) {
                            callbacks.refused(ValidationReason.SAMPLE, otherSample);
                        }
                        return false;
                    }
                    if (callbacks != null) {
                        callbacks.accepted(ValidationReason.SAMPLE);
                    }
                }

                byte[] entryHash = getSha256();
                if (entryHash != null) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    byte[] buffer = new byte[1048576];
                    int r;
                    while ((r = in.read(buffer, 0, buffer.length)) != -1) {
                        if (Thread.interrupted()) {
                            throw new InterruptedException();
                        }
                        count += r;
                        digest.update(buffer, 0, r);
                        if (callbacks != null) {
                            callbacks.progressUpdate(count);
                        }
                    }

                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    byte[] hash = digest.digest();
                    if (!Arrays.equals(entryHash, hash)) {
                        if (callbacks != null) {
                            callbacks.refused(ValidationReason.HASH, entryHash);
                        }
                        return false;
                    }
                    if (callbacks != null) {
                        callbacks.accepted(ValidationReason.HASH);
                    }
                }
            }
        }
        
        return true;
    }
    
    public String toCSVRecord() {
        StringBuilder b = new StringBuilder();
        b.append(ContentListUtils.escapeCSVField(getPath().toString()))
                .append(',');
        b.append(ContentListUtils.escapeCSVField(getType().name()))
                .append(',');
        b.append(ContentListUtils.escapeCSVField(Long.toString(getCreated())))
                .append(',');
        b.append(ContentListUtils.escapeCSVField(Long.toString(getModified())))
                .append(',');
        b.append(ContentListUtils.escapeCSVField(Long.toString(getSize())))
                .append(',');
        b.append(ContentListUtils.escapeCSVField(Integer.toString(getFiles())))
                .append(',');
        b.append(ContentListUtils.escapeCSVField(Integer.toString(getDirectories())))
                .append(',');
        b.append(ContentListUtils.escapeCSVField(ContentListUtils.toHexString(getSha256())))
                .append(',');
        b.append(ContentListUtils.escapeCSVField(ContentListUtils.toHexString(getSample())));
        return b.toString();
    }

    @Override
    public String toString() {
        return toCSVRecord();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + Objects.hashCode(this.path);
        hash = 13 * hash + Objects.hashCode(this.type);
        hash = 13 * hash + (int) (this.created ^ (this.created >>> 32));
        hash = 13 * hash + (int) (this.modified ^ (this.modified >>> 32));
        hash = 13 * hash + (int) (this.size ^ (this.size >>> 32));
        hash = 13 * hash + this.files;
        hash = 13 * hash + this.directories;
        hash = 13 * hash + Arrays.hashCode(this.sha256);
        hash = 13 * hash + Arrays.hashCode(this.sample);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ContentEntry other = (ContentEntry) obj;
        if (this.created != other.created) {
            return false;
        }
        if (this.modified != other.modified) {
            return false;
        }
        if (this.size != other.size) {
            return false;
        }
        if (this.files != other.files) {
            return false;
        }
        if (this.directories != other.directories) {
            return false;
        }
        if (!Objects.equals(this.path, other.path)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        if (!Arrays.equals(this.sha256, other.sha256)) {
            return false;
        }
        return Arrays.equals(this.sample, other.sample);
    }

}
