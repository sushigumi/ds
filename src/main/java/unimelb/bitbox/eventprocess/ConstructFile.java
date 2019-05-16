package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * ConstructFile is a Runnable which is constructed upon receiving a File Bytes Response message
 * It blocks until receiving all the required messages to fully construct the required file
 * before returning
 */
public class ConstructFile extends EventProcess {
    private FileSystemManager fileSystemManager;
    private Document fileBytesResponse;

    public ConstructFile(BufferedWriter output, FileSystemManager fileSystemManager,
                         Document fileBytesResponse) {
        super(output, ServerMain.MODE_TCP);
        this.fileSystemManager = fileSystemManager;
        this.fileBytesResponse = fileBytesResponse;
    }

    public ConstructFile(DatagramSocket datagramSocket, FileSystemManager fileSystemManager,
                         Document fileBytesResponse) {
        super(datagramSocket, ServerMain.MODE_UDP);
        this.fileSystemManager = fileSystemManager;
        this.fileBytesResponse = fileBytesResponse;
    }

    @Override
    public void run() {
        // Extract required information from the FileBytesResponse message
        String pathName = fileBytesResponse.getString("pathName");
        String encodedBytes = fileBytesResponse.getString("content");
        ByteBuffer bytes = ByteBuffer.wrap(Base64.getDecoder().decode(encodedBytes));
        long position = fileBytesResponse.getLong("position");
        boolean status = fileBytesResponse.getBoolean("status");

        // Status is false. So something must have happened reading the file. So we need
        // to request for the file again
        if (!status) {
            Document fileDescriptor = (Document) fileBytesResponse.get("fileDescriptor");
            sendMessage(Messages.genFileBytesRequest(fileDescriptor, pathName, position));
            return;
        }

        // Safe file name so can write successfully
        if (fileSystemManager.isSafePathName(pathName)) {
            try {
                fileSystemManager.writeFile(pathName, bytes, position);
                fileSystemManager.checkWriteComplete(pathName);
            }
            // Cannot write the bytes so request again.
            catch (IOException e) {
                Document fileDescriptor = (Document) fileBytesResponse.get("fileDescriptor");
                sendMessage(Messages.genFileBytesRequest(fileDescriptor, pathName, position));
            }
            // Ignore this exception
            catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        // Unsafe path name so don't do anything
        else {
            log.severe("WARNING: trying to write to an unsafe path");
        }
    }
}
