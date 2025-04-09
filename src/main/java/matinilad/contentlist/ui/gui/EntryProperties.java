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

import matinilad.contentlist.ContentEntry;
import matinilad.contentlist.ContentListUtils;
import matinilad.contentlist.ContentType;
import matinilad.contentlist.ui.UIUtils;

/**
 *
 * @author Cien
 */
@SuppressWarnings("serial")
public class EntryProperties extends javax.swing.JDialog {

    /**
     * Creates new form EntryProperties
     */
    public EntryProperties(ContentEntry[] entries, java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        initEntries(entries);
    }
    
    private String getFilesDirectoriesString(int files, int directories) {
        StringBuilder b = new StringBuilder();
        b.append(files);
        if (files != 1) {
            b.append(" Files, ");
        } else {
            b.append(" File, ");
        }
        b.append(directories);
        if (directories != 1) {
            b.append(" Directories");
        } else {
            b.append(" Directory");
        }
        return b.toString();
    }
    
    private void initEntries(ContentEntry[] entries) {
        this.createdField.setText("(unavailable)");
        this.nameField.setText("(unavailable)");
        this.filesDirectoriesField.setText("(unavailable)");
        this.modifiedField.setText("(unavailable)");
        this.pathField.setText("(unavailable)");
        this.sampleField.setText("(unavailable)");
        this.sha256Field.setText("(unavailable)");
        this.sizeField.setText("(unavailable)");
        this.typeField.setText("(unavailable)");
        
        this.createdField.setEnabled(false);
        this.nameField.setEnabled(false);
        this.filesDirectoriesField.setEnabled(false);
        this.modifiedField.setEnabled(false);
        this.pathField.setEnabled(false);
        this.sampleField.setEnabled(false);
        this.sha256Field.setEnabled(false);
        this.sizeField.setEnabled(false);
        this.typeField.setEnabled(false);
        
        if (entries.length == 0) {
            return;
        }
        
        if (entries.length > 1) {
            setTitle("Properties of Multiple Files");
            long totalSize = 0;
            int files = 0;
            int directories = 0;
            for (ContentEntry e:entries) {
                totalSize += e.getSize();
                switch (e.getType()) {
                    case DIRECTORY -> {
                        directories++;
                        files += e.getFiles();
                        directories += e.getDirectories();
                    }
                    default -> {
                        files++;
                    }
                }
            }
            this.sizeField.setEnabled(true);
            this.filesDirectoriesField.setEnabled(true);
            this.sizeField.setText(UIUtils.formatBytes(totalSize));
            this.filesDirectoriesField.setText(getFilesDirectoriesString(files, directories));
            return;
        }
        
        ContentEntry entry = entries[0];
        String name = entry.getPath().getName();
        if (name == null) {
            name = "(root)";
        }
        setTitle("Properties of "+name);
        
        this.nameField.setEnabled(true);
        this.pathField.setEnabled(true);
        this.typeField.setEnabled(true);
        this.createdField.setEnabled(true);
        this.modifiedField.setEnabled(true);
        this.sizeField.setEnabled(true);
        
        this.nameField.setText(name);
        this.pathField.setText(entry.getPath().getParent().toString());
        this.typeField.setText(entry.getType().toString());
        this.createdField.setText(UIUtils.asShortLocalizedDateTime(entry.getCreated()));
        this.modifiedField.setText(UIUtils.asShortLocalizedDateTime(entry.getModified()));
        this.sizeField.setText(UIUtils.formatBytes(entry.getSize()));
        
        if (ContentType.DIRECTORY.equals(entry.getType())) {
            this.filesDirectoriesField.setEnabled(true);
            this.filesDirectoriesField.setText(getFilesDirectoriesString(entry.getFiles(), entry.getDirectories()));
        }
        
        byte[] sha256 = entry.getSha256();
        if (sha256 != null) {
            this.sha256Field.setEnabled(true);
            this.sha256Field.setText(ContentListUtils.toHexString(sha256));
        }
        byte[] sample = entry.getSample();
        if (sample != null) {
            this.sampleField.setEnabled(true);
            this.sampleField.setText(ContentListUtils.toHexString(sample));
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        nameField = new javax.swing.JTextField();
        pathField = new javax.swing.JTextField();
        typeField = new javax.swing.JTextField();
        createdField = new javax.swing.JTextField();
        modifiedField = new javax.swing.JTextField();
        sizeField = new javax.swing.JTextField();
        sha256Field = new javax.swing.JTextField();
        sampleField = new javax.swing.JTextField();
        filesDirectoriesField = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Properties");
        setResizable(false);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("Name:");

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Path:");

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("Type:");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setText("Created On:");

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText("Modified On:");

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText("Size:");

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setText("SHA256:");

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel9.setText("Sample:");

        nameField.setEditable(false);
        nameField.setText("My File.txt");
        nameField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        pathField.setEditable(false);
        pathField.setText("/path/to/the/file");
        pathField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        typeField.setEditable(false);
        typeField.setText("FILE");
        typeField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        createdField.setEditable(false);
        createdField.setText("2024/12/05 16:64");
        createdField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        modifiedField.setEditable(false);
        modifiedField.setText("2024/12/05 16:64");
        modifiedField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        sizeField.setEditable(false);
        sizeField.setText("10 EB, 10TB, 10GB, 20MB, 1.3333333 Bytes");
        sizeField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        sha256Field.setEditable(false);
        sha256Field.setText("a4543e22d346dd71f7b0a69cafd689acb997f2385279928d0546543793a1b206");
        sha256Field.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        sampleField.setEditable(false);
        sampleField.setText("d1c46ae38b7afb5e01f8535fd0f454ecd837094ed4a60d203fcd801ba4e48d7b");
        sampleField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        sampleField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleFieldActionPerformed(evt);
            }
        });

        filesDirectoriesField.setEditable(false);
        filesDirectoriesField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        filesDirectoriesField.setText("10 Files, 10 Directories");
        filesDirectoriesField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(filesDirectoriesField)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nameField)
                            .addComponent(pathField)
                            .addComponent(typeField)
                            .addComponent(createdField)
                            .addComponent(modifiedField)
                            .addComponent(sizeField)
                            .addComponent(sha256Field, javax.swing.GroupLayout.DEFAULT_SIZE, 402, Short.MAX_VALUE)
                            .addComponent(sampleField))))
                .addGap(6, 6, 6))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(pathField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(typeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(createdField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(modifiedField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(sizeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(filesDirectoriesField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(sha256Field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(sampleField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void sampleFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sampleFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sampleFieldActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField createdField;
    private javax.swing.JTextField filesDirectoriesField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JTextField modifiedField;
    private javax.swing.JTextField nameField;
    private javax.swing.JTextField pathField;
    private javax.swing.JTextField sampleField;
    private javax.swing.JTextField sha256Field;
    private javax.swing.JTextField sizeField;
    private javax.swing.JTextField typeField;
    // End of variables declaration//GEN-END:variables
}
