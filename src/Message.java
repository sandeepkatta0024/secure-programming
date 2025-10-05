import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageType { PRIVATE, GROUP, FILE }

    // Core addressing
    private String senderId;
    private String receiverId;
    private MessageType messageType;

    // Timing & ordering
    private long timestamp;
    private long lamport;

    // Routing / replay control
    private String messageId;
    private int ttl;

    // Payload integrity
    private byte[] payload;
    private byte[] signature;

    // File transfer (optional)
    private int chunkIndex;
    private int totalChunks;

    /** Full constructor (used by PeerNode). */
    public Message(String senderId, String receiverId, MessageType messageType,
                   long timestamp, long lamport, int ttl,
                   byte[] payload, byte[] signature) {
        this(senderId, receiverId, messageType, timestamp, lamport,
                UUID.randomUUID().toString(), ttl, payload, signature, -1, -1);
    }

    /** Full internal constructor. */
    public Message(String senderId, String receiverId, MessageType messageType,
                   long timestamp, long lamport, String messageId, int ttl,
                   byte[] payload, byte[] signature, int chunkIndex, int totalChunks) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageType = messageType;
        this.timestamp = timestamp;
        this.lamport = lamport;
        this.messageId = (messageId != null) ? messageId : UUID.randomUUID().toString();
        this.ttl = ttl;
        this.payload = payload;
        this.signature = signature;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
    }

    // Getters
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public MessageType getMessageType() { return messageType; }
    public long getTimestamp() { return timestamp; }
    public long getLamport() { return lamport; }
    public String getMessageId() { return messageId; }
    public int getTtl() { return ttl; }
    public byte[] getPayload() { return payload; }
    public byte[] getSignature() { return signature; }
    public int getChunkIndex() { return chunkIndex; }
    public int getTotalChunks() { return totalChunks; }

    // Setters for in-flight modification
    public void setLamport(long lamport) { this.lamport = lamport; }
    public void setTtl(int ttl) { this.ttl = ttl; }
}

