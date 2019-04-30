package unimelb.bitbox.runnables;

import unimelb.bitbox.messages.InvalidProtocolType;
import unimelb.bitbox.messages.Messages;

import java.io.BufferedWriter;

public class InvalidProtocol extends BaseRunnable {
    private InvalidProtocolType type;
    private String field;
    private String message;

    public InvalidProtocol(BufferedWriter output, InvalidProtocolType type) {
        super(output);
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

    public InvalidProtocol(BufferedWriter output, String message) {
        super(output);
        type = InvalidProtocolType.CUSTOM;
        this.message = message;
    }

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
