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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static final Logger LOGGER = Logger.getLogger(ValidateCommand.class.getName());

    static {
        if (!CLInterface.ENABLE_VERBOSE_LOGGING) {
            LOGGER.setLevel(Level.WARNING);
        }
    }

    private final Path inputFile;
    private final Path rootDirectory;

    private boolean running = false;
    private FileEntry currentEntry = null;
    private Path currentFile = null;
    private boolean refused = false;
    private boolean sizeDisplayed = false;
    
    private int acceptedEntries = 0;
    private int refusedEntries = 0;

    public ValidateCommand(Path inputFile, Path rootDirectory) {
        this.inputFile = Objects.requireNonNull(inputFile, "inputFile is null");
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory is null");;
    }

    private void onStart() throws IOException {
        LOGGER.info("Initializing...");
        LOGGER.log(Level.INFO, "Input File is: {0}", this.inputFile.toString());
        LOGGER.log(Level.INFO, "Root Directory is: {0}", this.rootDirectory.toString());
    }
    
    private void onEntryStart(FileEntry entry, Path fileToValidate) throws IOException, InterruptedException {
        this.currentEntry = entry;
        this.currentFile = fileToValidate;
        this.refused = false;
        this.sizeDisplayed = false;

        LOGGER.log(Level.INFO, "Now Validating: {0}; Path is: {1}", new Object[]{entry.getPath().toString(), fileToValidate.toString()});
    }

    private void onEntryAccepted(FileEntryValidatorReason reason, Object expected, Object found) throws IOException, InterruptedException {
        HexFormat hex = HexFormat.of();
        String reasonString = "";
        switch (reason) {
            case EXISTENCE -> {
                reasonString = "File Exists!";
            }
            case TYPE -> {
                reasonString = "File Type is Correct; "+expected.toString();
            }
            case SIZE -> {
                reasonString = "File Size is Correct; "+UIUtils.formatBytes((long) expected);
            }
            case SAMPLE -> {
                reasonString = "File Sample is Correct; "+hex.formatHex((byte[]) expected);
            }
            case HASH -> {
                reasonString = "File Hash is Correct; "+hex.formatHex((byte[]) expected);
            }
        }
        
        LOGGER.log(Level.INFO, "Accepted For {0}; {1}", new Object[]{reason.name(), reasonString});
    }

    private void onEntryRefused(FileEntryValidatorReason reason, Object expected, Object found) throws IOException, InterruptedException {
        this.refused = true;
        
        String reasonText = "";
        HexFormat hex = HexFormat.of();
        switch (reason) {
            case EXISTENCE -> {
                reasonText = "File Does Not Exists!";
            }
            case TYPE -> {
                reasonText = "Wrong Type; Expected "+expected.toString()+"; Found "+found.toString();
            }
            case SIZE -> {
                reasonText = "Wrong Size; Expected "+UIUtils.formatBytes((long) expected)+"; Found "+UIUtils.formatBytes((long) found);
            }
            case SAMPLE -> {
                reasonText = "Wrong Sample; Expected "+hex.formatHex((byte[]) expected)+"; Found "+hex.formatHex((byte[]) found);
            }
            case HASH -> {
                reasonText = "Wrong Hash; Expected "+hex.formatHex((byte[]) expected)+"; Found "+hex.formatHex((byte[]) found);
            }
        }
        
        LOGGER.log(Level.WARNING, "Refused! Entry {0}; File: {1}; {2}", new Object[]{this.currentEntry.getPath().toString(), this.currentFile.toString(), reasonText});
    }

    private void onEntryProgressUpdate(long current, long total) throws IOException, InterruptedException {
        if (!this.sizeDisplayed) {
            LOGGER.log(Level.INFO, "Now Reading File: {0}; Size is {1}", new Object[]{this.currentFile.toString(), UIUtils.formatBytes(total)});
            this.sizeDisplayed = true;
        }
    }
    
    private void onEntryFinish() throws IOException, InterruptedException {
        String result;
        if (this.refused) {
            this.refusedEntries++;
            result = "Failed!";
        } else {
            this.acceptedEntries++;
            result = "Success!";
        }
        
        LOGGER.log(Level.INFO, "Entry Finished Validating; {0}", result);
    }

    private void onFinish() throws IOException, InterruptedException {
        LOGGER.log(Level.INFO, "Done! {0} Refused; {1} Accepted; {2} In Total.", new Object[]{this.refusedEntries, this.acceptedEntries, this.acceptedEntries+this.refusedEntries});
    }
    
    public int run() {
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
            return this.refusedEntries;
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "Operation Failed!", ex);
            return -1;
        }
    }
}
