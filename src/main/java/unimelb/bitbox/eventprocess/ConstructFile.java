package unimelb.bitbox.eventprocess;

import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * ConstructFile is a Runnable which is constructed upon receiving a File Bytes Response message
 * It blocks until receiving all the required messages to fully construct the required file
 * before returning
 */
public class ConstructFile extends BaseRunnable {
    private FileSystemManager fileSystemManager;
    private Document fileBytesResponse;

    public ConstructFile(BufferedWriter output, FileSystemManager fileSystemManager,
                         Document fileBytesResponse) {
        super(output);
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

        // Safe file name so can write successfully
        if (fileSystemManager.isSafePathName(pathName)) {
            try {
                fileSystemManager.writeFile(pathName, bytes, position);
                fileSystemManager.checkWriteComplete(pathName);
            }
            // Cannot write the bytes
            catch (IOException e) {
                Document fileDescriptor = (Document) fileBytesResponse.get("fileDescriptor");
                sendMessage(Messages.genFileBytesRequest(fileDescriptor, pathName, position));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        // Unsafe path name so don't do anything
        else {
            log.severe("WARNING: trying to write to an unsafe path");
        }
    }
}
