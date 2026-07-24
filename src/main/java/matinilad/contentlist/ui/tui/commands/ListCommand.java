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
public class ListCommand extends Command {

    public ListCommand() {
        super("ls", "List the files in the current directory");
    }

    @Override
    public String execute(TUIState state, String input) throws CommandException {
        StringBuilder b = new StringBuilder();

        PhantomFileSystem fs = state.getFileSystem();

        PhantomPath[] files = fs.listFiles(state.getWorkingDirectory(), true);
        for (int i = 0; i < files.length; i++) {
            PhantomPath file = files[i];
            
            b.append(file.relative(state.getWorkingDirectory()).toString());
            if (!file.getName().equals(".")
                    && !file.getName().equals("..")
                    && fs.isDirectory(file)) {
                b.append("/.");
            }
            
            if (i != (files.length - 1)) {
                b.append(System.lineSeparator());
            }
        }

        return b.toString();
    }

}
