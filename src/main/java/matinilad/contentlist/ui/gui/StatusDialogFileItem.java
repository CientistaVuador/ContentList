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

import matinilad.contentlist.ui.UIUtils;

/**
 *
 * @author Cien
 */
public class StatusDialogFileItem extends StatusDialogItem {
    
    private String fileName = null;
    private String fileStatus = null;
    
    private long lastUpdateTime = System.nanoTime();
    private long lastUpdateFileProgress = 0;
    
    private long fileProgress = 0;
    private long fileSize = 0;
    
    private double transferSpeed = 0.0;
    private double estimatedTime = 0.0;
    private double progress = 0f;
    
    public StatusDialogFileItem(StatusDialog dialog) {
        super(dialog);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileStatus() {
        return fileStatus;
    }

    public void setFileStatus(String fileStatus) {
        this.fileStatus = fileStatus;
    }
    
    @Override
    public String getItemName() {
        return (this.fileName == null ? "" : this.fileName);
    }

    @Override
    public String getItemStatus() {
        return (this.fileStatus == null ? "" : this.fileStatus);
    }
    
    public void reset() {
        this.lastUpdateTime = System.nanoTime();
        this.lastUpdateFileProgress = 0;
        
        this.fileProgress = 0;
        this.fileSize = 0;
        
        this.transferSpeed = 0;
        this.estimatedTime = 0;
        this.progress = 0;
    }
    
    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getFileProgress() {
        return fileProgress;
    }

    public void setFileProgress(long fileProgress) {
        this.fileProgress = fileProgress;
        
        long size = Math.max(this.fileSize, 0);
        long bytes = Math.min(Math.max(this.fileProgress, 0), size);
        
        double timePassed = (System.nanoTime() - this.lastUpdateTime) / 1E9d;
        if (timePassed >= 1.0) {
            long bytesProcessed = Math.max(bytes - this.lastUpdateFileProgress, 0);
            this.transferSpeed = bytesProcessed / timePassed;
            this.estimatedTime = (size - bytes) / this.transferSpeed;
            
            this.lastUpdateTime = System.nanoTime();
            this.lastUpdateFileProgress = bytes;
        }
        
        this.progress = bytes / ((double)size);
    }
    
    @Override
    public String getEstimatedTime() {
        long transferSpeedBytes = 0;
        if (Double.isFinite(this.transferSpeed)) {
            transferSpeedBytes = (long) this.transferSpeed;
        }
        String speedString = UIUtils.formatSpeed(transferSpeedBytes);
        
        String estimatedTimeString;
        if (Double.isFinite(this.estimatedTime)) {
            estimatedTimeString = UIUtils.formatTimeCountdown((long) this.estimatedTime)+" estimated";
        } else {
            estimatedTimeString = "Estimated time unknown";
        }
        estimatedTimeString = "(" + estimatedTimeString + ")";
        
        String sizeString = UIUtils.formatBytesShort(Math.max(this.fileSize, 0));
        String processedString = UIUtils.formatBytesShort(Math.max(this.fileProgress, 0));
        
        return processedString+" of "+sizeString+" -- "+speedString + " " + estimatedTimeString;
    }
    
    @Override
    public float getProgress() {
        return (float) this.progress;
    }
    
}
