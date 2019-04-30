package unimelb.bitbox.eventprocess;

import java.io.BufferedWriter;


import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.eventprocess.BaseRunnable;
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
		doc.append("command", Messages.FILE_MODIFY_REQUEST);
		doc.append("fileDescriptor",fileSystemEvent.fileDescriptor.toDoc());
		doc.append("pathName", fileSystemEvent.pathName);
		
		sendMessage(doc.toJson());
	}
	
	
	
	

}