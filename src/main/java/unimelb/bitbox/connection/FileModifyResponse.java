import java.io.IOException;

import org.w3c.dom.Document;

public class FileModifyResponse implements Runnable{
	
	private DataOutPutStreams output;
	private FileSystemEvent fileSystenEvent;
	
	public FileModifyResponse (DataOutputStream output, FileSystemEvent fileSystemEvent) {
		this.output= output;
		this.fileSystenEvent=fileSystenEvent;
	}
	
	public void run () {
		while (true) {	
			//if pathName is not a safePathName then print a notification and stop the loop
			if(!(fileSystemManager.isSafePathName(fileSystemEvent.pathName))) {
				system.out.println("unsafe pathname given");
				break;
			}
			//if pathName does not exist then print a notification and stop the loop
			if(!(fileSystemManager.dirNameExists(fileSystemEvent.pathName))) {
				system.out.println("pathname does not exist");
				break;
			}
			//if file content already exist then print a notification and stop the loop
			if(fileSystemManager.fileNameExists(fileSystemEvent.pathName,fileSystemEvent.md5)) {
				system.out.println("already exists with matching content");
				break;
			}
			
			Document doc = new Document();	
			doc.append("command:","FILE_MODIFY_RESPONSE");
			doc.append("fileDescriptor:",fileSystenEvent.fileDescriptor.toDoc());
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
