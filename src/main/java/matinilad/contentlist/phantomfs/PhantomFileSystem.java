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
package matinilad.contentlist.phantomfs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.entry.FileEntryReader;
import matinilad.contentlist.phantomfs.entry.FileEntryType;

/**
 *
 * @author Cien
 */
public class PhantomFileSystem {

    @Deprecated
    public static PhantomFileSystem of(File file, FileEntryReader.ContentListAccessCallbacks callbacks) throws FileNotFoundException, IOException, InterruptedException {
        return new PhantomFileSystem(new RandomAccessFile(file, "r"), callbacks);
    }

    @Deprecated
    public static PhantomFileSystem of(File file) throws FileNotFoundException, IOException, InterruptedException {
        return of(file, null);
    }
    
    private static class InternalFile {

        FileEntry entry = null;
        String name = null;
        boolean directory = false;
        InternalFile parent = null;
        final Map<String, InternalFile> children = new LinkedHashMap<>();
    }

    private final InternalFile root = new InternalFile();

    {
        root.parent = root;

        root.directory = true;
        root.name = "";
        root.children.put("..", root.parent);
        root.children.put(".", root);
    }
    
    @Deprecated
    private FileEntryReader access;
    
    @Deprecated
    public PhantomFileSystem(RandomAccessFile file, FileEntryReader.ContentListAccessCallbacks callbacks) throws FileNotFoundException, IOException, InterruptedException {
        Objects.requireNonNull(file, "file is null");
        this.access = new FileEntryReader(file, new FileEntryReader.ContentListAccessCallbacks() {
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

            private void entryRead(FileEntry entry, int index) throws IOException {
                writeEntryImpl(entry);
            }
            
            @Override
            public void onContentEntryRead(FileEntry entry, int index) throws IOException, InterruptedException {
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
    
    public PhantomFileSystem() {

    }

    private void writeEntryImpl(FileEntry entry) {
        PhantomPath path = entry.getPath();
        boolean directory = entry.getType().equals(FileEntryType.DIRECTORY);
        
        InternalFile currentDirectory = this.root;
        for (int i = 0; i < path.getNumberOfObjects() - 1; i++) {
            String directoryName = path.getObject(i);
            
            InternalFile dir = currentDirectory.children.get(directoryName);
            if (dir == null) {
                dir = new InternalFile();

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

        InternalFile file = (path.isRoot() ? this.root : currentDirectory.children.get(path.getName()));
        if (file == null) {
            file = new InternalFile();
            
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
    
    public void writeEntry(FileEntry entry) {
        writeEntryImpl(entry);
    }
    
    private void validateFile(InternalFile file) {
        if (!file.directory) {
            if (file.entry == null) {
                file.entry = new FileEntry(realPath(file), FileEntryType.FILE);
            }
            file.entry.setFiles(0);
            file.entry.setDirectories(0);
            return;
        }
        
        FileEntry dirEntry = file.entry;
        if (dirEntry == null) {
            dirEntry = new FileEntry(realPath(file), FileEntryType.DIRECTORY);
            file.entry = dirEntry;
        }
        
        long size = 0;
        int files = 0;
        int directories = 0;
        
        for (Entry<String, InternalFile> entry:file.children.entrySet()) {
            if (entry.getKey().equals(".") || entry.getKey().equals("..")) {
                continue;
            }
            
            InternalFile other = entry.getValue();
            validateFile(other);
            
            size += other.entry.getSize();
            if (other.directory) {
                directories++;
                
                files += other.entry.getFiles();
                directories += other.entry.getDirectories();
            } else {
                files++;
            }
        }
        
        dirEntry.setSize(size);
        dirEntry.setFiles(files);
        dirEntry.setDirectories(directories);
    }
    
    public void validate() {
        validateFile(this.root);
    }
    
    private InternalFile resolve(PhantomPath path) {
        Objects.requireNonNull(path, "path is null");
        if (path.isRelative()) {
            throw new IllegalArgumentException("path is relative");
        }

        InternalFile currentFile = this.root;
        for (int i = 0; i < path.getNumberOfObjects(); i++) {
            if (currentFile == null || !currentFile.directory) {
                return null;
            }
            currentFile = currentFile.children.get(path.getObject(i));
        }
        
        return currentFile;
    }

    private PhantomPath realPath(InternalFile file) {
        if (file == this.root) {
            return PhantomPath.of("/");
        }
        
        List<String> names = new ArrayList<>();
        InternalFile current = file;
        do {
            names.add(current.name);
        } while ((current = current.parent) != this.root);
        Collections.reverse(names);
        
        return PhantomPath.of(names.toArray(String[]::new), false);
    }

    /**
     * Checks if the current path exists in the file system
     *
     * @param path The path to check, not null, not relative
     * @return true if it exists
     */
    public boolean exists(PhantomPath path) {
        return resolve(path) != null;
    }
    
    /**
     * Checks if a path is a directory in the file system
     *
     * @param path The path to check, not null, not relative
     * @return true if it is a directory, false if it is a file or does not exists
     */
    public boolean isDirectory(PhantomPath path) {
        InternalFile resolveType = resolve(path);
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
    public boolean isFile(PhantomPath path) {
        return !isDirectory(path);
    }

    /**
     * Resolves all special links and returns the real path
     *
     * @param path The path to resolve, not null, not relative
     * @return the real path or null if it does not exists
     */
    public PhantomPath toRealPath(PhantomPath path) {
        InternalFile resolved = resolve(path);
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
    public PhantomPath[] listFiles(PhantomPath path, boolean includeSpecialLinks) {
        InternalFile resolved = resolve(path);
        if (resolved == null || !resolved.directory) {
            return null;
        }
        
        PhantomPath realPath = realPath(resolved);

        List<PhantomPath> files = new ArrayList<>();
        for (String e : resolved.children.keySet()) {
            if ((e.equals(".") || e.equals("..")) && !includeSpecialLinks) {
                continue;
            }
            files.add(realPath.resolve(e));
        }

        return files.toArray(PhantomPath[]::new);
    }
    
    public PhantomPath[] listFiles(PhantomPath path) {
        return listFiles(path, false);
    }

    private void listEntries(PhantomPath path, List<FileEntry> entries, Set<PhantomPath> processed) {
        path = toRealPath(path);
        if (path == null || processed.contains(path)) {
            return;
        }
        processed.add(path);

        FileEntry currentEntry = getEntry(path);
        if (currentEntry != null) {
            entries.add(currentEntry);
        }
        if (isDirectory(path)) {
            PhantomPath[] children = listFiles(path, false);
            for (PhantomPath c : children) {
                listEntries(c, entries, processed);
            }
        }
    }

    public FileEntry[] listEntries(PhantomPath[] paths) {
        Objects.requireNonNull(paths, "paths is null");
        List<FileEntry> entries = new ArrayList<>();
        Set<PhantomPath> processed = new HashSet<>();
        for (PhantomPath p : paths) {
            listEntries(p, entries, processed);
        }
        return entries.toArray(FileEntry[]::new);
    }

    private void search(
            List<PhantomPath> output,
            InternalFile f, String name, boolean caseSensitive, boolean exactName,
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
            for (Map.Entry<String, InternalFile> e : f.children.entrySet()) {
                if ((e.getKey().equals(".") || e.getKey().equals(".."))) {
                    continue;
                }
                search(output, e.getValue(), name, caseSensitive, exactName, depth + 1);
            }
        }
    }

    private static void sortByName(List<PhantomPath> list) {
        Comparator<PhantomPath> comparator = (o1, o2) -> {
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
    public PhantomPath[] search(PhantomPath path, String name, boolean caseSensitive, boolean exactName, boolean sort)
            throws InterruptedException {
        Objects.requireNonNull(name, "name is null");
        InternalFile resolved = resolve(path);
        if (resolved == null || !resolved.directory) {
            return null;
        }
        if (!caseSensitive) {
            name = name.toLowerCase();
        }
        List<PhantomPath> files = new ArrayList<>();
        search(files, resolved, name, caseSensitive, exactName, 0);
        if (sort) {
            List<PhantomPath> directoriesList = new ArrayList<>();
            List<PhantomPath> filesList = new ArrayList<>();
            for (PhantomPath p : files) {
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
        return files.toArray(PhantomPath[]::new);
    }

    public FileEntry getEntry(PhantomPath path) {
        InternalFile resolved = resolve(path);
        if (resolved == null) {
            return null;
        }
        return resolved.entry;
    }
    
    @Deprecated
    public FileEntryReader getAccess() {
        return access;
    }
    
}
