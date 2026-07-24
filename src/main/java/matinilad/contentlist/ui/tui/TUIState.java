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
import matinilad.contentlist.phantomfs.PhantomFileSystem;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.phantomfs.entry.FileEntry;

/**
 *
 * @author Cien
 */
public class TUIState {

    private PhantomFileSystem fileSystem = null;
    private PhantomPath workingDirectory = PhantomPath.of("/");

    public TUIState() {

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

    public FileEntry getEntry(PhantomPath path) throws CommandException {
        FileEntry entry = getFileSystem().getEntry(path);
        if (entry == null) {
            throw new CommandException("Entry not found" + System.lineSeparator() + path.toString());
        }
        return entry;
    }

}
