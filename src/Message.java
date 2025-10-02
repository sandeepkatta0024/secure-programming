import java.io.Serializable;

public class Message implements Serializable {
    public enum MessageType { PRIVATE, GROUP, FILE }

    private String senderId;
    private String receiverId; // Group ID or single peer
    private MessageType messageType;
    private long timestamp;
    private byte[] payload; // encrypted text or file chunk
    private byte[] signature;
    private int chunkIndex;        // for file chunks
    private int totalChunks;

    public Message(String senderId, String receiverId, MessageType messageType,
                   long timestamp, byte[] payload, byte[] signature) {
        this(senderId, receiverId, messageType, timestamp, payload, signature, -1, -1);
    }

    public Message(String senderId, String receiverId, MessageType messageType,
                   long timestamp, byte[] payload, byte[] signature,
                   int chunkIndex, int totalChunks) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageType = messageType;
        this.timestamp = timestamp;
        this.payload = payload;
        this.signature = signature;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
    }


    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public MessageType getMessageType() { return messageType; }
    public long getTimestamp() { return timestamp; }
    public byte[] getPayload() { return payload; }
    public byte[] getSignature() { return signature; }
    public int getChunkIndex() { return chunkIndex; }
    public int getTotalChunks() { return totalChunks; }
}
