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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Cien
 */
public class Commands {
    
    private final Map<String, Command> commands = new LinkedHashMap<>();
    
    public Commands() {
        
    }
    
    public boolean addCommand(Command command) {
        if (command == null || command.getParent() != null) {
            return false;
        }
        
        String commandName = command.getName().toLowerCase();
        if (this.commands.containsKey(commandName)) {
            return false;
        }
        
        this.commands.put(commandName, command);
        command.setParent(this);
        
        return true;
    }
    
    public boolean removeCommand(Command command) {
        if (command == null || command.getParent() != this) {
            return false;
        }
        
        if (this.commands.remove(command.getName().toLowerCase(), command)) {
            command.setParent(null);
            return true;
        }
        return false;
    }
    
    public Command[] getCommands() {
        return this.commands.values().toArray(Command[]::new);
    }
    
    public Command getCommand(String name) {
        if (name == null) {
            return null;
        }
        return this.commands.get(name.toLowerCase());
    }
    
}
