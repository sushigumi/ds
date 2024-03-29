package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.peer.UDPPeer;
import unimelb.bitbox.util.HostPort;

import java.io.BufferedWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DirectoryDeleteRequest extends EventProcess {

    private String pathName;

    public DirectoryDeleteRequest(BufferedWriter output, String pathName) {
        super(output);
        this.pathName = pathName;
    }

    public DirectoryDeleteRequest(DatagramSocket socket, HostPort hostPort, String pathName) {
        super(socket, hostPort);
        this.pathName = pathName;
    }

    public DirectoryDeleteRequest(DatagramSocket socket, HostPort hostPort, String pathName, UDPPeer peer) {
        super(socket, hostPort, peer);
        this.pathName = pathName;
    }

    @Override
    public void run() {

        sendMessage(Messages.genDirectoryDeleteRequest(pathName));
    }
}

