package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;

import java.io.BufferedWriter;
import java.net.DatagramSocket;

public class DirectoryCreateRequest extends EventProcess {

    private String pathName;

    public DirectoryCreateRequest(BufferedWriter output, String pathName) {
        super(output, ServerMain.MODE_TCP);
        this.pathName = pathName;
    }

    public DirectoryCreateRequest(DatagramSocket datagramSocket, String pathName) {
        super(datagramSocket, ServerMain.MODE_UDP);
        this.pathName = pathName;
    }

    @Override
    public void run() {

        sendMessage(Messages.genDirectoryCreateRequest(pathName));

    }
}
