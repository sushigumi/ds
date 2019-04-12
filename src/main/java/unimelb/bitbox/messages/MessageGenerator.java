package unimelb.bitbox.messages;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Generates a runnable which is just sends the required message to the right peer
 */
public class MessageGenerator {
    private MessageGenerator() {
    }

    /**
     * Genenrates a runnable that can send the handshake request to the specific peer
     * @param thisPort
     * @param output
     * @return
     */
    public static Runnable genHandshakeRequest(HostPort thisPort, DataOutputStream output) {
        Document doc = new Document();
        doc.append("command", MessageCommands.HANDSHAKE_REQUEST.getCommand());
        doc.append("hostPort", thisPort.toDoc());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    output.writeUTF(doc.toJson());
                } catch (IOException e) {
                    // TODO:
                    e.printStackTrace();
                }
            }
        };

        return runnable;
    }

    /**
     * Generates a runnable that can send a handshake response to the specific peer
     * @param thisPort
     * @param output
     * @return
     */
    public static Runnable generateHandshakeResponse(HostPort thisPort, DataOutputStream output) {
        Document doc = new Document();
        doc.append("command", MessageCommands.HANDSHAKE_RESPONSE.getCommand());
        doc.append("hostPort", thisPort.toDoc());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    output.writeUTF(doc.toJson());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        return runnable;
    }
}
