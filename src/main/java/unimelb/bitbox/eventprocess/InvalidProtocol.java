package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.InvalidProtocolType;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.HostPort;

import java.io.BufferedWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class InvalidProtocol extends EventProcess {
    private InvalidProtocolType type;
    private String field;
    private String message;

    public InvalidProtocol(BufferedWriter output, InvalidProtocolType type) {
        super(output);
        this.type = type;
    }

    public InvalidProtocol(DatagramSocket socket, HostPort hostPort,
                           InvalidProtocolType type) {
        super(socket, hostPort);
        this.type = type;
    }

    /**
     * Constructor for an Invalid Protocol resulting from a missing or invalid field
     * @param output
     * @param type
     * @param field
     */
    public InvalidProtocol(BufferedWriter output, InvalidProtocolType type, String field) {
        super(output);
        this.type = type;
        this.field = field;
    }

    public InvalidProtocol(DatagramSocket socket, HostPort hostPort, InvalidProtocolType type, String field) {
        super(socket, hostPort);
        this.type = type;
        this.field = field;
    }

    public InvalidProtocol(BufferedWriter output, String message) {
        super(output);
        type = InvalidProtocolType.CUSTOM;
        this.message = message;
    }

    //TODO add some for this type of messages

    @Override
    public void run() {
        switch (type) {
            case CUSTOM:
                sendMessage(Messages.genInvalidProtocol(message));
                break;

            case INVALID_COMMAND:
                sendMessage(Messages.genInvalidProtocol("invalid command"));
                break;

            case MISSING_FIELD:
                sendMessage(Messages.genInvalidProtocol("error in " + field + " field"));
                break;

            default:
                sendMessage(Messages.genInvalidProtocol("invalid protocol"));
                break;
        }
    }
}
