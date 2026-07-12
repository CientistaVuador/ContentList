package matinilad.contentlist.phantomfs;

import matinilad.contentlist.phantomfs.entry.FileEntryType;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import matinilad.contentlist.phantomfs.entry.FileEntryCreator;

/**
 *
 * @author Cien
 */
public abstract class PhantomCreator {

    private FileEntryCreator fileEntryCreator = new FileEntryCreator();

    public PhantomCreator() {

    }

    public FileEntryCreator getFileEntryCreator() {
        return fileEntryCreator;
    }

    public void setFileEntryCreator(FileEntryCreator fileEntryCreator) {
        if (fileEntryCreator == null) {
            fileEntryCreator = new FileEntryCreator();
        }
        this.fileEntryCreator = fileEntryCreator;
    }

    protected boolean onShouldInterrupt() throws IOException, InterruptedException {
        return Thread.interrupted();
    }

    protected void onShouldFileBeRejected(Path file) throws IOException, InterruptedException {

    }

    protected void onFileRejected(Path file, IOException reason) throws IOException, InterruptedException {

    }

    protected abstract void onEntry(FileEntry entry) throws IOException, InterruptedException;

    private void checkInterrupt() throws IOException, InterruptedException {
        if (onShouldInterrupt()) {
            throw new InterruptedException();
        }
    }

    private void nameSort(List<Path> list) {
        Comparator<Path> comparator = (o1, o2)
                -> String.CASE_INSENSITIVE_ORDER
                        .compare(o1.getFileName().toString(), o2.getFileName().toString());
        list.sort(comparator);
    }

    private List<Path> sort(List<Path> toProcess) {
        List<Path> directories = new ArrayList<>();
        List<Path> files = new ArrayList<>();

        for (Path path : toProcess) {
            if (Files.isDirectory(path)) {
                directories.add(path);
            } else {
                files.add(path);
            }
        }

        nameSort(directories);
        nameSort(files);

        List<Path> sorted = new ArrayList<>();
        sorted.addAll(directories);
        sorted.addAll(files);
        return sorted;
    }

    private void createRecursively(FileEntry parent, Path file, int depth) throws IOException, InterruptedException {
        try {
            checkInterrupt();

            if (!Files.isReadable(file)) {
                throw new IOException("file is unreadable");
            }

            onShouldFileBeRejected(file);

            FileEntry entry = getFileEntryCreator().create(file, depth);

            if (entry.getType().equals(FileEntryType.DIRECTORY)) {
                parent.setDirectories(parent.getDirectories() + 1);
            } else {
                parent.setFiles(parent.getFiles() + 1);
            }

            if (entry.getType().equals(FileEntryType.DIRECTORY)) {
                List<Path> children = sort(Files.list(file).toList());
                for (Path child : children) {
                    createRecursively(entry, child, depth - 1);
                }

                parent.setDirectories(parent.getDirectories() + entry.getDirectories());
                parent.setFiles(parent.getFiles() + entry.getFiles());
            }

            parent.setSize(parent.getSize() + entry.getSize());

            onEntry(entry);
        } catch (IOException ex) {
            onFileRejected(file, ex);
        }
    }

    private List<Path> validateAndSort(Path[] files) throws InterruptedException, IOException {
        List<Path> fileList = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Path p : files) {
            try {
                p = p.toRealPath();
            } catch (IOException ex) {
                onFileRejected(p, ex);
                continue;
            }

            Path fileName = p.getFileName();
            if (fileName == null) {
                if (!Files.isDirectory(p)) {
                    onFileRejected(p, new IOException("file is root and it's not a directory!"));
                    continue;
                }
                for (Path e : Files.list(p).toList()) {
                    Path name = e.getFileName();
                    if (!names.add(name.toString())) {
                        onFileRejected(e, new IOException("file is duplicated"));
                        continue;
                    }
                    fileList.add(e);
                }
                continue;
            }
            if (!names.add(fileName.toString())) {
                onFileRejected(p, new IOException("file is duplicated"));
                continue;
            }

            fileList.add(p);
        }
        return sort(fileList);
    }

    public void create(Path... files) throws IOException, InterruptedException {
        List<Path> fileList = validateAndSort(files);

        FileEntry root = getFileEntryCreator().create(null, 0);
        root.setCreated(System.currentTimeMillis());
        for (Path p : fileList) {
            createRecursively(root, p, 0);
        }
        root.setModified(System.currentTimeMillis());
        onEntry(root);
    }

}
