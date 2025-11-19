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

import java.awt.Desktop;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.PhantomFileSystem;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.ui.UIUtils;

/**
 *
 * @author Cien
 */
@SuppressWarnings("serial")
public class MainWindow extends javax.swing.JFrame {
    
    private static final Logger LOGGER = Logger.getLogger(MainWindow.class.getName());
    
    @Deprecated
    public static final int INFO_LEVEL = 0;
    @Deprecated
    public static final int ERROR_LEVEL = 1;
    @Deprecated
    public static final int WARN_LEVEL = 2;
    
    private StatusDialog log;
    private About about;
    
    private final ExecutorService searchThread = Executors.newSingleThreadExecutor();

    private final ImageIcon okIcon = new ImageIcon(MainWindow.class.getResource("check_ok.png"));
    private final ImageIcon failedIcon = new ImageIcon(MainWindow.class.getResource("check_failed.png"));

    private final ImageIcon infoIcon = new ImageIcon(MainWindow.class.getResource("info.png"));
    private final ImageIcon warnIcon = new ImageIcon(MainWindow.class.getResource("warn.png"));
    private final ImageIcon errorIcon = new ImageIcon(MainWindow.class.getResource("error.png"));
    private final ImageIcon themeIcon = new ImageIcon(MainWindow.class.getResource("theme.png"));
    
    private boolean restartWarningEmmited = false;

    private File rootDirectorySuggestion = null;
    private File rootDirectory = null;

    private PhantomPath currentPath = null;
    private PhantomFileSystem fileSystem = null;

    private PhantomPathTableModel fileSystemTableModel = null;
    private PhantomPathTableModel searchTableModel = null;
    private boolean searchModeEnabled = false;
    private Future<?> searchThreadTask = null;
    private long searchId = 0;

    private final Timer searchTimer = new Timer(500, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            updateSearch();
        }
    });

    {
        this.searchTimer.setRepeats(false);
    }

    /**
     * Creates new form MainWindow
     */
    public MainWindow() {
        initComponents();
        setupThemes();
        setupDialogs();
        setLocationRelativeTo(null);
    }
    
    private void setupDialogs() {
        this.log = new StatusDialog(this, true);
        this.log.removeStatusPanel();
        this.log.setTitle("Log");
        this.about = new About(this, true, UIUtils.about());
        
        Logger.getLogger("").addHandler(this.log.getLoggerHandler());
    }
    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        filePopupMenu = new javax.swing.JPopupMenu();
        systemMenu = new javax.swing.JMenu();
        existsSystemMenu = new javax.swing.JMenu();
        openFileButton = new javax.swing.JMenuItem();
        openDirectoryButton = new javax.swing.JMenuItem();
        copySystemButton = new javax.swing.JMenuItem();
        validateFileButton = new javax.swing.JMenuItem();
        moveToTrashButton = new javax.swing.JMenuItem();
        openLocationButton = new javax.swing.JMenuItem();
        filePropertiesButton = new javax.swing.JMenuItem();
        jScrollPane1 = new javax.swing.JScrollPane();
        fileTableList = new javax.swing.JTable();
        jToolBar1 = new javax.swing.JToolBar();
        jButton4 = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jLabel2 = new javax.swing.JLabel();
        pathField = new javax.swing.JTextField();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        jLabel1 = new javax.swing.JLabel();
        searchField = new javax.swing.JTextField();
        bottomInfoField = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        createButton = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        openButton = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        rootDirectoryButton = new javax.swing.JMenuItem();
        themeMenu = new javax.swing.JMenu();
        jMenu5 = new javax.swing.JMenu();
        caseSensitiveSearch = new javax.swing.JCheckBoxMenuItem();
        exactSearch = new javax.swing.JCheckBoxMenuItem();
        jMenu3 = new javax.swing.JMenu();
        logButton = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        aboutButton = new javax.swing.JMenuItem();

        filePopupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                filePopupMenuPopupMenuWillBecomeVisible(evt);
            }
        });

        systemMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/system.png"))); // NOI18N
        systemMenu.setText("System");

        existsSystemMenu.setText("Exists");
        systemMenu.add(existsSystemMenu);

        openFileButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/system_open.png"))); // NOI18N
        openFileButton.setText("Open");
        openFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFileButtonActionPerformed(evt);
            }
        });
        systemMenu.add(openFileButton);

        openDirectoryButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/folder.png"))); // NOI18N
        openDirectoryButton.setText("Open Directory");
        openDirectoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openDirectoryButtonActionPerformed(evt);
            }
        });
        systemMenu.add(openDirectoryButton);

        copySystemButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/save.png"))); // NOI18N
        copySystemButton.setText("Copy");
        copySystemButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copySystemButtonActionPerformed(evt);
            }
        });
        systemMenu.add(copySystemButton);

        validateFileButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/validate.png"))); // NOI18N
        validateFileButton.setText("Validate");
        validateFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validateFileButtonActionPerformed(evt);
            }
        });
        systemMenu.add(validateFileButton);

        moveToTrashButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/trash_can.png"))); // NOI18N
        moveToTrashButton.setText("Move to Trash");
        moveToTrashButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveToTrashButtonActionPerformed(evt);
            }
        });
        systemMenu.add(moveToTrashButton);

        filePopupMenu.add(systemMenu);

        openLocationButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/folder.png"))); // NOI18N
        openLocationButton.setText("Open Location");
        openLocationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openLocationButtonActionPerformed(evt);
            }
        });
        filePopupMenu.add(openLocationButton);

        filePropertiesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/log.png"))); // NOI18N
        filePropertiesButton.setText("Properties");
        filePropertiesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filePropertiesButtonActionPerformed(evt);
            }
        });
        filePopupMenu.add(filePropertiesButton);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Content List 2.0");
        setIconImage(FrameIcon.getIcon());

        fileTableList.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        fileTableList.setComponentPopupMenu(filePopupMenu);
        fileTableList.setEnabled(false);
        this.jScrollPane1.setTransferHandler(new FileDragAndDrop() {
            @Override
            protected boolean process(List<File> files) {
                openFileSystemCSV(files.get(0));
                return true;
            }
        });
        fileTableList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fileTableListMouseClicked(evt);
            }
        });
        fileTableList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                fileTableListKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(fileTableList);

        jToolBar1.setRollover(true);

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/info.png"))); // NOI18N
        jButton4.setToolTipText("Info");
        jButton4.setFocusable(false);
        jButton4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton4.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton4);
        jToolBar1.add(jSeparator3);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/home.png"))); // NOI18N
        jButton1.setToolTipText("Home");
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton1);

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/return.png"))); // NOI18N
        jButton2.setToolTipText("Return");
        jButton2.setFocusable(false);
        jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton2);

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/next.png"))); // NOI18N
        jButton3.setToolTipText("Next");
        jButton3.setFocusable(false);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton3);
        jToolBar1.add(jSeparator1);

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/folder.png"))); // NOI18N
        jLabel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jToolBar1.add(jLabel2);

        pathField.setEnabled(false);
        pathField.setMaximumSize(new java.awt.Dimension(3000, 2147483647));
        pathField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                pathFieldFocusLost(evt);
            }
        });
        pathField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                pathFieldKeyPressed(evt);
            }
        });
        jToolBar1.add(pathField);
        jToolBar1.add(jSeparator2);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/search.png"))); // NOI18N
        jLabel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jToolBar1.add(jLabel1);

        searchField.setEnabled(false);
        searchField.setMaximumSize(new java.awt.Dimension(1024, 2147483647));
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                searchFieldKeyTyped(evt);
            }
        });
        jToolBar1.add(searchField);

        bottomInfoField.setEditable(false);
        bottomInfoField.setText("Hello!");
        bottomInfoField.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        jMenu1.setText("File");

        createButton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        createButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/create.png"))); // NOI18N
        createButton.setText("New");
        createButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createButtonActionPerformed(evt);
            }
        });
        jMenu1.add(createButton);
        jMenu1.add(jSeparator4);

        openButton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        openButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/open.png"))); // NOI18N
        openButton.setText("Open");
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });
        jMenu1.add(openButton);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        rootDirectoryButton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        rootDirectoryButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/folder.png"))); // NOI18N
        rootDirectoryButton.setText("Root Directory");
        rootDirectoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rootDirectoryButtonActionPerformed(evt);
            }
        });
        jMenu2.add(rootDirectoryButton);

        themeMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/theme.png"))); // NOI18N
        themeMenu.setText("Theme");
        jMenu2.add(themeMenu);

        jMenu5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/search.png"))); // NOI18N
        jMenu5.setText("Search");

        caseSensitiveSearch.setText("Case Sensitive");
        caseSensitiveSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                caseSensitiveSearchActionPerformed(evt);
            }
        });
        jMenu5.add(caseSensitiveSearch);

        exactSearch.setText("Exact Search");
        exactSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exactSearchActionPerformed(evt);
            }
        });
        jMenu5.add(exactSearch);

        jMenu2.add(jMenu5);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("View");

        logButton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        logButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/log.png"))); // NOI18N
        logButton.setText("Log");
        logButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logButtonActionPerformed(evt);
            }
        });
        jMenu3.add(logButton);

        jMenuBar1.add(jMenu3);

        jMenu4.setText("Help");

        aboutButton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        aboutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/matinilad/contentlist/ui/gui/info.png"))); // NOI18N
        aboutButton.setText("About");
        aboutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutButtonActionPerformed(evt);
            }
        });
        jMenu4.add(aboutButton);

        jMenuBar1.add(jMenu4);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 688, Short.MAX_VALUE)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(bottomInfoField))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 397, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bottomInfoField, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void updateFileTable() {
        PhantomPath[] files = this.fileSystem.listFiles(this.currentPath, true);
        if (!(this.fileTableList.getModel() instanceof PhantomPathTableModel)) {
            this.fileSystemTableModel = new PhantomPathTableModel(this.fileSystem, false);
            this.fileTableList.setModel(this.fileSystemTableModel);
        }
        ((PhantomPathTableModel) this.fileTableList.getModel()).updatePaths(files);
        this.fileTableList.setEnabled(true);
    }
    
    public void updateBottomInfoField() {
        String separator = "  |  ";
        
        FileEntry root = this.fileSystem.getEntry(PhantomPath.of("/"));
        FileEntry current = this.fileSystem.getEntry(this.currentPath);
        
        String rootName = root.getMetadataName();
        if (rootName == null) {
            rootName = "Unnamed";
        }
        String currentName = current.getPath().getName();
        if (currentName == null) {
            currentName = "Root";
        }
        
        this.bottomInfoField.setText(new StringBuilder()
                .append(rootName).append(separator)
                .append(currentName).append(separator)
                .append(current.getFiles()).append(" Files").append(separator)
                .append(current.getDirectories()).append(" Directories").append(separator)
                .append(UIUtils.formatBytes(current.getSize()))
                .toString());
    }
    
    public void openFileSystem(PhantomFileSystem fs) {
        this.fileSystem = fs;
        this.currentPath = PhantomPath.of("/");
        this.searchField.setEnabled(true);
        this.pathField.setEnabled(true);
        this.searchField.setText("");
        this.pathField.setText(this.currentPath.toString());
        this.fileSystemTableModel = null;
        this.searchTableModel = null;
        this.fileTableList.setModel(new DefaultTableModel());
        this.searchModeEnabled = false;
        if (this.searchThreadTask != null) {
            this.searchThreadTask.cancel(true);
            this.searchThreadTask = null;
        }
        
        updateBottomInfoField();
        updateFileTable();
    }

    private void openFileSystemCSV(File file) {
        file = file.getAbsoluteFile();
        this.rootDirectorySuggestion = file.getParentFile();
        
        LOGGER.log(Level.INFO, "input file: {0}", file.toString());
        LOGGER.log(Level.INFO, "root directory suggestion: {0}", this.rootDirectorySuggestion);
        
        OpenFileSystemDialog s = new OpenFileSystemDialog(file, this, true);
        s.setLocationRelativeTo(this);
        s.setVisible(true);
    }

    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileFilter csvFile = new FileNameExtensionFilter("CSV File (.csv)", "csv");
        chooser.setFileFilter(csvFile);
        chooser.setMultiSelectionEnabled(false);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            openFileSystemCSV(selected);
        }
    }//GEN-LAST:event_openButtonActionPerformed

    private void logButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logButtonActionPerformed
        this.log.setLocationRelativeTo(this);
        this.log.setVisible(true);
    }//GEN-LAST:event_logButtonActionPerformed

    @Deprecated
    public void println(int level, String string) {
        switch (level) {
            case INFO_LEVEL -> {
                LOGGER.log(Level.INFO, string);
            }
            case WARN_LEVEL -> {
                LOGGER.log(Level.WARNING, string);
            }
            case ERROR_LEVEL -> {
                LOGGER.log(Level.SEVERE, string);
            }
            default -> {
                LOGGER.log(Level.INFO, string);
            }
        }
        
    }

    @SuppressWarnings("serial")
    static class ThemeJMenuItem extends JMenuItem {

        private final UIManager.LookAndFeelInfo lookAndFeelInfo;

        public ThemeJMenuItem(UIManager.LookAndFeelInfo lookAndFeelInfo) {
            super(lookAndFeelInfo.getName());
            this.lookAndFeelInfo = lookAndFeelInfo;
        }

        public UIManager.LookAndFeelInfo getLookAndFeelInfo() {
            return lookAndFeelInfo;
        }

    }

    private void updateLookAndFeel() {
        SwingUtilities.updateComponentTreeUI(this);
        for (Window w : this.getOwnedWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
        }

        this.revalidate();
        for (Window w : this.getOwnedWindows()) {
            w.revalidate();
        }

        this.fileTableList.setShowGrid(false);
        this.fileTableList.setShowHorizontalLines(false);
        this.fileTableList.setShowVerticalLines(false);
    }

    private void setupThemes() {
        try {
            Path themeFile = Path.of("cl_theme.txt");
            if (Files.exists(themeFile) && Files.isRegularFile(themeFile)) {
                String classname = Files.readString(themeFile, StandardCharsets.UTF_8).trim();
                try {
                    UIManager.setLookAndFeel(classname);
                    updateLookAndFeel();
                } catch (ClassNotFoundException
                        | InstantiationException
                        | IllegalAccessException
                        | UnsupportedLookAndFeelException ex) {
                    LOGGER.log(Level.WARNING, "failed to set theme from theme file!", ex);
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "failed to read theme file!", ex);
        }

        String currentTheme = UIManager.getLookAndFeel().getClass().getName();
        UIManager.LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo info : infos) {
            ThemeJMenuItem menuItem = new ThemeJMenuItem(info);
            menuItem.addActionListener(this::onThemeButtonPressed);
            if (info.getClassName().equals(currentTheme)) {
                menuItem.setIcon(this.themeIcon);
            }
            this.themeMenu.add(menuItem);
        }
    }

    private void onThemeButtonPressed(java.awt.event.ActionEvent evt) {
        ThemeJMenuItem themeButton = (ThemeJMenuItem) evt.getSource();
        try {
            String classname = themeButton.getLookAndFeelInfo().getClassName();
            if (UIManager.getLookAndFeel().getClass().getName().equals(classname)) {
                return;
            }
            UIManager.setLookAndFeel(classname);
            updateLookAndFeel();

            for (int i = 0; i < this.themeMenu.getItemCount(); i++) {
                JMenuItem otherTheme = this.themeMenu.getItem(i);
                if (otherTheme.getIcon() != null) {
                    otherTheme.setIcon(null);
                }
            }

            themeButton.setIcon(this.themeIcon);

            try {
                Files.writeString(Path.of("cl_theme.txt"), classname, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                Toolkit.getDefaultToolkit().beep();
                LOGGER.log(Level.WARNING, "failed to write theme file!", ex);
            }

            if (!this.restartWarningEmmited) {
                Toolkit.getDefaultToolkit().beep();
                this.restartWarningEmmited = true;
                JOptionPane.showMessageDialog(
                        this,
                        "It is highly recommended to restart after changing the theme",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        } catch (ClassNotFoundException
                | InstantiationException
                | IllegalAccessException
                | UnsupportedLookAndFeelException ex) {
            Toolkit.getDefaultToolkit().beep();
            LOGGER.log(Level.WARNING, "failed to change theme!", ex);
        }
    }

    private void createButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createButtonActionPerformed
        NewCreateDialog create = new NewCreateDialog(this, true);
        create.setVisible(true);
    }//GEN-LAST:event_createButtonActionPerformed

    private void aboutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutButtonActionPerformed
        this.about.setLocationRelativeTo(this);
        this.about.setVisible(true);
    }//GEN-LAST:event_aboutButtonActionPerformed

    private void rootDirectoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rootDirectoryButtonActionPerformed
        File dir = this.rootDirectorySuggestion != null ? this.rootDirectorySuggestion : this.rootDirectory;
        JFileChooser chooser = new JFileChooser(dir);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            this.rootDirectory = chooser.getSelectedFile();
            this.rootDirectorySuggestion = null;
            LOGGER.log(Level.INFO, "root directory: {0}", this.rootDirectory.toString());
        }
    }//GEN-LAST:event_rootDirectoryButtonActionPerformed
    
    private File getRootDirectory() {
        File dir = this.rootDirectory;
        if (dir == null) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(
                    this,
                    "Root Directory not set, use Edit -> Root Directory to set it.",
                    "Root Directory not set.",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }
        return dir;
    }

    private PhantomPath[] getSelectedPaths() {
        if (!(this.fileTableList.getModel() instanceof PhantomPathTableModel)) {
            return new PhantomPath[0];
        }
        PhantomPathTableModel table = (PhantomPathTableModel) this.fileTableList.getModel();
        int[] selectedRows = this.fileTableList.getSelectedRows();
        PhantomPath[] selected = new PhantomPath[selectedRows.length];
        for (int i = 0; i < selected.length; i++) {
            selected[i] = table.getContentPath(selectedRows[i]);
        }
        if (selected.length == 1) {
            return selected;
        }
        List<PhantomPath> filtered = new ArrayList<>();
        for (PhantomPath e:selected) {
            if (e.hasSpecialLinks()) {
                continue;
            }
            filtered.add(e);
        }
        return filtered.toArray(PhantomPath[]::new);
    }

    private File[] getSelectedFiles() {
        File base = getRootDirectory();
        if (base == null) {
            return null;
        }
        PhantomPath[] selectedPaths = getSelectedPaths();
        File[] selectedFiles = new File[selectedPaths.length];
        for (int i = 0; i < selectedPaths.length; i++) {
            selectedFiles[i] = this.fileSystem.toRealPath(selectedPaths[i]).resolveToPath(base.toPath()).toFile();
        }
        return selectedFiles;
    }

    private void showFileNotFoundMessage(String message, boolean beep) {
        if (beep) {
            Toolkit.getDefaultToolkit().beep();
        }
        JOptionPane.showMessageDialog(
                this,
                message,
                "File not found",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void openFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFileButtonActionPerformed
        File[] selected = getSelectedFiles();
        if (selected == null) {
            return;
        }
        if (selected.length == 0) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        boolean error = false;
        for (File sel : selected) {
            if (!sel.exists()) {
                LOGGER.log(Level.WARNING, "file not found: {0}", sel.toString());
                if (selected.length == 1) {
                    showFileNotFoundMessage(sel.toString(), true);
                    break;
                }
                error = true;
                continue;
            }
            try {
                Desktop.getDesktop().open(sel);
            } catch (IOException | UnsupportedOperationException ex) {
                LOGGER.log(Level.WARNING, "failed to open: "+sel.toString(), ex);
                error = true;
            }
        }
        if (error) {
            Toolkit.getDefaultToolkit().beep();
        }
    }//GEN-LAST:event_openFileButtonActionPerformed

    private void doubleClickOnRow(JTable table, int modelRow) {
        PhantomPath p = ((PhantomPathTableModel) table.getModel()).getContentPath(modelRow);
        if (this.fileSystem.isDirectory(p)) {
            updateCurrentPath(p);
        } else {
            openFileButtonActionPerformed(null);
        }
    }

    private void fileTableListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fileTableListMouseClicked
        JTable table = (JTable) evt.getSource();
        Point point = evt.getPoint();
        int row = table.rowAtPoint(point);
        if (evt.getClickCount() == 2 && table.getSelectedRow() != -1 && row != -1) {
            int modelRow = table.convertRowIndexToModel(row);
            doubleClickOnRow(table, modelRow);
        }
    }//GEN-LAST:event_fileTableListMouseClicked

    private void filePopupMenuPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_filePopupMenuPopupMenuWillBecomeVisible
        {
            boolean enableOptions = this.fileTableList.getSelectedRowCount() != 0;
            for (int i = 0; i < this.filePopupMenu.getComponentCount(); i++) {
                this.filePopupMenu.getComponent(i).setEnabled(enableOptions);
            }
        }

        {
            boolean enableGotoDirectory = this.fileTableList.getSelectedRowCount() == 1;
            this.openLocationButton.setEnabled(enableGotoDirectory);
        }

        {
            this.existsSystemMenu.removeAll();
            PhantomPath[] selected = getSelectedPaths();
            boolean checkFailed = false;
            int existsConfirmed = 0;
            for (PhantomPath s : selected) {
                boolean exists = false;
                if (this.rootDirectory != null) {
                    Path p = s.resolveToPath(this.rootDirectory.toPath());
                    exists = Files.exists(p);
                }

                String name = this.fileSystem.toRealPath(s).getName();
                if (name == null) {
                    name = "(root)";
                }
                JMenuItem menuItem = new JMenuItem(name);
                if (exists) {
                    menuItem.setIcon(this.okIcon);
                    existsConfirmed++;
                } else {
                    menuItem.setIcon(this.failedIcon);
                    checkFailed = true;
                }

                menuItem.addActionListener((e) -> {
                    File base = getRootDirectory();
                    if (base == null) {
                        return;
                    }
                    Path p = s.resolveToPath(base.toPath());
                    if (!Files.exists(p)) {
                        showFileNotFoundMessage(p.toString(), true);
                        return;
                    }
                    try {
                        Desktop.getDesktop().open(p.toFile());
                    } catch (IOException | UnsupportedOperationException ex) {
                        Toolkit.getDefaultToolkit().beep();
                        LOGGER.log(Level.WARNING, "failed to open: "+p.toString(), ex);
                    }
                });

                this.existsSystemMenu.add(menuItem);
            }
            this.existsSystemMenu.setText("Exists (" + existsConfirmed + "/" + selected.length + ")");
            if (checkFailed) {
                this.existsSystemMenu.setIcon(this.failedIcon);
            } else {
                this.existsSystemMenu.setIcon(this.okIcon);
            }
        }
        
        {
            this.openLocationButton.setEnabled(
                    this.searchModeEnabled && this.fileTableList.getSelectedRowCount() == 1
            );
        }
    }//GEN-LAST:event_filePopupMenuPopupMenuWillBecomeVisible

    private void openDirectoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDirectoryButtonActionPerformed
        File[] selected = getSelectedFiles();
        if (selected == null) {
            return;
        }
        if (selected.length == 0) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        Set<File> toOpen = new HashSet<>();
        for (File sel : selected) {
            sel = sel.getParentFile();
            if (!toOpen.contains(sel)) {
                toOpen.add(sel);
            }
        }
        selected = toOpen.toArray(File[]::new);

        boolean error = false;
        for (File sel : selected) {
            if (!sel.isDirectory()) {
                LOGGER.log(Level.WARNING, "directory not found: {0}", sel.toString());
                if (selected.length == 1) {
                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(
                            this,
                            sel.toString(),
                            "Directory not found",
                            JOptionPane.ERROR_MESSAGE
                    );
                    break;
                }
                error = true;
                continue;
            }
            try {
                Desktop.getDesktop().open(sel);
            } catch (IOException | UnsupportedOperationException ex) {
                LOGGER.log(Level.WARNING, "failed to open: "+sel.toString(), ex);
                error = true;
            }
        }
        if (error) {
            Toolkit.getDefaultToolkit().beep();
        }
    }//GEN-LAST:event_openDirectoryButtonActionPerformed

    private void moveToTrashButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveToTrashButtonActionPerformed
        File[] selected = getSelectedFiles();
        if (selected == null) {
            return;
        }
        if (selected.length == 0) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        String stringToShow;
        if (selected.length == 1) {
            stringToShow = "Delete " + selected[0].getName() + "?";
        } else {
            stringToShow = "Delete " + selected.length + " Files?";
        }
        int result = JOptionPane.showConfirmDialog(this, stringToShow);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        boolean error = false;
        for (File sel : selected) {
            if (!sel.exists()) {
                LOGGER.log(Level.WARNING, "file not found: {0}", sel.toString());
                if (selected.length == 1) {
                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(
                            this,
                            sel.toString(),
                            "File not found",
                            JOptionPane.ERROR_MESSAGE
                    );
                    break;
                }
                error = true;
                continue;
            }
            try {
                Desktop.getDesktop().moveToTrash(sel);
                LOGGER.log(Level.INFO, "deleted: {0}", sel.toString());
            } catch (UnsupportedOperationException ex) {
                LOGGER.log(Level.WARNING, "failed to delete: "+sel.toString(), ex);
                error = true;
            }
        }
        if (error) {
            Toolkit.getDefaultToolkit().beep();
        }
    }//GEN-LAST:event_moveToTrashButtonActionPerformed

    private void validateFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validateFileButtonActionPerformed
        File base = getRootDirectory();
        if (base == null) {
            return;
        }
        PhantomPath[] selectedPaths = getSelectedPaths();
        if (selectedPaths.length == 0) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        FileEntry[] entries = this.fileSystem.listEntries(selectedPaths);
        PathValidateDialog dialog = new PathValidateDialog(entries, base.toPath(), this, false);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }//GEN-LAST:event_validateFileButtonActionPerformed

    private void filePropertiesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filePropertiesButtonActionPerformed
        PhantomPath[] selectedPaths = getSelectedPaths();
        List<FileEntry> selectedEntries = new ArrayList<>();
        for (PhantomPath p : selectedPaths) {
            FileEntry e = this.fileSystem.getEntry(p);
            if (e != null) {
                selectedEntries.add(e);
            }
        }
        if (selectedEntries.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        EntryProperties properties = new EntryProperties(selectedEntries.toArray(FileEntry[]::new), this, false);
        properties.setLocationRelativeTo(this);
        properties.setVisible(true);
    }//GEN-LAST:event_filePropertiesButtonActionPerformed

    private void updateCurrentPath(PhantomPath newPath) {
        if (this.searchModeEnabled) {
            this.searchModeEnabled = false;
            onSearchDisabled();
        }
        this.currentPath = this.fileSystem.toRealPath(newPath);
        this.pathField.setText(this.currentPath.toString());
        updateBottomInfoField();
        updateFileTable();
    }

    private void openLocationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openLocationButtonActionPerformed
        PhantomPath[] selectedPaths = getSelectedPaths();
        if (selectedPaths.length == 0) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        PhantomPath selected = this.fileSystem.toRealPath(selectedPaths[0]);
        PhantomPath directory = selected.getParent();
        updateCurrentPath(directory);

        if (this.fileTableList.getModel() instanceof PhantomPathTableModel model) {
            int selectedIndex = -1;
            for (int i = 0; i < model.getRowCount(); i++) {
                if (model.getContentPath(i).equals(selected)) {
                    selectedIndex = i;
                    break;
                }
            }
            this.fileTableList.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
        }
    }//GEN-LAST:event_openLocationButtonActionPerformed

    public static class FileTransferable implements Transferable {

        private final List<File> listOfFiles;

        public FileTransferable(List<File> listOfFiles) {
            this.listOfFiles = listOfFiles;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            return listOfFiles;
        }
    }

    private void copySystemButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copySystemButtonActionPerformed
        File[] selected = getSelectedFiles();
        if (selected == null) {
            return;
        }
        if (selected.length == 0) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new FileTransferable(Arrays.asList(selected)), new ClipboardOwner() {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable contents) {

            }
        });
    }//GEN-LAST:event_copySystemButtonActionPerformed

    private void fileTableListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_fileTableListKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
            moveToTrashButtonActionPerformed(null);
            evt.consume();
        }
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            int row = this.fileTableList.getSelectedRow();
            if (row == -1) {
                Toolkit.getDefaultToolkit().beep();
            } else {
                doubleClickOnRow(this.fileTableList, row);
            }
            evt.consume();
        }
        if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_C) {
            copySystemButtonActionPerformed(null);
            evt.consume();
        }
    }//GEN-LAST:event_fileTableListKeyPressed

    private void pathFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_pathFieldFocusLost
        this.pathField.setText(this.currentPath.toString());
    }//GEN-LAST:event_pathFieldFocusLost

    private void pathFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_pathFieldKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            PhantomPath newPath;
            try {
                newPath = PhantomPath.of(this.pathField.getText());
            } catch (IllegalArgumentException ex) {
                LOGGER.log(Level.WARNING, "invalid path: {0}", this.pathField.getText());
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(
                        this,
                        ex.getLocalizedMessage(),
                        "Invalid path!",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            if (newPath.isRelative()) {
                newPath = this.currentPath.resolve(newPath);
            }

            if (!this.fileSystem.exists(newPath)) {
                LOGGER.log(Level.WARNING, "file not found: {0}", this.pathField.getText());
                showFileNotFoundMessage(this.pathField.getText(), true);
                return;
            }
            
            newPath = this.fileSystem.toRealPath(newPath);
            if (!this.fileSystem.isDirectory(newPath)) {
                File base = getRootDirectory();
                if (base == null) {
                    return;
                }
                File resolved = newPath.resolveToPath(base.toPath()).toFile();
                if (!resolved.exists()) {
                    LOGGER.log(Level.WARNING, "file not found: {0}", resolved.toString());
                    showFileNotFoundMessage(resolved.toString(), true);
                    return;
                }
                try {
                    Desktop.getDesktop().open(resolved);
                } catch (IOException | UnsupportedOperationException ex) {
                    Toolkit.getDefaultToolkit().beep();
                    LOGGER.log(Level.WARNING, "failed to open: "+resolved.toString(), ex);
                }
                return;
            }

            updateCurrentPath(newPath);
        }
    }//GEN-LAST:event_pathFieldKeyPressed

    private void updateSearch() {
        this.searchModeEnabled = !this.searchField.getText().isEmpty();
        if (this.searchModeEnabled) {
            onSearchEnabled();
        } else {
            onSearchDisabled();
        }
    }

    private void onSearchThreadException(Throwable t) {
        Toolkit.getDefaultToolkit().beep();
        LOGGER.log(Level.SEVERE, "search thread error:", t);
    }
    
    private void onSearchEnabled() {
        this.pathField.setEnabled(false);
        this.pathField.setText(this.currentPath.toString() + " (searching...)");

        this.fileTableList.setEnabled(false);
        if (this.searchTableModel == null) {
            this.searchTableModel = new PhantomPathTableModel(this.fileSystem, true);
        }
        this.fileTableList.setModel(this.searchTableModel);
        this.searchTableModel.updatePaths(new PhantomPath[]{});

        if (this.searchThreadTask != null) {
            this.searchThreadTask.cancel(true);
            this.searchThreadTask = null;
        }
        
        final PhantomFileSystem fs = this.fileSystem;
        final PhantomPath searchDirectory = this.currentPath;
        final String toSearch = this.searchField.getText();
        final boolean caseSensitive = this.caseSensitiveSearch.isSelected();
        final boolean exact = this.exactSearch.isSelected();
        final long id = this.searchId;
        this.searchId++;

        this.searchThreadTask = this.searchThread.submit(() -> {
            try {
                final PhantomPath[] result = fs.search(searchDirectory, toSearch, caseSensitive, exact, true);
                SwingUtilities.invokeLater(() -> {
                    onSearchDone(id, searchDirectory, result);
                });
            } catch (Throwable t) {
                if (t instanceof InterruptedException) {
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    onSearchThreadException(t);
                });
            }
        });
    }

    private void onSearchDone(long id, PhantomPath directory, PhantomPath[] results) {
        if (id != (this.searchId - 1) || !this.searchModeEnabled) {
            return;
        }
        this.fileTableList.setEnabled(true);
        this.searchTableModel.updatePaths(results);
        if (results.length == 1) {
            this.pathField.setText(directory.toString() + " (1 result)");
        } else {
            this.pathField.setText(directory.toString() + " (" + results.length + " results)");
        }

    }

    private void onSearchDisabled() {
        if (this.searchThreadTask != null) {
            this.searchThreadTask.cancel(true);
            this.searchThreadTask = null;
        }
        this.searchField.setText("");
        this.pathField.setEnabled(true);
        this.pathField.setText(this.currentPath.toString());
        this.fileTableList.setEnabled(true);
        this.fileTableList.setModel(this.fileSystemTableModel);
        updateFileTable();
    }

    private void searchFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchFieldKeyTyped
        this.searchTimer.restart();
        if (this.searchModeEnabled) {
            this.pathField.setText(this.currentPath.toString()+" (waiting)");
        }
    }//GEN-LAST:event_searchFieldKeyTyped

    private void caseSensitiveSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_caseSensitiveSearchActionPerformed
        updateSearch();
    }//GEN-LAST:event_caseSensitiveSearchActionPerformed

    private void exactSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exactSearchActionPerformed
        updateSearch();
    }//GEN-LAST:event_exactSearchActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutButton;
    private javax.swing.JTextField bottomInfoField;
    private javax.swing.JCheckBoxMenuItem caseSensitiveSearch;
    private javax.swing.JMenuItem copySystemButton;
    private javax.swing.JMenuItem createButton;
    private javax.swing.JCheckBoxMenuItem exactSearch;
    private javax.swing.JMenu existsSystemMenu;
    private javax.swing.JPopupMenu filePopupMenu;
    private javax.swing.JMenuItem filePropertiesButton;
    private javax.swing.JTable fileTableList;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JMenuItem logButton;
    private javax.swing.JMenuItem moveToTrashButton;
    private javax.swing.JMenuItem openButton;
    private javax.swing.JMenuItem openDirectoryButton;
    private javax.swing.JMenuItem openFileButton;
    private javax.swing.JMenuItem openLocationButton;
    private javax.swing.JTextField pathField;
    private javax.swing.JMenuItem rootDirectoryButton;
    private javax.swing.JTextField searchField;
    private javax.swing.JMenu systemMenu;
    private javax.swing.JMenu themeMenu;
    private javax.swing.JMenuItem validateFileButton;
    // End of variables declaration//GEN-END:variables
}
