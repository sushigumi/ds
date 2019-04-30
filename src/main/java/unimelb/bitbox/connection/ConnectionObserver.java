package unimelb.bitbox.connection;

import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

public interface ConnectionObserver {
    void closeConnection(HostPort remoteHostPort, boolean isIncoming);

    void interruptConnection(HostPort remoteHostPort, HostPort localHostPort, FileSystemManager fileSystemManager);
}
