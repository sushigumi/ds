package unimelb.bitbox.peer;

import unimelb.bitbox.eventprocess.*;
import unimelb.bitbox.messages.InvalidProtocolType;
import unimelb.bitbox.messages.MessageValidator;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public abstract class Connection {
    static Logger log = Logger.getLogger(Connection.class.getName());

    FileSystemManager fileSystemManager;
    boolean isIncoming;
    int nRetries;

    Socket socket;
    HostPort remoteHostPort;

    BufferedWriter output;
    BufferedReader input;

    ExecutorService listener;
    ExecutorService sender;
    ExecutorService background;

    ConnectionObserver connectionObserver;

    /**
     * Initialise input (Buffered Reader) and output (Buffered Writer)
     */
    public void initInputOutput() {
        try {
            output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        } catch (IOException e) {
            log.severe("unable to create buffered writer");
            // TODO exit
        }

        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            log.severe("unable to created buffered reader");
            //TODO exit
        }
    }

    /**
     * Close the current peer
     */
    public void close() {
        try {
            log.info("closing the peer for peer " + remoteHostPort);
            if (socket != null) socket.close();
            if (input != null) input.close();
            if (output != null) output.close();

            listener.shutdownNow();
            sender.shutdownNow();
            background.shutdownNow();

            connectionObserver.closeConnection(this, isIncoming);
        } catch (IOException e) {
            log.severe("error closing the peer " + remoteHostPort);
        }
    }

    /**
     * Process a file system event and submits it to the sender to broadcast to the peer
     * @param fileSystemEvent
     */
    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        switch(fileSystemEvent.event) {
            case FILE_CREATE:
                sender.submit(new FileCreateRequest(output, fileSystemEvent));
                break;

            case FILE_DELETE:
                sender.submit(new FileDeleteRequest(output, fileSystemEvent));
                break;

            case FILE_MODIFY:
                sender.submit(new FileModifyRequest(output, fileSystemEvent));
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
     * Runnable to listen to messages from a peer
     */
    class Listen implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    String in = input.readLine();

                    // Peer has closed the peer and EOF received
                    if (in == null) {
                        log.info("peer is disconnected");
                        close();
                        return;
                    }
                    Document doc = Document.parse(in);
                    String command = doc.getString("command");

                    //System.out.println("Received: " + command);  // Debugging async

                    // Received INVALID_PROTOCOL, close the peer
                    if (command.equals(Messages.INVALID_PROTOCOL)) {
                        close();
                        return;
                    }
                    else if (command.equals(Messages.FILE_CREATE_REQUEST)) {
                        String createRequest = MessageValidator.getInstance().validateFileChangeRequest(doc);
                        if (createRequest != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, createRequest));
                            close();
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
                            close();
                            return;
                        }
                    }
                    else if (command.equals(Messages.FILE_DELETE_REQUEST)) {
                        String deleteRequest = MessageValidator.getInstance().validateFileChangeRequest(doc);
                        if (deleteRequest != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, deleteRequest));
                            close();
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
                            close();
                            return;
                        }
                    }
                    else if (command.equals(Messages.FILE_MODIFY_REQUEST)) {
                        String modifyRequest = MessageValidator.getInstance().validateFileChangeRequest(doc);
                        if (modifyRequest != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, modifyRequest));
                            close();
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
                            close();
                            return;
                        }
                    }
                    else if (command.equals(Messages.FILE_BYTES_REQUEST)) {
                        String bytesRequest = MessageValidator.getInstance().validateFileBytesRequest(doc);
                        if (bytesRequest != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, bytesRequest));
                            close();
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
                            close();
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
                            close();
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
                            close();
                            return;
                        }
                    }
                    else if (command.equals(Messages.DIRECTORY_DELETE_REQUEST)) {
                        String dirDeleteRequest = MessageValidator.getInstance().validateDirectoryChangeRequest(doc);
                        if (dirDeleteRequest != null) {
                            background.submit(new InvalidProtocol(output, InvalidProtocolType.MISSING_FIELD, dirDeleteRequest));
                            close();
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
                            close();
                            return;
                        }
                    }
                    else {
                        background.submit(new InvalidProtocol(output, InvalidProtocolType.INVALID_COMMAND));
                        close();
                    }
                }

                // Enter here when the peer has closed the peer
               // log.info("peer unexpectedly closed peer");
               // close();
            }
            catch (SocketException e) {
                // Received a Connection Reset TCP RST so close the peer and try again
                log.info("peer reset");
                close();
                connectionObserver.retry(Connection.this);
            }
            catch (IOException e) {
                log.severe("error happened when reading input from peer: " + e.getMessage());
                close();
            }
            catch (Exception e) {
                e.printStackTrace();
                log.severe("big exception:" + e.getMessage());
                close();
            }
        }
    }
}

