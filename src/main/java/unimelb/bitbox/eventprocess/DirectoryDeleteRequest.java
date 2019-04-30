package unimelb.bitbox.eventprocess;

import unimelb.bitbox.messages.Messages;

import java.io.BufferedWriter;

public class DirectoryDeleteRequest extends BaseRunnable {

    private String pathName;

    public DirectoryDeleteRequest(BufferedWriter output, String pathName) {
        super(output);
        this.pathName = pathName;
    }

    @Override
    public void run() {

        sendMessage(Messages.genDirectoryDeleteRequest(pathName));
    }
}

