package unimelb.bitbox.runnables;

import unimelb.bitbox.messages.Messages;

import java.io.BufferedWriter;

public class DirectoryCreateRequest extends BaseRunnable {

    private String pathName;

    public DirectoryCreateRequest(BufferedWriter output, String pathName) {
        super(output);
        this.pathName = pathName;
    }

    @Override
    public void run() {

        sendMessage(Messages.genDirectoryCreateRequest(pathName));

    }
}
