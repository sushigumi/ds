package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.util.Document;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Logger;

public abstract class EventProcess implements Runnable {
    static Logger log = Logger.getLogger(EventProcess.class.getName());

    private String mode;
    private BufferedWriter writer = null;
    private DatagramSocket datagramSocket = null;
    private InetAddress address = null;

    public EventProcess(BufferedWriter writer) {
        this.mode = ServerMain.MODE_TCP;
        this.writer = writer;
    }

    public EventProcess(DatagramSocket socket, InetAddress address) {
        this.mode = ServerMain.MODE_UDP;
        this.datagramSocket = socket;
        this.address = address;
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
                DatagramPacket
                datagramSocket.send();
            }

        } catch (IOException e) {
            log.severe("error writing message to peer");
        }

    }
}
