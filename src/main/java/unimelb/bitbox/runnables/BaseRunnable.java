package unimelb.bitbox.runnables;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class BaseRunnable implements Runnable {
    private DataOutputStream output;

    public BaseRunnable(DataOutputStream output) {
        this.output = output;
    }

    public void updateOutput(DataOutputStream output) {
        this.output = output;
    }

    public void sendMessage(String message) {
        try {
            System.out.println(message);
            output.writeUTF(message);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
