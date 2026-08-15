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
package matinilad.contentlist.phantomfs.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Cien
 */
public class TempFileList {
    
    public static boolean tryCreateDirectories(Path directory) {
        TempFileList temp = new TempFileList();
        try {
            temp.createDirectories(directory);
            if (!Files.isDirectory(directory)) {
                throw new IOException();
            }
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            temp.deleteFiles();
        }
    }
    
    private final List<Path> files = new ArrayList<>();
    
    public TempFileList() {
        
    }
    
    public void addFile(Path file) {
        Objects.requireNonNull(file, "file is null");
        this.files.add(file);
    }
    
    public void createDirectories(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory is null");
        
        Path root = directory.getRoot();
        for (int i = 0; i < directory.getNameCount(); i++) {
            Path sub = directory.subpath(0, i + 1);
            if (root != null) {
                sub = root.resolve(sub);
            }
            if (Files.exists(sub)) {
                if (!Files.isDirectory(sub)) {
                    throw new IOException("not a directory: "+sub);
                }
            } else {
                Files.createDirectory(sub);
                addFile(sub);
            }
        }
    }
    
    public OutputStream newOutputStream(Path file) throws IOException {
        Objects.requireNonNull(file, "file is null");
        
        Path parent = file.getParent();
        if (parent != null) {
            createDirectories(parent);
        }
        OutputStream out = Files.newOutputStream(file);
        addFile(file);
        return out;
    }
    
    public void clearList() {
        this.files.clear();
    }
    
    public void deleteFiles() {
        for (int i = this.files.size() - 1; i >= 0; i--) {
            Path p = this.files.get(i);
            try {
                Files.delete(p);
            } catch (IOException ex) {
                //ignore
            }
        }
        clearList();
    }
}
