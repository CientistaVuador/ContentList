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
public class FileTransferStatus {

    private long size = 0;
    private boolean firstUpdateExecuted = false;
    private long count = 0;
    private long speedCount = 0;
    private float progress = 0f;
    private long speedTime = 0;
    private double speed = 0;

    public FileTransferStatus() {

    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("size is negative");
        }
        this.size = size;
        
        this.firstUpdateExecuted = false;
        this.count = 0;
        this.speedCount = 0;
        this.progress = 0f;
        this.speedTime = 0;
        this.speed = 0;
    }

    public long getCount() {
        return count;
    }

    public float getProgress() {
        return progress;
    }

    public double getSpeed() {
        return speed;
    }

    public void update(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("count is negative");
        }

        if (!this.firstUpdateExecuted) {
            this.speedTime = System.nanoTime();
            this.firstUpdateExecuted = true;
        }

        this.count += count;

        if (this.size != 0) {
            this.progress = (float) (((double) this.count) / this.size);
        }

        this.speedCount += count;
        long t = System.nanoTime() - this.speedTime;
        if (t >= 1_000_000_000L) {
            this.speed = this.speedCount / (t / 1E9d);
            this.speedCount = 0;
            this.speedTime = System.nanoTime();
        }
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b
                .append(UIUtils.formatBytesShort(getCount()))
                .append(" of ")
                .append(UIUtils.formatBytesShort(getSize()))
                .append(" at ")
                .append(UIUtils.formatSpeed((long) getSpeed()))
                .append(" (")
                .append(getCount())
                .append("/")
                .append(getSize())
                .append(")");
        return b.toString();
    }

}
