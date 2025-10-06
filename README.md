# 🔐 Secure Peer-to-Peer Chat Application

## 🧩 Overview
This project implements a **secure peer-to-peer (P2P) communication system** in Java that supports:

- 🔒 End-to-end encrypted messaging using **RSA-AES hybrid encryption**
- 🗂️ Secure file transfer with acknowledgement (ACK/FAIL)
- 👥 Dynamic peer discovery and **multi-peer broadcast messaging**
- 🧠 Lamport logical clocks for ordering events
- 💬 Private messaging, group broadcast, and online peer listing

Each peer runs as an independent node and can connect to others to form a secure overlay chat network.

---

## 🏗️ Architecture

### Components
| Class | Responsibility |
|--------|----------------|
| **Main.java** | CLI entry point; handles user commands and session control |
| **PeerNode.java** | Core P2P logic — handles connections, encryption, message routing, and file transfer |
| **HandshakeManager.java** | Performs RSA key exchange and AES session key setup |
| **SecurityManager.java** | Manages AES-GCM encryption/decryption and RSA signing |
| **OverlayRoutingManager.java** | Maintains routing table for active peers and message forwarding |
| **Message.java** | Serializable message container with metadata (sender, type, payload, etc.) |

---

## 🔐 Security Design

| Layer | Algorithm | Purpose |
|--------|------------|----------|
| **Key Exchange** | RSA (2048 bits) | Establishes a secure AES session key |
| **Session Encryption** | AES-256-GCM | Provides confidentiality + integrity |
| **Message Signing** | SHA-256 with RSA | Prevents tampering |
| **Transport** | Java sockets | Peer-to-peer over TCP |

Each peer negotiates a **unique AES session key** with every other peer via RSA handshake.  
All messages and files are encrypted using AES-GCM, which ensures both confidentiality and authenticity.

---

## ⚙️ Setup & Running

### 🧾 Prerequisites
- Java 17 or later
- Terminal access (macOS, Linux, or Windows PowerShell)

### 🧩 Compilation
```bash
javac *.java


💬 Commands
Command	Description	Example
/connect <peerId> <host> <port>	Connects securely to another peer	/connect B localhost 7200
/msg <peerId> <text>	Sends a private message	/msg B Hello there!
/broadcast <text>	Sends a message to all connected peers	/broadcast Hi everyone!
/sendfile <peerId> <path>	Sends an encrypted file to a peer	/sendfile C /Users/sandeep/Desktop/file.txt
/peers	Lists currently online peers (excluding self)	/peers
/exit	Exits the chat application	/exit



# A
> A listening on port 7100
/connect B localhost 7200
[Handshake] A connected securely to B
/msg B Hello B
[File] Transfer to B succeeded.

# B
> B listening on port 7200
/connect A localhost 7100
[Handshake] B connected securely to A
[A] Hello B
[Updated Online Peers] [A, B, C]
[File] Received from A -> recv_A_1759743357369

# C
> C listening on port 7300
/connect B localhost 7200
[Handshake] C connected securely to B
/broadcast Hello everyone!
