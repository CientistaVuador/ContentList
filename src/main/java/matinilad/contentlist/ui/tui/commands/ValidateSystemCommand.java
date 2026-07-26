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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HexFormat;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.entry.FileEntryValidator;
import matinilad.contentlist.phantomfs.entry.FileEntryValidatorResult;
import matinilad.contentlist.ui.UIUtils;
import matinilad.contentlist.ui.tui.CommandException;

/**
 *
 * @author Cien
 */
public class ValidateSystemCommand extends SystemCommand {

    public ValidateSystemCommand() {
        super("sys:validate");
    }

    @Override
    public boolean isDirectOutputEnabled() {
        return true;
    }

    @Override
    public String getHelpMessage() {
        return "Validates a system file";
    }

    @Override
    public String getDetailedHelpMessage() {
        return "Usage: sys:validate [file]\n" + getHelpMessage();
    }

    @Override
    protected String run(PhantomPath path, Path file) throws CommandException, IOException {
        PrintStream out = getDirectOutput();

        FileEntry[] entries = getFileSystem().listEntries(new PhantomPath[]{path});
        if (entries.length == 0) {
            throw new CommandException("Entry not found for " + path.toString());
        }
        
        if (entries.length != 1) {
            out.println("Validating "+entries.length+" entries");
        }
        
        int failed = 0;

        Path root = getState().getRootDirectoryChecked();
        for (FileEntry entry : entries) {
            try {
                FileEntryValidator validator = new FileEntryValidator(root, entry);
                if (entries.length == 1) {
                    out.println("Validating "+validator.getPath().toString());
                }
                
                FileEntryValidatorResult result = validator.validate();
                if (!result.success()) {
                    failed++;

                    Object expected = result.getExpectedValue();
                    Object found = result.getFoundValue();
                    HexFormat hex = HexFormat.of();
                    
                    if (entries.length == 1) {
                        out.print("Failed! ");
                    } else {
                        out.print("Failed: " + validator.getPath().toString() + " ");
                    }
                    switch (result.getReason()) {
                        case EXISTENCE -> {
                            out.println("Reason: Does not exists!");
                        }
                        case TYPE -> {
                            out.println("Reason: Expected type " + expected + ", Found " + found);
                        }
                        case SIZE -> {
                            out.println("Reason: Wrong Size! Expected " + UIUtils.formatBytes((long) expected) + "; Found " + UIUtils.formatBytes((long) found));
                        }
                        case SAMPLE -> {
                            out.println("Reason: Wrong sample! Expected: " + hex.formatHex((byte[]) expected) + " Found: " + hex.formatHex((byte[]) found));
                        }
                        case HASH -> {
                            out.println("Reason: Wrong hash! Expected: " + hex.formatHex((byte[]) expected) + " Found: " + hex.formatHex((byte[]) found));
                        }
                    }
                } else {
                    if (entries.length == 1) {
                        out.println("Success!");
                    }
                }
            } catch (InterruptedException ex) {
                throw new CommandException(ex);
            }
        }

        out.println(entries.length + " Total, " + (entries.length - failed) + " Success, "+failed+" Failed");

        return "";
    }

}
