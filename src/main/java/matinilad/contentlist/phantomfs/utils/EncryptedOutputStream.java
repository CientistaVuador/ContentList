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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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

/**
 *
 * @author Cien
 */
public class EncryptedOutputStream extends FilterOutputStream {
    
    public static final String EXTENSION = "bin";
    
    public static final String MAGIC = "EncryptedStream1";
    public static final int MAX_BUFFER_SIZE = 1 * 1024 * 1024 * 1024;
    
    private final byte[] userSalt;
    private final char[] password;

    private boolean header = false;

    private SecretKey key = null;
    private Cipher cipher = null;
    private long nonce = 0;
    
    private byte[] lastBuffer;
    private int lastBufferIndex;
    
    private byte[] currentBuffer;
    private int currentBufferIndex;
    
    private boolean closed = false;

    public EncryptedOutputStream(OutputStream out, int bufferSize, byte[] userSalt, char[] password) {
        super(Objects.requireNonNull(out, "out is null"));
        if (bufferSize < 1) {
            throw new IllegalArgumentException("bufferSize < 1");
        }
        if (bufferSize > MAX_BUFFER_SIZE) {
            throw new IllegalArgumentException("bufferSize > MAX_BUFFER_SIZE");
        }
        for (int i = 0; i < password.length; i++) {
            if (password[i] == '\0') {
                throw new IllegalArgumentException("password character at index "+i+" is null");
            }
        }
        if (userSalt == null) {
            this.userSalt = null;
        } else {
            this.userSalt = userSalt.clone();
        }
        this.password = password.clone();
        
        this.lastBuffer = new byte[bufferSize];
        this.lastBufferIndex = 0;
        
        this.currentBuffer = new byte[bufferSize];
        this.currentBufferIndex = 0;
    }
    
    public EncryptedOutputStream(OutputStream out, byte[] userSalt, char[] password) {
        this(out, 64 * 1024, userSalt, password);
    }
    
    public EncryptedOutputStream(OutputStream out, char[] password) {
        this(out, null, password);
    }

    private GCMParameterSpec nextIV() {
        byte[] iv = new byte[12];
        for (int i = 0; i < 8; i++) {
            iv[i] = (byte) (this.nonce >>> ((7 - i) * 8));
        }
        this.nonce++;
        
        return new GCMParameterSpec(128, iv);
    }
    
    private byte[] generateSalt() throws IOException {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            
            byte[] salt = new byte[32];
            new SecureRandom().nextBytes(salt);
            sha256.update(salt);
            
            //because a counter is being used as an IV, the whole
            //security of this algorithm depends on the salt value
            //being a unique (not random) value.
            //adding a timestamp to the salt can avoid a severe
            //security error even if the system rng fails completely.
            long timestamp = System.currentTimeMillis();
            sha256.update(ByteBuffer.allocate(8).putLong(timestamp).array());
            
            if (this.userSalt != null) {
                sha256.update(this.userSalt);
            }
            
            return sha256.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
    }

    private void writeHeader() throws IOException {
        try {
            byte[] salt = generateSalt();
            this.out.write(salt);
            
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKey signKey;
            SecretKey encryptKey;

            PBEKeySpec spec = new PBEKeySpec(this.password, salt, 1_000_000, 256);
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                SecretKey secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "HmacSHA256");
                
                mac.init(secretKey);
                
                mac.update((byte) 0x01);
                signKey = new SecretKeySpec(mac.doFinal(), "HmacSHA256");
                
                mac.update(signKey.getEncoded());
                mac.update((byte) 0x02);
                encryptKey = new SecretKeySpec(mac.doFinal(), "AES");
            } finally {
                Arrays.fill(this.password, '\0');
                spec.clearPassword();
            }
            
            mac.init(signKey);
            mac.update(MAGIC.getBytes(StandardCharsets.UTF_8));
            
            byte[] signedMagic = mac.doFinal();
            this.out.write(signedMagic);
            
            this.key = encryptKey;
            
            this.cipher = Cipher.getInstance("AES/GCM/NoPadding");
            this.cipher.init(Cipher.ENCRYPT_MODE, this.key, nextIV());
            
            this.cipher.updateAAD(salt);
            this.cipher.updateAAD(signedMagic);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException ex) {
            throw new IOException(ex);
        }
    }
    
    private void pushBuffer() throws IOException {
        try {
            byte[] shouldBeEmpty = this.cipher.update(new byte[] {
                (byte) (this.currentBufferIndex >>> 24),
                (byte) (this.currentBufferIndex >>> 16),
                (byte) (this.currentBufferIndex >>> 8),
                (byte) (this.currentBufferIndex >>> 0)
            });
            byte[] encrypted = this.cipher.doFinal(this.lastBuffer, 0, this.lastBufferIndex);
            
            if (shouldBeEmpty != null && shouldBeEmpty.length > 0) {
                this.out.write(shouldBeEmpty);
            }
            this.out.write(encrypted);
            
            int a = this.lastBufferIndex;
            int b = this.currentBufferIndex;
            this.lastBufferIndex = b;
            this.currentBufferIndex = a;
            
            byte[] arrayA = this.lastBuffer;
            byte[] arrayB = this.currentBuffer;
            this.lastBuffer = arrayB;
            this.currentBuffer = arrayA;
            
            this.currentBufferIndex = 0;
            
            this.cipher.init(Cipher.ENCRYPT_MODE, this.key, nextIV());
            this.cipher.updateAAD(encrypted, encrypted.length - 16, 16);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new IOException(ex);
        }
    }
    
    private void writeChecks() throws IOException {
        if (this.closed) {
            throw new IOException("stream is closed");
        }

        if (!this.header) {
            writeHeader();
            this.header = true;
        }
    }

    @Override
    public void write(int b) throws IOException {
        writeChecks();

        if (this.currentBufferIndex >= this.currentBuffer.length) {
            pushBuffer();
        }
        this.currentBuffer[this.currentBufferIndex] = (byte) b;
        this.currentBufferIndex++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        writeChecks();

        int from = off;
        int to = off + len;

        while (from < to) {
            if (this.currentBufferIndex >= this.currentBuffer.length) {
                pushBuffer();
            }
            int toCopy = Math.min(to - from, this.currentBuffer.length - this.currentBufferIndex);
            System.arraycopy(b, from, this.currentBuffer, this.currentBufferIndex, toCopy);
            this.currentBufferIndex += toCopy;
            from += toCopy;
        }
    }

    @Override
    public void flush() throws IOException {
        if (this.closed) {
            return;
        }
        
        writeChecks();
        
        if (this.currentBufferIndex != 0) {
            pushBuffer();
        }
        if (this.lastBufferIndex != 0) {
            pushBuffer();
        }
        
        super.flush();
    }
    
    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        
        if (!this.header) {
            writeHeader();
            this.header = true;
        }
        if (this.currentBufferIndex != 0) {
            pushBuffer();
        }
        this.currentBufferIndex = -1;
        pushBuffer();
        
        this.closed = true;
        this.out.close();
    }

}
