package unimelb.bitbox.connection;

import unimelb.bitbox.messages.Commands;
import unimelb.bitbox.messages.MessageGenerator;
import unimelb.bitbox.runnables.BaseRunnable;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class OutgoingConnection extends Connection {
    private ArrayList<HostPort> toConnect;

    public OutgoingConnection(LinkedBlockingQueue<Runnable> queue, HostPort localHostPort, HostPort remoteHostPort) {
        super(queue, localHostPort);

        this.toConnect = new ArrayList<>();
        this.toConnect.add(remoteHostPort);

        // Submit a handshake runnable to the listener
        this.listener.submit(new Handshake(output));
    }

    private class Handshake extends BaseRunnable {
        Handshake(DataOutputStream output) {
            super(output);
        }

        @Override
        public void run() {
            try {
                // Loop to search for peers until finally connected to one
                while (!toConnect.isEmpty()) {
                    HostPort remoteHostPort = toConnect.remove(0);
                    try {
                        socket = new Socket(remoteHostPort.host, remoteHostPort.port);
                    } catch (IOException e1) {
                        System.out.println("Unable to connect to " + remoteHostPort.toString() + ". Peer could be offline");
                        return;
                    }
                    // Setup a new writer and reader for the new socket
                    createWriterAndReader();
                    updateOutput(output);

                    sendMessage(MessageGenerator.genHandshakeRequest(localHostPort));

                    Document response = Document.parse(input.readUTF());
                    //System.out.println(response.toJson());

                    String command = response.getString("command");

                    // Connected so just exit this and proceed to listen for file events
                    if (command.equals(Commands.HANDSHAKE_RESPONSE.toString())) {
                        ConnectionManager.getInstance().connectedPeer(remoteHostPort, false);
                        listener.submit(new Listener());
                        toConnect.clear(); // Clear to connect since already connected
                        return;
                    }
                    // Connection refused
                    // Add the peers to connect to end of the queue to simulate breadth-first search of peers
                    else if (command.equals(Commands.CONNECTION_REFUSED.toString())) {
                        // If Connection is refused, start a new connection to the other peers
                        // Close the current socket first
                        // TODO maybe a try-catch here
                        socket.close();
                        System.out.println("Connection closed");

                        // Add more peer host ports to to be connected list
                        @SuppressWarnings("unchecked")
                        ArrayList<Document> peersDoc = (ArrayList<Document>) response.get("peers");
                        for (Document peerDoc : peersDoc) {
                            System.out.println(peerDoc.toJson());
                            toConnect.add(new HostPort(peerDoc));
                        }

                        System.out.println(toConnect);
                    }
                    // Received an invalid message so send INVALID_PROTOCOL message
                    // TODO add a loop to handle invalid protocol
                    else {
                        sendMessage(MessageGenerator.genInvalidProtocol("Invalid command. Expecting HANDSHAKE_RESPONSE " +
                                "or CONNECTION_REFUSED"));
                    }
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
