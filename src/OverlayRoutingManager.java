import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OverlayRoutingManager
 * ---------------------
 * Maintains a routing table of peerId → Socket and provides
 * helper functions for forwarding and broadcasting messages
 * across an overlay network.
 */
public class OverlayRoutingManager {

    private final Map<String, Socket> routingTable = new ConcurrentHashMap<>();

    /** Adds a peer to the routing table */
    public void addPeer(String peerId, Socket socket) {
        routingTable.put(peerId, socket);
        System.out.println("[Routing] Added peer: " + peerId + " (" + socket.getRemoteSocketAddress() + ")");
    }

    /** Removes a peer from the routing table */
    public void removePeer(String peerId) {
        routingTable.remove(peerId);
        System.out.println("[Routing] Removed peer: " + peerId);
    }

    /** Retrieves the socket for a destination peer */
    public Socket getNextHop(String destinationPeerId) {
        return routingTable.get(destinationPeerId);
    }

    /** Core routing logic: handles group vs private delivery */
    public void routeMessage(Message message) throws Exception {
        if (message.getMessageType() == Message.MessageType.GROUP) {
            broadcastMessage(message);
        } else {
            forwardToPeer(message.getReceiverId(), message);
        }
    }

    /** Send message to all connected peers (GROUP) */
    private void broadcastMessage(Message message) throws Exception {
        for (Map.Entry<String, Socket> entry : routingTable.entrySet()) {
            if (!entry.getValue().isClosed()) {
                sendMessageToSocket(entry.getValue(), message);
            }
        }
    }

    /** Send to a specific peer (PRIVATE or FILE) */
    private void forwardToPeer(String peerId, Message message) throws Exception {
        Socket socket = getNextHop(peerId);
        if (socket == null || socket.isClosed()) {
            System.err.println("[Routing] No route to peer: " + peerId);
            return;
        }
        sendMessageToSocket(socket, message);
    }

    /** Helper for serializing and sending a message */
    private void sendMessageToSocket(Socket socket, Message message) throws Exception {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(message);
        out.flush();
    }

    /** Debug helper: show routing table contents */
    public void printRoutingTable() {
        System.out.println("=== Routing Table ===");
        for (var e : routingTable.entrySet()) {
            System.out.println(e.getKey() + " → " + e.getValue().getRemoteSocketAddress());
        }
    }
}
