package unimelb.bitbox.messages;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.util.ArrayList;

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

    public static String genDirectoryDeleteRequest(String pathName){
        Document doc = new Document();
        doc.append("command",Commands.DIRECTORY_DELETE_REQUEST.toString());
        doc.append("pathName",pathName);

        return doc.toJson();
    }

    public static String genDirectoryDeleteResponse(FileSystemManager fileSystemManager, String pathName){
        Document doc = new Document();
        doc.append("command", Commands.DIRECTORY_DELETE_RESPONSE.toString());
        doc.append("pathName", pathName);

        if(fileSystemManager.deleteDirectory(pathName)) {
            doc.append("message", "directory deleted");
            doc.append("status", true);
        }
        else if(!fileSystemManager.dirNameExists(pathName)){
            doc.append("message", "pathname does not exist");
            doc.append("status", true);
        }
        else if(!fileSystemManager.isSafePathName(pathName)){
            doc.append("message", "unsafe pathname given");
            doc.append("status", false);
        }

        return doc.toJson();
    }

    public static String genDirectoryCreateRequest(String pathName){
        Document doc = new Document();
        doc.append("command", Commands.DIRECTORY_CREATE_REQUEST.toString());
        doc.append("pathName", pathName);

        return doc.toJson();
    }

    public static String genDirectoryCreateResponse(FileSystemManager fileSystemManager, String pathName){
        Document doc = new Document();
        doc.append("command", Commands.DIRECTORY_CREATE_RESPONSE.toString());
        doc.append("pathName", pathName);

        if(fileSystemManager.dirNameExists(pathName)){
            doc.append("message", "pathname already exists");
            doc.append("status", false);
        }
        else if(!fileSystemManager.isSafePathName(pathName)){
            doc.append("message", "unsafe pathname given");
            doc.append("status", false);
        }
        else if(fileSystemManager.makeDirectory(pathName)) {
            doc.append("message", "directory created");
            doc.append("status", true);
        }

        return doc.toJson();
    }
}
