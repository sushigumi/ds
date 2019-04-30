package unimelb.bitbox.eventprocess;

import java.io.BufferedWriter;
import java.io.IOException;

public abstract class BaseRunnable implements Runnable {
    private BufferedWriter runnableOutput;

    public BaseRunnable(BufferedWriter output) {
        this.runnableOutput = output;
    }

    public void updateOutput(BufferedWriter output) {
        this.runnableOutput = output;
    }

    public void sendMessage(String message) {
        try {
            System.out.println(message);
            runnableOutput.write(message);
            runnableOutput.newLine();
            runnableOutput.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
