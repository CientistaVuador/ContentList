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
    
    public static String asShortLocalizedDateTime(long utcTime) {
        return Instant
                .ofEpochMilli(utcTime)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
    }
    
    public static final long BYTE = 1;
    public static final long KILOBYTE = BYTE * 1000;
    public static final long MEGABYTE = KILOBYTE * 1000;
    public static final long GIGABYTE = MEGABYTE * 1000;
    public static final long TERABYTE = GIGABYTE * 1000;
    
    public static String formatPercentage(long current, long total) {
        if (total == 0) {
            return "--%";
        }
        return String.format("%.2f", (((double)current) / total) * 100.0)+"%";
    }
    
    public static String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond > TERABYTE) {
            return String.format("%.2f", ((double)bytesPerSecond) / TERABYTE)+" TB/s";
        }
        if (bytesPerSecond > GIGABYTE) {
            return String.format("%.2f", ((double)bytesPerSecond) / GIGABYTE)+" GB/s";
        }
        if (bytesPerSecond > MEGABYTE) {
            return String.format("%.2f", ((double)bytesPerSecond) / MEGABYTE)+" MB/s";
        }
        if (bytesPerSecond > KILOBYTE) {
            return String.format("%.2f", ((double)bytesPerSecond) / KILOBYTE)+" KB/s";
        }
        return bytesPerSecond+" B/s";
    }
    
    public static String formatBytes(long bytes) {
        long terabytes = bytes / TERABYTE;
        bytes -= terabytes * TERABYTE;
        long gigabytes = bytes / GIGABYTE;
        bytes -= gigabytes * GIGABYTE;
        long megabytes = bytes / MEGABYTE;
        bytes -= megabytes * MEGABYTE;
        long kilobytes = bytes / KILOBYTE;
        bytes -= kilobytes * KILOBYTE;
        
        StringBuilder b = new StringBuilder();
        
        if (terabytes != 0) {
            b.append(terabytes).append(" TB, ");
        }
        if (gigabytes != 0) {
            b.append(gigabytes).append(" GB, ");
        }
        if (megabytes != 0) {
            b.append(megabytes).append(" MB, ");
        }
        if (kilobytes != 0) {
            b.append(kilobytes).append(" KB, ");
        }
        b.append(bytes).append(" Bytes");
        
        return b.toString();
    }
    
    public static String stacktraceOf(Throwable t) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintStream print = new PrintStream(out, false, StandardCharsets.UTF_8)) {
            t.printStackTrace(print);
        }
        return out.toString(StandardCharsets.UTF_8);
    }
    
    private static volatile String aboutText = null;
    
    public static String about() {
        String about = aboutText;
        if (about != null) {
            return about;
        }
        try {
            try (InputStream in = UIUtils.class.getResourceAsStream("about.txt")) {
                about = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            about = "Failed to read about text!\n"+stacktraceOf(ex);
        }
        aboutText = about;
        return about;
    }
    
    private UIUtils() {
        
    }
}
