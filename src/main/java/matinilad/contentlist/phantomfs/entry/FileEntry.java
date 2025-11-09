package matinilad.contentlist.phantomfs.entry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import matinilad.contentlist.phantomfs.PhantomPath;

/**
 *
 * @author Cien
 */
public class FileEntry {

    @Deprecated
    public static interface ValidationCallbacks {

        public void setEntry(FileEntry entry) throws IOException, InterruptedException;

        public void setPath(Path path) throws IOException, InterruptedException;

        public void setSize(long size) throws IOException, InterruptedException;

        public void progressUpdate(long bytes) throws IOException, InterruptedException;

        public void accepted(FileEntryValidatorReason reason) throws IOException, InterruptedException;

        public void refused(FileEntryValidatorReason reason, Object foundValue) throws IOException, InterruptedException;
    }

    @Deprecated
    public static String csvHeader() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (FileEntryWriter writer = new FileEntryWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), FileEntryWriter.FLAG_NO_METADATA)) {
                writer.writeHeader();
            }
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static final String METADATA_NAME = "default.name";
    public static final String METADATA_AUTHOR = "default.author";
    public static final String METADATA_DESCRIPTION = "default.description";
    
    private final PhantomPath path;
    private final FileEntryType type;
    
    private long created = System.currentTimeMillis();
    private long modified = System.currentTimeMillis();
    private long size = 0;
    private int files = 0;
    private int directories = 0;
    private byte[] sha256 = null;
    private byte[] sample = null;
    
    private final Map<String, String> metadata = new LinkedHashMap<>();
    
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
    
    @Deprecated
    public FileEntry(
            PhantomPath path,
            FileEntryType type,
            long created, long modified, long size,
            int files, int directories,
            byte[] sha256, byte[] sample,
            Map<String, String> metadata
    ) {
        Objects.requireNonNull(path, "path is null");
        Objects.requireNonNull(type, "type is null");
        if (size < 0) {
            throw new IllegalArgumentException("size is negative");
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
        this.created = created;
        this.modified = modified;
        this.size = size;
        this.files = files;
        this.directories = directories;
        this.sha256 = (sha256 == null ? null : sha256.clone());
        this.sample = (sample == null ? null : sample.clone());
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
    }

    @Deprecated
    public FileEntry(
            PhantomPath path,
            FileEntryType type,
            long created, long modified, long size,
            int files, int directories,
            byte[] sha256, byte[] sample
    ) {
        this(path, type, created, modified, size, files, directories, sha256, sample, null);
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

    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public String getMetadata(String key) {
        return getMetadata().get(key);
    }
    
    public void setMetadata(String key, String value) {
        getMetadata().put(key, value);
    }
    
    public String getMetadataName() {
        return getMetadata(METADATA_NAME);
    }
    
    public void setMetadataName(String name) {
        setMetadata(METADATA_NAME, name);
    }
    
    public String getMetadataAuthor() {
        return getMetadata(METADATA_AUTHOR);
    }
    
    public void setMetadataAuthor(String author) {
        setMetadata(METADATA_AUTHOR, author);
    }
    
    public String getMetadataDescription() {
        return getMetadata(METADATA_DESCRIPTION);
    }
    
    public void setMetadataDescription(String description) {
        setMetadata(METADATA_DESCRIPTION, description);
    }
    
    @Deprecated
    public boolean validate(Path rootDirectory, ValidationCallbacks callbacks) throws IOException, InterruptedException {
        FileEntryValidator validator = new FileEntryValidator(rootDirectory, this) {
            @Override
            public boolean onShouldInterrupt() throws IOException, InterruptedException {
                return Thread.interrupted();
            }

            @Override
            public void onProgressUpdate(long bytes) throws IOException, InterruptedException {
                if (callbacks != null) {
                    callbacks.progressUpdate(bytes);
                }
            }
            
            @Override
            public void onEntryAccepted(FileEntryValidatorReason reason) throws IOException, InterruptedException {
                if (callbacks != null) {
                    callbacks.accepted(reason);
                }
                if (reason == FileEntryValidatorReason.SIZE) {
                    if (callbacks != null) {
                        callbacks.setSize(getEntry().getSize());
                    }
                }
            }
        };
        if (callbacks != null) {
            callbacks.setEntry(validator.getEntry());
            callbacks.setPath(validator.getPath());
        }
        FileEntryValidatorResult result = validator.validate();
        if (!result.success()) {
            if (callbacks != null) {
                callbacks.refused(result.getReason(), result.getFoundValue());
            }
            return false;
        }
        
        return true;
    }
    
    @Deprecated
    public String toCSVRecord() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (FileEntryWriter writer = new FileEntryWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), FileEntryWriter.FLAG_NO_METADATA)) {
                writer.writeFileEntry(this);
            }
            List<String> lines = out.toString(StandardCharsets.UTF_8).lines().toList();
            return lines.subList(1, lines.size()).stream().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
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
