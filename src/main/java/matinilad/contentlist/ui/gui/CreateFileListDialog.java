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

import java.awt.Toolkit;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;
import matinilad.contentlist.ContentEntry;
import matinilad.contentlist.ContentList;
import matinilad.contentlist.ContentListUtils;
import matinilad.contentlist.ContentPath;
import matinilad.contentlist.ContentType;
import matinilad.contentlist.ui.UIUtils;

/**
 *
 * @author Cien
 */
@SuppressWarnings("serial")
public class CreateFileListDialog extends javax.swing.JDialog implements ContentList.ContentListCallbacks {

    private static final AtomicLong instances = new AtomicLong(0);

    private final long instance = instances.getAndIncrement();

    private long initialTime = System.currentTimeMillis();
    private int directories = 0;
    private int files = 0;

    private final OutputStream output;
    private final Path[] inputPaths;
    private final Thread thread;

    public CreateFileListDialog(
            OutputStream output, Path[] inputPaths,
            java.awt.Frame parent, boolean modal
    ) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(null);
        this.output = output;
        this.inputPaths = inputPaths;
        this.thread = new Thread(() -> {
            try {
                this.run();
            } catch (Throwable t) {
                SwingUtilities.invokeLater(() -> {
                    onException(t);
                });
            }
            SwingUtilities.invokeLater(() -> {
                CreateFileListDialog.this.cancelButton.setEnabled(false);
            });
        });
    }

    private void log(int level, String text) {
        if (getParent() instanceof MainWindow w) {
            String[] lines = text.lines().toArray(String[]::new);
            for (String line : lines) {
                w.println(level, "C" + this.instance + " " + line);
            }
        }
    }

    private void run() throws Throwable {
        ContentList.create(this.output, this, this.inputPaths);
    }

    private void onException(Throwable t) {
        if (t == null || t instanceof InterruptedException) {
            return;
        }
        setTitle("Error!");
        Toolkit.getDefaultToolkit().beep();
        log(MainWindow.ERROR_LEVEL, "Fatal error!");
        log(MainWindow.ERROR_LEVEL, UIUtils.stacktraceOf(t));
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        currentEntry = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        progressLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lastEntry = new javax.swing.JTextArea();
        cancelButton = new javax.swing.JButton();
        entriesInformation = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Creating");
        setMinimumSize(new java.awt.Dimension(550, 380));
        setPreferredSize(new java.awt.Dimension(550, 380));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Current Entry"));

        currentEntry.setText("/current/entry/name/file/or/dir/a.exe");
        currentEntry.setMaximumSize(new java.awt.Dimension(484, 16));
        currentEntry.setMinimumSize(new java.awt.Dimension(484, 16));
        currentEntry.setPreferredSize(new java.awt.Dimension(484, 16));

        progressBar.setToolTipText("");
        progressBar.setValue(33);
        progressBar.setPreferredSize(new java.awt.Dimension(484, 29));

        progressLabel.setText("96.55% - 10TB 10GB 10MB 10KB 102 Bytes out of 10TB 10GB 10MB 12KB 150 Bytes - 150 MB/s");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(currentEntry, javax.swing.GroupLayout.DEFAULT_SIZE, 566, Short.MAX_VALUE)
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(progressLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(currentEntry, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Last Entry"));

        lastEntry.setEditable(false);
        lastEntry.setColumns(20);
        lastEntry.setRows(5);
        lastEntry.setText("/path/to/the/entry\nType: FILE\nCreated on: 12/54/87 12:45\nModified on: 12/65/98 65:12\nSize: 10GB 20MB 30KB 20 Bytes\nSHA256: asdasdasdasdasdasdasdasdas\nSample: asdasdasdasdasdasdasdasdas\n100 Files, 100 Directories");
        jScrollPane2.setViewportView(lastEntry);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
                .addContainerGap())
        );

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        entriesInformation.setText("1000 Files, 1000 Directories, 1000 Entries in total");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(entriesInformation, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(entriesInformation))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        if (this.thread.isAlive()) {
            setTitle("Canceled");
            log(MainWindow.INFO_LEVEL, "Canceled");
            this.thread.interrupt();
        } else {
            CreateFileListDialog.this.cancelButton.setEnabled(false);
        }
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        this.currentEntry.setText("");
        this.progressLabel.setText("");
        this.progressBar.setValue(0);
        this.lastEntry.setText("");
        this.entriesInformation.setText("");

        this.thread.start();
        log(MainWindow.INFO_LEVEL, "Processing thread initiated");
    }//GEN-LAST:event_formWindowOpened

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        if (this.thread.isAlive()) {
            log(MainWindow.INFO_LEVEL, "Canceled");
            this.thread.interrupt();
        }
    }//GEN-LAST:event_formWindowClosed

    @Override
    public void onStart() throws IOException, InterruptedException {
        SwingUtilities.invokeLater(() -> {
            log(MainWindow.INFO_LEVEL, "Running");
        });
    }

    @Override
    public void onFileUnreadable(Path path) throws IOException, InterruptedException {
        SwingUtilities.invokeLater(() -> {
            log(MainWindow.WARN_LEVEL, "Warning: " + path.toString() + " is unreadable");
        });
    }

    @Override
    public void onFileDuplicated(Path path) throws IOException, InterruptedException {
        SwingUtilities.invokeLater(() -> {
            log(MainWindow.WARN_LEVEL, "Warning: " + path.toString() + " is duplicated");
        });
    }

    @Override
    public void onEntryStart(ContentPath path) throws IOException, InterruptedException {
        SwingUtilities.invokeLater(() -> {
            this.initialTime = System.currentTimeMillis();
            CreateFileListDialog.this.currentEntry.setText(path.toString());
        });
    }

    @Override
    public void onEntryFinish(ContentEntry entry) throws IOException, InterruptedException {
        SwingUtilities.invokeLater(() -> {
            StringBuilder b = new StringBuilder();
            b.append(entry.getPath().toString()).append("\n");
            b.append("Type: ").append(entry.getType()).append("\n");
            b.append("Created on: ").append(UIUtils.asShortLocalizedDateTime(entry.getCreated())).append("\n");
            b.append("Modified on: ").append(UIUtils.asShortLocalizedDateTime(entry.getModified())).append("\n");
            b.append("Size: ").append(UIUtils.formatBytes(entry.getSize())).append("\n");
            if (entry.getType().equals(ContentType.DIRECTORY)) {
                b.append(entry.getFiles()).append(" Files, ").append(entry.getDirectories()).append(" Directories").append("\n");
            }
            byte[] sha256 = entry.getSha256();
            byte[] sample = entry.getSample();
            if (sha256 != null) {
                b.append("SHA256: ").append(ContentListUtils.toHexString(sha256)).append("\n");
            }
            if (sample != null) {
                b.append("Sample: ").append(ContentListUtils.toHexString(sample)).append("\n");
            }
            CreateFileListDialog.this.lastEntry.setText(b.toString());

            if (entry.getType().equals(ContentType.DIRECTORY)) {
                this.directories++;
            } else {
                this.files++;
            }
            CreateFileListDialog.this.entriesInformation.setText(this.files + " Files, " + this.directories + " Directories, " + (this.directories + this.files) + " Entries in total");
        });
    }

    @Override
    public void onEntryProgressUpdate(long current, long total) throws IOException, InterruptedException {
        SwingUtilities.invokeLater(() -> {
            if (total != 0) {
                CreateFileListDialog.this.progressBar.setValue((int) ((((double) current) / total) * 100));
            }
            long time = ((System.currentTimeMillis() - this.initialTime) / 1000);
            long averageSpeed;
            if (time != 0) {
                averageSpeed = current / time;
            } else {
                averageSpeed = 0;
            }
            CreateFileListDialog.this.progressLabel.setText(UIUtils.formatPercentage(current, total) + " - " + UIUtils.formatBytes(current) + " out of " + UIUtils.formatBytes(total) + " - " + UIUtils.formatSpeed(averageSpeed));
        });
    }

    @Override
    public void onFinish() throws IOException {
        SwingUtilities.invokeLater(() -> {
            setTitle("Done!");
            log(MainWindow.INFO_LEVEL, "Done!");
            CreateFileListDialog.this.cancelButton.setEnabled(false);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel currentEntry;
    private javax.swing.JLabel entriesInformation;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea lastEntry;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel progressLabel;
    // End of variables declaration//GEN-END:variables
}
