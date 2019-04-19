package unimelb.bitbox.messages;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;

/**
 * Generates a string which is supposedly the right message to be sent to the peer
 */
public class MessageGenerator {
    // Messages to send back
    private static final String CONNECTION_LIMIT_REACHED = "connection limit reached";

    private MessageGenerator() {
    }

    /**
     * Generates a string that represents an invalid protocol
     * @param message Message to be placed in the "message" field in the JSON string
     * @return
     */
    public static String genInvalidProtocol(String message) {
        Document doc = new Document();
        doc.append("command", Commands.INVALID_PROTOCOL.toString());
        doc.append("message", message);

        return doc.toJson();
    }

    /**
     * Genenrates a string that represents the handshake request to a peer
     * @param localPort
     * @return
     */
    public static String genHandshakeRequest(HostPort localPort) {
        Document doc = new Document();
        doc.append("command", Commands.HANDSHAKE_REQUEST.toString());
        doc.append("hostPort", localPort.toDoc());

        return doc.toJson();
    }

    /**
     * Generates a string that represents the handshake response to a peer
     * @param localPort
     * @return
     */
    public static String genHandshakeResponse(HostPort localPort) {
        Document doc = new Document();
        doc.append("command", Commands.HANDSHAKE_RESPONSE.toString());
        doc.append("hostPort", localPort.toDoc());

        return doc.toJson();
    }

    /**
     * Generates a string that represents a connection refused message
     * @param peers
     * @return
     */
    public static String genConnectionRefused(ArrayList<HostPort> peers) {
        ArrayList<Document> peersDoc = new ArrayList<> ();
        for (HostPort peer : peers) {
            peersDoc.add(peer.toDoc());
        }
        Document doc = new Document();
        doc.append("command", Commands.CONNECTION_REFUSED.toString());
        doc.append("message", CONNECTION_LIMIT_REACHED);
        doc.append("peers", peersDoc);

        return doc.toJson();
    }

    /**
     * Generates a list of strings that represent file bytes requests
     * A list of strings is used since a large file needs to be broken up into many other smaller chunks
     * and then each string is sent sequentially to the peer
     * @return
     */
    public static ArrayList<String> genFileBytesRequests(Document fileDescriptor, String pathName) {
        int blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));

        ArrayList<String> messages = new ArrayList<>();
        long fileSize = Integer.parseInt(fileDescriptor.getString("fileSize"));
        for (int i = 0; i < fileSize; i += blockSize) {
            Document doc = new Document();
            doc.append("command", Commands.FILE_BYTES_REQUEST.toString());
            doc.append("fileDescriptor", fileDescriptor);
            doc.append("pathName", pathName);
            doc.append("position", i);
            if (i + blockSize < fileSize) {
                doc.append("length", blockSize);
            } else {
                doc.append("length", fileSize - i);
            }

            messages.add(doc.toJson());
        }
        return messages;
    }
//    public static ArrayList<String> genFileBytesRequests(FileSystemManager fileSystemManager,
//                                                        FileSystemManager.FileSystemEvent fileSystemEvent) {
//        int blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
//
//        ArrayList<String> messages = new ArrayList<>();
//
//        for (int i = 0; i < fileSystemEvent.fileDescriptor.fileSize; i += blockSize) {
//            Document doc = new Document();
//            doc.append("command", Commands.FILE_BYTES_REQUEST.toString());
//            doc.append("fileDescriptor", fileSystemEvent.fileDescriptor.toDoc());
//            doc.append("pathName", fileSystemEvent.pathName);
//            doc.append("position", i);
//            if (i + blockSize < fileSystemEvent.fileDescriptor.fileSize) {
//                doc.append("length", blockSize);
//            } else {
//                // TODO check if need -1
//                doc.append("length", fileSystemEvent.fileDescriptor.fileSize - 1 - i);
//            }
//
//            messages.add(doc.toJson());
//        }
//
//        return messages;
//    }

    /**
     * Generates a string which respresents a single file bytes request message which requests for bytes at
     * a given position and of how long
     * This method is usually invoked only when a peer has not received some bytes which have been previously
     * requested and have been lost during transmission
     * @param fileSystemManager Reference to File System Manager
     * @param fileSystemEvent File System Event
     * @param position Position of the bytes to start reading from
     * @param length Length of the bytes to read (Dependent on FileBytesResponse sender)
     * @return
     */



    public static String genFileBytesResponse(FileSystemManager fileSystemManager,
                                              Document fileDescriptor,
                                              String pathName,
                                              long position,
                                              long length) {

        String md5 = fileDescriptor.getString("md5");
        ByteBuffer bytes = fileSystemManager.readFile(md5, position, length);
        ByteBuffer encodedBytes = Base64.getEncoder().encode(bytes);

        Document doc = new Document();
        doc.append("command", Commands.FILE_BYTES_RESPONSE.toString());
        doc.append("fileDescriptor", fileDescriptor);
        doc.append("pathName", pathName);
        doc.append("position", position);
        doc.append("length", length);
        if (bytes != null) {
            doc.append("content", new String(encodedBytes.array()));
            doc.append("message", "successful read");
            doc.append("status", true);
        } else {
            doc.append("content", "");
            doc.append("message", "unsuccessful read");
            doc.append("status", false);
        }
    }
}
