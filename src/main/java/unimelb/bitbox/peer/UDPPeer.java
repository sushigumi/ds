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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.logging.Logger;

//TODO add retry mechanism
public class UDPPeer {
    private Logger log = Logger.getLogger(UDPPeer.class.getName());

    private final int MAX_LIMIT = Integer.parseInt(Configuration.getConfigurationValue("retries"));

    public enum STATE {
        HANDSHAKE, // In the middle of a handshake process
        OK // After handshake successful
    }

    private ExecutorService sender;
    private ScheduledExecutorService retry;
    private int timeout;

    MessageDigest messageDigest;

    private STATE state;

    private FileSystemManager fileSystemManager;

    private DatagramSocket serverSocket;
    private HostPort advertisedHostPort;
    private HostPort remoteHostPort;

    private ArrayList<HostPort> queue;

    private HashMap<String, Pair> futures;

    public UDPPeer(FileSystemManager fileSystemManager, DatagramSocket serverSocket, HostPort remoteHostPort, boolean isIncoming) {
        this.serverSocket = serverSocket;
        this.remoteHostPort = null;
        this.fileSystemManager = fileSystemManager;
        sender = Executors.newSingleThreadExecutor();
        retry = Executors.newScheduledThreadPool(1);

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            log.severe("error getting hash function");
        }

        // Add the host port to the queue
        queue = new ArrayList<>();
        queue.add(remoteHostPort);

        futures = new HashMap<>();

        // Read the timeout value and max retries value
        timeout = Integer.parseInt(Configuration.getConfigurationValue("timeout"));

        // Set the state
        this.state = STATE.HANDSHAKE;

        // Start the handshake process
        if (isIncoming) {
            // Set the host port since it is an incoming connection and we will not receive a CONNECTION_REFUSED
            this.advertisedHostPort = remoteHostPort;
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
        sender.shutdown();
        retry.shutdown();
    }

    /**
     * Queue a retry
     * @param runnable
     * @param doc
     */
    public void queueRetry(Runnable runnable, Document doc) {
        String command = doc.getString("command");
        String msg;
        String digest;

        if (command.equals(Messages.HANDSHAKE_REQUEST)) {
            Document hostPort = (Document) doc.get("hostPort");

            msg = command + hostPort.toJson();

            messageDigest.update(msg.getBytes());

            digest = messageDigest.toString();
        }
        else if (command.equals(Messages.FILE_DELETE_REQUEST) || command.equals(Messages.FILE_MODIFY_REQUEST) ||
        command.equals(Messages.FILE_CREATE_REQUEST)) {
            Document fd = (Document) doc.get("fileDescriptor");
            String pathName = doc.getString("pathName");

            msg = command + fd.toJson() + pathName;

            messageDigest.update(msg.getBytes());

            digest = messageDigest.toString();
        }
        else if (command.equals(Messages.DIRECTORY_CREATE_REQUEST) || command.equals(Messages.DIRECTORY_DELETE_REQUEST)) {
            String pathName = doc.getString("pathName");

            msg = command + pathName;

            messageDigest.update(msg.getBytes());

            digest = messageDigest.toString();
        }
        else if (command.equals(Messages.FILE_BYTES_REQUEST)) {
            Document fd = (Document) doc.get("fileDescriptor");
            String pathName = doc.getString("pathName");
            long length = doc.getLong("length");
            long position = doc.getLong("position");

            msg = command + fd.toJson() + pathName + position + length;

            messageDigest.update(msg.getBytes());

            digest = messageDigest.toString();
        }
        else {
            digest = null;
        }

        if (digest != null) {
            if (futures.containsKey(digest)) {
                Pair pair = futures.get(digest);
                pair.future.cancel(true);
                if (!pair.isExceedLimit()) {
                    pair.future = retry.schedule(runnable, timeout, TimeUnit.SECONDS);
                    pair.incRetries();
                }else {
                    futures.remove(digest);
                    close();
                }

            }else {
                futures.put(digest, new Pair(retry.schedule(runnable, timeout, TimeUnit.SECONDS)));
            }
        }
    }

    /**
     * Cancel a pending retry
     * @param doc
     */
    public void cancelRetry(Document doc) {
        String command = doc.getString("command");
        String msg;
        String digest;

        // TODO connection refused
        if (command.equals(Messages.HANDSHAKE_RESPONSE)) {
            Document hostPort = (Document) doc.get("hostPort");

            msg = Messages.HANDSHAKE_REQUEST + hostPort.toJson();

            messageDigest.update(msg.getBytes());

            digest = messageDigest.toString();
        }
        else if (command.equals(Messages.FILE_DELETE_RESPONSE) || command.equals(Messages.FILE_MODIFY_RESPONSE) ||
                command.equals(Messages.FILE_CREATE_RESPONSE)) {
            Document fd = (Document) doc.get("fileDescriptor");
            String pathName = doc.getString("pathName");

            String commandToDigest = null;
            if (command.equals(Messages.FILE_DELETE_RESPONSE)) {
                commandToDigest = Messages.FILE_DELETE_REQUEST;
            }
            else if (command.equals(Messages.FILE_MODIFY_RESPONSE)) {
                commandToDigest = Messages.FILE_MODIFY_REQUEST;
            }
            else if (command.equals(Messages.FILE_CREATE_RESPONSE)) {
                commandToDigest = Messages.FILE_CREATE_REQUEST;
            }

            msg = commandToDigest + fd.toJson() + pathName;

            messageDigest.update(msg.getBytes());

            digest = messageDigest.toString();
        }
        else if (command.equals(Messages.DIRECTORY_CREATE_RESPONSE) || command.equals(Messages.DIRECTORY_DELETE_RESPONSE)) {
            String pathName = doc.getString("pathName");

            String commandToDigest;
            if (command.equals(Messages.DIRECTORY_CREATE_RESPONSE)) {
                commandToDigest = Messages.DIRECTORY_CREATE_REQUEST;
            }
            else if (command.equals(Messages.DIRECTORY_DELETE_RESPONSE)) {
                commandToDigest = Messages.DIRECTORY_DELETE_REQUEST;
            }
            else {
                commandToDigest = null;
            }

            msg = commandToDigest + pathName;

            messageDigest.update(msg.getBytes());

            digest = messageDigest.toString();
        }
        else if (command.equals(Messages.FILE_BYTES_RESPONSE)) {
            Document fd = (Document) doc.get("fileDescriptor");
            String pathName = doc.getString("pathName");
            long length = doc.getLong("length");
            long position = doc.getLong("position");

            msg = Messages.FILE_BYTES_REQUEST + fd.toJson() + pathName + position + length;

            messageDigest.update(msg.getBytes());

            digest = messageDigest.toString();
        }
        else {
            digest = null;
        }

        // Remove the pair
        if (digest != null) {
            Pair pair = futures.remove(digest);
            if (pair != null) {
                pair.future.cancel(true);
            }
        }
    }

    private class Pair {
        int retries;
        ScheduledFuture future;

        public Pair(ScheduledFuture future) {
            this.future = future;
            this.retries = 0;
        }

        public void incRetries() {
            retries++;
        }


        public boolean isExceedLimit() {
            return retries >= MAX_LIMIT;
        }
    }

    /**
     * Start a handshake, sends a HANDSHAKE_REQUEST to the specified peer to request for a connection. If the connection
     * is refused, then tries to connect with all the others returned from the CONNECTION_REFUSED message.
     */
    private void onNewOutgoing() {
        this.remoteHostPort = queue.remove(0);

//        try {
//            String ipaddress = InetAddress.getByName(remoteHostPort.host).getHostAddress();
//            this.remoteHostPort = new HostPort(ipaddress, remoteHostPort.port);
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//            UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
//        }

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
            //syncEvents();
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
                if (!sender.isShutdown()) {
                    sender.submit(new FileCreateRequest(serverSocket, remoteHostPort, fileSystemEvent, this));
                }
                break;

            case FILE_DELETE:
                if (!sender.isShutdown()) {
                    sender.submit(new FileDeleteRequest(serverSocket, remoteHostPort, fileSystemEvent, this));
                }
                break;

            case FILE_MODIFY:
                if (!sender.isShutdown()) {
                    sender.submit(new FileModifyRequest(serverSocket, remoteHostPort, fileSystemEvent, this));
                }
                break;

            case DIRECTORY_CREATE:
                if (!sender.isShutdown()) {
                    sender.submit(new DirectoryCreateRequest(serverSocket, remoteHostPort, fileSystemEvent.pathName, this));
                }
                break;

            case DIRECTORY_DELETE:
                if (!sender.isShutdown()) {
                    sender.submit(new DirectoryDeleteRequest(serverSocket, remoteHostPort, fileSystemEvent.pathName, this));
                }
                break;
        }
    }

    private void close() {
        sender.shutdown();
        retry.shutdown();
        UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
    }

    /**
     * Get the host port of the client
     * @return
     */
    public HostPort getRemoteHostPort() {
        return remoteHostPort;
    }


    /**
     * Get the advertised host port of the client
     * @return
     */
    public HostPort getAdvertisedHostPort() {
        return advertisedHostPort;
    }
    /** Set the remote host port and update it from the advertised name to the ip address
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
