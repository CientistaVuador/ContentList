package matinilad.contentlist.phantomfs;

import java.io.BufferedOutputStream;
import matinilad.contentlist.phantomfs.entry.FileEntryType;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import matinilad.contentlist.phantomfs.entry.FileEntryCreator;
import matinilad.contentlist.phantomfs.entry.FileEntryWriter;

/**
 *
 * @author Cien
 */
public abstract class PhantomCreator {

    @Deprecated
    public static interface ContentListCallbacks {

        public void onStart() throws IOException, InterruptedException;

        public void onFileUnreadable(Path path) throws IOException, InterruptedException;

        public void onFileDuplicated(Path path) throws IOException, InterruptedException;

        public void onEntryStart(PhantomPath path) throws IOException, InterruptedException;

        public void onEntryFinish(FileEntry entry) throws IOException, InterruptedException;

        public void onEntryProgressUpdate(long current, long total) throws IOException, InterruptedException;

        public void onFinish() throws IOException;

    }

    @Deprecated
    public static void create(
            OutputStream output, ContentListCallbacks callbacks, Path... paths
    ) throws IOException, InterruptedException {
        if (callbacks != null) {
            callbacks.onStart();
        }
        
        try (FileEntryWriter writer = new FileEntryWriter(new OutputStreamWriter(new BufferedOutputStream(output), StandardCharsets.UTF_8), 0)) {
            PhantomCreator creator = new PhantomCreator() {
                @Override
                protected void onFileRejected(Path file, IOException reason) throws IOException, InterruptedException {
                    if (callbacks != null) {
                        if ("file is duplicated".equals(reason.getMessage())) {
                            callbacks.onFileDuplicated(file);
                        } else {
                            callbacks.onFileUnreadable(file);
                        }
                    }
                }
                
                @Override
                protected void onEntry(FileEntry entry) throws IOException, InterruptedException {
                    if (callbacks != null) {
                        callbacks.onEntryFinish(entry);
                    }
                    writer.writeFileEntry(entry);
                }
            };
            creator.setFileEntryCreatorFactory(() -> new FileEntryCreator() {
                @Override
                protected void onEntryCreated(FileEntry entry) throws IOException, InterruptedException {
                    if (callbacks != null) {
                        callbacks.onEntryStart(entry.getPath());
                        callbacks.onEntryProgressUpdate(0, 0);
                    }
                }
                
                @Override
                protected void onEntryProgress(FileEntry entry, long bytes) throws IOException, InterruptedException {
                    if (callbacks != null) {
                        callbacks.onEntryProgressUpdate(bytes, entry.getSize());
                    }
                }
            });
            creator.create(paths);
        }
        
        if (callbacks != null) {
            callbacks.onFinish();
        }
    }
    
    private FileEntryCreator.Factory fileEntryCreatorFactory = FileEntryCreator::new;

    public PhantomCreator() {

    }

    public FileEntryCreator.Factory getFileEntryCreatorFactory() {
        return fileEntryCreatorFactory;
    }

    public void setFileEntryCreatorFactory(FileEntryCreator.Factory fileEntryCreatorFactory) {
        if (fileEntryCreatorFactory == null) {
            fileEntryCreatorFactory = FileEntryCreator::new;
        }
        this.fileEntryCreatorFactory = fileEntryCreatorFactory;
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

            FileEntryCreator creator = getFileEntryCreatorFactory().newFileEntryCreator();
            FileEntry entry = creator.create(file, depth);

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
                onFileRejected(p, new IOException("file is root"));
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
        
        FileEntry root = getFileEntryCreatorFactory().newFileEntryCreator().create(null, 0);
        root.setCreated(System.currentTimeMillis());
        for (Path p : fileList) {
            createRecursively(root, p, 0);
        }
        root.setModified(System.currentTimeMillis());
        onEntry(root);
    }

}
