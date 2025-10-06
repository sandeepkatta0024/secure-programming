import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public class SecurityManager {
    private static final int AES_BITS = 256;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private final SecureRandom rng = new SecureRandom();

    private final KeyPair keyPair;
    private volatile SecretKey aesKey;

    public SecurityManager() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        this.keyPair = kpg.generateKeyPair();
        this.aesKey = generateAesKey(AES_BITS);
        System.out.println("[DEBUG] Generated AES key: "
                + Base64.getEncoder().encodeToString(aesKey.getEncoded()));
    }

    public byte[] encrypt(byte[] plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_BYTES];
        rng.nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, aesKey, spec);
        byte[] ct = c.doFinal(plaintext);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(iv);
        out.write(ct);
        return out.toByteArray();
    }
    public byte[] getCurrentKey() {
        return aesKey.getEncoded();
    }

    public byte[] decrypt(byte[] in) throws Exception {
        byte[] iv = Arrays.copyOfRange(in, 0, GCM_IV_BYTES);
        byte[] ct = Arrays.copyOfRange(in, GCM_IV_BYTES, in.length);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, aesKey, spec);
        return c.doFinal(ct);
    }

    public byte[] sign(byte[] data) throws Exception {
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(keyPair.getPrivate());
        s.update(data);
        return s.sign();
    }

    public boolean verify(byte[] data, byte[] sig, PublicKey key) throws Exception {
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initVerify(key);
        s.update(data);
        return s.verify(sig);
    }

    public void setSharedKey(byte[] raw) {
        this.aesKey = new SecretKeySpec(raw, "AES");
    }

    public PublicKey getPublicKey() { return keyPair.getPublic(); }

    private SecretKey generateAesKey(int bits) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(bits, rng);
        return kg.generateKey();
    }
}
