package matinilad.contentlist;

import matinilad.contentlist.phantomfs.entry.FileEntryType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 *
 * @author Cien
 */
@Deprecated
public class ContentListUtils {
    
    @Deprecated
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

    @Deprecated
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

    private ContentListUtils() {

    }

}
