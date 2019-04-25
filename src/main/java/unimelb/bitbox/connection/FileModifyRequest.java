package unimelb.bitbox.connection;

import java.io.BufferedWriter;
import java.io.DataOutputStream;


import unimelb.bitbox.messages.Command;
import unimelb.bitbox.runnables.BaseRunnable;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

public class FileModifyRequest extends BaseRunnable {
	private FileSystemManager.FileSystemEvent fileSystemEvent;
	
	public FileModifyRequest (BufferedWriter output, FileSystemManager.FileSystemEvent fileSystemEvent) {
		super(output);
		this.fileSystemEvent=fileSystemEvent;
	}
	
	public void run () {
		Document doc = new Document();
		doc.append("command", Command.FILE_MODIFY_REQUEST.toString());
		doc.append("fileDescriptor",fileSystemEvent.fileDescriptor.toDoc());
		doc.append("pathName", fileSystemEvent.pathName);
		
		sendMessage(doc.toJson());
	}
	
	
	
	

}
