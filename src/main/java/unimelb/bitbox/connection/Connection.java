package unimelb.bitbox.connection;

import unimelb.bitbox.eventprocess.*;
import unimelb.bitbox.messages.MessageValidator;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.messages.InvalidProtocolType;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents a TCP connection between two peers.
 * Before asynchronous moving on to the asynchronous part of the protocol,
 * connections must either send a HANDSHAKE_REQUEST message and wait for a HANDSHAKE_RESPONSE message
 * or receive a HANDSHAKE_REQUEST and respond with a HANDSHAKE_RESPONSE message
 * If a HANDSHAKE_REQUEST is received but the maximum number of connections has been achieved, then
 * a CONNECTION_REFUSED message must be sent back to the peer and the connection is closed.
 * Similarly, if a CONNECTION_REFUSED message is received, the connection must be closed.
 */
public abstract class Connection {
    Socket socket; // Other peer's socket

    HostPort localHostPort;
    HostPort remoteHostPort;

    BufferedWriter output;
    BufferedReader input;

    ExecutorService listener;
    ExecutorService sender;
    ExecutorService background;

    FileSystemManager fileSystemManager;
    ConnectionObserver observer;

    //TODO might need hashmap here to count the number of files needed to receive if not done in one sitting
    //TODO updating while bytes response comes in

    /**
     * Called when receiving a connection from another peer
     * Only when receiving a connection do we add to the counter of connections based on the
     * maximumIncomingConnections parameter in the properties file
     * @param socket
     * @param localHostPort
     */
    Connection(FileSystemManager fileSystemManager, Socket socket, HostPort localHostPort) {
        this.socket = socket;
        this.localHostPort = localHostPort;
        this.fileSystemManager = fileSystemManager;

        createWriterAndReader();

        this.listener = Executors.newSingleThreadExecutor();

        // Create the single thread executor to send messages based on a queue when it requires messages to be
        // sent
        this.sender = Executors.newSingleThreadExecutor();
        this.background = Executors.newSingleThreadExecutor();
    }

    /**
     * Called when making a connection to another peer
     * So this peer needs to send a handshake request to the other peer
     * @param localHostPort
     */
    Connection(FileSystemManager fileSystemManager, HostPort localHostPort) {
        this.localHostPort = localHostPort;
        this.fileSystemManager = fileSystemManager;

        this.listener = Executors.newSingleThreadExecutor();

        // Create the single thread executor to send messages based on a queue when it requires messages to be
        // sent
        this.sender = Executors.newSingleThreadExecutor();
        this.background = Executors.newSingleThreadExecutor();
    }

    void updateRemoteHostPort(HostPort remoteHostPort) {
        this.remoteHostPort = remoteHostPort;
    }

    void addConnectionObserver(ConnectionObserver observer) {
        this.observer = observer;
    }

    void createWriterAndReader() {
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        } catch (IOException e) {
            System.out.println("HERE IS THE PROBLEM");
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * Accepts a FileSystemEvent as an argument and constructs an appropriate Runnable to be submitted
     * to the sender ExecutorService
     * @param fileSystemEvent
     */
    public void submitEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        switch (fileSystemEvent.event) {
            /*
            case FILE_CREATE:
                String request = Messages.genFileBytesRequests(fileSystemEvent.fileDescriptor.toDoc(), fileSystemEvent.pathName).remove(0);
                // TODO change to sending FILE_CREATE_REQUEST
                sender.submit(new FileBytesResponse(output, fileSystemManager, Document.parse(request)));
                break;
                */

            case FILE_CREATE:
                sender.submit(new FileCreateRequest(output, fileSystemEvent));
                break;
            case FILE_DELETE:
                sender.submit(new FileDeleteRequest(output,fileSystemEvent));
                break;
            case FILE_MODIFY:
                sender.submit(new FileModifyRequest(output,fileSystemEvent));
                break;
            case DIRECTORY_CREATE:
                sender.submit(new DirectoryCreateRequest(output, fileSystemEvent.pathName));
                break;
            case DIRECTORY_DELETE:
                sender.submit(new DirectoryDeleteRequest(output, fileSystemEvent.pathName));
                break;

        }

    }

    /**
     * Calls generateSyncEvents method of the File System Manager to synchronize files between all peers
     * at the start of the connection.
     * This method is only called at the start of each connection since further synchronized events will
     * be generated collectively for all other events in the main loop
     */
    void initSyncPeers() {
        ArrayList<FileSystemManager.FileSystemEvent> fileSystemEvents = fileSystemManager.generateSyncEvents();
        // For each file system event send an appropriate message to the connected peer
        for (FileSystemManager.FileSystemEvent event : fileSystemEvents) {
            submitEvent(event);
        }
    }

    private void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        observer.closeConnection(remoteHostPort);
    }

    class Listener implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    String in = input.readLine();

                    Document doc = Document.parse(in);

                    System.out.println("Received: " + doc.toJson());

                    if (doc.getString("command") == null) {
                        background.submit(new InvalidProtocol(output, "message must contain command key"));
                        closeConnection();
                        return;
                    }

                    String command = doc.getString("command");

                    // Invalid protocol received so close the connection then try to reconnect at least three times
                    if (command.equals(Messages.INVALID_PROTOCOL)) {
                        closeConnection();
                        observer.interruptConnection(remoteHostPort, localHostPort, fileSystemManager);
                    }
                    else if (command.equals(Messages.FILE_CREATE_REQUEST)) {
                        String createRequest = MessageValidator.getInstance().validateFileChangeRequest(doc);
                        if (createRequest != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, createRequest));
                            closeConnection();
                            return;
                        }
                        else {
                            background.submit(new FileCreateResponse(output, doc, fileSystemManager));
                        }
                    }
                    else if (command.equals(Messages.FILE_CREATE_RESPONSE)) {
                        String createResponse = MessageValidator.getInstance().validateFileChangeResponse(doc);
                        if (createResponse != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, createResponse));
                            closeConnection();
                            return;
                        }
                    }
                    else if (command.equals(Messages.FILE_DELETE_REQUEST)) {
                        String deleteRequest = MessageValidator.getInstance().validateFileChangeRequest(doc);
                        if (deleteRequest != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, deleteRequest));
                            closeConnection();
                            return;
                        }
                        else {
                            background.submit(new FileDeleteResponse(output, doc, fileSystemManager));
                        }
                    }
                    else if (command.equals(Messages.FILE_DELETE_RESPONSE)) {
                        String deleteResponse = MessageValidator.getInstance().validateFileChangeResponse(doc);
                        if (deleteResponse != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, deleteResponse));
                            closeConnection();
                            return;
                        }
                    }
                    else if (command.equals(Messages.FILE_MODIFY_REQUEST)) {
                        String modifyRequest = MessageValidator.getInstance().validateFileChangeRequest(doc);
                        if (modifyRequest != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, modifyRequest));
                            closeConnection();
                            return;
                        }
                        else {
                            background.submit(new FileModifyResponse(output, doc, fileSystemManager));
                        }
                    }
                    else if (command.equals(Messages.FILE_MODIFY_RESPONSE)) {
                        String modifyResponse = MessageValidator.getInstance().validateFileChangeResponse(doc);
                        if (modifyResponse != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, modifyResponse));
                            closeConnection();
                            return;
                        }
                    }
                    else if (command.equals(Messages.FILE_BYTES_REQUEST)) {
                        String bytesRequest = MessageValidator.getInstance().validateFileBytesRequest(doc);
                        if (bytesRequest != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, bytesRequest));
                            closeConnection();
                            return;
                        }
                        else {
                            background.submit(new FileBytesResponse(output, fileSystemManager, doc));
                        }
                    }
                    else if (command.equals(Messages.FILE_BYTES_RESPONSE)) {
                        String bytesResponse = MessageValidator.getInstance().validateFileBytesResponse(doc);
                        if (bytesResponse != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, bytesResponse));
                            closeConnection();
                            return;
                        }
                        else {
                            background.submit(new ConstructFile(output, fileSystemManager, doc));
                        }
                    }
                    else if (command.equals(Messages.DIRECTORY_CREATE_REQUEST)) {
                        String dirCreateRequest = MessageValidator.getInstance().validateDirectoryChangeRequest(doc);
                        if (dirCreateRequest != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, dirCreateRequest));
                            closeConnection();
                            return;
                        }
                        else {
                            background.submit(new DirectoryCreateResponse(output, fileSystemManager, doc));
                        }
                    }
                    else if (command.equals(Messages.DIRECTORY_CREATE_RESPONSE)) {
                        String dirCteateResponse = MessageValidator.getInstance().validateDirectoryChangeResponse(doc);
                        if (dirCteateResponse != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, dirCteateResponse));
                            closeConnection();
                            return;
                        }
                    }
                    else if (command.equals(Messages.DIRECTORY_DELETE_REQUEST)) {
                        String dirDeleteRequest = MessageValidator.getInstance().validateDirectoryChangeRequest(doc);
                        if (dirDeleteRequest != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, dirDeleteRequest));
                            closeConnection();
                            return;
                        }
                        else {
                            background.submit(new DirectoryDeleteResponse(output, fileSystemManager, doc));
                        }
                    }
                    else if (command.equals(Messages.DIRECTORY_DELETE_RESPONSE)) {
                        String dirDeleteResponse = MessageValidator.getInstance().validateDirectoryChangeResponse(doc);
                        if (dirDeleteResponse != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, dirDeleteResponse));
                            closeConnection();
                            return;
                        }
                    }
                    else {
                        background.submit(new InvalidProtocol(output, InvalidProtocolType.INVALID_COMMAND));
                        closeConnection();
                    }
                }
            }
            // When the peer has closed the connection
            catch (IOException e) {
                e.printStackTrace();
                System.out.println("Peer has closed the connection");
                observer.closeConnection(remoteHostPort);
            }
        }
    }
}
