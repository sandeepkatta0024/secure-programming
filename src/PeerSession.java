import javax.crypto.SecretKey;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PeerSession {
    public final String peerId;
    public final Socket socket;
    public final ObjectInputStream in;
    public final ObjectOutputStream out;
    public final SecretKey aesKey;

    public PeerSession(String peerId, Socket socket, ObjectInputStream in, ObjectOutputStream out, SecretKey aesKey) {
        this.peerId = peerId;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.aesKey = aesKey;
    }
}
