package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.util.Document;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.util.logging.Logger;

public abstract class EventProcess implements Runnable {
    static Logger log = Logger.getLogger(EventProcess.class.getName());

    private String mode;
    private BufferedWriter writer;
    private DatagramSocket datagramSocket;

    public EventProcess(BufferedWriter writer, String mode) {
        this.mode = mode;
        this.writer = writer;
    }

    public EventProcess(DatagramSocket socket, String mode) {
        this.mode = mode;
        this.datagramSocket = socket;
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
            if (mode.equals(ServerMain.MODE_TCP)) {
                // Write the message to the other peer
                writer.write(message + "\n");
                writer.flush();
            } else if (mode.equals(ServerMain.MODE_UDP)) {
                // Convert the message to a bytes buffer before sending to the other peer

            }

        } catch (IOException e) {
            log.severe("error writing message to peer");
        }

    }
}
