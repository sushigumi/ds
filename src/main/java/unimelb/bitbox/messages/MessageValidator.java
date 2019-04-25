package unimelb.bitbox.messages;

import unimelb.bitbox.util.Document;

public class MessageValidator {
    private static MessageValidator ourInstance = new MessageValidator();

    public static MessageValidator getInstance() {
        return ourInstance;
    }

    private MessageValidator() {
    }

    public String validateFileCreateRequest(Document doc) {
        if (doc.getString("command") == null) {
            return "message must contain a command field as string";
        }
        if (doc.getString())

        //return error message
        return null;
    }

    public String validateFileCreateResponse(Document doc) {

    }
}
