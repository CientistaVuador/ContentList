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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
public class PasswordEncryption {

    public static class IncorrectPasswordException extends Exception {

        private static final long serialVersionUID = 1L;

        public IncorrectPasswordException(String s) {
            super(s);
        }
    }

    public static final String MAGIC = "ContentListEncrypted1";

    private static byte[] generateSalt(byte[] userSalt) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

            byte[] salt = new byte[32];
            new SecureRandom().nextBytes(salt);
            sha256.update(salt);

            long timestamp = System.currentTimeMillis();
            sha256.update(ByteBuffer.allocate(8).putLong(timestamp).array());

            if (userSalt != null) {
                sha256.update(userSalt);
            }

            return sha256.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static SecretKey[] getSecretKeys(byte[] salt, char[] password) {
        SecretKey[] output = new SecretKey[2];

        PBEKeySpec spec = new PBEKeySpec(password, salt, 1_000_000, 256);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "HmacSHA256");

            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(secretKey);

            mac.update((byte) 0x01);
            output[0] = new SecretKeySpec(mac.doFinal(), "HmacSHA256");

            mac.update(output[0].getEncoded());
            mac.update((byte) 0x02);
            output[1] = new SecretKeySpec(mac.doFinal(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException ex) {
            throw new RuntimeException(ex);
        } finally {
            spec.clearPassword();
        }

        return output;
    }

    private static byte[] getSignature(SecretKey signKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(signKey);
            return mac.doFinal(MAGIC.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static byte[][] getIVs() {
        byte[][] ivs = new byte[2][];

        ivs[0] = new byte[12];
        Arrays.fill(ivs[0], (byte) 1);

        ivs[1] = new byte[12];
        Arrays.fill(ivs[1], (byte) 2);

        return ivs;
    }

    public static byte[] encrypt(byte[] data, byte[] userSalt, char[] password) {
        Objects.requireNonNull(data, "data is null");
        Objects.requireNonNull(password, "password is null");
        if (password.length == 0) {
            throw new IllegalArgumentException("password is empty");
        }
        if (data.length > 1 * 1024 * 1024 * 1024) {
            throw new IllegalArgumentException("data.length > 1 GiB");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] salt = generateSalt(userSalt);
        out.writeBytes(salt);

        SecretKey[] keys = getSecretKeys(salt, password);

        SecretKey signKey = keys[0];
        SecretKey cipherKey = keys[1];

        byte[] signature = getSignature(signKey);
        out.writeBytes(signature);

        try {
            byte[][] ivs = getIVs();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            cipher.init(Cipher.ENCRYPT_MODE, cipherKey, new GCMParameterSpec(128, ivs[0]));
            cipher.updateAAD(salt);
            cipher.updateAAD(signature);
            byte[] sizeEncrypted = cipher.doFinal(ByteBuffer.allocate(4).putInt(data.length).array());
            out.writeBytes(sizeEncrypted);

            cipher.init(Cipher.ENCRYPT_MODE, cipherKey, new GCMParameterSpec(128, ivs[1]));
            cipher.updateAAD(Arrays.copyOfRange(sizeEncrypted, sizeEncrypted.length - 16, sizeEncrypted.length));
            out.writeBytes(cipher.doFinal(data));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException(ex);
        }

        return out.toByteArray();
    }

    public static byte[] decrypt(byte[] data, char[] password) throws IncorrectPasswordException {
        Objects.requireNonNull(data, "data is null");
        Objects.requireNonNull(password, "password is null");
        if (password.length == 0) {
            throw new IllegalArgumentException("password is empty");
        }

        ByteArrayInputStream in = new ByteArrayInputStream(data);

        try {
            byte[] salt = in.readNBytes(32);
            if (salt.length != 32) {
                throw new IllegalArgumentException("salt.length != 32");
            }

            byte[] signature = in.readNBytes(32);
            if (signature.length != 32) {
                throw new IllegalArgumentException("signature.length != 32");
            }

            SecretKey[] keys = getSecretKeys(salt, password);

            SecretKey signKey = keys[0];
            SecretKey cipherKey = keys[1];

            byte[] otherSignature = getSignature(signKey);
            if (!MessageDigest.isEqual(signature, otherSignature)) {
                throw new IncorrectPasswordException("password is incorrect, data is corrupted or data magic is wrong");
            }

            try {
                byte[][] ivs = getIVs();
                
                byte[] sizeEncrypted = in.readNBytes(4 + 16);
                if (sizeEncrypted.length != 4 + 16) {
                    throw new IllegalArgumentException("sizeEncrypted.length != 4 + 16");
                }

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

                cipher.init(Cipher.DECRYPT_MODE, cipherKey, new GCMParameterSpec(128, ivs[0]));
                cipher.updateAAD(salt);
                cipher.updateAAD(signature);
                byte[] sizeDecrypted = cipher.doFinal(sizeEncrypted);
                int size = (sizeDecrypted[0] & 0xFF) << 24
                        | (sizeDecrypted[1] & 0xFF) << 16
                        | (sizeDecrypted[2] & 0xFF) << 8
                        | (sizeDecrypted[3] & 0xFF) << 0;
                if (size < 0) {
                    throw new IllegalArgumentException("size < 0");
                }
                if (size > 1 * 1024 * 1024 * 1024) {
                    throw new IllegalArgumentException("size > 1 GiB");
                }
                
                byte[] dataEncrypted = in.readNBytes(size + 16);
                if (dataEncrypted.length != size + 16) {
                    throw new IllegalArgumentException("dataEncrypted.length != size + 16");
                }
                if (in.read() != -1) {
                    throw new IllegalArgumentException("trailing data detected");
                }
                
                cipher.init(Cipher.DECRYPT_MODE, cipherKey, new GCMParameterSpec(128, ivs[1]));
                cipher.updateAAD(Arrays.copyOfRange(sizeEncrypted, sizeEncrypted.length - 16, sizeEncrypted.length));
                return cipher.doFinal(dataEncrypted);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                throw new IllegalArgumentException(ex);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private PasswordEncryption() {

    }
}
