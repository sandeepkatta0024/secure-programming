import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OverlayRoutingManager {
    private final Map<String, Socket> routingTable = new ConcurrentHashMap<>();

    public void addPeer(String id, Socket socket) {
        routingTable.put(id, socket);
        System.out.println("[Routing] Added peer " + id);
    }

    public void removePeer(String id) {
        routingTable.remove(id);
        System.out.println("[Routing] Removed peer " + id);
    }

    public Map<String, Socket> getRoutingTable() {
        return routingTable;
    }

    public void routeMessage(Message msg) throws Exception {
        if (msg.getMessageType() == Message.MessageType.GROUP ||
                "all".equalsIgnoreCase(msg.getReceiverId())) {
            for (Socket s : routingTable.values()) sendMessage(s, msg);
        } else {
            Socket dest = routingTable.get(msg.getReceiverId());
            if (dest != null && !dest.isClosed()) sendMessage(dest, msg);
            else System.err.println("[Routing] No route to " + msg.getReceiverId());
        }
    }

    private void sendMessage(Socket s, Message msg) throws Exception {
        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        out.writeObject(msg);
        out.flush();
    }
}
