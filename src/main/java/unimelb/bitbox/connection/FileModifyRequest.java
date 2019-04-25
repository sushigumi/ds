import java.io.IOException;

import org.w3c.dom.Document;

public class FileModifyRequest implements Runnable {
	private DataOutPutStreams output;
	private FileSystemEvent fileSystenEvent;
	
	public FileModifyRequest (DataOutputStream output, FileSystemEvent fileSystemEvent) {
		this.output= output;
		this.fileSystenEvent=fileSystenEvent;
	}
	
	public void run () {
		Document doc = new Document();
		doc.append("command","FILE_MODIFY_REQUESTE");
		doc.append("fileDescriptor",fileSystenEvent.fileDescriptor.toDoc());
		doc.append("pathName"£¬fileSystemEvent.pathName);
		
		try {
			output.writeUTF(doc.toJson());
			output.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	

}
