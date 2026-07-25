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

/**
 *
 * @author Cien
 */
public class HelpCommand extends Command {

    public HelpCommand() {
        super("help");
    }

    @Override
    public String getHelpMessage() {
        return "Shows information about commands";
    }

    @Override
    public String getDetailedHelpMessage() {
        return "Usage: help [command]\n"+getHelpMessage()+"\nLeave [command] empty for a list of commands";
    }
    
    @Override
    public String execute(String input) throws CommandException {
        if (input == null || input.isBlank()) {
            StringBuilder b = new StringBuilder();
            
            Command[] commands = getCommands().getCommands(false);
            
            b.append("Type help [command] to display information about a command.").append(System.lineSeparator());
            b.append(commands.length).append(" Commands available:").append(System.lineSeparator()).append(System.lineSeparator());
            
            for (int i = 0; i < commands.length; i++) {
                Command c = commands[i];
                b.append(c.getName()).append(" - ").append(c.getHelpMessage());
                if (i != (commands.length - 1)) {
                    b.append(System.lineSeparator());
                }
            }
            
            return b.toString();
        }
        
        Command c = getCommands().getCommand(input.split(" ")[0].trim());
        if (c == null) {
            throw new CommandException("Unknown command: "+input);
        }
        return c.getDetailedHelpMessage();
    }
    
}
