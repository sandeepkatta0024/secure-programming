import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class PeerNode {
    private final String peerId;
    private final int port;
    private final ServerSocket server;
    private final HandshakeManager handshake;
    private final Map<String, PeerSession> sessions = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Set<String> onlinePeers = ConcurrentHashMap.newKeySet();
    private long lamport = 0L;

    public PeerNode(String id, int port) throws Exception {
        this.peerId = id;
        this.port = port;
        this.server = new ServerSocket(port);
        this.handshake = new HandshakeManager();
    }

    // Start server
    public void start() {
        pool.submit(() -> {
            System.out.println(peerId + " listening on port " + port);
            while (true) {
                Socket s = server.accept();
                pool.submit(() -> handleIncoming(s));
            }
        });
    }

    // Incoming handler
    private void handleIncoming(Socket s) {
        try {
            byte[] keyBytes = handshake.performHandshake(s, false);
            SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");

            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            String tempId = "PendingPeer_" + s.getPort();

            PeerSession session = new PeerSession(tempId, s, in, out, aesKey);
            sessions.put(tempId, session);

            while (true) {
                Message msg = (Message) in.readObject();
                processMessage(msg, session);
            }
        } catch (Exception e) {
            System.err.println("[" + peerId + "] Connection closed: " + e.getMessage());
        }
    }

    // Outgoing connection
    public void connect(String targetId, String host, int port) throws Exception {
        Socket s = new Socket(host, port);
        byte[] keyBytes = handshake.performHandshake(s, true);
        SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");

        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(s.getInputStream());
        PeerSession session = new PeerSession(targetId, s, in, out, aesKey);
        sessions.put(targetId, session);
        onlinePeers.add(targetId);

        sendHello(session);

        pool.submit(() -> {
            try {
                while (true) {
                    Message msg = (Message) in.readObject();
                    processMessage(msg, session);
                }
            } catch (Exception e) {
                System.err.println("[" + peerId + "] Lost connection to " + targetId + ": " + e.getMessage());
                sessions.remove(targetId);
                onlinePeers.remove(targetId);
            }
        });

        System.out.println("[Handshake] " + peerId + " connected securely to " + targetId);
    }

    private void processMessage(Message msg, PeerSession session) throws Exception {
        byte[] plain = SecurityUtils.decrypt(session.aesKey, msg.getPayload());
        String content = new String(plain, StandardCharsets.UTF_8);

        switch (msg.getMessageType()) {
            case HELLO -> {
                sessions.remove(session.peerId);
                PeerSession updated = new PeerSession(content, session.socket, session.in, session.out, session.aesKey);
                sessions.put(content, updated);
                onlinePeers.add(content);
                System.out.println("[Handshake] " + content + " joined the chat.");
                broadcastPeers();
            }

            case PRIVATE -> System.out.println("[" + msg.getSenderId() + "] " + content);
            case GROUP -> System.out.println("[Group][" + msg.getSenderId() + "] " + content);

            case FILE -> {
                Path path = Paths.get("recv_" + msg.getSenderId() + "_" + System.currentTimeMillis());
                Files.write(path, plain);
                System.out.println("[File] Received from " + msg.getSenderId() + " -> " + path);
                sendFileAck(msg.getSenderId());
            }

            case FILE_ACK -> System.out.println("[File] Transfer to " + msg.getSenderId() + " succeeded.");
            case FILE_FAIL -> System.err.println("[File] Failed to send: " + content);

            case PEER_LIST -> {
                Set<String> newPeers = new HashSet<>(Arrays.asList(content.split(",")));
                newPeers.removeIf(String::isBlank);
                newPeers.remove(peerId); // skip self
                boolean updated = onlinePeers.addAll(newPeers);
                if (updated)
                    System.out.println("[Updated Online Peers] " + onlinePeers);

                // 🌐 auto-connect to any peers we don’t yet know
                for (String p : newPeers) {
                    if (!sessions.containsKey(p)) {
                        try {
                            // assume peers use port pattern: 7100 + offset per ID letter
                            int portGuess = 7100 + (p.charAt(0) - 'A') * 100;
                            connect(p, "localhost", portGuess);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }


    private void sendHello(PeerSession session) throws Exception {
        byte[] enc = SecurityUtils.encrypt(session.aesKey, peerId.getBytes(StandardCharsets.UTF_8));
        Message hello = new Message(peerId, "all", Message.MessageType.HELLO,
                System.currentTimeMillis(), lamport, 10, enc, null);
        synchronized (session.out) {
            session.out.writeObject(hello);
            session.out.flush();
        }
    }

    private void sendFileAck(String recv) throws Exception {
        sendControlMessage(recv, "OK", Message.MessageType.FILE_ACK);
    }

    private void sendControlMessage(String recv, String text, Message.MessageType type) throws Exception {
        PeerSession ps = sessions.get(recv);
        if (ps == null) return;
        byte[] enc = SecurityUtils.encrypt(ps.aesKey, text.getBytes(StandardCharsets.UTF_8));
        Message m = new Message(peerId, recv, type,
                System.currentTimeMillis(), lamport, 10, enc, null);
        synchronized (ps.out) {
            ps.out.writeObject(m);
            ps.out.flush();
        }
    }

    private void broadcastPeers() throws Exception {
        String list = String.join(",", onlinePeers);
        for (PeerSession ps : sessions.values()) {
            byte[] enc = SecurityUtils.encrypt(ps.aesKey, list.getBytes(StandardCharsets.UTF_8));
            Message m = new Message(peerId, "all", Message.MessageType.PEER_LIST,
                    System.currentTimeMillis(), lamport, 10, enc, null);
            synchronized (ps.out) {
                ps.out.writeObject(m);
                ps.out.flush();
            }
        }
    }

    public void sendMessage(String recv, Message.MessageType type, String text) throws Exception {
        if (!"all".equalsIgnoreCase(recv) && !sessions.containsKey(recv)) {
            System.out.println("[Error] " + recv + " is not online.");
            return;
        }
        if ("all".equalsIgnoreCase(recv)) {
            for (PeerSession ps : sessions.values()) {
                byte[] enc = SecurityUtils.encrypt(ps.aesKey, text.getBytes(StandardCharsets.UTF_8));
                Message m = new Message(peerId, ps.peerId, Message.MessageType.GROUP,
                        System.currentTimeMillis(), lamport, 10, enc, null);
                synchronized (ps.out) {
                    ps.out.writeObject(m);
                    ps.out.flush();
                }
            }
            return;
        }
        PeerSession ps = sessions.get(recv);
        byte[] enc = SecurityUtils.encrypt(ps.aesKey, text.getBytes(StandardCharsets.UTF_8));
        lamport++;
        Message m = new Message(peerId, recv, type,
                System.currentTimeMillis(), lamport, 10, enc, null);
        synchronized (ps.out) {
            ps.out.writeObject(m);
            ps.out.flush();
        }
    }

    public void sendFile(String recv, String path) throws Exception {
        PeerSession ps = sessions.get(recv);
        if (ps == null) {
            System.out.println("[Error] " + recv + " is not online.");
            return;
        }
        byte[] file = Files.readAllBytes(Paths.get(path));
        byte[] enc = SecurityUtils.encrypt(ps.aesKey, file);
        lamport++;
        Message m = new Message(peerId, recv, Message.MessageType.FILE,
                System.currentTimeMillis(), lamport, 10, enc, null);
        synchronized (ps.out) {
            ps.out.writeObject(m);
            ps.out.flush();
        }
    }

    public void showPeers() {
        System.out.println("=== Online Peers ===");
        onlinePeers.forEach(System.out::println);
    }
}
