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

import java.util.Arrays;
import java.util.Objects;
import javax.swing.table.DefaultTableModel;
import matinilad.contentlist.phantomfs.PhantomFileSystem;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.ui.UIUtils;

/**
 *
 * @author Cien
 */
@SuppressWarnings("serial")
public class PhantomPathTableModel extends DefaultTableModel {
    
    private static final String[] header = new String[] {"Type", "Name", "Size", "Created", "Modified"};
    private static final String[] searchHeader = new String[] {"Type", "Name", "Path", "Size"};
    
    private final PhantomFileSystem fileSystem;
    private final boolean searchMode;
    private PhantomPath[] paths = new PhantomPath[0];

    public PhantomPathTableModel(PhantomFileSystem fs, boolean searchMode) {
        super(searchMode ? searchHeader : header, 0);
        this.fileSystem = Objects.requireNonNull(fs, "file system is null");
        this.searchMode = searchMode;
    }
    
    public PhantomFileSystem getFileSystem() {
        return fileSystem;
    }

    public boolean isSearchMode() {
        return searchMode;
    }

    public PhantomPath getContentPath(int index) {
        return this.paths[index];
    }

    private String getName(PhantomPath p) {
        String name = p.getName();
        if (".".equals(name) || "..".equals(name)) {
            String realPathName = getFileSystem().toRealPath(p).getName();
            if (realPathName == null) {
                realPathName = "(root)";
            }
            if (name.equals(".")) {
                return ". [current] " + realPathName;
            }
            if (name.equals("..")) {
                return ".. [parent] " + realPathName;
            }
        }
        return name == null ? "(root)" : name;
    }
    
    private Object[][] createTableData() {
        Object[][] tableData = new Object[this.paths.length][header.length];
        for (int row = 0; row < this.paths.length; row++) {
            Object[] rowData = new Object[header.length];
            Arrays.fill(rowData, "");
            PhantomPath p = this.paths[row];
            FileEntry entry = getFileSystem().getEntry(p);
            String name = getName(p);
            rowData[1] = name;
            if (entry != null) {
                rowData[0] = entry.getType().toString();
                rowData[2] = UIUtils.formatBytes(entry.getSize());
                rowData[3] = UIUtils.asShortLocalizedDateTime(entry.getCreated());
                rowData[4] = UIUtils.asShortLocalizedDateTime(entry.getModified());
            }
            tableData[row] = rowData;
        }
        return tableData;
    }
    
    private Object[][] createSearchModeTableData() {
        Object[][] tableData = new Object[this.paths.length][searchHeader.length];
        for (int row = 0; row < this.paths.length; row++) {
            Object[] rowData = new Object[searchHeader.length];
            Arrays.fill(rowData, "");
            PhantomPath p = this.paths[row];
            FileEntry entry = getFileSystem().getEntry(p);
            String name = getName(p);
            rowData[1] = name;
            if (entry != null) {
                rowData[0] = entry.getType().toString();
                rowData[2] = entry.getPath().getParent().toString();
                rowData[3] = UIUtils.formatBytes(entry.getSize());
            }
            tableData[row] = rowData;
        }
        return tableData;
    }
    
    public void updatePaths(PhantomPath[] newData) {
        this.paths = newData.clone();
        Object[][] tableData = (isSearchMode() ? createSearchModeTableData() : createTableData());
        
        setRowCount(0);
        for (int i = 0; i < tableData.length; i++) {
            addRow(tableData[i]);
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
    
}
