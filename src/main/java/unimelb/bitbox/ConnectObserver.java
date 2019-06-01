package unimelb.bitbox;

public interface ConnectObserver {
    void notifyConnectionSuccessful();

    void notifyConnectionUnsuccessful();
}
