package matinilad.contentlist.phantomfs.entry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import matinilad.contentlist.phantomfs.PhantomPath;

/**
 *
 * @author Cien
 */
public class FileEntry {

    public static final PhantomPath METADATA_NAME = PhantomPath.of("/default/name.txt");
    public static final PhantomPath METADATA_AUTHOR = PhantomPath.of("/default/author.txt");
    public static final PhantomPath METADATA_DESCRIPTION = PhantomPath.of("/default/description.txt");

    private final PhantomPath path;
    private final FileEntryType type;

    private long created = System.currentTimeMillis();
    private long modified = System.currentTimeMillis();
    private long size = 0;
    private int files = 0;
    private int directories = 0;
    private byte[] sha256 = null;
    private byte[] sample = null;

    private final FileEntryMetadata metadata = new FileEntryMetadata();

    public FileEntry(PhantomPath path, FileEntryType type) {
        Objects.requireNonNull(path, "path is null");
        Objects.requireNonNull(type, "type is null");
        if (path.hasSpecialLinks()) {
            throw new IllegalArgumentException("path must not contain special links!");
        }
        if (path.isRelative()) {
            throw new IllegalArgumentException("path must be absolute!");
        }
        this.path = path;
        this.type = type;
    }

    public PhantomPath getPath() {
        return path;
    }

    public FileEntryType getType() {
        return type;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("size is negative");
        }
        this.size = size;
    }

    public int getFiles() {
        return files;
    }

    public void setFiles(int files) {
        if (files < 0) {
            throw new IllegalArgumentException("files is negative");
        }
        this.files = files;
    }

    public int getDirectories() {
        return directories;
    }

    public void setDirectories(int directories) {
        if (directories < 0) {
            throw new IllegalArgumentException("directories is negative");
        }
        this.directories = directories;
    }

    public byte[] getSha256() {
        return (this.sha256 == null ? null : this.sha256.clone());
    }

    public void setSha256(byte[] sha256) {
        if (sha256 != null && sha256.length != 32) {
            throw new IllegalArgumentException("Invalid sha256 length! " + sha256.length + " found, but 32 is required.");
        }
        this.sha256 = (sha256 == null ? null : sha256.clone());
    }

    public byte[] getSample() {
        return (this.sample == null ? null : this.sample.clone());
    }

    public void setSample(byte[] sample) {
        this.sample = (sample == null ? null : sample.clone());
    }

    public FileEntryMetadata getMetadata() {
        return metadata;
    }
    
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (FileEntryWriter writer = new FileEntryWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), FileEntryWriter.FLAG_NO_HEADER)) {
                writer.writeFileEntry(this);
            }
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
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
        final FileEntry other = (FileEntry) obj;
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
