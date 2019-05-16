package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.BufferedWriter;
import java.net.DatagramSocket;

public class DirectoryCreateResponse extends EventProcess {


    private FileSystemManager fileSystemManager;
    private Document request;

    public DirectoryCreateResponse(BufferedWriter output, FileSystemManager fileSystemManager,
                                   Document request) {
        super(output, ServerMain.MODE_TCP);
        this.fileSystemManager = fileSystemManager;
        this.request = request;

    }

    public DirectoryCreateResponse(DatagramSocket socket, FileSystemManager fileSystemManager,
                                   Document request) {
        super(socket, ServerMain.MODE_UDP);
        this.fileSystemManager = fileSystemManager;
        this.request = request;

    }

    @Override
    public void run() {

        sendMessage(Messages.genDirectoryCreateResponse(fileSystemManager,
                request.getString("pathName")));

    }
}
