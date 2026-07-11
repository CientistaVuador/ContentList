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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import matinilad.contentlist.phantomfs.PhantomCreator;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.entry.FileEntryCreator;
import matinilad.contentlist.phantomfs.entry.FileEntryWriter;
import matinilad.contentlist.ui.UIUtils;

/**
 * TODO: test new create command
 *
 * @author Cien
 */
public class CreateCommand {

    private static final Logger LOGGER = Logger.getLogger(CreateCommand.class.getName());

    static {
        if (!CLInterface.ENABLE_VERBOSE_LOGGING) {
            LOGGER.setLevel(Level.WARNING);
        }
    }

    public static final String NAMESPACE = UIUtils.internalName() + ".cli.create";

    public static boolean WRITE_FILES_AND_DIRECTORIES_COUNT = CLInterface.readBooleanProperty(NAMESPACE + ".count", true);
    public static boolean ENABLE_SHA256 = CLInterface.readBooleanProperty(NAMESPACE + ".sha256", true);
    public static boolean ENABLE_SAMPLE = CLInterface.readBooleanProperty(NAMESPACE + ".sample", true);
    public static int SAMPLE_SIZE = CLInterface.readIntegerProperty(NAMESPACE + ".sampleSize", 32, 0, 32);
    public static boolean ENABLE_METADATA = CLInterface.readBooleanProperty(NAMESPACE + ".metadata", true);
    public static boolean ENABLE_HEADER = CLInterface.readBooleanProperty(NAMESPACE + ".header", true);

    private final Path[] inputFiles;
    private final Path outputFile;

    private boolean running = false;

    private int processedEntries = 0;
    private int failedEntries = 0;
    private int writtenEntries = 0;

    private FileEntry lastEntry = null;
    private boolean fileSizeDisplayed = false;

    public CreateCommand(Path[] inputFiles, Path outputFile) {
        this.inputFiles = Objects.requireNonNull(inputFiles, "inputFiles is null").clone();
        this.outputFile = Objects.requireNonNull(outputFile, "outputFile is null");
    }

    private void onStart() throws IOException {
        LOGGER.info("Initializing...");

        LOGGER.log(Level.INFO, "Output is: {0}", this.outputFile.toString());
        LOGGER.log(Level.INFO, "Number of Inputs: {0}", this.inputFiles.length);
        for (int i = 0; i < this.inputFiles.length; i++) {
            LOGGER.log(Level.INFO, "Input {0} is: {1}", new Object[]{i, this.inputFiles[i].toString()});
        }
    }

    private void logEntriesCount() {
        LOGGER.log(Level.INFO,
                "Entries (Total Processed): {0}; Entries (Failed): {1}; Entries (Written): {2}",
                new Object[]{this.processedEntries, this.failedEntries, this.writtenEntries}
        );
    }

    private void onFileError(Path path, IOException error) throws IOException {
        this.processedEntries++;
        this.failedEntries++;

        LOGGER.log(Level.WARNING, "Failed: " + path.toString(), error);
        logEntriesCount();
    }

    private void onEntryStart(FileEntry entry) throws IOException {
        LOGGER.log(Level.INFO, "Now Processing: {0}", entry.getPath().toString());
        this.fileSizeDisplayed = false;
    }

    private void onEntryFinish(FileEntry entry) throws IOException {
        this.lastEntry = entry;
        this.processedEntries++;
        this.writtenEntries++;

        LOGGER.log(Level.INFO, "Finished: {0}", entry.getPath().toString());
        logEntriesCount();

        try {
            String metadata = System.getProperty(NAMESPACE + ".entry.metadata." + entry.getPath().toString());
            if (metadata != null) {
                File metadataFile = new File(metadata);
                if (metadataFile.isFile()) {
                    Properties properties = new Properties();
                    try (FileReader reader = new FileReader(metadataFile, StandardCharsets.UTF_8)) {
                        properties.load(reader);
                    }
                    for (Entry<Object, Object> e:properties.entrySet()) {
                        entry.getMetadata().writeString(PhantomPath.of(e.getKey().toString()), e.getValue().toString());
                    }
                    LOGGER.log(Level.INFO, "Metadata Set For {0}", entry.getPath().toString());
                } else {
                    throw new IOException("not a file.");
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Failed To Set Metadata Of "+entry.getPath().toString(), t);
        }
    }

    private void onEntryProgressUpdate(long current, long total) throws IOException {
        if (!this.fileSizeDisplayed) {
            LOGGER.log(Level.INFO, "File Size is: {0}", UIUtils.formatBytes(total));
            this.fileSizeDisplayed = true;
        }
    }

    private void onFinish() throws IOException {
        LOGGER.info("Done!");
        logEntriesCount();
        LOGGER.log(Level.INFO, "Total Size: {0}", UIUtils.formatBytes(this.lastEntry.getSize()));
        LOGGER.log(Level.INFO, "Files: {0}; Directories: {1} ", new Object[]{this.lastEntry.getFiles(), this.lastEntry.getDirectories()});
    }

    public void run() {
        if (this.running) {
            throw new RuntimeException("already running!");
        }
        this.running = true;

        try {
            int flags = 0;
            if (!WRITE_FILES_AND_DIRECTORIES_COUNT) {
                flags |= FileEntryWriter.FLAG_NO_FILES_AND_DIRECTORIES;
            }
            if (!ENABLE_SHA256) {
                flags |= FileEntryWriter.FLAG_NO_SHA256;
            }
            if (!ENABLE_SAMPLE || SAMPLE_SIZE == 0) {
                flags |= FileEntryWriter.FLAG_NO_SAMPLE;
            }
            if (!ENABLE_METADATA) {
                flags |= FileEntryWriter.FLAG_NO_METADATA;
            }
            if (!ENABLE_HEADER) {
                flags |= FileEntryWriter.FLAG_NO_HEADER;
                LOGGER.warning("Header is Disabled! This Will Cause Compatibility Issues!");
            }

            try (FileEntryWriter writer = new FileEntryWriter(new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(this.outputFile), StandardCharsets.UTF_8)), flags)) {
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
                FileEntryCreator entryCreator = new FileEntryCreator() {
                    @Override
                    protected void onEntryCreated(FileEntry entry) throws IOException, InterruptedException {
                        onEntryStart(entry);
                        onEntryProgressUpdate(0, 0);
                    }

                    @Override
                    protected void onEntryProgress(FileEntry entry, long bytes) throws IOException, InterruptedException {
                        onEntryProgressUpdate(bytes, entry.getSize());
                    }
                };

                entryCreator.setSha256Enabled(ENABLE_SHA256);
                entryCreator.setSampleSize(SAMPLE_SIZE);

                creator.setFileEntryCreator(entryCreator);
                creator.create(this.inputFiles);

                onFinish();
            }
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "Operation Failed!", ex);
        }
    }

}
