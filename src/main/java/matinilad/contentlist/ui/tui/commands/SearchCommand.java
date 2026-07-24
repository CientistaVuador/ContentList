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

import matinilad.contentlist.phantomfs.PhantomFileSystem;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.ui.tui.Command;
import matinilad.contentlist.ui.tui.CommandException;
import matinilad.contentlist.ui.tui.TUIState;

/**
 *
 * @author Cien
 */
public class SearchCommand extends Command {
    
    public static class CaseSensitive extends SearchCommand {

        public CaseSensitive() {
            super("csearch");
        }

        @Override
        public String execute(TUIState state, String input) throws CommandException {
            return super.execute(state, input, true, false);
        }
    }
    
    public static class Exact extends SearchCommand {
        public Exact() {
            super("esearch");
        }
        
        @Override
        public String execute(TUIState state, String input) throws CommandException {
            return super.execute(state, input, false, true);
        }
    }
    
    public static class ExactCaseSensitive extends SearchCommand {
        public ExactCaseSensitive() {
            super("ecsearch");
        }
        
        @Override
        public String execute(TUIState state, String input) throws CommandException {
            return super.execute(state, input, true, true);
        }
    }
    
    protected SearchCommand(String name) {
        super(name, "Searches for a file in the current directory and subdirectories.");
    }
    
    public SearchCommand() {
        super("search", "Searches for a file in the current directory and subdirectories.");
    }
    
    @Override
    public String execute(TUIState state, String input) throws CommandException {
        return execute(state, input, false, false);
    }
    
    protected String execute(TUIState state, String input, boolean caseSensitive, boolean exact) throws CommandException {
        if (input == null) {
            throw new CommandException("Usage: " + getName() + " [name]");
        }
        
        PhantomFileSystem fs = state.getFileSystem();
        
        PhantomPath[] p;
        try {
            p = fs.search(state.getWorkingDirectory(), input, caseSensitive, exact, true);
        } catch (InterruptedException ex) {
            throw new CommandException(ex);
        }
        
        if (p.length == 0) {
            return "No files found for '" + input + "'!";
        }
        
        StringBuilder b = new StringBuilder();
        
        for (PhantomPath e : p) {
            b.append(e.relative(state.getWorkingDirectory()));
            if (fs.isDirectory(e)) {
                b.append("/.");
            }
            b.append(System.lineSeparator());
        }
        b.append(p.length)
                .append(" ")
                .append(p.length == 1 ? "File" : "Files")
                .append(" found for '")
                .append(input)
                .append("'!")
                .append(System.lineSeparator());
        
        return b.toString();
    }

}
