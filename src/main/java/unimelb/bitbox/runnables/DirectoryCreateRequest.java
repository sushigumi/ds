package unimelb.bitbox.runnables;

import unimelb.bitbox.messages.MessageGenerator;

import java.io.DataOutputStream;

public class DirectoryCreateRequest extends BaseRunnable {

    private String pathName;

    public DirectoryCreateRequest(DataOutputStream output, String pathName) {
        super(output);
        this.pathName = pathName;
    }

    @Override
    public void run() {

        sendMessage(MessageGenerator.genDirectoryCreateRequest(pathName));

    }
}
