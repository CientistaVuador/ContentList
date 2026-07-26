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

import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.entry.FileEntryMetadata;
import matinilad.contentlist.ui.tui.Command;
import matinilad.contentlist.ui.tui.CommandException;

/**
 *
 * @author Cien
 */
public abstract class InfoCommand extends Command {
    
    public static class Name extends InfoCommand {

        public Name() {
            super("name");
        }
        
        @Override
        public String getHelpMessage() {
            return "Displays the name of the list";
        }

        @Override
        public String getDetailedHelpMessage() {
            return "Usage: name\n"+getHelpMessage();
        }

        @Override
        public String execute(String input) throws CommandException {
            return display("name");
        }
        
    }
    
    public static class Author extends InfoCommand {

        public Author() {
            super("author");
        }
        
        @Override
        public String getHelpMessage() {
            return "Displays the author of the list";
        }

        @Override
        public String getDetailedHelpMessage() {
            return "Usage: author\n"+getHelpMessage();
        }

        @Override
        public String execute(String input) throws CommandException {
            return display("author");
        }
    }
    
    public static class Description extends InfoCommand {
        public Description() {
            super("desc");
        }

        @Override
        public String getHelpMessage() {
            return "Displays the description of the list";
        }

        @Override
        public String getDetailedHelpMessage() {
            return "Usage: desc\n"+getHelpMessage();
        }

        @Override
        public String execute(String input) throws CommandException {
            return display("description");
        }
    }
    
    public InfoCommand(String name) {
        super(name);
    }

    protected String display(String property) throws CommandException {
        FileEntryMetadata meta = getState().getEntry(PhantomPath.of("/")).getMetadata();
        switch (property) {
            case "name" -> {
                String name = meta.readString(FileEntry.METADATA_NAME);
                if (name == null || name.isEmpty()) {
                    return "No name";
                }
                return "Name: "+name;
            }
            case "author" -> {
                String author = meta.readString(FileEntry.METADATA_AUTHOR);
                if (author == null || author.isEmpty()) {
                    return "No author";
                }
                return "Author: "+author;
            }
            case "description" -> {
                String description = meta.readString(FileEntry.METADATA_DESCRIPTION);
                if (description == null || description.isEmpty()) {
                    return "No description";
                }
                return "Description:\n"+description;
            }
        }
        throw new CommandException(property);
    }
    
}
