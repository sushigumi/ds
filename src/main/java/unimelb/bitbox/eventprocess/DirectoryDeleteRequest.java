package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;

import java.io.BufferedWriter;
import java.net.DatagramSocket;

public class DirectoryDeleteRequest extends EventProcess {

    private String pathName;

    public DirectoryDeleteRequest(BufferedWriter output, String pathName) {
        super(output, ServerMain.MODE_TCP);
        this.pathName = pathName;
    }

    public DirectoryDeleteRequest(DatagramSocket socket, String pathName) {
        super(socket, ServerMain.MODE_UDP);
        this.pathName = pathName;
    }

    @Override
    public void run() {

        sendMessage(Messages.genDirectoryDeleteRequest(pathName));
    }
}

