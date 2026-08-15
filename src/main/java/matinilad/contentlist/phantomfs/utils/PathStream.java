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
package matinilad.contentlist.phantomfs.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author Cien
 */
public class PathStream {

    private static final boolean LINUX_OS;

    static {
        String osName = System.getProperty("os.name");
        if (osName != null) {
            osName = osName.toLowerCase();
            LINUX_OS = osName.contains("nix") || osName.contains("nux") || osName.contains("aix");
        } else {
            LINUX_OS = false;
        }
    }

    public static class Entry {

        private final Path root;
        private final Path path;
        private final IOException error;

        public Entry(Path root, Path path, IOException error) {
            this.root = root;
            this.path = path;
            this.error = error;
        }

        public Path getRoot() {
            return root;
        }

        public Path getPath() {
            return path;
        }

        public IOException getError() {
            return error;
        }
    }

    private final Path[] inputs;
    private final boolean hiddenFilesEnabled;

    public PathStream(Path[] input, boolean hiddenFilesEnabled) {
        this.inputs = Objects.requireNonNull(input, "inputs is null").clone();
        for (int i = 0; i < this.inputs.length; i++) {
            Objects.requireNonNull(this.inputs[i], "input at index " + i + " is null");
        }
        this.hiddenFilesEnabled = hiddenFilesEnabled;
    }

    public Path[] getInputs() {
        return inputs.clone();
    }

    public boolean isHiddenFilesEnabled() {
        return hiddenFilesEnabled;
    }

    private List<Path> preprocess(List<Path> input, Consumer<Entry> consumer) {
        List<Path> realFiles = new ArrayList<>();
        for (Path e : input) {
            Path real;
            try {
                real = e.toRealPath();
            } catch (IOException ex) {
                consumer.accept(new Entry(null, e, ex));
                continue;
            }

            Path parent = real.getParent();
            if (parent == null) {
                try {
                    List<Path> files = new ArrayList<>();
                    try (Stream<Path> stream = Files.list(real)) {
                        stream.forEach((c) -> {
                            files.add(c);
                        });
                    }
                    realFiles.addAll(files);
                } catch (IOException ex) {
                    consumer.accept(new Entry(null, real, ex));
                    continue;
                } catch (UncheckedIOException ex) {
                    consumer.accept(new Entry(null, real, ex.getCause()));
                    continue;
                }
                continue;
            }

            realFiles.add(real);
        }

        List<Path> directories = new ArrayList<>();
        List<Path> files = new ArrayList<>();

        for (Path real : realFiles) {
            try {
                if (!isHiddenFilesEnabled() && Files.isHidden(real)) {
                    consumer.accept(new Entry(null, real, new IOException("hidden file")));
                    continue;
                }
            } catch (IOException ex) {
                consumer.accept(new Entry(null, real, ex));
                continue;
            }

            if (Files.isDirectory(real)) {
                directories.add(real);
            } else if (Files.isRegularFile(real)) {
                files.add(real);
            } else {
                consumer.accept(new Entry(null, real, new IOException("not a file or directory")));
            }
        }

        Comparator<Path> comparator = (o1, o2) -> {
            return String.CASE_INSENSITIVE_ORDER
                    .compare(o1.getFileName().toString(), o2.getFileName().toString());
        };

        files.sort(comparator);
        directories.sort(comparator);

        List<Path> output = new ArrayList<>();
        output.addAll(directories);
        output.addAll(files);
        return output;
    }

    private void process(Consumer<Entry> consumer, Path root, Path path) {
        consumer.accept(new Entry(root, path, null));

        if (Files.isDirectory(path)) {
            List<Path> files = new ArrayList<>();
            try {
                try (Stream<Path> stream = Files.list(path)) {
                    stream.forEach((c) -> {
                        files.add(c);
                    });
                }
            } catch (IOException ex) {
                files.clear();
                consumer.accept(new Entry(null, path, ex));
            } catch (UncheckedIOException ex) {
                files.clear();
                consumer.accept(new Entry(null, path, ex.getCause()));
            }
            
            List<Path> preprocessed = preprocess(files, consumer);
            for (Path p : preprocessed) {
                process(consumer, root, p);
            }
        }
    }

    public void stream(Consumer<Entry> consumer) {
        Set<String> names = new HashSet<>();

        List<Path> preprocessed = preprocess(Arrays.asList(this.inputs), consumer);
        for (Path e : preprocessed) {
            String name = e.getFileName().toString();
            if (!LINUX_OS) {
                name = name.toLowerCase();
            }
            if (!names.add(name)) {
                consumer.accept(new Entry(null, e, new IOException("duplicated filename")));
                continue;
            }

            process(consumer, e.getParent(), e);
        }
    }
}
