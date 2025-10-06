===========================================================
        SECURE OVERLAY CHAT SYSTEM (VULNERABLE VERSION)
===========================================================

Group Name: SecureVibes
University: The University of Adelaide
Course: Advanced Secure Programming (2025)
Assignment: Secure Overlay Chat System – Vulnerable Version
Submission: Week 9 – Implementation for Peer Review

-----------------------------------------------------------
OVERVIEW
-----------------------------------------------------------
This is a peer-to-peer (overlay) chat system developed in Java as part of the
Advanced Secure Programming assignment. The system allows multiple peers to
communicate securely without a central server. It supports private messaging,
group broadcasting, and point-to-point file transfer.

This submitted version *intentionally contains ethical vulnerabilities and
backdoors* as required by the assignment. These are deliberately not disclosed
in this README. Reviewers are expected to identify and analyse them during the
Week 10 peer-review and hackathon exercises.

-----------------------------------------------------------
SYSTEM FEATURES
-----------------------------------------------------------
1. Secure Handshake using RSA/AES hybrid encryption.
2. Encrypted private and group messages.
3. File transfer between peers.
4. Lamport timestamps for message ordering.
5. Overlay-based routing (no central authority).
6. Dynamic peer discovery and automatic connection.

-----------------------------------------------------------
SYSTEM REQUIREMENTS
-----------------------------------------------------------
• Java SE 17 or newer
• Works on macOS, Linux, or Windows
• No external dependencies (uses built-in Java libraries)

-----------------------------------------------------------
COMPILATION INSTRUCTIONS
-----------------------------------------------------------
1. Open terminal in the source folder containing `.java` files.
2. Compile all files:
       javac *.java
3. Run the main program:
       java Main

-----------------------------------------------------------
RUNNING EXAMPLES
-----------------------------------------------------------
Example 1 – Start first peer:
    java Main
    Enter peerId: A
    Enter port: 7100

Example 2 – Start second peer:
    java Main
    Enter peerId: B
    Enter port: 7200
    /connect A localhost 7100

Example 3 – Start third peer:
    java Main
    Enter peerId: C
    Enter port: 7300
    /connect B localhost 7200

-----------------------------------------------------------
COMMANDS
-----------------------------------------------------------
/connect <peerId> <host> <port>   Connect to another peer.
/msg <peerId> <message>          Send a private message.
/broadcast <message>             Send a group message to all peers.
/sendfile <peerId> <path>        Send a file to another peer.
/peers                           Show currently online peers.
/login <username> <password>     [Feature reserved for testing – do not disclose details.]
/exit                            Quit the program.

-----------------------------------------------------------
DEMO WALKTHROUGH (EXAMPLE)
-----------------------------------------------------------

This example demonstrates how three peers (A, B, and C) can securely connect
to each other, exchange private and group messages, and transfer a file.

🟩 Step 1 – Start Peer A (acts as initial listener)
----------------------------------------------------
$ java Main
Enter peerId: A
Enter port: 7100
> A listening on port 7100


🟩 Step 2 – Start Peer B and connect to A
----------------------------------------------------
$ java Main
Enter peerId: B
Enter port: 7200
> B listening on port 7200
/connect A localhost 7100
[Handshake] B connected securely to A
> [Updated Online Peers] [A, B]


🟩 Step 3 – Start Peer C and connect to B
----------------------------------------------------
$ java Main
Enter peerId: C
Enter port: 7300
> C listening on port 7300
/connect B localhost 7200
[Handshake] C connected securely to B
> [Updated Online Peers] [A, B, C]


🟩 Step 4 – Private Messaging
----------------------------------------------------
From B → A:
/msg A Hello A
Output on A’s console:
[B] Hello A

From A → C:
/msg C Hey C
Output on C’s console:
[A] Hey C


🟩 Step 5 – Group Broadcast
----------------------------------------------------
From any peer:
/broadcast Hello Everyone!
Output appears on all connected peers as:
[Group][<sender>] Hello Everyone!


🟩 Step 6 – File Transfer
----------------------------------------------------
From A → C:
/sendfile C /Users/<username>/Desktop/sample.txt
Output:
[File] Transfer to C succeeded.
On peer C:
[File] Received from A -> recv_A_<timestamp>


🟩 Step 7 – List Online Peers
----------------------------------------------------
/peers
Output:
=== Online Peers ===
A
B
C


🟩 Step 8 – Exit the program
----------------------------------------------------
/exit


-----------------------------------------------------------
REVIEWER INSTRUCTIONS
-----------------------------------------------------------
1. Compile and run using the commands above.
2. Test interoperability by connecting multiple peers on localhost.
3. Explore functionality through provided commands.
4. Review the source code manually and/or with static/dynamic tools to identify
   embedded vulnerabilities and ethical backdoors.
5. Provide constructive feedback focusing on:
     – secure coding practices
     – protocol design adherence
     – potential exploit scenarios

-----------------------------------------------------------
SUPPORT & CONTACT
-----------------------------------------------------------
For clarification or collaboration during peer review:

    Contact: Vamsi Krishna Chirumamilla
    Email:  a1979571@adelaide.edu.au

    Contact: Sandeep Katta
    Email:   a1990024@adelaide.edu.au

    Contact: Rupesh Ashok Kumar
    Email:   a1983942@adelaide.edu.au

We welcome feedback to improve both code security and interoperability.
