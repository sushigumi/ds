package unimelb.bitbox.peer;

import unimelb.bitbox.eventprocess.*;
import unimelb.bitbox.util.FileSystemManager;

import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPClient {
    private ExecutorService sender;

    private DatagramSocket socket;

    public UDPClient(DatagramSocket socket) {
        this.socket = socket;
        sender = Executors.newSingleThreadExecutor();
    }

    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        switch(fileSystemEvent.event) {
            case FILE_CREATE:
                sender.submit(new FileCreateRequest(socket, fileSystemEvent));
                break;

            case FILE_DELETE:
                sender.submit(new FileDeleteRequest(socket, fileSystemEvent));
                break;

            case FILE_MODIFY:
                sender.submit(new FileModifyRequest(socket, fileSystemEvent));
                break;

            case DIRECTORY_CREATE:
                sender.submit(new DirectoryCreateRequest(socket, fileSystemEvent.pathName));
                break;

            case DIRECTORY_DELETE:
                sender.submit(new DirectoryDeleteRequest(socket, fileSystemEvent.pathName));
                break;
        }
    }
}
