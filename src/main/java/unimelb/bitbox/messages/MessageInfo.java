package unimelb.bitbox.messages;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import javax.print.Doc;
import java.util.concurrent.ScheduledFuture;

public class MessageInfo {
    private String command;
    private Document doc;

    private ScheduledFuture future;
    private int retries;

    public MessageInfo(Document doc) {
        this.command = doc.getString("command");
        this.doc = doc;

        this.retries = 0;
    }

    public Document getDoc() {
        return doc;
    }

    public ScheduledFuture getFuture() {
        return future;
    }

    public void setFuture(ScheduledFuture future) {
        this.future = future;
    }

    public void updateFuture(ScheduledFuture future) {
        this.future = future;
        incRetries();
    }

    private void incRetries() {
        retries++;
    }

    public int getRetries() {
        return retries;
    }

    public boolean isExceedRetryLimit() {
        return retries >= ServerMain.MAX_RETRIES - 1;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;

        if (!(o instanceof MessageInfo)) return false;
        MessageInfo otherInfo = (MessageInfo) o;

        Document other = otherInfo.getDoc();

        String otherCommand = other.getString("command");

        if (!command.equals(otherCommand)) return false;

        if (command.equals(Messages.HANDSHAKE_REQUEST)) {
            HostPort hostPort = new HostPort((Document) doc.get("hostPort"));
            HostPort otherHostPort = new HostPort((Document) other.get("hostPort"));

            return hostPort.equals(otherHostPort);
        }
        else if (command.equals(Messages.DIRECTORY_CREATE_REQUEST) || command.equals(Messages.DIRECTORY_DELETE_REQUEST)) {
            String pathName = doc.getString("pathName");
            String otherPathName = other.getString("pathName");
            return pathName.equals(otherPathName);
        }
        else {
            //TODO need to change instead of fild descriptor since u cant do that
            Document fd = (Document)doc.get("fileDescriptor");
            Document otherfd = (Document)other.get("fileDescriptor");

            if (!fd.getString("md5").equals(otherfd.getString("md5"))) return false;
            if (!(fd.getLong("lastModified") == otherfd.getLong("lastModified"))) return false;
            if (!(fd.getLong("fileSize") == otherfd.getLong("fileSize"))) return false;

            String pathName = doc.getString("pathName");
            String otherPathName = other.getString("pathname");
            if (!pathName.equals(otherPathName)) return false;


            // Handle the case of file bytes request where there are additional fields
            if (command.equals(Messages.FILE_BYTES_REQUEST)) {
                long position = doc.getLong("position");
                long otherPosition = other.getLong("position");
                if (!(position == otherPosition)) return false;

                long length = doc.getLong("length");
                long otherLength = other.getLong("length");
                if (!(length == otherLength)) return false;
            }
        }

        return true;
    }
}
