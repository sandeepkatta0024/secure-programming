import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OverlayRoutingManager
 * ----------------------
 * Handles routing and broadcast propagation across multiple peers.
 * Prevents message loops using messageId caching.
 */
public class OverlayRoutingManager {
    private final Map<String, Socket> routingTable = new ConcurrentHashMap<>();
    private final Map<String, ObjectOutputStream> streamTable = new ConcurrentHashMap<>();
    private final Set<String> seenMessages = ConcurrentHashMap.newKeySet();

    // ==================== Add / Remove Peers ====================
    public void addPeer(String id, Socket socket, ObjectOutputStream out) {
        routingTable.put(id, socket);
        streamTable.put(id, out);
        System.out.println("[Routing] Added peer " + id);
    }

    public void addPeer(String id, Socket socket) throws Exception {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        addPeer(id, socket, out);
    }

    public void removePeer(String id) {
        routingTable.remove(id);
        streamTable.remove(id);
        System.out.println("[Routing] Removed peer " + id);
    }

    public void updatePeerId(Socket socket, String newId) {
        Optional<String> oldKey = routingTable.entrySet()
                .stream()
                .filter(e -> e.getValue().equals(socket))
                .map(Map.Entry::getKey)
                .findFirst();

        oldKey.ifPresent(old -> {
            routingTable.remove(old);
            ObjectOutputStream out = streamTable.remove(old);
            routingTable.put(newId, socket);
            streamTable.put(newId, out);
            System.out.println("[Routing] Peer " + old + " renamed to " + newId);
        });
    }

    // ==================== Main Routing Logic ====================
    public void routeMessage(Message msg) throws Exception {
        if (!seenMessages.add(msg.getMessageId())) {
            // Already processed this message; avoid loops
            return;
        }

        if (msg.getMessageType() == Message.MessageType.GROUP ||
                msg.getMessageType() == Message.MessageType.PEER_LIST ||
                "all".equalsIgnoreCase(msg.getReceiverId())) {

            // Broadcast to all peers except sender
            for (Map.Entry<String, ObjectOutputStream> entry : streamTable.entrySet()) {
                String peerId = entry.getKey();
                ObjectOutputStream out = entry.getValue();

                if (!peerId.equals(msg.getSenderId())) {
                    synchronized (out) {
                        out.writeObject(msg);
                        out.flush();
                    }
                }
            }
        } else {
            // Direct message
            ObjectOutputStream out = streamTable.get(msg.getReceiverId());
            if (out != null) {
                synchronized (out) {
                    out.writeObject(msg);
                    out.flush();
                }
            } else {
                System.err.println("[Routing] No route to " + msg.getReceiverId());
            }
        }
    }

    public void printPeers() {
        System.out.println("=== Connected Peers ===");
        routingTable.keySet().forEach(System.out::println);
    }
}
