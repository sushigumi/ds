package unimelb.bitbox.connection;

import unimelb.bitbox.util.HostPort;

public interface ConnectionObserver {
    void closeConnection(HostPort remoteHostPort);
}
