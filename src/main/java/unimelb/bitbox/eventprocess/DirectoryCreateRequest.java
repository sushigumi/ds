package unimelb.bitbox.eventprocess;

import unimelb.bitbox.messages.Messages;

import java.io.BufferedWriter;

public class DirectoryCreateRequest extends EventProcess {

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
