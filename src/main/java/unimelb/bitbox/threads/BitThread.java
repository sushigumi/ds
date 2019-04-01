package unimelb.bitbox.threads;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * This is Thread which can handle up to "maximumIncomingConnections" connections as defined in the properties
 * file.
 * BitThreads are handled by the Thread Controller and any new instances are created there
 */
public class BitThread extends Thread {
    private final Socket socket;

    private boolean running;

    // TODO: Maybe a state here

    public BitThread(Socket socket) {
        this.socket = socket;
        this.running = true;

    }

    @Override
    public void run() {
        // Try to create the print writer and buffered reader.
        // TODO Check if requires pw and br to pass large json message
        try  {
            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (running) {
                pw.println("String:");
                String in = br.readLine();

                if (in.equals("exit")) end();
                System.out.println(in);
            }

            // Release the semaphore once it is done
            ThreadController.getInstance().releaseSemaphore();
            pw.close();
            br.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets running field to false to exit the loop and stop the thread
     */
    public void end() {
        running = false;
    }
}
