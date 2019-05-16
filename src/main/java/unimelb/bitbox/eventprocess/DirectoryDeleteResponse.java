package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.BufferedWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DirectoryDeleteResponse extends EventProcess {

    private FileSystemManager fileSystemManager;
    private Document request;

    public DirectoryDeleteResponse(BufferedWriter output, FileSystemManager fileSystemManager,
                                   Document request) {
        super(output);
        this.fileSystemManager = fileSystemManager;
        this.request = request;
    }

    public DirectoryDeleteResponse(DatagramSocket socket, HostPort hostPort, FileSystemManager fileSystemManager,
                                   Document request) {
        super(socket, hostPort);
        this.fileSystemManager = fileSystemManager;
        this.request = request;
    }


    @Override
    public void run() {
        sendMessage(Messages.genDirectoryDeleteResponse(fileSystemManager,
                request.getString("pathName")));
    }
}
