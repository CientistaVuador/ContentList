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
package matinilad.contentlist.phantomfs.entry;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import matinilad.contentlist.phantomfs.PhantomPath;

/**
 *
 * @author Cien
 */
public class FileEntryMetadata {

    private static class MetadataNode {

        MetadataNode parent = null;

        String name = null;
        String value = null;

        final Map<String, MetadataNode> children = new HashMap<>();
    }

    private final MetadataNode rootNode = new MetadataNode();

    {
        rootNode.children.put(".", this.rootNode);
        rootNode.children.put("..", this.rootNode);
        rootNode.name = "";
        rootNode.parent = rootNode;
    }

    public FileEntryMetadata() {

    }

    private MetadataNode resolve(PhantomPath path, boolean createNewNodes) {
        MetadataNode current = this.rootNode;
        for (int i = 0; i < path.getNumberOfObjects(); i++) {
            String obj = path.getObject(i);

            MetadataNode child = current.children.get(obj);
            if (child != null) {
                current = child;
                continue;
            }

            if (!createNewNodes) {
                return null;
            }

            child = new MetadataNode();
            child.name = obj;
            child.parent = current;
            child.children.put(".", child);
            child.children.put("..", child.parent);

            current.children.put(child.name, child);
            current = child;
        }
        return current;
    }

    private void cleanup(MetadataNode node) {
        if (node == null || node.parent == node) {
            return;
        }
        do {
            if (node.value != null || node.children.size() > 2) {
                return;
            }

            node.parent.children.remove(node.name);
        } while ((node = node.parent) != node.parent);
    }

    private void checkPathNull(PhantomPath path) {
        Objects.requireNonNull(path, "path is null");
    }

    public String readString(PhantomPath path) {
        checkPathNull(path);

        MetadataNode resolved = resolve(path, false);
        if (resolved == null) {
            return null;
        }
        return resolved.value;
    }

    public void writeString(PhantomPath path, String value) {
        checkPathNull(path);

        MetadataNode resolved = resolve(path, true);
        resolved.value = value;

        if (value == null) {
            cleanup(resolved);
        }
    }

    public PhantomPath[] list(PhantomPath path, boolean sort, boolean includeSpecial) {
        checkPathNull(path);

        MetadataNode node = resolve(path, false);
        if (node == null) {
            return null;
        }

        List<PhantomPath> paths = new ArrayList<>();
        for (Entry<String, MetadataNode> entry : node.children.entrySet()) {
            String name = entry.getKey();
            if ((name.equals(".") || name.equals("..")) && !includeSpecial) {
                continue;
            }

            paths.add(path.resolve(name));
        }

        if (sort) {
            paths.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()));
        }

        return paths.toArray(PhantomPath[]::new);
    }

    public PhantomPath[] list(PhantomPath path) {
        return list(path, false, false);
    }

    public PhantomPath toRealPath(PhantomPath path) {
        checkPathNull(path);

        MetadataNode node = resolve(path, false);
        if (node == null) {
            return null;
        }
        if (node.parent == node) {
            return PhantomPath.of("/");
        }

        List<String> list = new ArrayList<>();
        do {
            list.add(node.name);
        } while ((node = node.parent) != node.parent);

        String[] objects = new String[list.size()];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = list.get((objects.length - 1) - i);
        }

        return PhantomPath.of(objects, false);
    }

    public boolean exists(PhantomPath path) {
        checkPathNull(path);
        return resolve(path, false) != null;
    }

    public boolean delete(PhantomPath path) {
        checkPathNull(path);

        MetadataNode node = resolve(path, false);
        if (node == null) {
            return false;
        }

        node.value = null;
        node.children.clear();
        node.children.put(".", node);
        node.children.put("..", node.parent);

        cleanup(node);
        return true;
    }

    public void save(Writer writer) throws IOException {
        Objects.requireNonNull(writer, "writer is null");

        save(writer, PhantomPath.of("/"), true);
    }

    private String escape(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&', '=', '\\' -> {
                    b.append('\\');
                }
                case '\r' -> {
                    b.append("\\r");
                    continue;
                }
                case '\n' -> {
                    b.append("\\n");
                    continue;
                }
                case '\t' -> {
                    b.append("\\t");
                    continue;
                }
            }
            b.append(c);
        }
        return b.toString();
    }

    private boolean save(Writer writer, PhantomPath path, boolean first) throws IOException {
        String value = readString(path);
        if (value != null) {
            if (!first) {
                writer.write("&");
            }
            writer.write(escape(path.toString()));
            writer.write("=");
            writer.write(escape(value));
            first = false;
        }

        PhantomPath[] children = list(path, true, false);
        for (PhantomPath child : children) {
            if (!save(writer, child, first)) {
                first = false;
            }
        }

        return first;
    }

    public void load(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "reader is null");

        String key = null;
        StringBuilder b = new StringBuilder();
        boolean escape = false;

        while (true) {
            int r = reader.read();
            if (r == -1) {
                if (key != null) {
                    writeString(PhantomPath.of(key), b.toString());
                }
                break;
            }
            char c = (char) r;
            if (escape) {
                switch (c) {
                    case 'n' -> {
                        c = '\n';
                    }
                    case 'r' -> {
                        c = '\r';
                    }
                    case 't' -> {
                        c = '\t';
                    }
                }
                b.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '=') {
                key = b.toString();
                b.setLength(0);
                continue;
            }
            if (c == '&') {
                if (key == null) {
                    b.setLength(0);
                    continue;
                }
                writeString(PhantomPath.of(key), b.toString());
                key = null;
                b.setLength(0);
                continue;
            }
            b.append(c);
        }
    }

    public String save() {
        try (StringWriter writer = new StringWriter()) {
            save(writer);
            return writer.toString();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public void load(String data) {
        try (StringReader reader = new StringReader(data)) {
            load(reader);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public String toString() {
        return save();
    }

}
