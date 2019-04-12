package unimelb.bitbox.connection;

import unimelb.bitbox.messages.MessageGenerator;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Connection {
    private Socket socket; // Other peer's socket

    private HostPort localHostPort;
    private HostPort remoteHostPort;

    private DataOutputStream output;
    private DataInputStream input;

    private Thread listener;
    private ExecutorService sender;

    /**
     * Called when receiving a connection from another peer
     * @param socket
     * @param localHostPort
     */
    public Connection(Socket socket, HostPort localHostPort) {
        this.socket = socket;
        this.localHostPort = localHostPort;

        createWriterAndReader();

        // Create the thread to listen to messages
        listener = new Thread(new Listener());
        listener.start();

        // Create the single thread executor to send messages based on a queue when it requires messages to be
        // sent
        sender = Executors.newSingleThreadExecutor();
        sender.submit(MessageGenerator.generateHandshakeResponse(localHostPort, output));
    }

    /**
     * Called when making a connection to another peer
     * So this peer needs to send a handshake request to the other peer
     * @param socket
     * @param localHostPort
     * @param remoteHostPort HostPort of the peer to be connected
     */
    public Connection(Socket socket, HostPort localHostPort, HostPort remoteHostPort) {
        this.socket = socket;
        this.localHostPort = localHostPort;
        this.remoteHostPort = remoteHostPort;

        createWriterAndReader();

        // Create the thread to listen to messages
        listener = new Thread(new Listener());
        listener.start();

        // Create a single thread executor to send messages based on a queue when it requires messages to be sent
        sender = Executors.newSingleThreadExecutor();
        sender.submit(MessageGenerator.genHandshakeRequest(localHostPort, output));
    }

    private void createWriterAndReader() {
        try {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * A runnable to only listen and receive messages from other peers
     */
    private class Listener implements Runnable {
        @Override
        public void run() {
            // If cannot read what is being sent, then just ignore and wait for the other peer to send
            // the message again
            try {
                while (true) {
                    String in = input.readUTF();

                    Document doc = Document.parse(in);

                    System.out.println(in);
                }
            } catch (IOException e) {

            }
        }
    }
}
