package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;

import java.io.BufferedWriter;
import java.net.DatagramSocket;
import java.util.ArrayList;

/**
 * This Runnable is queued on the sender thread after a File Create Request has been sent and the file
 * needs to be copied from the peer.
 * Sends a series of messages depending on how big the file that needs to requested is and also the blockSize
 * to receive the content of the file.
 */
public class FileBytesRequest extends EventProcess {
    private Document fileDescriptor;
    private String pathName;

    public FileBytesRequest(BufferedWriter output, Document fileDescriptor, String pathName) {
        super(output, ServerMain.MODE_TCP);
        this.fileDescriptor = fileDescriptor;
        this.pathName = pathName;
    }

    public FileBytesRequest(DatagramSocket socket, Document fileDescriptor, String pathName) {
        super(socket, ServerMain.MODE_UDP);
        this.fileDescriptor = fileDescriptor;
        this.pathName = pathName;
    }

    @Override
    public void run() {
        ArrayList<String> messages = Messages.genFileBytesRequests(fileDescriptor, pathName);

        for (String message : messages) {
            sendMessage(message);
        }
    }
}
