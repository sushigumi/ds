package unimelb.bitbox.peer;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.eventprocess.*;
import unimelb.bitbox.messages.MessageInfo;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.*;

//TODO add retry mechanism
public class UDPPeer {
    public enum STATE {
        HANDSHAKE, // In the middle of a handshake process
        OK // After handshake successful
    }

    private ExecutorService sender;
    private ScheduledExecutorService retry;
    private int timeout;

    private STATE state;

    private FileSystemManager fileSystemManager;

    private DatagramSocket serverSocket;
    private HostPort remoteHostPort;

    private ArrayList<HostPort> queue;

    private ArrayList<MessageInfo> messagesSent;

    public UDPPeer(FileSystemManager fileSystemManager, DatagramSocket serverSocket, HostPort remoteHostPort, boolean isIncoming) {
        this.serverSocket = serverSocket;
        this.remoteHostPort = null;
        this.fileSystemManager = fileSystemManager;
        sender = Executors.newSingleThreadExecutor();
        retry = Executors.newScheduledThreadPool(1);

        // Add the host port to the queue
        queue = new ArrayList<>();
        queue.add(remoteHostPort);

        messagesSent = new ArrayList<>();

        // Read the timeout value and max retries value
        timeout = Integer.parseInt(Configuration.getConfigurationValue("timeout"));

        // Set the state
        this.state = STATE.HANDSHAKE;

        // Start the handshake process
        if (isIncoming) {
            // Set the host port since it is an incoming connection and we will not receive a CONNECTION_REFUSED
            this.remoteHostPort = remoteHostPort;
            onNewIncoming();
        } else {
            onNewOutgoing();
        }
    }

    /**
     * Called when the UDP peer is offline
     */
    public void shutdown() {
        sender.shutdownNow();
        retry.shutdownNow();
    }

    /**
     * Queue a runnable to be retried after a certain time
     * @param runnable
     * @param doc
     */
    public void queueRetry(Runnable runnable, Document doc) {

        // Save the document into the message info and hash it
        MessageInfo info = new MessageInfo(doc);

        synchronized (this) {

            int index = messagesSent.indexOf(info);

            if (index == -1) {
                info.setFuture(retry.schedule(runnable, timeout, TimeUnit.SECONDS));
                messagesSent.add(info);
            } else {
                info = messagesSent.get(index);
                if (!info.isExceedRetryLimit()) {
                    info.updateFuture(retry.schedule(runnable, timeout, TimeUnit.SECONDS));
                } else {
                    System.out.println(doc.toJson());
                    UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
                }
            }
        }
    }

    /**
     * Cancel the retry which is about to happen after timeout period reaches
     * @param doc
     */
    public void cancelRetry(Document doc) {
        String command = doc.getString("command");

        MessageInfo toRemove = null;
        synchronized (this) {

            for (MessageInfo info : messagesSent) {
                if (info.getCommand().equals(Messages.HANDSHAKE_REQUEST)) {
                    if (command.equals(Messages.HANDSHAKE_RESPONSE) || command.equals(Messages.CONNECTION_REFUSED)) {
                        toRemove = info;
                    }
                } else if (info.getCommand().equals(Messages.DIRECTORY_CREATE_REQUEST) || info.getCommand().equals(Messages.DIRECTORY_DELETE_REQUEST)) {
                    if (!command.equals(Messages.DIRECTORY_CREATE_RESPONSE) && !command.equals(Messages.DIRECTORY_DELETE_RESPONSE))
                        continue;
                    String pathName = doc.getString("pathName");
                    String otherPathName = info.getDoc().getString("pathName");

                    if (pathName.equals(otherPathName)) {
                        toRemove = info;
                    }
                } else {
                    if (info.getCommand().equals(Messages.FILE_BYTES_REQUEST)) {
                        if (!command.equals(Messages.FILE_BYTES_RESPONSE)) continue;

                        if (!info.getDoc().getString("position").equals(doc.getString("position"))) {
                            continue;
                        }
                        if (!info.getDoc().getString("length").equals(doc.getString("length"))) {
                            continue;
                        }
                    }

                    if (!command.equals(Messages.FILE_CREATE_RESPONSE) && !command.equals(Messages.FILE_DELETE_RESPONSE) &&
                            !command.equals(Messages.FILE_MODIFY_RESPONSE)) {
                        continue;
                    }

                    Document infofd = (Document) info.getDoc().get("fileDescriptor");
                    Document fd = (Document) info.getDoc().get("fileDescriptor");

                    if (!(infofd.getLong("lastModified") == fd.getLong("lastModified"))) continue;

                    if (!infofd.getString("md5").equals(fd.getString("md5"))) continue;

                    if (!(infofd.getLong("fileSize") == fd.getLong("fileSize"))) continue;

                    if (!info.getDoc().getString("pathName").equals(doc.getString("pathName"))) {
                        continue;
                    }

                    toRemove = info;
                }
            }
        }

        int index = messagesSent.indexOf(toRemove);
        
        if (index < 0) return;

        MessageInfo info = messagesSent.remove(index);
        info.getFuture().cancel(false);
    }

    /**
     * Start a handshake, sends a HANDSHAKE_REQUEST to the specified peer to request for a connection. If the connection
     * is refused, then tries to connect with all the others returned from the CONNECTION_REFUSED message.
     */
    private void onNewOutgoing() {
        this.remoteHostPort = queue.remove(0);

        try {
            String ipaddress = InetAddress.getByName(remoteHostPort.host).getHostAddress();
            this.remoteHostPort = new HostPort(ipaddress, remoteHostPort.port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
        }

        // Send a HANDSHAKE_REQUEST to the peer
        sender.submit(new HandshakeReq(serverSocket, remoteHostPort));
    }

    /**
     * Calls generateSyncEvents() from the file system manager and sends it to the peers. Usually called at the
     * start of a peer.
     */
    void syncEvents() {
        ArrayList<FileSystemManager.FileSystemEvent> events = fileSystemManager.generateSyncEvents();

        for (FileSystemManager.FileSystemEvent event : events) {
            processFileSystemEvent(event);
        }
    }

    /**
     * Send a HANDSHAKE_RESPONSE or CONNECTION_REFUSED depending if there are available connections left for the peer
     */
    private void onNewIncoming() {
        // Send HANDSHAKE_RESPONSE
        if (UDPPeerManager.getInstance().isAvailableConnections()) {
            sender.submit(new HandshakeRes(serverSocket, remoteHostPort));
            state = STATE.OK;

            //TODO Update the remote host port to be ip


            // Generate the sync events
            syncEvents();
        }
        // Send CONNECTION_REFUSED
        else {
            sender.submit(new EventProcess() {
                @Override
                public void run() {
                    sendMessage(Messages.genConnectionRefused(UDPPeerManager.getInstance().getConnectedPeers()));
                }
            });

            // Disconnect the peer
            UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
        }
    }

    /**
     * Called after a CONNECTION_REFUSED message is received in the ServerMain.
     */
    public void tryOtherPeer(ArrayList<HostPort> otherPeers) {
        // Add all the other peers to the end of the queue so that it simulates a bfs
        queue.addAll(otherPeers);

        // Start a new connection
        onNewIncoming();
    }

    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        switch(fileSystemEvent.event) {
            case FILE_CREATE:
                sender.submit(new FileCreateRequest(serverSocket, remoteHostPort, fileSystemEvent, this));
                break;

            case FILE_DELETE:
                sender.submit(new FileDeleteRequest(serverSocket, remoteHostPort, fileSystemEvent, this));
                break;

            case FILE_MODIFY:
                sender.submit(new FileModifyRequest(serverSocket, remoteHostPort, fileSystemEvent, this));
                break;

            case DIRECTORY_CREATE:
                sender.submit(new DirectoryCreateRequest(serverSocket, remoteHostPort, fileSystemEvent.pathName, this));
                break;

            case DIRECTORY_DELETE:
                sender.submit(new DirectoryDeleteRequest(serverSocket, remoteHostPort, fileSystemEvent.pathName, this));
                break;
        }
    }

    /**
     * Get the host port of the client
     * @return
     */
    public HostPort getRemoteHostPort() {
        return remoteHostPort;
    }

    /**
     * Set the remote host port and update it from the advertised name to the ip address
     * @param remoteHostPort
     */
    public void setRemoteHostPort(HostPort remoteHostPort) {
        this.remoteHostPort = remoteHostPort;
    }

    /**
     * Get the current state of the UDP client
     * @return
     */
    public STATE getState() {
        return state;
    }

    /**
     * Set the state of the UDP client
     * @param state
     */
    public void setState(STATE state) {
        this.state = state;
    }

    /**
     * Send a handshake request to the peer
     */
    private class HandshakeReq extends EventProcess {
        public HandshakeReq(DatagramSocket socket, HostPort hostPort) {
            super(socket, hostPort, UDPPeer.this);
        }

        @Override
        public void run() {
            // Send handshake request
            sendMessage(Messages.genHandshakeRequest(ServerMain.getLocalHostPort()));
        }
    }

    private class HandshakeRes extends EventProcess {
        public HandshakeRes(DatagramSocket socket, HostPort hostPort) {
            super(socket, hostPort);
        }

        @Override
        public void run() {
            // Send handshake response
            sendMessage(Messages.genHandshakeResponse(ServerMain.getLocalHostPort()));
        }
    }

}
