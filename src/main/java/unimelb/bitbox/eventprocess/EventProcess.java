package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.peer.UDPPeer;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

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

    DatagramSocket socket = null;
    HostPort hostPort = null;
    UDPPeer peer = null;

    public EventProcess(BufferedWriter writer) {
        this.mode = ServerMain.MODE_TCP;
        this.writer = writer;
    }

    public EventProcess(DatagramSocket socket, HostPort hostPort) {
        this.mode = ServerMain.MODE_UDP;
        this.socket = socket;
        this.hostPort = hostPort;
    }

    public EventProcess(DatagramSocket socket, HostPort hostPort, UDPPeer peer) {
        this.mode = ServerMain.MODE_UDP;
        this.socket = socket;
        this.hostPort = hostPort;
        this.peer = peer;
    }

    public EventProcess() {
        this.mode = ServerMain.MODE_TCP;
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
                message = message + "\n";
                byte[] buf = message.getBytes("UTF-8");
                //System.out.println(message);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(hostPort.host), hostPort.port);
//                System.out.println(packet.getLength());
//                System.out.println(new String(packet.getData()));
                //System.out.println(packet.getAddress() + "," + packet.getPort());
                socket.send(packet);

                // Insert a retry only for requests sent
                if (peer != null) {
                    peer.queueRetry(this, Document.parse(message));
                }
            }

        } catch (IOException e) {
            log.severe("error writing message to peer. " + e.getMessage());
        }
        catch (Exception e) {
            log.severe(e.getMessage());
        }

    }
}
