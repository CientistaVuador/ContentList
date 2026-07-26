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

import java.awt.Desktop;
import java.io.PrintStream;
import java.util.Objects;
import matinilad.contentlist.phantomfs.PhantomFileSystem;

/**
 *
 * @author Cien
 */
public abstract class Command {

    private Commands parent;
    private final String name;

    public Command(String name) {
        this.name = Objects.requireNonNull(name, "name is null");
    }
    
    public Commands getParent() {
        return parent;
    }

    public void setParent(Commands parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }
    
    public boolean isHidden() {
        return false;
    }
    
    public boolean isDirectOutputEnabled() {
        return false;
    }
    
    public abstract String getHelpMessage();

    public abstract String getDetailedHelpMessage();

    protected Commands getCommands() throws CommandException {
        Commands p = getParent();
        if (p == null) {
            throw new CommandException("Command has no parent");
        }
        return p;
    }

    protected TUIState getState() throws CommandException {
        TUIState state = getCommands().getParent();
        if (state == null) {
            throw new CommandException("Command has no state");
        }
        return state;
    }

    protected PhantomFileSystem getFileSystem() throws CommandException {
        PhantomFileSystem fileSystem = getState().getFileSystem();
        if (fileSystem == null) {
            throw new CommandException("Command has no file system");
        }
        return fileSystem;
    }
    
    protected Desktop getDesktop() throws CommandException {
        if (!Desktop.isDesktopSupported()) {
            throw new CommandException("Desktop is not supported");
        }
        return Desktop.getDesktop();
    }
    
    protected PrintStream getDirectOutput() throws CommandException {
        if (!isDirectOutputEnabled()) {
            throw new CommandException("Error: Command is trying to use direct output without enabling it");
        }
        PrintStream direct = getState().getDirectOutput();
        if (direct == null) {
            throw new CommandException("Direct output stream not found");
        }
        return direct;
    }

    public abstract String execute(String input) throws CommandException;
}
