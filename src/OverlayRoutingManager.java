import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OverlayRoutingManager {
    private Map<String, Socket> routingTable = new ConcurrentHashMap<>();

    public void addPeer(String peerId, Socket socket) {
        routingTable.put(peerId, socket);
    }

    public void removePeer(String peerId) {
        routingTable.remove(peerId);
    }

    public Socket getNextHop(String destinationPeerId) {
        return routingTable.get(destinationPeerId);
    }

    public void routeMessage(Message message) throws Exception {
        if (message.getMessageType() == Message.MessageType.GROUP) {
            for (Socket peerSocket : routingTable.values()) {
                sendMessageToSocket(peerSocket, message);
            }
        } else {
            Socket nextHopSocket = getNextHop(message.getReceiverId());
            if (nextHopSocket != null && !nextHopSocket.isClosed()) {
                sendMessageToSocket(nextHopSocket, message);
            } else {
                System.err.println("No route to peer " + message.getReceiverId());
            }
        }
    }

    private void sendMessageToSocket(Socket socket, Message message) throws Exception {
        var out = socket.getOutputStream();
        var objOut = new java.io.ObjectOutputStream(out);
        objOut.writeObject(message);
        objOut.flush();
    }
}
