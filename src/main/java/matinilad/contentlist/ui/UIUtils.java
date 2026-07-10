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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 *
 * @author Cien
 */
public class UIUtils {
    
    public static final long BYTE = DecimalSpaceUnit.BYTE.getSize();
    
    public static final long KILOBYTE = DecimalSpaceUnit.KILOBYTE.getSize();
    public static final long MEGABYTE = DecimalSpaceUnit.MEGABYTE.getSize();
    public static final long GIGABYTE = DecimalSpaceUnit.GIGABYTE.getSize();
    public static final long TERABYTE = DecimalSpaceUnit.TERABYTE.getSize();
    
    public static final long KIBIBYTE = BinarySpaceUnit.KIBIBYTE.getSize();
    public static final long MEBIBYTE = BinarySpaceUnit.MEBIBYTE.getSize();
    public static final long GIBIBYTE = BinarySpaceUnit.GIBIBYTE.getSize();
    public static final long TEBIBYTE = BinarySpaceUnit.TEBIBYTE.getSize();
    
    private static SpaceUnit spaceUnit = DecimalSpaceUnit.BYTE;
    
    public static void setSpaceUnit(SpaceUnit unit) {
        spaceUnit = (unit == null ? DecimalSpaceUnit.BYTE : unit);
    }
    
    public static SpaceUnit getSpaceUnit() {
        return spaceUnit;
    }
    
    public static String formatBytesShort(long bytes) {
        return SpaceUnit.format(spaceUnit, bytes, true);
    }
    
    public static String formatBytes(long bytes) {
         return SpaceUnit.format(spaceUnit, bytes, false);
    }
    
    public static String asShortLocalizedDateTime(long utcTime) {
        return Instant
                .ofEpochMilli(utcTime)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
    }
    
    public static String formatPercentage(long current, long total) {
        if (total == 0) {
            return "--%";
        }
        return String.format("%.2f", (((double)current) / total) * 100.0)+"%";
    }
    
    public static String formatSpeed(long bytesPerSecond) {
        return formatBytesShort(bytesPerSecond)+"/s";
    }
    
    public static String stacktraceOf(Throwable t) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintStream print = new PrintStream(out, false, StandardCharsets.UTF_8)) {
            t.printStackTrace(print);
        }
        return out.toString(StandardCharsets.UTF_8);
    }
    
    private static String readFile(String name) {
        try {
            try (InputStream in = UIUtils.class.getResourceAsStream(name)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            return "Failed to read '"+name+"'! "+ex.getLocalizedMessage();
        }
    }
    
    private static volatile String aboutText = null;
    private static volatile String nameText = null;
    private static volatile String versionText = null;
    private static volatile String internalNameText = null;
    
    public static String about() {
        if (aboutText == null) {
            aboutText = readFile("about.txt");
        }
        return aboutText;
    }
    
    public static String name() {
        if (nameText == null) {
            nameText = readFile("name.txt").trim();
        }
        return nameText;
    }
    
    public static String version() {
        if (versionText == null) {
            versionText = readFile("version.txt").trim();
        }
        return versionText;
    }
    
    public static String internalName() {
        if (internalNameText == null) {
            internalNameText = name().toLowerCase().replace(' ', '_');
        }
        return internalNameText;
    }
    
    private UIUtils() {
        
    }
}
