import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OverlayRoutingManager {
    private final Map<String, Socket> routingTable = new ConcurrentHashMap<>();
    private final Map<String, ObjectOutputStream> streamTable = new ConcurrentHashMap<>();



    public void removePeer(String id) {
        routingTable.remove(id);
        streamTable.remove(id);
        System.out.println("[Routing] Removed peer " + id);
    }

    public Map<String, Socket> getRoutingTable() {
        return routingTable;
    }

    public void routeMessage(Message msg) throws Exception {
        if (msg.getMessageType() == Message.MessageType.GROUP ||
                "all".equalsIgnoreCase(msg.getReceiverId())) {
            for (ObjectOutputStream out : streamTable.values()) {
                synchronized (out) {
                    out.writeObject(msg);
                    out.flush();
                }
            }
        } else {
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
    public void addPeer(String id, Socket socket, ObjectOutputStream out) {
        routingTable.put(id, socket);
        streamTable.put(id, out);
        System.out.println("[Routing] Added peer " + id);
    }
    public void addPeer(String id, Socket socket) throws Exception {
        routingTable.put(id, socket);
        streamTable.put(id, new ObjectOutputStream(socket.getOutputStream()));
        System.out.println("[Routing] Added peer " + id);
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

    public void printPeers() {
        System.out.println("=== Connected Peers ===");
        routingTable.keySet().forEach(System.out::println);
    }
}
