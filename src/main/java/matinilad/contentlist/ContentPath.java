package matinilad.contentlist;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A Content Path represents a path (files, folders, links, ...) in the file system, it has a very similar syntax to paths in unix systems.
 *
 * <p>
 * Directories are separated by '/'<br>
 * A path starting with '/' is a absolute path, relative otherwise<br>
 * A path ending with '/' or not has no meaning<br>
 * A path with only a '/' is the absolute root directory<br>
 * A empty path '' is the relative root directory<br>
 * All characters except the 'null' character and the '/' character are allowed<br>
 * For compatibility reasons, a '\' (Backslash) is interpreted the same as a '/' (Slash)<br>
 * '..' and '.' refers to the parent directory and the current directory respectively (called special links in here)</p>
 *
 * @author Cien
 */
public class ContentPath {

    private static final ContentPath ABSOLUTE_ROOT = new ContentPath(new String[0], false);
    private static final ContentPath RELATIVE_ROOT = new ContentPath(new String[0], true);

    /**
     * Creates a new ContentPath from a array of objects
     *
     * @param objects the array of objects, not null.
     * @param relative true if the path is relative
     * @return A ContentPath
     */
    public static ContentPath of(String[] objects, boolean relative) {
        Objects.requireNonNull(objects, "objects is null");
        for (int i = 0; i < objects.length; i++) {
            String object = objects[i];
            if (object.isEmpty()) {
                throw new IllegalArgumentException("object at index " + i + " is empty.");
            }
            for (int j = 0; j < object.length(); j++) {
                char c = object.charAt(j);
                if (c == '\0' || c == '/' || c == '\\') {
                    throw new IllegalArgumentException(
                            "object at index " + i + " contains illegal characters");
                }
            }
        }

        return new ContentPath(objects.clone(), relative);
    }
    
    /**
     * Creates a content path object by parsing a path string
     *
     * @param path The path string
     * @return The content object
     * @throws IllegalArgumentException if any parsing error occurred
     */
    public static ContentPath of(String path) {
        if (path == null || path.isEmpty()) {
            return RELATIVE_ROOT;
        }
        if (path.equals("/") || path.equals("\\")) {
            return ABSOLUTE_ROOT;
        }
        
        List<String> objectList = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean relative = true;
        int unicode;
        for (int i = 0; i < path.length(); i += Character.charCount(unicode)) {
            unicode = path.codePointAt(i);
            if (i == 0 && (unicode == '/' || unicode == '\\')) {
                relative = false;
                continue;
            }
            if (unicode == '\0') {
                throw new IllegalArgumentException("Path contains null character at index " + i);
            }
            boolean lastChar = ((i + Character.charCount(unicode)) >= path.length());
            if ((unicode == '/' || unicode == '\\') || lastChar) {
                if (lastChar && !(unicode == '/' || unicode == '\\')) {
                    b.appendCodePoint(unicode);
                }
                String result = b.toString();
                if (result.isEmpty()) {
                    throw new IllegalArgumentException("Path contains empty object at index " + i);
                }
                b.setLength(0);
                objectList.add(result);
                continue;
            }
            b.appendCodePoint(unicode);
        }
        String[] objects = objectList.toArray(String[]::new);
        return new ContentPath(objects, relative);
    }

    /**
     * Concatenates two paths into one (a + b)<br>
     * If the result is relative or absolute only depends on the 'a' path<br>
     * if a is relative, then the result is relative<br>
     * if a is absolute, then the result is absolute
     *
     * @param a The a path
     * @param b The b path
     * @return a + b
     */
    public static ContentPath resolve(ContentPath a, ContentPath b) {
        Objects.requireNonNull(a, "a is null");
        Objects.requireNonNull(b, "b is null");
        List<String> fields = new ArrayList<>();
        for (int i = 0; i < a.getNumberOfObjects(); i++) {
            fields.add(a.getObject(i));
        }
        for (int i = 0; i < b.getNumberOfObjects(); i++) {
            fields.add(b.getObject(i));
        }
        return new ContentPath(fields.toArray(String[]::new), a.isRelative());
    }
    
    private final String[] objects;
    private final boolean relative;
    private final boolean specialLinks;

    private ContentPath(String[] objects, boolean relative) {
        this.objects = objects;
        this.relative = relative;
        boolean special = false;
        for (String obj : objects) {
            if (obj.equals(".") || obj.equals("..")) {
                special = true;
                break;
            }
        }
        this.specialLinks = special;
    }

    /**
     * Returns the number of objects in this path<br>
     * <br>
     * For example:<br>
     * a/b/c<br>
     * has 3 objects
     *
     * @return The number of objects in this path
     */
    public int getNumberOfObjects() {
        return this.objects.length;
    }

    /**
     * Returns a object of this path by the index<br>
     * <br>
     * For example:<br>
     * a/b/c/d<br>
     * 'b' has index 1 and 'd' has index 3
     *
     * @param index The object index
     * @return The object
     */
    public String getObject(int index) {
        return this.objects[index];
    }

    /**
     * The last object of this path or null if this is a root path.<br>
     * <br>
     * For example:<br>
     * a/b/c/myfile.txt<br>
     * The last object is "myfile.txt"
     *
     * @return The last object of this path (the name of the file) or null if this is a root path.
     */
    public String getName() {
        if (isRoot()) {
            return null;
        }
        return this.objects[this.objects.length - 1];
    }

    /**
     * Returns true if this a relative directory or false if absolute
     *
     * @return true if relative
     */
    public boolean isRelative() {
        return relative;
    }

    /**
     * Returns true if this is a root directory
     *
     * @return true if is a root directory
     */
    public boolean isRoot() {
        return this.objects.length == 0;
    }

    /**
     * Return true if this path contains special links such as '..' or '.'
     *
     * @return true if contains special links
     */
    public boolean hasSpecialLinks() {
        return this.specialLinks;
    }

    /**
     * Returns the parent of this path<br>
     * <br>
     * For example:<br>
     * a/b/c<br>
     * The parent is:<br>
     * a/b
     *
     * @return the parent or the root if this is already a root directory
     */
    public ContentPath getParent() {
        if (this.objects.length <= 1) {
            if (isRelative()) {
                return RELATIVE_ROOT;
            } else {
                return ABSOLUTE_ROOT;
            }
        }
        return new ContentPath(Arrays.copyOf(this.objects, this.objects.length - 1), isRelative());
    }

    /**
     * Converts this path to a relative one<br>
     * <br>
     * For example:<br>
     * /a/b/c<br>
     * Becomes<br>
     * a/b/c
     *
     * @return A relative path or the current instance if already relative
     */
    public ContentPath toRelative() {
        if (isRelative()) {
            return this;
        }
        return new ContentPath(this.objects, true);
    }

    /**
     * Converts this path to a absolute one<br>
     * <br>
     * For example:<br>
     * a/b/c<br>
     * Becomes<br>
     * /a/b/c
     *
     * @return A absolute path or the current instance if already absolute
     */
    public ContentPath toAbsolute() {
        if (!isRelative()) {
            return this;
        }
        return new ContentPath(this.objects, false);
    }

    /**
     * Same as resolve(this, of(other))
     *
     * @param other
     * @return
     */
    public ContentPath resolve(String other) {
        return resolve(this, of(other));
    }

    /**
     * Same as resolve(this, other)
     *
     * @param other
     * @return
     */
    public ContentPath resolve(ContentPath other) {
        return resolve(this, other);
    }

    /**
     * Resolves this content path into a java path using another path as anchor, the content file separator is converted to the path's file system separator.
     *
     * <p>
     * For example:<br>
     * /a/b/c as the content path<br>
     * C:\folder as the anchor path<br>
     * will be resolved into:<br>
     * C:\folder\a\b\c
     * </p>
     *
     * @param anchor The anchor path, not null
     * @return
     */
    public Path resolveToPath(Path anchor) {
        if (isRoot()) {
            return anchor;
        }
        String separator = anchor.getFileSystem().getSeparator();

        StringBuilder b = new StringBuilder();
        for (int i = 0; i < getNumberOfObjects(); i++) {
            b.append(getObject(i));
            if (i < (getNumberOfObjects() - 1)) {
                b.append(separator);
            }
        }

        return anchor.resolve(b.toString());
    }

    /**
     * Renames this path (changes the last object in this path or adds a new one if this is a root path)
     *
     * <p>
     * For example:<br>
     * /a/b/c/file.txt renamed to music.ogg becomes /a/b/c/music.ogg<br>
     * / renamed to documents becomes /documents</p>
     *
     * <p>
     * This method only changes the last object, it is not the same as resolving with another path, using '/' or '\' in the name will cause a exception</p>
     *
     * <p>
     * For renaming with a resolve, use getParent().resolve(of(name))</p>
     *
     * @param name
     * @return
     */
    public ContentPath rename(String name) {
        if (isRoot()) {
            return of(new String[]{name}, isRelative());
        }
        String[] copy = this.objects.clone();
        copy[copy.length - 1] = name;
        return of(copy, isRelative());
    }
    
    /**
     * Makes this directory relative to a root path
     * 
     * <p>
     * For example:<br>
     * /a/b/c/file.txt relative to /a/b/c becomes file.txt
     * </p>
     * 
     * <p>
     * This method does not care if this or root is either absolute or relative.</p>
     * 
     * @param root The root path, not null, either absolute or relative.
     * @return A relative path or null if the root path does not match the start of this path
     */
    public ContentPath relative(ContentPath root) {
        Objects.requireNonNull(root, "root is null");
        if (getNumberOfObjects() < root.getNumberOfObjects()) {
            return null;
        }
        for (int i = 0; i < root.getNumberOfObjects(); i++) {
            if (!getObject(i).equals(root.getObject(i))) {
                return null;
            }
        }
        return new ContentPath(
                Arrays.copyOfRange(this.objects, root.getNumberOfObjects(), this.objects.length),
                true
        );
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + Arrays.deepHashCode(this.objects);
        hash = 19 * hash + (this.relative ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ContentPath other = (ContentPath) obj;
        if (this.relative != other.relative) {
            return false;
        }
        return Arrays.deepEquals(this.objects, other.objects);
    }

    /**
     * Returns the string representation of this path
     *
     * @return The string representation of this path
     */
    @Override
    public String toString() {
        if (isRoot()) {
            if (isRelative()) {
                return "";
            } else {
                return "/";
            }
        }
        StringBuilder b = new StringBuilder();
        if (!isRelative()) {
            b.append("/");
        }
        for (int i = 0; i < getNumberOfObjects(); i++) {
            b.append(getObject(i));
            if (i != (getNumberOfObjects() - 1)) {
                b.append("/");
            }
        }
        return b.toString();
    }

}