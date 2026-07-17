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
package matinilad.contentlist.ui.gui;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.entry.FileEntryValidator;
import matinilad.contentlist.phantomfs.entry.FileEntryValidatorResult;
import matinilad.contentlist.ui.UIUtils;

/**
 *
 * @author Cien
 */
@SuppressWarnings("serial")
public class ValidateDialog extends StatusDialog {

    private static final Logger LOGGER = Logger.getLogger(ValidateDialog.class.getName());

    private Thread thread = null;

    public ValidateDialog(Frame parent, boolean modal) {
        super(parent, modal);
        init();
    }

    public ValidateDialog(Dialog parent, boolean modal) {
        super(parent, modal);
        init();
    }

    private void init() {
        getCancelButton().addActionListener((e) -> {
            if (this.thread != null) {
                this.thread.interrupt();
                this.thread = null;
            }
            setVisible(false);
        });
    }

    public boolean isValidating() {
        return this.thread != null;
    }

    public void validate(FileEntry[] entries, File rootDirectory) {
        validate(entries, rootDirectory.toPath());
    }

    public void validate(FileEntry[] entries, Path rootDirectory) {
        if (this.thread != null) {
            return;
        }
        Objects.requireNonNull(entries, "entries is null");
        for (int i = 0; i < entries.length; i++) {
            if (entries[i] == null) {
                throw new NullPointerException("entry at index " + i + " is null");
            }
        }
        Objects.requireNonNull(rootDirectory, "rootDirectory is null");

        LOGGER.addHandler(getLoggerHandler());

        LOGGER.log(Level.INFO, "Number of entries: {0}", entries.length);
        LOGGER.log(Level.INFO, "Root directory: {0}", rootDirectory.toString());

        StatusDialogFileItem item = new StatusDialogFileItem(this);

        setTitle(rootDirectory.toString());
        getCurrentGlobalStatus().setText("0 Success, 0 Failed (0 of " + entries.length + " total)");
        getCancelButton().setEnabled(true);
        item.updateDialog(true);

        this.thread = new Thread(() -> {
            try {
                try {
                    validateEntries(item, entries, rootDirectory);
                } catch (InterruptedException ex) {
                    LOGGER.info("Canceled");
                }
            } finally {
                SwingUtilities.invokeLater(() -> {
                    this.thread = null;
                    LOGGER.removeHandler(getLoggerHandler());
                });
            }
        });
        this.thread.setDaemon(true);
        this.thread.start();
    }

    private void validateEntries(StatusDialogFileItem item, FileEntry[] entries, Path root) throws InterruptedException {
        int success = 0;
        int failed = 0;
        for (int i = 0; i < entries.length; i++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            FileEntry entry = entries[i];
            final int entryIndex = i;

            item.reset();

            String entryPathString = entry.getPath().toString();
            item.setFileName(entryPathString);

            LOGGER.log(Level.INFO, "Now validating: {0}", entryPathString);
            item.updateDialog(false);

            try {
                FileEntryValidator validator = new FileEntryValidator(root, entry) {
                    @Override
                    protected void onFileSize(long bytes) throws IOException, InterruptedException {
                        item.setFileSize(bytes);
                    }

                    @Override
                    protected void onProgressUpdate(long bytes) throws IOException, InterruptedException {
                        item.setFileProgress(bytes);
                        item.updateDialog(false);
                    }
                };
                FileEntryValidatorResult result = validator.validate();

                if (result.success()) {
                    success++;
                    LOGGER.log(Level.INFO, "Entry {0} validated with success!", entryPathString);
                } else {
                    failed++;
                    Object expected = result.getExpectedValue();
                    Object found = result.getFoundValue();
                    HexFormat hex = HexFormat.of();
                    switch (result.getReason()) {
                        case EXISTENCE -> {
                            LOGGER.log(Level.WARNING, "Entry {0} validation failed! Reason: Does not exists!", entryPathString);
                        }
                        case TYPE -> {
                            LOGGER.log(Level.WARNING, "Entry {0} validation failed! Reason: Expected type {1}, Found {2}", new Object[]{entryPathString, expected, found});
                        }
                        case SIZE -> {
                            LOGGER.log(Level.WARNING, "Entry {0} validation failed! Reason: Wrong Size! Expected {1}; Found {2}", new Object[]{entryPathString, UIUtils.formatBytes((long) expected), UIUtils.formatBytes((long) found)});
                        }
                        case SAMPLE -> {
                            LOGGER.log(Level.WARNING, "Entry {0} validation failed! Reason: Wrong sample! Expected: {1} Found: {2}", new Object[]{entryPathString, hex.formatHex((byte[]) expected), hex.formatHex((byte[]) found)});
                        }
                        case HASH -> {
                            LOGGER.log(Level.WARNING, "Entry {0} validation failed! Reason: Wrong hash! Expected: {1} Found: {2}", new Object[]{entryPathString, hex.formatHex((byte[]) expected), hex.formatHex((byte[]) found)});
                        }
                    }
                }

                updateCurrentGlobalStatusAsync(success + " Success, " + failed + " Failed (" + (success + failed) + " of " + entries.length + " total)", entryIndex == (entries.length - 1));
            } catch (Throwable t) {
                if (t instanceof InterruptedException interruptedException) {
                    throw interruptedException;
                }
                LOGGER.log(Level.SEVERE, "Error while validating", t);
            }
        }

        int finalFailed = failed;
        int finalSuccess = success;

        SwingUtilities.invokeLater(() -> {
            getCancelButton().setEnabled(false);
            getCurrentItemName().setText("Done!");
            getCurrentItemStatus().setText("");
            setProgress(0f);

            Toolkit.getDefaultToolkit().beep();

            if (finalFailed == 0) {
                setVisible(false);
                dispose();

                if (finalSuccess == 1) {
                    JOptionPane.showMessageDialog(
                            getParent(),
                            "Entry "+entries[0].getPath().toString()+" validated with success!",
                            "Success!",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } else {
                    JOptionPane.showMessageDialog(
                            getParent(),
                            "All entries (" + finalSuccess + ") validated with success!",
                            "Success!",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });
    }
}
