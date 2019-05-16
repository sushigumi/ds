package unimelb.bitbox.peer;

import unimelb.bitbox.eventprocess.*;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPClient {
    private ExecutorService sender;

    private DatagramSocket serverSocket;
    private HostPort remoteHostPort;

    public UDPClient(DatagramSocket serverSocket, HostPort remoteHostPort) {
        this.serverSocket = serverSocket;
        this.remoteHostPort = remoteHostPort;
        sender = Executors.newSingleThreadExecutor();
    }

    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        switch(fileSystemEvent.event) {
            case FILE_CREATE:
                sender.submit(new FileCreateRequest(serverSocket, remoteHostPort, fileSystemEvent));
                break;

            case FILE_DELETE:
                sender.submit(new FileDeleteRequest(serverSocket, remoteHostPort, fileSystemEvent));
                break;

            case FILE_MODIFY:
                sender.submit(new FileModifyRequest(serverSocket, remoteHostPort, fileSystemEvent));
                break;

            case DIRECTORY_CREATE:
                sender.submit(new DirectoryCreateRequest(serverSocket, remoteHostPort, fileSystemEvent.pathName));
                break;

            case DIRECTORY_DELETE:
                sender.submit(new DirectoryDeleteRequest(serverSocket, remoteHostPort, fileSystemEvent.pathName));
                break;
        }
    }
}
