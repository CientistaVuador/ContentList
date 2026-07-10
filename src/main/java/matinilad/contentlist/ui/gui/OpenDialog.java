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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import matinilad.contentlist.phantomfs.PhantomFileSystem;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.entry.FileEntryReader;

/**
 *
 * @author Cien
 */
@SuppressWarnings("serial")
public class OpenDialog extends StatusDialog {

    private static final Logger LOGGER = Logger.getLogger(OpenDialog.class.getName());

    private Thread thread = null;

    public OpenDialog(Frame parent, boolean modal) {
        super(parent, modal);
        init();
    }

    public OpenDialog(Dialog parent, boolean modal) {
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

    protected void onFileSystemReady(PhantomFileSystem fs) {

    }

    public boolean isOpening() {
        return this.thread != null;
    }

    public void open(Path path) {
        openObject(path);
    }

    public void open(File file) {
        openObject(file);
    }

    private String getFilePath(Object obj) {
        if (obj instanceof File f) {
            return f.toString();
        }
        if (obj instanceof Path p) {
            return p.toString();
        }
        if (obj == null) {
            throw new NullPointerException("obj is null");
        }
        throw new IllegalArgumentException("Unsupported file type: " + obj.getClass().getName());
    }

    private InputStream getFileStream(Object obj) throws FileNotFoundException, IOException {
        if (obj instanceof File f) {
            return new FileInputStream(f);
        }
        if (obj instanceof Path p) {
            return Files.newInputStream(p);
        }
        if (obj == null) {
            throw new NullPointerException("obj is null");
        }
        throw new IllegalArgumentException("Unsupported file type: " + obj.getClass().getName());
    }

    private long getFileSize(Object obj) throws IOException {
        if (obj instanceof File f) {
            return f.length();
        }
        if (obj instanceof Path p) {
            return Files.size(p);
        }
        if (obj == null) {
            throw new NullPointerException("obj is null");
        }
        throw new IllegalArgumentException("Unsupported file type: " + obj.getClass().getName());
    }

    private void openObject(Object obj) {
        if (this.thread != null) {
            return;
        }

        LOGGER.addHandler(getLoggerHandler());

        String filePath = getFilePath(obj);
        LOGGER.log(Level.INFO, "Now reading: {0}", filePath);

        setTitle(filePath);
        updateAndResetTime(filePath);
        getCurrentGlobalStatus().setText("");
        getCancelButton().setEnabled(true);

        this.thread = new Thread(() -> {
            try {
                try {
                    FileTransferStatus transferStatus = new FileTransferStatus();
                    transferStatus.setSize(getFileSize(obj));

                    Future<?> updateTask = null;

                    PhantomFileSystem fs = new PhantomFileSystem();
                    try (FileEntryReader reader = new FileEntryReader(new BufferedReader(new InputStreamReader(new StatusInputStream(transferStatus, getFileStream(obj)), StandardCharsets.UTF_8)))) {
                        int entryCount = 0;

                        FileEntry entry;
                        while ((entry = reader.readEntry()) != null) {
                            if (Thread.interrupted()) {
                                throw new InterruptedException();
                            }

                            fs.writeEntry(entry);
                            entryCount++;

                            if (updateTask == null || updateTask.isDone()) {
                                final float currentProgress = transferStatus.getProgress();
                                final PhantomPath currentEntryPath = entry.getPath();
                                final int currentEntryCount = entryCount;
                                updateTask = CompletableFuture.runAsync(() -> {
                                    try {
                                        SwingUtilities.invokeAndWait(() -> {
                                            setCurrentProgress(currentProgress);
                                            getCurrentItemStatus().setText(currentEntryPath.toString());
                                            getCurrentGlobalStatus().setText(currentEntryCount + (currentEntryCount == 1 ? " Entry " : " Entries"));
                                        });
                                    } catch (InterruptedException | InvocationTargetException ex) {
                                        LOGGER.log(Level.WARNING, "Error at update task", ex);
                                    }
                                });
                            }
                        }
                    }
                    
                    fs.validate();
                    
                    SwingUtilities.invokeLater(() -> {
                        setVisible(false);
                        if (this.thread != null) {
                            onFileSystemReady(fs);
                        }
                    });
                } catch (Throwable t) {
                    if (!(t instanceof InterruptedException)) {
                        LOGGER.log(Level.SEVERE, "Failed to read file!", t);
                        SwingUtilities.invokeLater(() -> {
                            Toolkit.getDefaultToolkit().beep();
                            getCancelButton().setEnabled(false);
                            JOptionPane.showMessageDialog(OpenDialog.this,
                                    "Failed to read file! Check log for details!",
                                    "Failed!",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        });
                    } else {
                        LOGGER.info("Canceled");
                    }
                }
            } finally {
                SwingUtilities.invokeLater(() -> {
                    this.thread = null;
                    LOGGER.removeHandler(getLoggerHandler());
                });
            }
        });
        this.thread.start();
    }
}
