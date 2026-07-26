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

import java.io.PrintStream;
import matinilad.contentlist.ui.tui.Command;
import matinilad.contentlist.ui.tui.CommandException;

/**
 *
 * @author Cien
 */
public class LinesCommand extends Command {

    public LinesCommand() {
        super("lines");
    }

    @Override
    public boolean isDirectOutputEnabled() {
        return true;
    }
    
    @Override
    public String getHelpMessage() {
        return "Changes the number of lines per page";
    }

    @Override
    public String getDetailedHelpMessage() {
        return "Usage: lines [number]\n"+getHelpMessage()+"\nUse lines with no arguments to see the current number of lines";
    }

    @Override
    public String execute(String input) throws CommandException {
        PrintStream out = getDirectOutput();
        
        if (input == null || input.isEmpty()) {
            out.println(Integer.toString(getState().getLinesPerPage())+" Lines per page");
            return "";
        }
        
        int lines;
        try {
            lines = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            throw new CommandException("Unknown number: "+input, ex);
        }
        
        if (lines < 1) {
            throw new CommandException("Number of lines must be larger than 0");
        }
        
        getState().setLinesPerPage(lines);
        out.println("Success!");
        return "";
    }
    
}
