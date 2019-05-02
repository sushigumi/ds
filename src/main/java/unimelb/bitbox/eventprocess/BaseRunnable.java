package unimelb.bitbox.eventprocess;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.logging.Logger;

public abstract class BaseRunnable implements Runnable {
    static Logger log = Logger.getLogger(BaseRunnable.class.getName());
    private BufferedWriter runnableOutput;

    public BaseRunnable(BufferedWriter output) {
        this.runnableOutput = output;
    }

    public void updateOutput(BufferedWriter output) {
        this.runnableOutput = output;
    }

    public void sendMessage(String message) {
        // Dont print anything if the output is null
        if (runnableOutput == null) {
            log.warning("trying to send to unknown remote");
            return;
        }

        try {
            //System.out.println(message);
            runnableOutput.write(message);
            runnableOutput.newLine();
            runnableOutput.flush();
        } catch (IOException e) {
            System.out.println("Base Runnable: Peer has closed the connection");
            System.out.println(e.getMessage());
            //e.printStackTrace();
        }
    }
}
