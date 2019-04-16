package unimelb.bitbox.messages;

import unimelb.bitbox.util.Document;
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
        doc.append("command", MessageCommands.INVALID_PROTOCOL.getCommand());
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
        doc.append("command", MessageCommands.HANDSHAKE_REQUEST.getCommand());
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
        doc.append("command", MessageCommands.HANDSHAKE_RESPONSE.getCommand());
        doc.append("hostPort", localPort.toDoc());

        return doc.toJson();
    }

    /**
     * Generates a string that represents a connection refused message
     * @param peers
     * @return
     */
    public static String genConnectionRefused(ArrayList<HostPort> peers) {
        Document doc = new Document();
        doc.append("command", MessageCommands.CONNECTION_REFUSED.getCommand());
        doc.append("message", CONNECTION_LIMIT_REACHED);
        doc.append("peers", peers);

        return doc.toJson();
    }
}
