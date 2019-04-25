package unimelb.bitbox.runnables;

import unimelb.bitbox.messages.MessageGenerator;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.util.ArrayList;

/**
 * This Runnable is queued on the sender thread after a File Create Request has been sent and the file
 * needs to be copied from the peer.
 * Sends a series of messages depending on how big the file that needs to requested is and also the blockSize
 * to receive the content of the file.
 */
public class FileBytesRequest extends BaseRunnable {
    private Document fileDescriptor;
    private String pathName;

    public FileBytesRequest(BufferedWriter output, Document fileDescriptor, String pathName) {
        super(output);
        this.fileDescriptor = fileDescriptor;
        this.pathName = pathName;
    }

    @Override
    public void run() {
        ArrayList<String> messages = MessageGenerator.genFileBytesRequests(fileDescriptor, pathName);

        for (String message : messages) {
            sendMessage(message);
        }
    }
}
