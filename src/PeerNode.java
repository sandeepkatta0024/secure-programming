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
    private final OverlayRoutingManager routingManager;
    private final SecurityManager security;
    private final HandshakeManager handshake;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Set<String> onlinePeers = ConcurrentHashMap.newKeySet();
    private long lamport = 0L;

    public PeerNode(String id, int port) throws Exception {
        this.peerId = id;
        this.port = port;
        this.server = new ServerSocket(port);
        this.routingManager = new OverlayRoutingManager();
        this.security = new SecurityManager();
        this.handshake = new HandshakeManager();
    }

    // ==================== SERVER START ====================
    public void start() {
        pool.submit(() -> {
            System.out.println(peerId + " listening on port " + port);
            while (true) {
                Socket s = server.accept();
                pool.submit(() -> handleIncoming(s));
            }
        });
    }

    // ==================== INCOMING CONNECTION HANDLER ====================
    private void handleIncoming(Socket s) {
        try {
            byte[] key = handshake.performHandshake(s, false);
            security.setSharedKey(key);

            ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            routingManager.addPeer("PendingPeer_" + s.getPort(), s, out);

            // Wait for the HELLO message (so we know their actual ID)
            while (true) {
                Message msg = (Message) in.readObject();
                processMessage(msg, s, out);
            }
        } catch (Exception e) {
            System.err.println("[" + peerId + "] Connection closed: " + e.getMessage());
        }
    }

    // ==================== CLIENT CONNECTION INITIATOR ====================
    public void connect(String targetId, String host, int port) throws Exception {
        Socket s = new Socket(host, port);
        byte[] key = handshake.performHandshake(s, true);
        security.setSharedKey(key);
        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(s.getInputStream());
        routingManager.addPeer(targetId, s, out);
        onlinePeers.add(targetId);

        // Send HELLO to identify yourself
        sendHello(out);

        // Start listening to that socket for replies
        pool.submit(() -> {
            try {
                while (true) {
                    Message msg = (Message) in.readObject();
                    processMessage(msg, s, out);
                }
            } catch (Exception e) {
                System.err.println("[" + peerId + "] Lost connection to " + targetId);
                routingManager.removePeer(targetId);
                onlinePeers.remove(targetId);
            }
        });

        System.out.println("[Handshake] " + peerId + " connected securely to " + targetId);
    }

    // ==================== PROCESS INCOMING MESSAGE ====================
    private void processMessage(Message msg, Socket s, ObjectOutputStream out) throws Exception {
        byte[] plain = security.decrypt(msg.getPayload());
        String content = new String(plain, StandardCharsets.UTF_8);

        switch (msg.getMessageType()) {
            case HELLO -> {
                // Register their ID
                routingManager.updatePeerId(s, content);
                onlinePeers.add(content);
                System.out.println("[Handshake] " + content + " joined the chat.");
                broadcastOnlinePeers();
            }
            case PRIVATE -> {
                System.out.println("[" + msg.getSenderId() + "] " + content);
            }
            case GROUP -> {
                System.out.println("[Group][" + msg.getSenderId() + "] " + content);
            }
            case FILE -> {
                try {
                    Path filePath = Paths.get("recv_" + msg.getSenderId() + "_" + System.currentTimeMillis());
                    Files.write(filePath, plain);
                    sendFileAck(msg.getSenderId());
                    System.out.println("[File] Received from " + msg.getSenderId() + " -> " + filePath);
                } catch (Exception e) {
                    sendFileFail(msg.getSenderId(), e.getMessage());
                }
            }
            case FILE_ACK -> System.out.println("[File] Transfer to " + msg.getSenderId() + " succeeded.");
            case FILE_FAIL -> System.err.println("[File] Failed to send: " + content);
            case PEER_LIST -> {
                for (String p : content.split(",")) {
                    if (!p.isBlank()) onlinePeers.add(p.trim());
                }
                System.out.println("[Updated Online Peers] " + onlinePeers);
            }
        }
    }

    // ==================== HELLO (IDENTITY EXCHANGE) ====================
    private void sendHello(ObjectOutputStream out) throws Exception {
        byte[] enc = security.encrypt(peerId.getBytes(StandardCharsets.UTF_8));
        byte[] sig = security.sign(enc);
        Message hello = new Message(peerId, "all", Message.MessageType.HELLO,
                System.currentTimeMillis(), lamport, 10, enc, sig);
        synchronized (out) {
            out.writeObject(hello);
            out.flush();
        }
    }

    // ==================== FILE ACKNOWLEDGEMENT ====================
    private void sendFileAck(String recv) throws Exception {
        sendControlMessage(recv, "OK", Message.MessageType.FILE_ACK);
    }

    private void sendFileFail(String recv, String reason) throws Exception {
        sendControlMessage(recv, reason, Message.MessageType.FILE_FAIL);
    }

    private void sendControlMessage(String recv, String text, Message.MessageType type) throws Exception {
        byte[] enc = security.encrypt(text.getBytes(StandardCharsets.UTF_8));
        byte[] sig = security.sign(enc);
        Message m = new Message(peerId, recv, type, System.currentTimeMillis(), lamport, 10, enc, sig);
        routingManager.routeMessage(m);
    }

    // ==================== BROADCAST ONLINE PEERS ====================
    private void broadcastOnlinePeers() throws Exception {
        String list = String.join(",", onlinePeers);
        byte[] enc = security.encrypt(list.getBytes(StandardCharsets.UTF_8));
        byte[] sig = security.sign(enc);
        Message m = new Message(peerId, "all", Message.MessageType.PEER_LIST,
                System.currentTimeMillis(), lamport, 10, enc, sig);
        routingManager.routeMessage(m);
    }

    // ==================== PUBLIC COMMANDS ====================
    public void sendMessage(String recv, Message.MessageType type, String text) throws Exception {
        if (!onlinePeers.contains(recv) && !"all".equalsIgnoreCase(recv)) {
            System.out.println("[Error] " + recv + " is not online.");
            return;
        }
        byte[] enc = security.encrypt(text.getBytes(StandardCharsets.UTF_8));
        byte[] sig = security.sign(enc);
        lamport++;
        Message m = new Message(peerId, recv, type, System.currentTimeMillis(), lamport, 10, enc, sig);
        routingManager.routeMessage(m);
    }

    public void sendFile(String recv, String path) throws Exception {
        if (!onlinePeers.contains(recv)) {
            System.out.println("[Error] " + recv + " is not online.");
            return;
        }
        try {
            byte[] file = Files.readAllBytes(Paths.get(path));
            byte[] enc = security.encrypt(file);
            byte[] sig = security.sign(enc);
            lamport++;
            Message m = new Message(peerId, recv, Message.MessageType.FILE,
                    System.currentTimeMillis(), lamport, 10, enc, sig);
            routingManager.routeMessage(m);
        } catch (Exception e) {
            sendFileFail(recv, e.getMessage());
        }
    }

    public void showPeers() {
        System.out.println("=== Online Peers ===");
        onlinePeers.forEach(System.out::println);
    }
}
