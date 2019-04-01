package unimelb.bitbox.server;

public class MessageController {
    private static MessageController ourInstance = new MessageController();

    public static MessageController getInstance() {
        return ourInstance;
    }

    private MessageController() {}
}
