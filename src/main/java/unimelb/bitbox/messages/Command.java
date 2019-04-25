package unimelb.bitbox.messages;

public enum Command {
    INVALID_PROTOCOL("INVALID PROTOCOL"),
    CONNECTION_REFUSED("CONNECTION REFUSED"),
    HANDSHAKE_REQUEST("HANDSHAKE REQUEST"),
    HANDSHAKE_RESPONSE("HANDSHAKE RESPONSE"),
    FILE_CREATE_REQUEST("FILE CREATE REQUEST"),
    FILE_CREATE_RESPONSE("FILE CREATE RESPONSE"),
    FILE_BYTES_REQUEST("FILE BYTES REQUEST"),
    FILE_BYTES_RESPONSE("FILE BYTES RESPONSE"),
    FILE_DELETE_REQUEST("FILE DELETE REQUEST"),
    FILE_DELETE_RESPONSE("FILE DELETE RESPONSE"),
    FILE_MODIFY_REQUEST("FILE MODIFY REQUEST"),
    FILE_MODIFY_RESPONSE("FILE MODIFY RESPONSE"),
    DIRECTORY_CREATE_REQUEST("DIRECTORY CREATE REQUEST"),
    DIRECTORY_CREATE_RESPONSE("DIRECTORY CREATE RESPONSE"),
    DIRECTORY_DELETE_REQUEST("DIRECTORY DELETE REQUEST"),
    DIRECTORY_DELETE_RESPONSE("DIRECTORY DELETE RESPONSE");

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
        if (val.contains("_")) {
            return valueOf(val);
        }
        String enumRep = val.replace(' ', '_');
        return valueOf(enumRep);
    }
}
