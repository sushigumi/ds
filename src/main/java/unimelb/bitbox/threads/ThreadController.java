package unimelb.bitbox.threads;

import unimelb.bitbox.util.Configuration;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class ThreadController {
    private static ThreadController ourInstance = new ThreadController();

    private int MAX_THREADS;
    private Semaphore available;

    public static ThreadController getInstance() {
        return ourInstance;
    }

    private ThreadController() {
        MAX_THREADS = Integer.parseInt(Configuration.getConfigurationValue("maximumIncomingConnections"));

        available= new Semaphore(MAX_THREADS, true);
    }

    /**
     * Creates a new thread to handle the connection with peers if the current limit has not been reached. Otherwise
     * a CONNECTION_REFUSED message should be sent back to the client thorugh the MessageController
     * @param socket
     */
    public void newThread(Socket socket) {
        // Debug message
        System.out.println(available.availablePermits());

        if (available.availablePermits() == 0) {
            // TODO: Send the REFUSED message
        } else {
            try {
                available.acquire();
                Thread t = new BitThread(socket);
                t.start();
            } catch (InterruptedException e) {
                available.release();
                // TODO MIght need to send an appropriate message
                // Close the socket if there is error starting the thread
                try {
                    socket.close();
                } catch (IOException f) {
                    f.printStackTrace();
                }
                e.printStackTrace();
            }
        }
    }

    /**
     * Allows other classes to release a semaphore
     */
    public void releaseSemaphore() {
        available.release();
    }
}
