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

import matinilad.contentlist.ui.tui.Command;
import matinilad.contentlist.ui.tui.CommandException;
import matinilad.contentlist.ui.tui.TUIState;

/**
 *
 * @author Cien
 */
public class PageCommand extends Command {

    public PageCommand() {
        super("page");
    }

    @Override
    public String getHelpMessage() {
        return "Displays a page of a command output";
    }

    @Override
    public String getDetailedHelpMessage() {
        return "Usage: page [index starting from 1]\n" + getHelpMessage() + "\nIf the output of a command is too long, it will be split into pages.\nUse page with no arguments to view the number of available pages";
    }

    @Override
    public String execute(String input) throws CommandException {
        TUIState state = getState();
        int pages = state.getNumberOfPages();

        if (input == null || input.isBlank()) {
            return pages + (pages == 1 ? " Page" : " Pages");
        }

        int pageIndex;
        try {
            pageIndex = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            throw new CommandException("Unknown number: " + input, ex);
        }
        
        String page = getState().getPage(pageIndex - 1);
        if (page == null) {
            throw new CommandException("Unknown page " + pageIndex + "\n"+pages+(pages == 1 ? " Page" : " Pages")+" available");
        }
        pageIndex--;
        
        String message = "";
        if (pages > 1) {
            message = "\n\nPage "+(pageIndex+1)+" of "+pages;
            if (pageIndex < (pages - 1)) {
                message += "\nSee the next page with page "+(pageIndex + 2);
            }
        }
        
        return page + message;
    }

}
