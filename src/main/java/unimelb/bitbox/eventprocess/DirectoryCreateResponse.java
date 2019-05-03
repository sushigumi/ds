package unimelb.bitbox.eventprocess;

import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.BufferedWriter;

public class DirectoryCreateResponse extends EventProcess {


    private FileSystemManager fileSystemManager;
    private Document request;

    public DirectoryCreateResponse(BufferedWriter output, FileSystemManager fileSystemManager,
                                   Document request) {
        super(output);
        this.fileSystemManager = fileSystemManager;
        this.request = request;

    }

    @Override
    public void run() {

        sendMessage(Messages.genDirectoryCreateResponse(fileSystemManager,
                request.getString("pathName")));

    }
}
