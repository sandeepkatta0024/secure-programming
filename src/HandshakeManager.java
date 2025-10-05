import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;

/**
 * Handles secure RSA-based key exchange between peers.
 * Each peer sends its RSA public key, receives the other's key,
 * and encrypts a new AES session key for the other side.
 */
public class HandshakeManager {
    private final KeyPair keyPair;

    public HandshakeManager() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        this.keyPair = gen.generateKeyPair();
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    /** Performs a mutual handshake and sets AES key on both peers. */
    public byte[] performHandshake(Socket socket, boolean initiator) throws Exception {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        if (initiator) {
            // Send my public key
            out.writeObject(keyPair.getPublic());
            out.flush();
            // Receive their key
            PublicKey remoteKey = (PublicKey) in.readObject();
            // Create AES key and encrypt for peer
            SecretKey aesKey = generateAesKey(256);
            byte[] encryptedAes = encryptAesKey(aesKey.getEncoded(), remoteKey);
            out.writeObject(encryptedAes);
            out.flush();
            return aesKey.getEncoded();
        } else {
            // Receive their key
            PublicKey remoteKey = (PublicKey) in.readObject();
            // Send my key
            out.writeObject(keyPair.getPublic());
            out.flush();
            // Receive AES key encrypted to me
            byte[] encryptedAes = (byte[]) in.readObject();
            byte[] aesKeyBytes = decryptAesKey(encryptedAes);
            return aesKeyBytes;
        }
    }

    private SecretKey generateAesKey(int bits) throws Exception {
        KeyGenerator gen = KeyGenerator.getInstance("AES");
        gen.init(bits);
        return gen.generateKey();
    }

    private byte[] encryptAesKey(byte[] aesKey, PublicKey pubKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return cipher.doFinal(aesKey);
    }

    private byte[] decryptAesKey(byte[] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        return cipher.doFinal(encrypted);
    }

    public static String encodeKey(byte[] key) {
        return Base64.getEncoder().encodeToString(key);
    }
}
