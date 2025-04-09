package matinilad.contentlist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import matinilad.contentlist.ContentListAccess.ContentListAccessCallbacks;

/**
 *
 * @author Cien
 */
public class ContentFileSystem {

    public static ContentFileSystem of(File file, ContentListAccessCallbacks callbacks) throws FileNotFoundException, IOException, InterruptedException {
        return new ContentFileSystem(new RandomAccessFile(file, "r"), callbacks);
    }

    public static ContentFileSystem of(File file) throws FileNotFoundException, IOException, InterruptedException {
        return of(file, null);
    }

    private static class ContentFile {

        ContentEntry entry = null;
        String name = null;
        boolean directory = false;
        ContentFile parent = null;
        final Map<String, ContentFile> children = new LinkedHashMap<>();
    }

    private final ContentFile root = new ContentFile();

    {
        root.parent = root;

        root.directory = true;
        root.name = "";
        root.children.put("..", root.parent);
        root.children.put(".", root);
    }

    private final ContentListAccess access;

    public ContentFileSystem(RandomAccessFile file, ContentListAccessCallbacks callbacks)
            throws FileNotFoundException, IOException, InterruptedException {
        Objects.requireNonNull(file, "file is null");
        this.access = new ContentListAccess(file, new ContentListAccessCallbacks() {
            @Override
            public void onStart() throws IOException, InterruptedException {
                if (callbacks != null) {
                    callbacks.onStart();
                }
            }

            @Override
            public void onReadProgressUpdate(long current, long total) throws IOException, InterruptedException {
                if (callbacks != null) {
                    callbacks.onReadProgressUpdate(current, total);
                }
            }

            private void entryRead(ContentEntry entry, int index) throws IOException {
                ContentPath path = entry.getPath();
                boolean directory = entry.getType().equals(ContentType.DIRECTORY);

                ContentFile currentDirectory = ContentFileSystem.this.root;
                for (int i = 0; i < path.getNumberOfObjects() - 1; i++) {
                    String directoryName = path.getObject(i);

                    ContentFile dir = currentDirectory.children.get(directoryName);
                    if (dir == null) {
                        dir = new ContentFile();

                        dir.entry = null;
                        dir.name = directoryName;
                        dir.directory = true;

                        dir.parent = currentDirectory;
                        currentDirectory.children.put(dir.name, dir);

                        dir.children.put("..", dir.parent);
                        dir.children.put(".", dir);
                    }

                    if (!dir.directory) {
                        return;
                    }

                    currentDirectory = dir;
                }

                ContentFile file = (path.isRoot()
                        ? ContentFileSystem.this.root
                        : currentDirectory.children.get(path.getName()));
                if (file == null) {
                    file = new ContentFile();

                    file.entry = entry;
                    file.name = path.getName();
                    file.directory = directory;

                    file.parent = currentDirectory;
                    currentDirectory.children.put(file.name, file);

                    if (file.directory) {
                        file.children.put("..", file.parent);
                        file.children.put(".", file);
                    }
                }

                if (file.entry == null && file.directory == directory) {
                    file.entry = entry;
                }
            }

            @Override
            public void onContentEntryRead(ContentEntry entry, int index) throws IOException, InterruptedException {
                entryRead(entry, index);
                if (callbacks != null) {
                    callbacks.onContentEntryRead(entry, index);
                }
            }

            @Override
            public void onFinish() throws IOException, InterruptedException {
                if (callbacks != null) {
                    callbacks.onFinish();
                }
            }
        });
    }

    public ContentListAccess getAccess() {
        return access;
    }

    private ContentFile resolve(ContentPath path) {
        Objects.requireNonNull(path, "path is null");
        if (path.isRelative()) {
            throw new IllegalArgumentException("path is relative");
        }

        ContentFile currentFile = this.root;
        for (int i = 0; i < path.getNumberOfObjects(); i++) {
            if (currentFile == null || !currentFile.directory) {
                return null;
            }
            currentFile = currentFile.children.get(path.getObject(i));
        }

        return currentFile;
    }

    private ContentPath realPath(ContentFile file) {
        if (file == this.root) {
            return ContentPath.of("/");
        }

        List<String> names = new ArrayList<>();
        ContentFile current = file;
        do {
            names.add(current.name);
        } while ((current = current.parent) != this.root);
        names = names.reversed();

        return ContentPath.of(names.toArray(String[]::new), false);
    }

    /**
     * Checks if the current path exists in the file system
     *
     * @param path The path to check, not null, not relative
     * @return true if it exists
     */
    public boolean exists(ContentPath path) {
        return resolve(path) != null;
    }

    /**
     * Checks if a path is a directory in the file system
     *
     * @param path The path to check, not null, not relative
     * @return true if it is a directory, false if it is a file or does not exists
     */
    public boolean isDirectory(ContentPath path) {
        ContentFile resolveType = resolve(path);
        if (resolveType == null) {
            return false;
        }
        return resolveType.directory;
    }

    /**
     * Checks if a path is a file in the file system
     *
     * @param path The path to check, not null, not relative
     * @return true if it a file, false if it is a directory or does not exists
     */
    public boolean isFile(ContentPath path) {
        return !isDirectory(path);
    }

    /**
     * Resolves all special links and returns the real path
     *
     * @param path The path to resolve, not null, not relative
     * @return the real path or null if it does not exists
     */
    public ContentPath toRealPath(ContentPath path) {
        ContentFile resolved = resolve(path);
        if (resolved == null) {
            return null;
        }

        return realPath(resolved);
    }

    /**
     * Lists all files and directories in the path
     *
     * @param path The path, not null, not relative
     * @param includeSpecialLinks If special links such as . and .. should be included
     * @return the files and directories in the path or null if it does not exists or is not a directory
     */
    public ContentPath[] listFiles(ContentPath path, boolean includeSpecialLinks) {
        ContentFile resolved = resolve(path);
        if (resolved == null || !resolved.directory) {
            return null;
        }

        ContentPath realPath = realPath(resolved);

        List<ContentPath> files = new ArrayList<>();
        for (String e : resolved.children.keySet()) {
            if ((e.equals(".") || e.equals("..")) && !includeSpecialLinks) {
                continue;
            }
            files.add(realPath.resolve(e));
        }

        return files.toArray(ContentPath[]::new);
    }

    private void listEntries(ContentPath path, List<ContentEntry> entries, Set<ContentPath> processed) {
        path = toRealPath(path);
        if (path == null || processed.contains(path)) {
            return;
        }
        processed.add(path);

        ContentEntry currentEntry = readEntry(path);
        if (currentEntry != null) {
            entries.add(currentEntry);
        }
        if (isDirectory(path)) {
            ContentPath[] children = listFiles(path, false);
            for (ContentPath c : children) {
                listEntries(c, entries, processed);
            }
        }
    }

    public ContentEntry[] listEntries(ContentPath[] paths) {
        Objects.requireNonNull(paths, "paths is null");
        List<ContentEntry> entries = new ArrayList<>();
        Set<ContentPath> processed = new HashSet<>();
        for (ContentPath p : paths) {
            listEntries(p, entries, processed);
        }
        return entries.toArray(ContentEntry[]::new);
    }

    private void search(
            List<ContentPath> output,
            ContentFile f, String name, boolean caseSensitive, boolean exactName,
            int depth
    ) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        if (depth != 0) {
            String filename = (f.name == null ? "" : f.name);
            if (!caseSensitive) {
                filename = filename.toLowerCase();
            }
            if ((exactName && filename.equals(name)) || (!exactName && filename.contains(name))) {
                output.add(realPath(f));
            }
        }

        if (f.directory) {
            for (Map.Entry<String, ContentFile> e : f.children.entrySet()) {
                if ((e.getKey().equals(".") || e.getKey().equals(".."))) {
                    continue;
                }
                search(output, e.getValue(), name, caseSensitive, exactName, depth + 1);
            }
        }
    }

    private static void sortByName(List<ContentPath> list) {
        Comparator<ContentPath> comparator = (o1, o2) -> {
            String n1 = o1.getName();
            if (n1 == null) {
                n1 = "";
            }
            String n2 = o2.getName();
            if (n2 == null) {
                n2 = "";
            }
            return String.CASE_INSENSITIVE_ORDER.compare(n1, n2);
        };
        list.sort(comparator);
    }

    /**
     * Searches in a directory for all files and directories containing a name
     *
     * @param path The directory to search in, not null, not relative
     * @param name The name to search for, not null
     * @param caseSensitive If the name is case sensitive
     * @param exactName If the file name should be the exact name
     * @return the files containing the name or null if the path does not exists or is not a directory
     */
    public ContentPath[] search(ContentPath path, String name, boolean caseSensitive, boolean exactName, boolean sort)
            throws InterruptedException {
        Objects.requireNonNull(name, "name is null");
        ContentFile resolved = resolve(path);
        if (resolved == null || !resolved.directory) {
            return null;
        }
        if (!caseSensitive) {
            name = name.toLowerCase();
        }
        List<ContentPath> files = new ArrayList<>();
        search(files, resolved, name, caseSensitive, exactName, 0);
        if (sort) {
            List<ContentPath> directoriesList = new ArrayList<>();
            List<ContentPath> filesList = new ArrayList<>();
            for (ContentPath p : files) {
                if (isDirectory(p)) {
                    directoriesList.add(p);
                } else {
                    filesList.add(p);
                }
            }
            
            sortByName(directoriesList);
            sortByName(filesList);
            
            files.clear();
            files.addAll(directoriesList);
            files.addAll(filesList);
        }
        return files.toArray(ContentPath[]::new);
    }

    public ContentEntry readEntry(ContentPath path) {
        ContentFile resolved = resolve(path);
        if (resolved == null) {
            return null;
        }
        return resolved.entry;
    }
}
