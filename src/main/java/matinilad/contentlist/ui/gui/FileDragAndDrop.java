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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 *
 * @author Cien
 */
@SuppressWarnings("serial")
public abstract class FileDragAndDrop extends TransferHandler {
    
    private static final Logger LOG = Logger.getLogger(FileDragAndDrop.class.getName());
    
    public FileDragAndDrop() {
        super(null);
    }
    
    @Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.LINK;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        DataFlavor[] flavors = support.getTransferable().getTransferDataFlavors();
        boolean found = false;
        for (DataFlavor e : flavors) {
            if (e.equals(DataFlavor.javaFileListFlavor)) {
                found = true;
                break;
            }
        }
        if (!found) {
            LOG.log(Level.WARNING, "data flavor not found");
            Toolkit.getDefaultToolkit().beep();
            return false;
        }
        try {
            List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            if (files.isEmpty()) {
                LOG.log(Level.WARNING, "file list is empty");
                Toolkit.getDefaultToolkit().beep();
                return false;
            }
            return process(files);
        } catch (UnsupportedFlavorException | IOException ex) {
            LOG.log(Level.WARNING, null, ex);
            Toolkit.getDefaultToolkit().beep();
            return false;
        }
    }
    
    protected abstract boolean process(List<File> files);
}
