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
package matinilad.contentlist.ui.cli;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import matinilad.contentlist.phantomfs.PhantomCreator;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.entry.FileEntryCreator;
import matinilad.contentlist.phantomfs.entry.FileEntryWriter;
import matinilad.contentlist.ui.UIUtils;

/**
 *
 * @author Cien
 */
public class CreateCommand {

    private final PrintStream out;
    private final Path[] inputFiles;
    private final Path outputFile;

    private boolean running = false;
    private long nextUpdate = System.currentTimeMillis();

    private PhantomPath currentPath = null;
    private FileEntry lastEntry = null;
    private long current = 0;
    private long total = 0;
    private int processedEntries = 0;

    public CreateCommand(PrintStream out, Path[] inputFiles, Path outputFile) {
        this.out = Objects.requireNonNull(out);
        this.inputFiles = Objects.requireNonNull(inputFiles, "inputFiles is null").clone();
        this.outputFile = Objects.requireNonNull(outputFile, "outputFile is null");
    }

    private void update() {
        if (System.currentTimeMillis() < this.nextUpdate) {
            return;
        }
        this.nextUpdate = System.currentTimeMillis() + 10000;

        this.out.println("Total processed: " + this.processedEntries + " entries");
        this.out.println(" Current Path: " + this.currentPath);
        if (this.total == 0) {
            out.println("  --% Done");
        } else {
            double p = ((this.current / ((double) this.total)) * 100);
            this.out.println("  " + String.format("%.2f", p) + "% Done");
        }
    }

    private void onStart() throws IOException {
        this.out.println("Initializing...");
    }

    private void onFileError(Path path, IOException error) throws IOException {
        this.out.println("Warning: '" + path.toString() + "' was rejected, reason below:");
        error.printStackTrace(this.out);
    }

    private void onEntryStart(FileEntry entry) throws IOException {
        this.currentPath = entry.getPath();
        this.current = 0;
        this.total = 0;

        update();
    }

    private void onEntryFinish(FileEntry entry) throws IOException {
        this.lastEntry = entry;
        this.processedEntries++;

        update();
    }

    private void onEntryProgressUpdate(long current, long total) throws IOException {
        this.current = current;
        this.total = total;

        update();
    }

    private void onFinish() throws IOException {
        this.out.println("Done! " + this.processedEntries + " Entries in total!");
        this.out.println("Total Size: " + UIUtils.formatBytes(this.lastEntry.getSize()));
        this.out.println(" " + this.lastEntry.getFiles() + " Files, " + this.lastEntry.getDirectories() + " Directories");
    }

    public void run() {
        if (this.running) {
            throw new RuntimeException("already running!");
        }
        this.running = true;

        try {
            try (FileEntryWriter writer = new FileEntryWriter(new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(this.outputFile), StandardCharsets.UTF_8)), 0)) {
                onStart();
                
                PhantomCreator creator = new PhantomCreator() {
                    @Override
                    protected void onEntry(FileEntry entry) throws IOException, InterruptedException {
                        onEntryFinish(entry);
                        writer.writeFileEntry(entry);
                    }

                    @Override
                    protected void onFileRejected(Path file, IOException reason) throws IOException, InterruptedException {
                        onFileError(file, reason);
                    }

                };
                creator.setFileEntryCreator(new FileEntryCreator() {
                    @Override
                    protected void onEntryCreated(FileEntry entry) throws IOException, InterruptedException {
                        onEntryStart(entry);
                        onEntryProgressUpdate(0, 0);
                    }

                    @Override
                    protected void onEntryProgress(FileEntry entry, long bytes) throws IOException, InterruptedException {
                        onEntryProgressUpdate(bytes, entry.getSize());
                    }
                });
                creator.create(this.inputFiles);

                onFinish();
            }
        } catch (IOException | InterruptedException ex) {
            this.out.println("Operation failed!");
            this.out.println(ex.getLocalizedMessage());
            ex.printStackTrace(this.out);
        }
    }

}
