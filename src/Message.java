import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        PRIVATE, GROUP, FILE, FILE_ACK, FILE_FAIL, PEER_LIST, HELLO
    }


    private String senderId;
    private String receiverId;
    private MessageType messageType;
    private long timestamp;
    private long lamport;
    private String messageId;
    private int ttl;
    private byte[] payload;
    private byte[] signature;
    private int chunkIndex;
    private int totalChunks;

    public Message(String senderId, String receiverId, MessageType type,
                   long timestamp, long lamport, int ttl, byte[] payload, byte[] signature) {
        this(senderId, receiverId, type, timestamp, lamport,
                UUID.randomUUID().toString(), ttl, payload, signature, -1, -1);
    }

    public Message(String senderId, String receiverId, MessageType type,
                   long timestamp, long lamport, String msgId, int ttl,
                   byte[] payload, byte[] signature, int chunkIdx, int totalChunks) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageType = type;
        this.timestamp = timestamp;
        this.lamport = lamport;
        this.messageId = msgId;
        this.ttl = ttl;
        this.payload = payload;
        this.signature = signature;
        this.chunkIndex = chunkIdx;
        this.totalChunks = totalChunks;
    }

    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public MessageType getMessageType() { return messageType; }
    public long getTimestamp() { return timestamp; }
    public long getLamport() { return lamport; }
    public String getMessageId() { return messageId; }
    public int getTtl() { return ttl; }
    public byte[] getPayload() { return payload; }
    public byte[] getSignature() { return signature; }

    public void setTtl(int ttl) { this.ttl = ttl; }
    public void setLamport(long lamport) { this.lamport = lamport; }
}
