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
}
