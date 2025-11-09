package matinilad.contentlist.phantomfs.entry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 *
 * @author Cien
 */
public enum FileEntryType {
    FILE("File"),
    DIRECTORY("Directory"),
    SYMBOLIC_LINK("Link"),
    UNKNOWN("Unknown");

    public static FileEntryType typeOf(Path file) {
        Objects.requireNonNull(file, "file is null");

        FileEntryType type;
        if (Files.isRegularFile(file)) {
            type = FileEntryType.FILE;
        } else if (Files.isDirectory(file)) {
            type = FileEntryType.DIRECTORY;
        } else if (Files.isSymbolicLink(file)) {
            type = FileEntryType.SYMBOLIC_LINK;
        } else {
            type = FileEntryType.UNKNOWN;
        }

        return type;
    }

    private final String displayName;

    private FileEntryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
