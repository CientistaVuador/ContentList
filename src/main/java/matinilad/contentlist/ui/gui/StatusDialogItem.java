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

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import javax.swing.SwingUtilities;

/**
 *
 * @author Cien
 */
public abstract class StatusDialogItem {

    private final StatusDialog dialog;
    private Future<?> currentUpdate = null;
    
    public StatusDialogItem(StatusDialog dialog) {
        this.dialog = Objects.requireNonNull(dialog, "dialog is null");
    }

    public StatusDialog getDialog() {
        return dialog;
    }
    
    public abstract String getItemName();
    public abstract String getItemStatus();
    public abstract String getEstimatedTime();
    public abstract float getProgress();
    
    protected Runnable getUpdateTask() {
        StatusDialog d = getDialog();
        
        String itemName = getItemName();
        String itemStatus = getItemStatus();
        String estimatedTime = getEstimatedTime();
        float progress = getProgress();
        
        Runnable r = () -> {
            d.getCurrentItemName().setText(itemName);
            d.getCurrentItemStatus().setText(itemStatus);
            d.getEstimatedTime().setText(estimatedTime);
            d.setProgress(progress);
        };
        
        return r;
    }
    
    public void updateDialog(boolean force) {
        Runnable updateTask = getUpdateTask();
        if (force) {
            SwingUtilities.invokeLater(updateTask);
            return;
        }
        
        if (this.currentUpdate != null && !this.currentUpdate.isDone()) {
            return;
        }
        
        this.currentUpdate = CompletableFuture.runAsync(() -> {
            try {
                SwingUtilities.invokeAndWait(updateTask);
            } catch (InterruptedException | InvocationTargetException ex) {}
        });
    }
}
