package unimelb.bitbox.connection;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.DataOutputStream;
import java.io.IOException;


public class FileModifyResponse implements Runnable{
	
	private DataOutputStream output;
	private FileSystemManager fileSystemManager;
	private FileSystemManager.FileSystemEvent fileSystemEvent;
	
	public FileModifyResponse (DataOutputStream output, FileSystemManager fileSystemManager, FileSystemManager.FileSystemEvent fileSystemEvent) {
		this.output= output;
		this.fileSystemEvent =fileSystemEvent;
	}
	
	public void run () {
		while (true) {	
			//if pathName is not a safePathName then print a notification and stop the loop
			if(!(fileSystemManager.isSafePathName(fileSystemEvent.pathName))) {
				System.out.println("unsafe pathname given");
				break;
			}
			//if pathName does not exist then print a notification and stop the loop
			if(!(fileSystemManager.dirNameExists(fileSystemEvent.pathName))) {
				System.out.println("pathname does not exist");
				break;
			}
			//if file content already exist then print a notification and stop the loop
			if(fileSystemManager.fileNameExists(fileSystemEvent.pathName,fileSystemEvent.fileDescriptor.md5)) {
				System.out.println("already exists with matching content");
				break;
			}
			
			Document doc = new Document();
			doc.append("command:","FILE_MODIFY_RESPONSE");
			doc.append("fileDescriptor:", fileSystemEvent.fileDescriptor.toDoc());
			doc.append("pathName:",fileSystemEvent.pathName);
			doc.append("message:","file loader ready");
			doc.append("statue:","true");
			
			try {
				output.writeUTF(doc.toJson());
				output.flush();
			} catch (IOException e) {
				e.printStackTrace();
				}  break;
		}
	}
 
}
