package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.BufferedWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class FileBytesResponse extends EventProcess {
    private FileSystemManager fileSystemManager;

    private Document fileBytesRequest;

    public FileBytesResponse(BufferedWriter output, FileSystemManager fileSystemManager,
                             Document fileBytesRequest) {
        super(output);
        this.fileSystemManager = fileSystemManager;
        this.fileBytesRequest = fileBytesRequest;
    }

    public FileBytesResponse(DatagramSocket socket, HostPort hostPort, FileSystemManager fileSystemManager,
                             Document fileBytesRequest) {
        super(socket, hostPort);
        this.fileSystemManager = fileSystemManager;
        this.fileBytesRequest = fileBytesRequest;
    }

    @Override
    public void run() {
        Document fileDescriptor = (Document) fileBytesRequest.get("fileDescriptor");
        String pathName = fileBytesRequest.getString("pathName");
        long position = fileBytesRequest.getLong("position");
        long length = fileBytesRequest.getLong("length");

        sendMessage(Messages.genFileBytesResponse(fileSystemManager, fileDescriptor,
                pathName, position, length));
    }
}
