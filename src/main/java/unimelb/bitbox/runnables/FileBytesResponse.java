package unimelb.bitbox.runnables;

import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.BufferedWriter;

public class FileBytesResponse extends BaseRunnable {
    private FileSystemManager fileSystemManager;

    private Document fileBytesRequest;

    public FileBytesResponse(BufferedWriter output, FileSystemManager fileSystemManager,
                             Document fileBytesRequest) {
        super(output);
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
