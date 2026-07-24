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

import java.util.HexFormat;
import matinilad.contentlist.phantomfs.PhantomFileSystem;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.entry.FileEntryType;
import matinilad.contentlist.ui.UIUtils;
import matinilad.contentlist.ui.tui.Command;
import matinilad.contentlist.ui.tui.CommandException;
import matinilad.contentlist.ui.tui.TUIState;

/**
 *
 * @author Cien
 */
public class AboutCommand extends Command {

    public AboutCommand() {
        super("about", "Shows information about a file/directory");
    }

    @Override
    public String execute(TUIState state, String input) throws CommandException {
        StringBuilder b = new StringBuilder();
        
        if (input == null) {
            throw new CommandException("Usage: about [file]");
        }
        
        PhantomFileSystem fs = state.getFileSystem();
        
        PhantomPath p = state.parsePath(input);
        if (p.isRelative()) {
            p = state.getWorkingDirectory().resolve(p);
        }
        if (!fs.exists(p)) {
            throw new CommandException(input + " does not exists!");
        }
        FileEntry entry = state.getEntry(p);
        
        b.append("Properties of:").append(System.lineSeparator());
        b.append(entry.getPath().toString()).append(System.lineSeparator());
        b.append(" Type: ").append(entry.getType()).append(System.lineSeparator());
        b.append(" Created: ").append(UIUtils.asShortLocalizedDateTime(entry.getCreated())).append(System.lineSeparator());
        b.append(" Modified: ").append(UIUtils.asShortLocalizedDateTime(entry.getModified())).append(System.lineSeparator());
        b.append(" Size: ").append(UIUtils.formatBytes(entry.getSize())).append(System.lineSeparator());
        if (entry.getType().equals(FileEntryType.DIRECTORY)) {
            b.append("  ").append(entry.getFiles()).append(" Files, ").append(entry.getDirectories()).append(" Directories").append(System.lineSeparator());
        }

        byte[] sha256 = entry.getSha256();
        byte[] sample = entry.getSample();

        if (sha256 != null || sample != null) {
            HexFormat hex = HexFormat.of();
            if (sha256 != null) {
                b.append(" SHA256: ").append(hex.formatHex(sha256)).append(System.lineSeparator());
            }
            if (sample != null) {
                b.append(" Sample: ").append(hex.formatHex(sample)).append(System.lineSeparator());
            }
        }
        
        return b.toString();
    }

}
