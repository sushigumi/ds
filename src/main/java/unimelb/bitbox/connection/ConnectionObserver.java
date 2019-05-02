package unimelb.bitbox.connection;

import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

public interface ConnectionObserver {
    void closeConnection(Connection connection, boolean isIncoming);
}
