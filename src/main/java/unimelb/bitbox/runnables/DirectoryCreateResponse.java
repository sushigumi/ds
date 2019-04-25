package unimelb.bitbox.runnables;

import unimelb.bitbox.messages.MessageGenerator;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.BufferedWriter;
import java.io.DataOutputStream;

public class DirectoryCreateResponse extends BaseRunnable {


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

        sendMessage(MessageGenerator.genDirectoryCreateResponse(fileSystemManager,
                request.getString("pathName")));

    }
}
