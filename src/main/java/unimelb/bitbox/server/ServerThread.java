package unimelb.bitbox.server;

import unimelb.bitbox.peer.TCPPeerManager;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ServerThread extends Thread {
    FileSystemManager fileSystemManager;
    ScheduledExecutorService timer;

    ServerThread(String name, FileSystemManager fileSystemManager) {
        super(name);

        this.fileSystemManager = fileSystemManager;

        // Generate a timer to periodically sync events to peers
        Runnable periodicSync = new Runnable() {
            @Override
            public void run() {
                ArrayList<FileSystemManager.FileSystemEvent> fileSystemEvents = fileSystemManager.generateSyncEvents();

                for (FileSystemManager.FileSystemEvent event : fileSystemEvents) {
                    TCPPeerManager.getInstance().processFileSystemEvent(event);
                }
            }
        };

        timer = Executors.newSingleThreadScheduledExecutor();
        long syncInterval = Long.parseLong(Configuration.getConfigurationValue("syncInterval"));
        timer.scheduleAtFixedRate(periodicSync, syncInterval, syncInterval, TimeUnit.SECONDS);
    }
}
