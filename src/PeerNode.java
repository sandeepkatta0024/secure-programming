import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public void start() {
        pool.submit(() -> {
            System.out.println(peerId + " listening on port " + port);
            while (true) {
                Socket s = server.accept();
                pool.submit(() -> handleIncoming(s));
            }
        });
    }

    private void handleIncoming(Socket s) {
        try (ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            while (true) {
                Message msg = (Message) in.readObject();
                byte[] plain = security.decrypt(msg.getPayload());
                String content = new String(plain, StandardCharsets.UTF_8);

                switch (msg.getMessageType()) {
                    case PRIVATE -> System.out.println("[" + msg.getSenderId() + "] " + content);
                    case GROUP -> System.out.println("[Group][" + msg.getSenderId() + "] " + content);
                    case FILE -> {
                        Files.write(Paths.get("recv_" + msg.getSenderId() + "_" + System.currentTimeMillis()), plain);
                        sendFileAck(msg.getSenderId());
                    }
                    case FILE_ACK -> System.out.println("[File] Transfer to " + msg.getSenderId() + " succeeded.");
                    case FILE_FAIL -> System.err.println("[File] Failed: " + content);
                    case PEER_LIST -> {
                        for (String p : content.split(",")) if (!p.isBlank()) onlinePeers.add(p.trim());
                        System.out.println("[Online Peers Updated] " + onlinePeers);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[" + peerId + "] Connection error: " + e.getMessage());
        }
    }

    public void connect(String id, String host, int port) throws Exception {
        Socket s = new Socket(host, port);
        byte[] key = handshake.performHandshake(s, true);
        security.setSharedKey(key);
        routingManager.addPeer(id, s);
        onlinePeers.add(id);
        broadcastPeers();
        System.out.println(peerId + " connected securely to " + id);
    }

    public void sendMessage(String recv, Message.MessageType type, String text) throws Exception {
        byte[] enc = security.encrypt(text.getBytes(StandardCharsets.UTF_8));
        byte[] sig = security.sign(enc);
        lamport++;
        Message m = new Message(peerId, recv, type, System.currentTimeMillis(), lamport, 10, enc, sig);
        routingManager.routeMessage(m);
    }

    public void sendFile(String recv, String path) throws Exception {
        try {
            byte[] file = Files.readAllBytes(Paths.get(path));
            byte[] enc = security.encrypt(file);
            byte[] sig = security.sign(enc);
            lamport++;
            Message m = new Message(peerId, recv, Message.MessageType.FILE,
                    System.currentTimeMillis(), lamport, 10, enc, sig);
            routingManager.routeMessage(m);
        } catch (Exception e) {
            byte[] enc = security.encrypt(e.getMessage().getBytes(StandardCharsets.UTF_8));
            byte[] sig = security.sign(enc);
            Message fail = new Message(peerId, recv, Message.MessageType.FILE_FAIL,
                    System.currentTimeMillis(), lamport, 10, enc, sig);
            routingManager.routeMessage(fail);
        }
    }

    private void sendFileAck(String recv) throws Exception {
        byte[] enc = security.encrypt("OK".getBytes(StandardCharsets.UTF_8));
        byte[] sig = security.sign(enc);
        Message ack = new Message(peerId, recv, Message.MessageType.FILE_ACK,
                System.currentTimeMillis(), lamport, 10, enc, sig);
        routingManager.routeMessage(ack);
    }

    private void broadcastPeers() throws Exception {
        String list = String.join(",", onlinePeers);
        byte[] enc = security.encrypt(list.getBytes(StandardCharsets.UTF_8));
        byte[] sig = security.sign(enc);
        Message m = new Message(peerId, "all", Message.MessageType.PEER_LIST,
                System.currentTimeMillis(), lamport, 10, enc, sig);
        routingManager.routeMessage(m);
    }

    public void showPeers() {
        System.out.println("=== Online Peers ===");
        onlinePeers.forEach(System.out::println);
    }
}
