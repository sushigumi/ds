package unimelb.bitbox.messages;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;

/**
 * Generates a string which is supposedly the right message to be sent to the peer
 */
public class Messages {
    // Commands
    public static final String INVALID_PROTOCOL = "INVALID_PROTOCOL";
    public static final String CONNECTION_REFUSED = "CONNECTION_REFUSED";
    public static final String HANDSHAKE_REQUEST = "HANDSHAKE_REQUEST";
    public static final String HANDSHAKE_RESPONSE = "HANDSHAKE_RESPONSE";
    public static final String FILE_CREATE_REQUEST = "FILE_CREATE_REQUEST";
    public static final String FILE_CREATE_RESPONSE = "FILE_CREATE_RESPONSE";
    public static final String FILE_DELETE_REQUEST = "FILE_DELETE_REQUEST";
    public static final String FILE_DELETE_RESPONSE = "FILE_DELETE_RESPONSE";
    public static final String FILE_MODIFY_REQUEST = "FILE_MODIFY_REQUEST";
    public static final String FILE_MODIFY_RESPONSE = "FILE_MODIFY_RESPONSE";
    public static final String DIRECTORY_CREATE_REQUEST = "DIRECTORY_CREATE_REQUEST";
    public static final String DIRECTORY_CREATE_RESPONSE = "DIRECTORY_CREATE_RESPONSE";
    public static final String DIRECTORY_DELETE_REQUEST = "DIRECTORY_DELETE_REQUEST";
    public static final String DIRECTORY_DELETE_RESPONSE = "DIRECTORY_DELETE_RESPONSE";
    public static final String FILE_BYTES_REQUEST = "FILE_BYTES_REQUEST";
    public static final String FILE_BYTES_RESPONSE = "FILE_BYTES_RESPONSE";

    // Messages to send back
    private static final String CONNECTION_LIMIT_REACHED = "peer limit reached";

    private Messages() {
    }

    /**
     * Generates a string that represents an invalid protocol
     * @param message Message to be placed in the "message" field in the JSON string
     * @return
     */
    public static String genInvalidProtocol(String message) {
        Document doc = new Document();
        doc.append("command", INVALID_PROTOCOL);
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
        doc.append("command", HANDSHAKE_REQUEST);
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
        doc.append("command", HANDSHAKE_RESPONSE);
        doc.append("hostPort", localPort.toDoc());

        return doc.toJson();
    }

    /**
     * Generates a string that represents a peer refused message
     * @param peers
     * @return
     */
    public static String genConnectionRefused(ArrayList<HostPort> peers) {
        ArrayList<Document> peersDoc = new ArrayList<> ();
        for (HostPort peer : peers) {
            peersDoc.add(peer.toDoc());
        }
        Document doc = new Document();
        doc.append("command", CONNECTION_REFUSED);
        doc.append("message", CONNECTION_LIMIT_REACHED);
        doc.append("peers", peersDoc);

        return doc.toJson();
    }


    public static String genDirectoryDeleteRequest(String pathName){
        Document doc = new Document();
        doc.append("command", DIRECTORY_DELETE_REQUEST);
        doc.append("pathName",pathName);

        return doc.toJson();
    }

    public static String genDirectoryDeleteResponse(FileSystemManager fileSystemManager, String pathName){
        Document doc = new Document();
        doc.append("command", DIRECTORY_DELETE_RESPONSE);
        doc.append("pathName", pathName);


        if(!fileSystemManager.dirNameExists(pathName)){
            doc.append("message", "pathname does not exist");
            doc.append("status", true);
        }
        else if(!fileSystemManager.isSafePathName(pathName)){
            doc.append("message", "unsafe pathname given");
            doc.append("status", false);
        }
        else if(fileSystemManager.deleteDirectory(pathName)) {
            doc.append("message", "directory deleted");
            doc.append("status", true);
        }
        else {
            doc.append("message", "there was a problem deleting the directory");
            doc.append("status", false);
        }

        return doc.toJson();
    }

    public static String genDirectoryCreateRequest(String pathName) {
        Document doc = new Document();
        doc.append("command", DIRECTORY_CREATE_REQUEST);
        doc.append("pathName", pathName);

        return doc.toJson();
    }

    public static String genDirectoryCreateResponse(FileSystemManager fileSystemManager, String pathName) {
        Document doc = new Document();
        doc.append("command", DIRECTORY_CREATE_RESPONSE);
        doc.append("pathName", pathName);

        if (fileSystemManager.dirNameExists(pathName)) {
            doc.append("message", "pathname already exists");
            doc.append("status", false);
        } else if (!fileSystemManager.isSafePathName(pathName)) {
            doc.append("message", "unsafe pathname given");
            doc.append("status", false);
        } else if (fileSystemManager.makeDirectory(pathName)) {
            doc.append("message", "directory created");
            doc.append("status", true);
        } else {
            doc.append("message", "there was a problem creating the directory");
            doc.append("status", false);
        }

        return doc.toJson();
    }

    /**
     * Generates a list of strings that represent file bytes requests
     * A list of strings is used since a large file needs to be broken up into many other smaller chunks
     * and then each string is sent sequentially to the peer
     * @return
     */
    public static ArrayList<String> genFileBytesRequests(Document fileDescriptor, String pathName) {
        long blockSize = Long.parseLong(Configuration.getConfigurationValue("blockSize"));

        ArrayList<String> messages = new ArrayList<>();
        long fileSize = fileDescriptor.getLong("fileSize");
        for (long i = 0; i <= fileSize; i += blockSize) {
            Document doc = new Document();
            doc.append("command", FILE_BYTES_REQUEST);
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
//            doc.append("command", Command.FILE_BYTES_REQUEST.toString());
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
     * Generates a string which represents a single file bytes request message which requests for bytes at
     * a given position and of a specific length
     * This method is usually invoked only when a peer has not received some bytes which have been previously
     * requested and have been lost during transmission.
     * @param fileDescriptor File descriptor obtained from the previous File Bytes Response message
     * @param pathName Path name of file to copy
     * @param position Position of bytes to start reading from
     * @return
     */
    public static String genFileBytesRequest(Document fileDescriptor, String pathName, long position) {
        long blockSize = Long.parseLong(Configuration.getConfigurationValue("blockSize"));

        Document doc = new Document();
        doc.append("command", FILE_BYTES_REQUEST);
        doc.append("fileDescriptor", fileDescriptor);
        doc.append("pathName", pathName);
        doc.append("position", position);
        doc.append("length", blockSize);

        return doc.toJson();
    }



    /**
     * Generates a string which represents a file bytes response message which sends the correct bytes of
     * a file from position and of length 'length' to the peer who sent the matching file bytes request
     * @param fileSystemManager Reference to file system manager
     * @param fileDescriptor File descriptor obtained from the file bytes request message
     * @param pathName Path name of file relative to share directory
     * @param position Position of bytes to start reading from
     * @param length Number of bytes to read
     * @return File Bytes Response message
     */
    public static String genFileBytesResponse(FileSystemManager fileSystemManager,
                                              Document fileDescriptor,
                                              String pathName,
                                              long position,
                                              long length) {

        String md5 = fileDescriptor.getString("md5");


        Document doc = new Document();
        doc.append("command", FILE_BYTES_RESPONSE);
        doc.append("fileDescriptor", fileDescriptor);
        doc.append("pathName", pathName);
        doc.append("position", position);
        doc.append("length", length);
        try {
            // If there exists the file then we can read it otherwise return failure
            ByteBuffer bytes = fileSystemManager.readFile(md5, position, length);
            if (bytes != null) {
                // Encode the bytes in base64, convert it to a byte[] so that can be converted to a
                // string and sent
                bytes.rewind();
                byte[] bytesArray = new byte[bytes.remaining()];
                bytes.get(bytesArray);
                String encodedBytes = Base64.getEncoder().encodeToString(bytesArray);
                doc.append("content", encodedBytes);
                doc.append("message", "successful read");
                doc.append("status", true);
            } else {
                doc.append("content", "");
                doc.append("message", "unsuccessful read");
                doc.append("status", false);
            }
        }
        // Return appropriate errors when exceptions are caught
        // Error accessing file system
        // TODO Retry?
        catch (IOException e) {
            doc.append("content", "");
            doc.append("message", "error accessing file system");
            doc.append("status", false);
        }
        // md5 hashing algorithm is not found in the system
        catch (NoSuchAlgorithmException e) {
            doc.append("content", "");
            doc.append("message", "md5 hash algorithm is unavailable");
            doc.append("status", false);
        }

        return doc.toJson();
    }
}
