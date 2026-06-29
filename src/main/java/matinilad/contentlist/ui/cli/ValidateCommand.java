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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Objects;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.entry.FileEntryReader;
import matinilad.contentlist.phantomfs.entry.FileEntryValidator;
import matinilad.contentlist.phantomfs.entry.FileEntryValidatorReason;
import matinilad.contentlist.phantomfs.entry.FileEntryValidatorResult;
import matinilad.contentlist.ui.UIUtils;

/**
 *
 * @author Cien
 */
public class ValidateCommand {

    private final PrintStream out;
    private final Path inputFile;
    private final Path rootDirectory;

    private boolean running = false;

    private long nextUpdate = System.currentTimeMillis();

    private FileEntry currentEntry = null;
    private boolean refused = false;
    private long current = 0;
    private long total = 0;
    private boolean ignoreWarnings = false;

    private int acceptedEntries = 0;
    private int refusedEntries = 0;

    public ValidateCommand(PrintStream out, Path inputFile, Path rootDirectory) {
        this.out = Objects.requireNonNull(out, "out is null");
        this.inputFile = Objects.requireNonNull(inputFile, "inputFile is null");
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory is null");;
    }

    private void onStart() throws IOException {
        this.out.println("Initializing...");
    }

    private void update() {
        if (System.currentTimeMillis() < this.nextUpdate) {
            return;
        }
        this.nextUpdate = System.currentTimeMillis() + 10000;

        this.out.println(this.acceptedEntries + " Accepted, " + this.refusedEntries + " Refused, " + (this.acceptedEntries + this.refusedEntries) + " Total.");
        this.out.println(" Current Path: " + this.currentEntry.getPath().toString());
        if (this.total == 0) {
            this.out.println("  --% Done");
        } else {
            double p = ((this.current / ((double) this.total)) * 100);
            this.out.println("  " + String.format("%.2f", p) + "% Done");
        }
    }

    private void onEntryStart(FileEntry entry, Path fileToValidate) throws IOException, InterruptedException {
        this.currentEntry = entry;
        this.refused = false;

        update();
    }

    private void onEntryAccepted(FileEntryValidatorReason reason, Object expected, Object found) throws IOException, InterruptedException {
        update();
    }

    private void onEntryRefused(FileEntryValidatorReason reason, Object expected, Object found) throws IOException, InterruptedException {
        this.refused = true;

        if (this.refusedEntries > 1000 && !this.ignoreWarnings) {
            this.out.println("Too many refused entries!");
            this.out.println("Warnings will now be ignored.");
            this.ignoreWarnings = true;
        }
        if (!this.ignoreWarnings) {
            HexFormat hex = HexFormat.of();

            this.out.println("Warning:");
            this.out.println(" Path: " + this.currentEntry.getPath().toString());
            this.out.println(" Was refused due to:");
            switch (reason) {
                case EXISTENCE -> {
                    out.println("  File does not exists.");
                }
                case TYPE -> {
                    out.println("  Expected type " + expected.toString() + ", found " + found.toString());
                }
                case SIZE -> {
                    out.println("  Expected size " + UIUtils.formatBytes((long) expected) + "; found " + UIUtils.formatBytes((long) found));
                }
                case SAMPLE -> {
                    out.println("  Expected sample " + hex.formatHex((byte[]) expected));
                    out.println("            found " + hex.formatHex((byte[]) found));
                }
                case HASH -> {
                    out.println("  Expected hash " + hex.formatHex((byte[]) expected));
                    out.println("          found " + hex.formatHex((byte[]) found));
                }
            }
        }

        update();
    }

    private void onEntryProgressUpdate(long current, long total) throws IOException, InterruptedException {
        this.current = current;
        this.total = total;

        update();
    }

    private void onEntryFinish() throws IOException, InterruptedException {
        if (this.refused) {
            this.refusedEntries++;
        } else {
            this.acceptedEntries++;
        }

        update();
    }

    private void onFinish() throws IOException, InterruptedException {
        this.out.println("Done!");
        this.out.println(" " + this.acceptedEntries + " Accepted, " + this.refusedEntries + " Refused");
        this.out.println("  " + (this.acceptedEntries + this.refusedEntries) + " Total");
    }

    public void run() {
        if (this.running) {
            throw new RuntimeException("already running!");
        }
        this.running = true;

        try {
            try (FileEntryReader reader = new FileEntryReader(new BufferedReader(new InputStreamReader(Files.newInputStream(this.inputFile), StandardCharsets.UTF_8)))) {
                onStart();

                FileEntry entry;
                while ((entry = reader.readEntry()) != null) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    FileEntryValidator validator = new FileEntryValidator(rootDirectory, entry) {
                        @Override
                        protected void onEntryAccepted(FileEntryValidatorReason reason) throws IOException, InterruptedException {
                            FileEntry e = getEntry();
                            switch (reason) {
                                case EXISTENCE -> {
                                    ValidateCommand.this.onEntryAccepted(reason, true, true);
                                }
                                case TYPE -> {
                                    ValidateCommand.this.onEntryAccepted(reason, e.getType(), e.getType());
                                }
                                case SIZE -> {
                                    ValidateCommand.this.onEntryAccepted(reason, e.getSize(), e.getSize());
                                }
                                case SAMPLE -> {
                                    ValidateCommand.this.onEntryAccepted(reason, e.getSample(), e.getSample());
                                }
                                case HASH -> {
                                    ValidateCommand.this.onEntryAccepted(reason, e.getSha256(), e.getSha256());
                                }
                            }
                        }

                        @Override
                        protected void onProgressUpdate(long bytes) throws IOException, InterruptedException {
                            onEntryProgressUpdate(bytes, getEntry().getSize());
                        }
                    };

                    onEntryStart(entry, validator.getPath());
                    onEntryProgressUpdate(0, 0);
                    FileEntryValidatorResult result = validator.validate();
                    if (!result.success()) {
                        onEntryRefused(
                                result.getReason(),
                                result.getExpectedValue(),
                                result.getFoundValue());
                    }
                    onEntryFinish();
                }

                onFinish();
            }
        } catch (IOException | InterruptedException ex) {
            this.out.println("Operation failed!");
            this.out.println(ex.getLocalizedMessage());
            ex.printStackTrace(this.out);
        }

    }
}
