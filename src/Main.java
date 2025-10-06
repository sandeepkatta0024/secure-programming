import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter peerId: ");
        String id = sc.nextLine();
        System.out.print("Enter port: ");
        int port = Integer.parseInt(sc.nextLine());

        PeerNode peer = new PeerNode(id, port);
        peer.start();

        while (true) {
            System.out.print("> ");
            String cmd = sc.nextLine().trim();

            if (cmd.startsWith("/connect")) {
                String[] p = cmd.split(" ");
                peer.connect(p[1], p[2], Integer.parseInt(p[3]));
            } else if (cmd.startsWith("/msg")) {
                String[] p = cmd.split(" ", 3);
                peer.sendMessage(p[1], Message.MessageType.PRIVATE, p[2]);
            } else if (cmd.startsWith("/broadcast")) {
                peer.sendMessage("all", Message.MessageType.GROUP, cmd.substring(11));
            } else if (cmd.startsWith("/sendfile")) {
                String[] p = cmd.split(" ", 3);
                peer.sendFile(p[1], p[2]);
            } else if (cmd.equals("/peers")) {
                peer.showPeers();
            } else if (cmd.equals("/exit")) {
                System.exit(0);
            } else {
                System.out.println("Commands:\n" +
                        "/connect <peerId> <host> <port>\n" +
                        "/msg <peerId> <text>\n" +
                        "/broadcast <text>\n" +
                        "/sendfile <peerId> <path>\n" +
                        "/peers\n/exit");
            }
        }
    }
}
