package unimelb.bitbox.eventprocess;

import unimelb.bitbox.util.Document;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.logging.Logger;

public abstract class EventProcess implements Runnable {
    static Logger log = Logger.getLogger(EventProcess.class.getName());

    private BufferedWriter writer;

    public EventProcess(BufferedWriter writer) {
        this.writer = writer;
    }

    public EventProcess() {
        this.writer = null;
    }

    public void updateWriter(BufferedWriter writer) {
        this.writer = writer;
    }

    public void sendMessage(String message) {
        //System.out.println(Document.parse(message).getString("command")); // Debugging async
        try {
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            log.severe("error writing message to peer");
        }

    }
}
