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

import matinilad.contentlist.ui.BinarySpaceUnit;
import matinilad.contentlist.ui.DecimalSpaceUnit;
import matinilad.contentlist.ui.SpaceUnit;
import matinilad.contentlist.ui.UIUtils;
import matinilad.contentlist.ui.tui.Command;
import matinilad.contentlist.ui.tui.CommandException;

/**
 *
 * @author Cien
 */
public class SpaceUnitCommand extends Command {

    public SpaceUnitCommand() {
        super("unit");
    }

    @Override
    public String getHelpMessage() {
        return "Changes the storage format to binary or decimal";
    }

    @Override
    public String getDetailedHelpMessage() {
        return "Usage: unit [bin/dec]\n" + getHelpMessage() + "\nOn decimal: 1KB = 1000B\nOn binary: 1KiB = 1024B\nUse unit with no arguments to display the current unit";
    }

    private String getName(SpaceUnit unit) {
        if (unit instanceof BinarySpaceUnit) {
            return "Binary, 1KiB = 1024B";
        }
        if (unit instanceof DecimalSpaceUnit) {
            return "Decimal, 1KB = 1000B";
        }
        return "Unknown, " + UIUtils.getSpaceUnit().getSuffix();
    }

    @Override
    public String execute(String input) throws CommandException {
        if (input == null || input.isEmpty()) {
            return getName(UIUtils.getSpaceUnit());
        }
        input = input.toLowerCase().trim();
        switch (input) {
            case "bin", "binary" -> {
                UIUtils.setSpaceUnit(BinarySpaceUnit.KIBIBYTE);
            }
            case "dec", "decimal" -> {
                UIUtils.setSpaceUnit(DecimalSpaceUnit.KILOBYTE);
            }
            default -> {
                throw new CommandException("Usage: unit [bin/dec]");
            }
        }
        return "Success!\n"+getName(UIUtils.getSpaceUnit());
    }

}
