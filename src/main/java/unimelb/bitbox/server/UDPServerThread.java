package unimelb.bitbox.server;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.eventprocess.*;
import unimelb.bitbox.messages.InvalidProtocolType;
import unimelb.bitbox.messages.MessageValidator;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.peer.UDPPeer;
import unimelb.bitbox.peer.UDPPeerManager;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Start a UDP Server thread
 */
public class UDPServerThread extends ServerThread {
    private static Logger log = Logger.getLogger(UDPServerThread.class.getName());

    private DatagramSocket serverSocket;
    private int blockSize;

    private ExecutorService backgroundExecutor;

    public UDPServerThread(FileSystemManager fileSystemManager) throws IOException {
        super("UDPServer", fileSystemManager);

        // Start the server with the appropriate port number
        int portNumber = Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
        serverSocket = new DatagramSocket(portNumber);

        // Get the blocksize of datagram packet
        blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));

        // Set up a background thread to handle whatever incoming request received
        this.backgroundExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Listen for incoming packets from the client
     */
    @Override
    public void run() {
        // Send a HANDSHAKE_REQUEST to all the peers in the configuration file
        // Connect to all the peers first
        String[] peers = Configuration.getConfigurationValue("peers").split(",");
        for (String peer : peers) {
            UDPPeerManager.getInstance().addPeer(serverSocket, peer, fileSystemManager);
        }

        // Listen for any messages
        while (true) {
            try {
                // Create a new buffer to accept requests
                byte[] buf = new byte[blockSize];

                // Receive the request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);

                // Get the message from the packet in UTF-8 format
                String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                HostPort remoteHostPort = new HostPort(packet.getAddress().getHostName(), packet.getPort());

                //TODO remoteHostPort checking to ensure that nothing received from hostport which has been disconnected

                processMessage(message, remoteHostPort);
            }
            // Caught exception so exit and log to user
            catch (IOException e) {
                e.printStackTrace();
                log.severe("error occurred when accepting a packet");
                serverSocket.close();
                return;
            }
        }
    }

    /**
     * Helper method to submit the appropriate runnable to the background thread.
     * @param message
     */
    private void processMessage(String message, HostPort remoteHostPort) {
        // Peer has closed the peer and EOF received
        if (message == null) {
            log.info("peer is disconnected");
            UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
            return;
        }

        Document doc = Document.parse(message);
        String command = doc.getString("command");

        //System.out.println("Received: " + command);  // Debugging async

        // Get the state of the peer
        UDPPeer peer = UDPPeerManager.getInstance().getPeer(remoteHostPort);

        UDPPeer.STATE peerState;
        if (peer == null) {
            peerState = null;
        } else {
            peerState = peer.getState();
        }

        // Received INVALID_PROTOCOL, close the peer
        if (command.equals(Messages.INVALID_PROTOCOL)) {
            UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
            return;
        }

        // Peer does not exist, so can only accept HANDSHAKE_REQUEST
        if (peerState == null) {
            if (command.equals(Messages.HANDSHAKE_REQUEST)) {
                // Need to change the remote host port because it depends on the advertised name
                UDPPeerManager.getInstance().addPeer(serverSocket, remoteHostPort, fileSystemManager);
            }
            else {
                backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.INVALID_COMMAND));
            }
        }

        // Peer is in handshaking state so should only accept HANDSHAKE_RESPONSE or CONNECTION_REFUSED
        else if (peerState == UDPPeer.STATE.HANDSHAKE) {
            // Received a handshake response. This means everything went well and peer is remembered
            if (command.equals(Messages.HANDSHAKE_RESPONSE)) {
                UDPPeerManager.getInstance().setStateOfPeer(remoteHostPort, UDPPeer.STATE.OK);
                peer.cancelRetry(doc);
            }
            else if (command.equals(Messages.CONNECTION_REFUSED)) {
                // Get the other peers list
                // Get the list of peers
                Object o = doc.get("peers");
                if (o instanceof ArrayList) {
                    ArrayList<Document> peerDocs = (ArrayList) o;

                    // Convert documents to HostPorts
                    ArrayList<HostPort> otherPeers = new ArrayList<>();
                    for (Document peerDoc : peerDocs) {
                        otherPeers.add(new HostPort(peerDoc));
                    }

                    UDPPeerManager.getInstance().onConnectionRefused(remoteHostPort, otherPeers);
                    peer.cancelRetry(doc);
                } else {
                    // Send INVALID_PROTOCOL and close connection
                    backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.CUSTOM, "invalid list of peers"));
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                }
            }
            // Received wrong command, so need to send an INVALID_PROTOCOL
            else {
                backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.INVALID_COMMAND));
                UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
            }
        }

        else if (peerState == UDPPeer.STATE.OK) {
            // Received a connection request again, means the message has got lost. So we can just send a HANDSHAKE_RESPONSE
            // since the previous message was a HANDSHAKE_RESPONSE
            if (command.equals(Messages.HANDSHAKE_REQUEST)) {
                backgroundExecutor.submit(new EventProcess() {
                    @Override
                    public void run() {
                        sendMessage(Messages.genHandshakeResponse(ServerMain.getLocalHostPort()));
                    }
                });
            }
            else if (command.equals(Messages.FILE_CREATE_REQUEST)) {
                String createRequest = MessageValidator.getInstance().validateFileChangeRequest(doc);
                if (createRequest != null) {
                    backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD));
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                } else {
                    backgroundExecutor.submit(new FileCreateResponse(serverSocket, remoteHostPort, doc, fileSystemManager));
                }
            } else if (command.equals(Messages.FILE_CREATE_RESPONSE)) {
                String createResponse = MessageValidator.getInstance().validateFileChangeResponse(doc);
                if (createResponse != null) {
                    backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, createResponse));
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                }
                peer.cancelRetry(doc);
            } else if (command.equals(Messages.FILE_DELETE_REQUEST)) {
                String deleteRequest = MessageValidator.getInstance().validateFileChangeRequest(doc);
                if (deleteRequest != null) {
                    backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, deleteRequest));
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                } else {
                    backgroundExecutor.submit(new FileDeleteResponse(serverSocket, remoteHostPort, doc, fileSystemManager));
                }
            } else if (command.equals(Messages.FILE_DELETE_RESPONSE)) {
                String deleteResponse = MessageValidator.getInstance().validateFileChangeResponse(doc);
                if (deleteResponse != null) {
                    backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, deleteResponse));
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                }
                peer.cancelRetry(doc);
            } else if (command.equals(Messages.FILE_MODIFY_REQUEST)) {
                String modifyRequest = MessageValidator.getInstance().validateFileChangeRequest(doc);
                if (modifyRequest != null) {
                    backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, modifyRequest));
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                } else {
                    backgroundExecutor.submit(new FileModifyResponse(serverSocket, remoteHostPort, doc, fileSystemManager));
                }
            } else if (command.equals(Messages.FILE_MODIFY_RESPONSE)) {
                String modifyResponse = MessageValidator.getInstance().validateFileChangeResponse(doc);
                if (modifyResponse != null) {
                    backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, modifyResponse));
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                }
                peer.cancelRetry(doc);
            } else if (command.equals(Messages.FILE_BYTES_REQUEST)) {
                String bytesRequest = MessageValidator.getInstance().validateFileBytesRequest(doc);
                if (bytesRequest != null) {
                    backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, bytesRequest));
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                } else {
                    backgroundExecutor.submit(new FileBytesResponse(serverSocket, remoteHostPort, fileSystemManager, doc));
                }
            } else if (command.equals(Messages.FILE_BYTES_RESPONSE)) {
                String bytesResponse = MessageValidator.getInstance().validateFileBytesResponse(doc);
                if (bytesResponse != null) {
                    backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, bytesResponse));
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                } else {
                    backgroundExecutor.submit(new ConstructFile(serverSocket, remoteHostPort, fileSystemManager, doc));
                    peer.cancelRetry(doc);
                }
            } else if (command.equals(Messages.DIRECTORY_CREATE_REQUEST)) {
                String dirCreateRequest = MessageValidator.getInstance().validateDirectoryChangeRequest(doc);
                if (dirCreateRequest != null) {
                    backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, dirCreateRequest));
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                } else {
                    backgroundExecutor.submit(new DirectoryCreateResponse(serverSocket, remoteHostPort, fileSystemManager, doc));
                }
            } else if (command.equals(Messages.DIRECTORY_CREATE_RESPONSE)) {
                String dirCteateResponse = MessageValidator.getInstance().validateDirectoryChangeResponse(doc);
                if (dirCteateResponse != null) {
                    backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, dirCteateResponse));
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                }
                peer.cancelRetry(doc);
            } else if (command.equals(Messages.DIRECTORY_DELETE_REQUEST)) {
                String dirDeleteRequest = MessageValidator.getInstance().validateDirectoryChangeRequest(doc);
                if (dirDeleteRequest != null) {
                    backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, dirDeleteRequest));
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                } else {
                    backgroundExecutor.submit(new DirectoryDeleteResponse(serverSocket, remoteHostPort, fileSystemManager, doc));
                }
            } else if (command.equals(Messages.DIRECTORY_DELETE_RESPONSE)) {
                String dirDeleteResponse = MessageValidator.getInstance().validateDirectoryChangeResponse(doc);
                if (dirDeleteResponse != null) {
                    backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, dirDeleteResponse));
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                }
                peer.cancelRetry(doc);
            } else {
                backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.INVALID_COMMAND));
                UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
            }
        }
    }
}
