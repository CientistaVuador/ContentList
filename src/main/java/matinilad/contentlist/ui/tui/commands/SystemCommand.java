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
package matinilad.contentlist.ui.tui.commands;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.ui.tui.Command;
import matinilad.contentlist.ui.tui.CommandException;
import matinilad.contentlist.ui.tui.TUIState;

/**
 *
 * @author Cien
 */
public abstract class SystemCommand extends Command {

    public static class Exists extends SystemCommand {

        public Exists() {
            super("sys:exists");
        }

        @Override
        public String getHelpMessage() {
            return "Checks if a file from the system exists";
        }

        @Override
        public String getDetailedHelpMessage() {
            return "Usage: sys:exists [file]\n" + getHelpMessage();
        }

        @Override
        protected String run(PhantomPath path, Path file) throws CommandException, IOException {
            if (Files.exists(file)) {
                return "Exists!";
            } else {
                return "Does not exists!";
            }
        }
    }

    public static class Open extends SystemCommand {

        public Open() {
            super("sys:open");
        }

        @Override
        public String getHelpMessage() {
            return "Opens a file from the system";
        }

        @Override
        public String getDetailedHelpMessage() {
            return "Usage: sys:open [file]\n" + getHelpMessage();
        }

        @Override
        protected String run(PhantomPath path, Path file) throws CommandException, IOException {
            if (!Files.exists(file)) {
                throw new CommandException("Does not exists: " + file.toString());
            }
            getDesktop().open(file.toFile());
            return "";
        }
    }

    public static class OpenDirectory extends SystemCommand {

        public OpenDirectory() {
            super("sys:openDir");
        }

        @Override
        public String getHelpMessage() {
            return "Opens a directory from the system";
        }

        @Override
        public String getDetailedHelpMessage() {
            return "Usage: sys:openDir [file]\n" + getHelpMessage() + "\nIf file is a file, the parent directory will open instead";
        }

        @Override
        protected String run(PhantomPath path, Path file) throws CommandException, IOException {
            if (!Files.exists(file)) {
                throw new CommandException("Does not exists: " + file.toString());
            }
            if (Files.isDirectory(file)) {
                getDesktop().open(file.toFile());
                return "";
            }
            getDesktop().open(file.toRealPath().getParent().toFile());
            return "";
        }
    }

    public static class Copy extends SystemCommand {

        public static class FileTransferable implements Transferable {

            private final List<File> listOfFiles;

            public FileTransferable(List<File> listOfFiles) {
                this.listOfFiles = listOfFiles;
            }

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{DataFlavor.javaFileListFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.javaFileListFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                return listOfFiles;
            }
        }

        public Copy() {
            super("sys:copy");
        }

        @Override
        public String getHelpMessage() {
            return "Copies a file from the system";
        }

        @Override
        public String getDetailedHelpMessage() {
            return "Usage: sys:copy [file]\n" + getHelpMessage() + "\n(This is the same as pressing CTRL+C on the file)";
        }

        @Override
        protected String run(PhantomPath path, Path file) throws CommandException, IOException {
            if (!Files.exists(file)) {
                throw new CommandException("Does not exists: " + file.toString());
            }
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new FileTransferable(Arrays.asList(new File[] {file.toFile()})),
                    (Clipboard clipboard, Transferable contents) -> {}
            );
            return "";
        }
    }
    
    public static class Trash extends SystemCommand {
        public Trash() {
            super("sys:trash");
        }

        @Override
        public String getHelpMessage() {
            return "Moves a file to the system trash can";
        }

        @Override
        public String getDetailedHelpMessage() {
            return "Usage: sys:trash [file]\n"+getHelpMessage();
        }

        @Override
        protected String run(PhantomPath path, Path file) throws CommandException, IOException {
            if (!Files.exists(file)) {
                throw new CommandException("Does not exists: " + file.toString());
            }
            if (getDesktop().moveToTrash(file.toFile())) {
                return "Success!";
            } else {
                return "Failed!";
            }
        }
    }

    public SystemCommand(String name) {
        super(name);
    }

    protected abstract String run(PhantomPath path, Path file) throws CommandException, IOException;

    @Override
    public String execute(String input) throws CommandException {
        if (input == null || input.isEmpty()) {
            throw new CommandException("Usage: " + getName() + " [file]");
        }
        TUIState state = getState();
        PhantomPath path = state.resolveToWorkingDirectoryChecked(input);
        Path file = state.resolveToRootDirectory(path);
        try {
            return file.toString() + "\n" + run(path, file);
        } catch (IOException ex) {
            throw new CommandException("Failed to process " + file.toString(), ex);
        }
    }

}
