package matinilad.contentlist;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 *
 * @author Cien
 */
public class ContentListUtils {

    public static ContentType typeOf(Path file) {
        Objects.requireNonNull(file, "file is null");
        
        ContentType type;
        if (Files.isRegularFile(file)) {
            type = ContentType.FILE;
        } else if (Files.isDirectory(file)) {
            type = ContentType.DIRECTORY;
        } else if (Files.isSymbolicLink(file)) {
            type = ContentType.SYMBOLIC_LINK;
        } else {
            type = ContentType.UNKNOWN;
        }
        
        return type;
    }

    public static byte[] readHexString(String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return null;
        }
        if (hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex String must have a even length!");
        }
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            char a = hexString.charAt((i * 2) + 0);
            char b = hexString.charAt((i * 2) + 1);
            bytes[i] = (byte) Integer.parseInt(new String(new char[]{a, b}), 16);
        }
        return bytes;
    }

    public static String toHexString(byte[] data) {
        if (data == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            byte e = data[i];
            String hex = Integer.toHexString(e & 0xFF);
            if (hex.length() == 1) {
                b.append("0");
            }
            b.append(hex);
        }
        return b.toString();
    }

    public static String escapeCSVField(String s) {
        StringBuilder b = new StringBuilder();
        boolean quotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\n' || c == '\r' || c == ',') {
                quotes = true;
            }
            if (c == '"') {
                b.append('"');
            }
            b.append(c);
        }
        String result = b.toString();
        if (quotes) {
            return '"' + result + '"';
        }
        return result;
    }

    private ContentListUtils() {

    }

}
