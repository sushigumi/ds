package unimelb.bitbox.eventprocess;

import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.BufferedWriter;

public class DirectoryDeleteResponse extends BaseRunnable {

    private FileSystemManager fileSystemManager;
    private Document request;

    public DirectoryDeleteResponse(BufferedWriter output, FileSystemManager fileSystemManager,
                                   Document request) {
        super(output);
        this.fileSystemManager = fileSystemManager;
        this.request = request;
    }

    @Override
    public void run() {
        sendMessage(Messages.genDirectoryDeleteResponse(fileSystemManager,
                request.getString("pathName")));
    }
}
