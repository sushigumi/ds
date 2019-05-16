package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.BufferedWriter;
import java.net.DatagramSocket;

public class FileBytesResponse extends EventProcess {
    private FileSystemManager fileSystemManager;

    private Document fileBytesRequest;

    public FileBytesResponse(BufferedWriter output, FileSystemManager fileSystemManager,
                             Document fileBytesRequest) {
        super(output);
        this.fileSystemManager = fileSystemManager;
        this.fileBytesRequest = fileBytesRequest;
    }

    public FileBytesResponse(DatagramSocket socket, FileSystemManager fileSystemManager,
                             Document fileBytesRequest) {
        super(socket);
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
