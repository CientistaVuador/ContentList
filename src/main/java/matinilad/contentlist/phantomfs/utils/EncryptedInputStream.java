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
package matinilad.contentlist.phantomfs.utils;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import static matinilad.contentlist.phantomfs.utils.EncryptedOutputStream.MAGIC;

/**
 *
 * @author Cien
 */
public class EncryptedInputStream extends FilterInputStream {

    public static class IncorrectPasswordException extends IOException {

        private static final long serialVersionUID = 1L;

        public IncorrectPasswordException(String message) {
            super(message);
        }
    }

    private final char[] password;
    private final int maxBufferSize;
    
    private boolean header = false;

    private SecretKey key = null;
    private Cipher cipher = null;
    private long nonce = 0;

    private int nextBufferSize = -1;

    private byte[] buffer = null;
    private int bufferIndex = 0;

    private boolean closed = false;
    
    public EncryptedInputStream(InputStream in, char[] password, int maxBufferSize) {
        super(Objects.requireNonNull(in, "in is null"));
        this.password = password.clone();
        if (maxBufferSize < 1) {
            throw new IllegalArgumentException("maxBufferSize < 1");
        }
        if (maxBufferSize > EncryptedOutputStream.MAX_BUFFER_SIZE) {
            throw new IllegalArgumentException("maxBufferSize > MAX_BUFFER_SIZE");
        }
        this.maxBufferSize = maxBufferSize;
    }
    
    public EncryptedInputStream(InputStream in, char[] password) {
        this(in, password, EncryptedOutputStream.MAX_BUFFER_SIZE);
    }

    private GCMParameterSpec nextIV() {
        byte[] iv = new byte[12];
        for (int i = 0; i < 8; i++) {
            iv[i] = (byte) (this.nonce >>> ((7 - i) * 8));
        }
        this.nonce++;

        return new GCMParameterSpec(128, iv);
    }

    private void readHeader() throws IOException {
        try {
            byte[] salt = this.in.readNBytes(32);
            if (salt.length != 32) {
                throw new EOFException("unexpected EOF, expected salt");
            }

            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKey signKey;
            SecretKey encryptionKey;

            PBEKeySpec spec = new PBEKeySpec(this.password, salt, 1_000_000, 256);
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                SecretKey secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "HmacSHA256");

                mac.init(secretKey);

                mac.update((byte) 0x01);
                signKey = new SecretKeySpec(mac.doFinal(), "HmacSHA256");

                mac.update(signKey.getEncoded());
                mac.update((byte) 0x02);
                encryptionKey = new SecretKeySpec(mac.doFinal(), "AES");
            } finally {
                spec.clearPassword();
                Arrays.fill(this.password, '\0');
            }

            mac.init(signKey);
            mac.update(MAGIC.getBytes(StandardCharsets.UTF_8));

            byte[] signedMagic = mac.doFinal();
            byte[] headerMagic = this.in.readNBytes(signedMagic.length);
            if (headerMagic.length != signedMagic.length) {
                throw new EOFException("unexpected EOF, expected signed magic");
            }
            if (!MessageDigest.isEqual(signedMagic, headerMagic)) {
                throw new IncorrectPasswordException("incorrect password or invalid magic");
            }

            this.key = encryptionKey;

            this.cipher = Cipher.getInstance("AES/GCM/NoPadding");
            this.cipher.init(Cipher.DECRYPT_MODE, this.key, nextIV());

            this.cipher.updateAAD(salt);
            this.cipher.updateAAD(signedMagic);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException ex) {
            throw new IOException(ex);
        }
    }

    private void readBuffer() throws IOException {
        try {
            if (this.nextBufferSize > this.maxBufferSize) {
                throw new IOException("nextBufferSize > maxBufferSize");
            }
            
            int encryptedSize = 4 + this.nextBufferSize + 16;
            byte[] encrypted = this.in.readNBytes(encryptedSize);
            if (encrypted.length != encryptedSize) {
                throw new EOFException("invalid buffer size! expected " + encryptedSize);
            }

            byte[] decrypted = this.cipher.doFinal(encrypted);
            this.nextBufferSize
                    = ((decrypted[0] & 0xFF) << 24)
                    | ((decrypted[1] & 0xFF) << 16)
                    | ((decrypted[2] & 0xFF) << 8)
                    | ((decrypted[3] & 0xFF) << 0);
            
            this.buffer = Arrays.copyOfRange(decrypted, 4, decrypted.length);
            this.bufferIndex = 0;
            
            this.cipher.init(Cipher.DECRYPT_MODE, this.key, nextIV());
            this.cipher.updateAAD(encrypted, encrypted.length - 16, 16);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new IOException(ex);
        }
    }

    private boolean readChecks() throws IOException {
        if (this.closed) {
            throw new IOException("stream is closed");
        }

        if (!this.header) {
            readHeader();
            this.header = true;
        }

        if (this.buffer == null) {
            this.nextBufferSize = 0;
            readBuffer();
        }

        if (this.bufferIndex >= this.buffer.length) {
            do {
                if (this.nextBufferSize <= -1) {
                    return false;
                }
                readBuffer();
            } while (this.buffer.length == 0);
        }
        
        return true;
    }

    @Override
    public int read() throws IOException {
        if (!readChecks()) {
            return -1;
        }

        int b = this.buffer[this.bufferIndex] & 0xFF;
        this.bufferIndex++;

        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        if (!readChecks()) {
            return -1;
        }
        
        int toCopy = Math.min(this.buffer.length - this.bufferIndex, len);
        System.arraycopy(this.buffer, this.bufferIndex, b, off, toCopy);
        this.bufferIndex += toCopy;
        
        return toCopy;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        byte[] buf = new byte[4096];
        long count = 0;
        int r;
        while ((r = read(buf, 0, (int) Math.min(buf.length, n - count))) != -1) {
            count += r;
            if (count >= n) {
                break;
            }
        }
        return count;
    }

    @Override
    public synchronized void mark(int readlimit) {

    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int available() throws IOException {
        if (this.buffer == null) {
            return 0;
        }
        return this.buffer.length - this.bufferIndex;
    }
    
    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        super.close();
    }

}
