package unimelb.bitbox.connection;

import java.io.DataOutputStream;
import java.io.IOException;


import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

public class FileModifyRequest implements Runnable {
	private DataOutputStream output;
	private FileSystemManager.FileSystemEvent fileSystemEvent;
	
	public FileModifyRequest (DataOutputStream output, FileSystemManager.FileSystemEvent fileSystemEvent) {
		this.output= output;
		this.fileSystemEvent=fileSystemEvent;
	}
	
	public void run () {
		Document doc = new Document();
		doc.append("command","FILE_MODIFY_REQUESTE");
		doc.append("fileDescriptor",fileSystemEvent.fileDescriptor.toDoc());
		doc.append("pathName", fileSystemEvent.pathName);
		
		try {
			output.writeUTF(doc.toJson());
			output.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	

}
