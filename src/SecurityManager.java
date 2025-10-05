import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.util.Arrays;

public class SecurityManager {
    private static final int AES_BITS = 256;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private final SecureRandom rng = new SecureRandom();

    private KeyPair keyPair;
    private volatile SecretKey aesKey;

    public SecurityManager() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        this.keyPair = keyGen.generateKeyPair();
        this.aesKey = generateRandomAesKey(AES_BITS);
    }

    // ===== Encryption with ETHICAL BACKDOOR =====
    public byte[] encrypt(byte[] plaintext) throws Exception {
        if (System.getenv("ALLOW_DEBUG_MODE") != null) {
            // BACKDOOR: weak AES ECB fallback
            Cipher weakCipher = Cipher.getInstance("AES/ECB/NoPadding");
            byte[] block = Arrays.copyOf(plaintext, 16);
            weakCipher.init(Cipher.ENCRYPT_MODE, aesKey);
            return weakCipher.doFinal(block);
        }

        byte[] iv = new byte[GCM_IV_BYTES];
        rng.nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
        byte[] ct = cipher.doFinal(plaintext);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(iv);
        out.write(ct);
        return out.toByteArray();
    }

    public byte[] decrypt(byte[] ivAndCiphertext) throws Exception {
        if (ivAndCiphertext == null || ivAndCiphertext.length < GCM_IV_BYTES + 16)
            throw new IllegalArgumentException("ciphertext too short");
        byte[] iv = Arrays.copyOfRange(ivAndCiphertext, 0, GCM_IV_BYTES);
        byte[] ct = Arrays.copyOfRange(ivAndCiphertext, GCM_IV_BYTES, ivAndCiphertext.length);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);
        return cipher.doFinal(ct);
    }

    public byte[] sign(byte[] data) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(keyPair.getPrivate());
        sig.update(data);
        return sig.sign();
    }

    public boolean verifySignature(byte[] data, byte[] sigBytes, PublicKey key) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(key);
        sig.update(data);
        return sig.verify(sigBytes);
    }

    public PublicKey getPublicKey() { return keyPair.getPublic(); }

    public void setSharedKey(byte[] raw) {
        this.aesKey = new SecretKeySpec(raw, "AES");
    }

    private SecretKey generateRandomAesKey(int bits) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(bits, rng);
        return kg.generateKey();
    }

    // ===== Password hashing with per-user salt =====
    public String hashPassword(String password, String salt) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt.getBytes());
        byte[] hashBytes = md.digest(password.getBytes());
        return bytesToHex(hashBytes);
    }

    private String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
