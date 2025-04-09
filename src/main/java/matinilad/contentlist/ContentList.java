package matinilad.contentlist;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Cien
 */
public class ContentList {

    public static interface ContentListCallbacks {

        public void onStart() throws IOException, InterruptedException;

        public void onFileUnreadable(Path path) throws IOException, InterruptedException;

        public void onFileDuplicated(Path path) throws IOException, InterruptedException;

        public void onEntryStart(ContentPath path) throws IOException, InterruptedException;

        public void onEntryFinish(ContentEntry entry) throws IOException, InterruptedException;

        public void onEntryProgressUpdate(long current, long total) throws IOException, InterruptedException;

        public void onFinish() throws IOException;

    }

    private static void sortByName(List<Path> list) {
        Comparator<Path> comparator = (o1, o2)
                -> String.CASE_INSENSITIVE_ORDER
                        .compare(o1.getFileName().toString(), o2.getFileName().toString());
        list.sort(comparator);
    }

    private static ContentPath getContentPath(Path parent, Path file) {
        Path relative = file;
        if (parent != null) {
            relative = parent.relativize(file);
        }
        List<String> entryNames = new ArrayList<>();
        for (int i = 0; i < relative.getNameCount(); i++) {
            entryNames.add(relative.getName(i).toString());
        }
        return ContentPath.of(entryNames.toArray(String[]::new), false);
    }

    private static class ReadFileReturn {

        final long size;
        final int files;
        final int directories;

        public ReadFileReturn(long size, int files, int directories) {
            this.size = size;
            this.files = files;
            this.directories = directories;
        }
    }

    private static ReadFileReturn readFile(
            Path parent, Path file,
            BufferedWriter output, ContentListCallbacks callbacks
    ) throws IOException, InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!Files.isReadable(file) || !Files.exists(file)) {
            if (callbacks != null) {
                callbacks.onFileUnreadable(file);
            }
            return new ReadFileReturn(0, 0, 0);
        }

        ContentPath contentPath = getContentPath(parent, file);

        ContentType type = ContentListUtils.typeOf(file);

        long created;
        long modified;
        try {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);

            created = attributes.creationTime().toMillis();
            modified = attributes.lastModifiedTime().toMillis();
        } catch (UnsupportedOperationException ex) {
            created = 0;
            modified = 0;
        }

        long size = 0;
        int fileCount = 0;
        int directoryCount = 0;

        byte[] sample = null;
        byte[] hash = null;

        if (type.equals(ContentType.FILE)) {
            if (callbacks != null) {
                callbacks.onEntryStart(contentPath);
            }

            long fileSize = Files.size(file);

            try (InputStream in = Files.newInputStream(file)) {
                long count = 0;

                sample = new byte[32];
                int realSampleSize = 0;

                MessageDigest digest;
                try {
                    digest = MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException ex) {
                    throw new RuntimeException(ex);
                }

                if (callbacks != null) {
                    callbacks.onEntryProgressUpdate(count, fileSize);
                }

                int b = -1;
                for (int i = 0; i < sample.length; i++) {
                    b = in.read();
                    if (b == -1) {
                        break;
                    }
                    sample[i] = (byte) b;
                    realSampleSize++;
                    count++;
                    if (callbacks != null) {
                        callbacks.onEntryProgressUpdate(count, fileSize);
                    }
                }
                sample = Arrays.copyOf(sample, realSampleSize);
                digest.update(sample, 0, sample.length);

                if (b != -1) {
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
                }

                hash = digest.digest();
                size = count;
            }
        }

        if (type.equals(ContentType.DIRECTORY)) {
            List<Path> children = Files.list(file).toList();

            List<Path> directories = new ArrayList<>();
            List<Path> files = new ArrayList<>();

            for (Path p : children) {
                if (Files.isDirectory(p)) {
                    directories.add(p);
                } else {
                    files.add(p);
                }
            }

            sortByName(directories);
            sortByName(files);

            for (Path d : directories) {
                ReadFileReturn r = readFile(parent, d, output, callbacks);
                size += r.size;
                fileCount += r.files;
                directoryCount += r.directories;

                directoryCount++;
            }

            for (Path f : files) {
                ReadFileReturn r = readFile(parent, f, output, callbacks);
                size += r.size;
                fileCount += r.files;
                directoryCount += r.directories;

                fileCount++;
            }

        }

        if (callbacks != null && !type.equals(ContentType.FILE)) {
            callbacks.onEntryStart(contentPath);
            callbacks.onEntryProgressUpdate(0, 0);
        }

        ContentEntry entry = new ContentEntry(
                contentPath, type,
                created, modified, size,
                fileCount, directoryCount,
                hash, sample
        );

        output.write(entry.toCSVRecord());
        output.newLine();

        if (callbacks != null) {
            callbacks.onEntryFinish(entry);
        }
        
        return new ReadFileReturn(size, fileCount, directoryCount);
    }

    public static void create(
            OutputStream output, ContentListCallbacks callbacks, Path... paths
    ) throws IOException, InterruptedException {
        if (callbacks != null) {
            callbacks.onStart();
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(output, StandardCharsets.UTF_8), 1048576)) {
            writer.write(ContentEntry.csvHeader());
            writer.newLine();

            long created = System.currentTimeMillis();

            List<Path> toProcess = new ArrayList<>();
            for (int i = 0; i < paths.length; i++) {
                Path p = paths[i];
                Objects.requireNonNull(p, "path is null at index " + i);
                if (!Files.isReadable(p)) {
                    if (callbacks != null) {
                        callbacks.onFileUnreadable(p);
                    }
                    continue;
                }
                p = p.toRealPath();
                if (p.getNameCount() == 0 && Files.isDirectory(p)) {
                    List<Path> children = Files.list(p).toList();
                    for (Path e : children) {
                        if (!Files.isReadable(e)) {
                            if (callbacks != null) {
                                callbacks.onFileUnreadable(e);
                            }
                            continue;
                        }
                        toProcess.add(e);
                    }
                    continue;
                }
                toProcess.add(p);
            }

            {
                Set<String> names = new HashSet<>();
                List<Path> otherToProcess = new ArrayList<>();

                for (Path p : toProcess) {
                    if (!names.add(p.getFileName().toString())) {
                        if (callbacks != null) {
                            callbacks.onFileDuplicated(p);
                        }
                        continue;
                    }
                    otherToProcess.add(p);
                }

                toProcess = otherToProcess;
            }

            List<Path> directories = new ArrayList<>();
            List<Path> files = new ArrayList<>();

            for (Path path : toProcess) {
                if (Files.isDirectory(path)) {
                    directories.add(path);
                } else {
                    files.add(path);
                }
            }

            sortByName(directories);
            sortByName(files);

            long size = 0;
            int fileCount = 0;
            int directoryCount = 0;

            for (Path d : directories) {
                ReadFileReturn r = readFile(d.getParent(), d, writer, callbacks);
                size += r.size;
                fileCount += r.files;
                directoryCount += r.directories;

                directoryCount++;
            }

            for (Path f : files) {
                ReadFileReturn r = readFile(f.getParent(), f, writer, callbacks);
                size += r.size;
                fileCount += r.files;
                directoryCount += r.directories;

                fileCount++;
            }

            ContentPath rootPath = ContentPath.of("/");
            if (callbacks != null) {
                callbacks.onEntryStart(rootPath);
                callbacks.onEntryProgressUpdate(0, 0);
            }
            ContentEntry rootEntry = new ContentEntry(
                    ContentPath.of("/"),
                    ContentType.DIRECTORY,
                    created, System.currentTimeMillis(), size,
                    fileCount, directoryCount,
                    null, null
            );
            if (callbacks != null) {
                callbacks.onEntryFinish(rootEntry);
            }
            writer.write(rootEntry.toCSVRecord());
        }

        if (callbacks != null) {
            callbacks.onFinish();
        }
    }

    private ContentList() {

    }

}
