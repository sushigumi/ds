package unimelb.bitbox.peer;

import unimelb.bitbox.util.FileSystemManager;

import java.util.ArrayList;

public class UDPPeerManager {
    private ArrayList<UDPClient> rememberedPeers;

    private static UDPPeerManager ourInstance = new UDPPeerManager();

    public static UDPPeerManager getInstance() {
        return ourInstance;
    }

    private UDPPeerManager() {
        rememberedPeers = new ArrayList<>();
    }

    public void addPeer() {

    }

    /**
     * Process a file system event and send it to all the peers
     * @param fileSystemEvent
     */
    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        for (UDPClient peer : rememberedPeers) {
            peer.processFileSystemEvent(fileSystemEvent);
        }
    }
}
