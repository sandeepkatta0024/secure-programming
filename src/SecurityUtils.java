import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Arrays;

public class SecurityUtils {
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final SecureRandom RNG = new SecureRandom();

    public static byte[] encrypt(SecretKey key, byte[] plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_BYTES];
        RNG.nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] ct = cipher.doFinal(plaintext);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(iv);
        out.write(ct);
        return out.toByteArray();
    }

    public static byte[] decrypt(SecretKey key, byte[] ciphertext) throws Exception {
        byte[] iv = Arrays.copyOfRange(ciphertext, 0, GCM_IV_BYTES);
        byte[] ct = Arrays.copyOfRange(ciphertext, GCM_IV_BYTES, ciphertext.length);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(ct);
    }
}
