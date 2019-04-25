package unimelb.bitbox.messages;

public enum Command {
    INVALID_PROTOCOL("INVALID_PROTOCOL"),
    CONNECTION_REFUSED("CONNECTION_REFUSED"),
    HANDSHAKE_REQUEST("HANDSHAKE_REQUEST"),
    HANDSHAKE_RESPONSE("HANDSHAKE_RESPONSE"),
    FILE_CREATE_REQUEST("FILE_CREATE_REQUEST"),
    FILE_CREATE_RESPONSE("FILE_CREATE_RESPONSE"),
    FILE_BYTES_REQUEST("FILE_BYTES_REQUEST"),
    FILE_BYTES_RESPONSE("FILE_BYTES_RESPONSE"),
    FILE_DELETE_REQUEST("FILE_DELETE_REQUEST"),
    FILE_DELETE_RESPONSE("FILE_DELETE_RESPONSE"),
    FILE_MODIFY_REQUEST("FILE_MODIFY_REQUEST"),
    FILE_MODIFY_RESPONSE("FILE_MODIFY_RESPONSE"),
    DIRECTORY_CREATE_REQUEST("DIRECTORY_CREATE_REQUEST"),
    DIRECTORY_CREATE_RESPONSE("DIRECTORY_CREATE_RESPONSE"),
    DIRECTORY_DELETE_REQUEST("DIRECTORY_DELETE_REQUEST"),
    DIRECTORY_DELETE_RESPONSE("DIRECTORY_DELETE_RESPONSE");

    private String command;
    Command(String command) {
        this.command = command;
    }

    public String toString() {
        return command;
    }

    /**
     * Returns a Command enum based on its String representation
     * @param val String representation of the enum
     * @return
     */
    public static Command fromString(String val) {
        if (val == null) {
            return null;
        }
        return valueOf(val);
    }
}
