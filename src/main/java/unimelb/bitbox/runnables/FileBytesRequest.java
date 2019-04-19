package unimelb.bitbox.runnables;

import unimelb.bitbox.util.FileSystemManager;

import java.io.DataOutputStream;

public class FileBytesRequest extends BaseRunnable {
    private FileSystemManager manager;

    public FileBytesRequest(DataOutputStream output, FileSystemManager manager) {
        super(output);
        this.manager = manager;
    }

    @Override
    public void run() {

    }
}
