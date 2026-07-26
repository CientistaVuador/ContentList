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
package matinilad.contentlist.ui.tui;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import matinilad.contentlist.phantomfs.PhantomFileSystem;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.phantomfs.entry.FileEntry;

/**
 *
 * @author Cien
 */
public class TUIState {

    private Commands commands = null;

    private PhantomFileSystem fileSystem = null;
    private PhantomPath workingDirectory = PhantomPath.of("/");
    
    private int linesPerPage = 25;
    private String[] lines = null;
    
    private Path rootDirectory = null;
    private PrintStream directOutput = null;
    
    public TUIState() {

    }

    public Commands getCommands() {
        return commands;
    }
    
    public boolean setCommands(Commands commands) {
        if (commands == null) {
            if (this.commands != null) {
                this.commands.setParent(null);
                this.commands = null;
                return true;
            }
            return false;
        }
        if (commands.getParent() != null) {
            return false;
        }
        if (this.commands != null) {
            this.commands.setParent(null);
        }
        this.commands = commands;
        commands.setParent(this);
        return true;
    }

    public PhantomFileSystem getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(PhantomFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public PhantomPath getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(PhantomPath workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public PhantomPath parsePath(String input) throws CommandException {
        PhantomPath path;
        try {
            path = PhantomPath.of(input);
        } catch (IllegalArgumentException ex) {
            throw new CommandException(input + " is not a valid path!", ex);
        }
        return path;
    }
    
    public PhantomPath resolveToWorkingDirectory(PhantomPath path) throws CommandException {
        if (path.isRelative()) {
            path = getWorkingDirectory().resolve(path);
        }
        return path;
    }
    
    public PhantomPath resolveToWorkingDirectory(String path) throws CommandException {
        return resolveToWorkingDirectory(parsePath(path));
    }
    
    public PhantomPath resolveToWorkingDirectoryChecked(String path) throws CommandException {
        PhantomPath p = resolveToWorkingDirectory(path);
        if (!getFileSystem().exists(p)) {
            throw new CommandException(path + " does not exists!");
        }
        return p;
    }

    public FileEntry getEntry(PhantomPath path) throws CommandException {
        FileEntry entry = getFileSystem().getEntry(path);
        if (entry == null) {
            throw new CommandException("Entry not found: " + path.toString());
        }
        return entry;
    }

    public int getLinesPerPage() {
        return linesPerPage;
    }

    public void setLinesPerPage(int linesPerPage) {
        if (linesPerPage < 1) {
            throw new IllegalArgumentException("linesPerPage < 1");
        }
        this.linesPerPage = linesPerPage;
    }
    
    public void setCommandOutput(String text) {
        if (text == null) {
            this.lines = null;
            return;
        }
        this.lines = text.lines().toArray(String[]::new);
    }
    
    public int getNumberOfPages() {
        if (this.lines == null) {
            return 0;
        }
        return (this.lines.length / this.linesPerPage) + ((this.lines.length % this.linesPerPage) != 0 ? 1 : 0);
    }
    
    public String getPage(int index) {
        if (this.lines == null || this.lines.length == 0 || index < 0) {
            return null;
        }
        
        int startLine = index * this.linesPerPage;
        int endLine = Math.min((index * this.linesPerPage) + this.linesPerPage, this.lines.length);
        
        if (startLine >= this.lines.length) {
            return null;
        }
        
        StringBuilder b = new StringBuilder();
        
        for (int i = startLine; i < endLine; i++) {
            b.append(this.lines[i]);
            if (i != (endLine - 1)) {
                b.append(System.lineSeparator());
            }
        }
        
        return b.toString();
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }
    
    public Path getRootDirectoryChecked() throws CommandException {
        Path root = getRootDirectory();
        if (root == null) {
            throw new CommandException("Root directory not set! set with root [directory]");
        }
        if (!Files.isDirectory(root)) {
            throw new CommandException("Root directory is not a valid directory! "+root.toString());
        }
        return root;
    }
    
    public Path resolveToRootDirectory(PhantomPath path) throws CommandException {
        return path.resolveToPath(getRootDirectoryChecked());
    }
    
    public Path resolveToRootDirectoryChecked(PhantomPath path) throws CommandException {
        Path resolved = resolveToRootDirectory(path);
        if (!Files.exists(resolved)) {
            throw new CommandException("File does not exists: "+resolved.toString());
        }
        return resolved;
    }

    public PrintStream getDirectOutput() {
        return directOutput;
    }

    public void setDirectOutput(PrintStream directOutput) {
        this.directOutput = directOutput;
    }
    
}
