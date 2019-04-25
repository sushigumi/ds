package unimelb.bitbox.runnables;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class BaseRunnable implements Runnable {
    private BufferedWriter output;

    public BaseRunnable(BufferedWriter output) {
        this.output = output;
    }

    public void updateOutput(BufferedWriter output) {
        this.output = output;
    }

    public void sendMessage(String message) {
        try {
            System.out.println(message);
            output.write(message);
            output.newLine();
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
