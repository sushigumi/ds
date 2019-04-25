package unimelb.bitbox.runnables;

import unimelb.bitbox.messages.MessageGenerator;

import java.io.DataOutputStream;

public class DirectoryDeleteRequest extends BaseRunnable {

    private String pathName;

    public DirectoryDeleteRequest(DataOutputStream output, String pathName) {
        super(output);
        this.pathName = pathName;
    }

    @Override
    public void run() {

        sendMessage(MessageGenerator.genDirectoryDeleteRequest(pathName));
    }
}

