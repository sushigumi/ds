package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.peer.UDPPeer;
import unimelb.bitbox.util.HostPort;

import java.io.BufferedWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DirectoryCreateRequest extends EventProcess {

    private String pathName;

    public DirectoryCreateRequest(BufferedWriter output, String pathName) {
        super(output);
        this.pathName = pathName;
    }

    public DirectoryCreateRequest(DatagramSocket datagramSocket, HostPort hostPort, String pathName) {
        super(datagramSocket, hostPort);
        this.pathName = pathName;
    }

    public DirectoryCreateRequest(DatagramSocket datagramSocket, HostPort hostPort, String pathName, UDPPeer peer) {
        super(datagramSocket, hostPort, peer);
        this.pathName = pathName;
    }

    @Override
    public void run() {

        sendMessage(Messages.genDirectoryCreateRequest(pathName));

    }
}
