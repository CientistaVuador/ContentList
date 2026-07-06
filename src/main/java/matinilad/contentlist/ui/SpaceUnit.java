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
package matinilad.contentlist.ui;


/**
 *
 * @author Cien
 */
public interface SpaceUnit {

    public static String format(SpaceUnit unit, long bytes, boolean shortened) {
        SpaceUnit[] units = unit.getSpaceUnits();
        SpaceUnit byteUnit = units[0];
        
        if (bytes == 0) {
            return "0 " + byteUnit.getSuffix();
        }
        
        SpaceUnit selectedUnit = byteUnit;
        for (int i = (units.length - 1); i >= 0; i--) {
            selectedUnit = units[i];
            if (bytes >= selectedUnit.getSize()) {
                break;
            }
        }

        if (selectedUnit == byteUnit) {
            return bytes + " " + byteUnit.getSuffix();
        }
        
        float divided;
        if (bytes != selectedUnit.getSize()) {
            divided = (float) ((double) bytes / selectedUnit.getSize());
        } else {
            divided = 1f;
        }

        StringBuilder b = new StringBuilder();

        b.append(String.format("%.2f", divided)).append(" ").append(selectedUnit.getSuffix());
        
        if (!shortened) {
            b.append(" (").append(bytes).append(" ").append(byteUnit.getSuffix()).append(")");
        }

        return b.toString();
    }

    public SpaceUnit[] getSpaceUnits();

    public long getSize();

    public String getSuffix();
}
