package unimelb.bitbox.runnables;

import unimelb.bitbox.messages.MessageGenerator;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * ConstructFile is a Runnable which is constructed upon receiving a File Bytes Response message
 * It blocks until receiving all the required messages to fully construct the required file
 * before returning
 */
public class ConstructFile extends BaseRunnable {
    private FileSystemManager fileSystemManager;
    private Document fileBytesResponse;

    public ConstructFile(DataOutputStream output, FileSystemManager fileSystemManager,
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

//        Document fileDescriptor = (Document) fileBytesResponse.get("fileDescriptor");
//        long length = fileBytesResponse.getLong("length");

        // Safe file name so can write successfully
        if (fileSystemManager.isSafePathName(pathName)) {
            // TODO Remove
//            try {
//                fileSystemManager.createFileLoader(pathName, fileDescriptor.getString("md5"), length, fileDescriptor.getLong("lastModified"));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            // If there is a failure in writing then should request for the bytes again
            try {
                fileSystemManager.writeFile(pathName, bytes, position);
            }
            // Cannot write the bytes
            catch (IOException e) {
                Document fileDescriptor = (Document) fileBytesResponse.get("fileDescriptor");
                sendMessage(MessageGenerator.genFileBytesRequest(fileDescriptor, pathName, position));
            }
        }
        // Unsafe path name so don't do anything
        else {
            // TODO more?
            System.out.println("WARNING: Trying to write to an unsafe path");
        }
    }
}
