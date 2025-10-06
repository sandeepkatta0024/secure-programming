import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;

public class HandshakeManager {
    private final KeyPair keyPair;

    public HandshakeManager() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        this.keyPair = gen.generateKeyPair();
    }

    public byte[] performHandshake(Socket socket, boolean initiator) throws Exception {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        if (initiator) {
            out.writeObject(keyPair.getPublic());
            out.flush();
            PublicKey remoteKey = (PublicKey) in.readObject();
            SecretKey aesKey = generateAesKey(256);
            byte[] encryptedAes = encryptAesKey(aesKey.getEncoded(), remoteKey);
            out.writeObject(encryptedAes);
            out.flush();
            return aesKey.getEncoded();
        } else {
            PublicKey remoteKey = (PublicKey) in.readObject();
            out.writeObject(keyPair.getPublic());
            out.flush();
            byte[] encryptedAes = (byte[]) in.readObject();
            return decryptAesKey(encryptedAes);
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
}
