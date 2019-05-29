package unimelb.bitbox.peer;

public interface ConnectionObserver {
    void closeConnection(Connection connection, boolean isIncoming);

    void retry(Connection connection);
}
