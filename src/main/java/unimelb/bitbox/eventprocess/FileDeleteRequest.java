/**
 * 
 */
package unimelb.bitbox.eventprocess;
import java.io.BufferedWriter;

import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

/**
 * @author yanli
 *
 */
public class FileDeleteRequest extends BaseRunnable
{
	private FileSystemEvent fileSystemEvent;

	
	public FileDeleteRequest(BufferedWriter output, FileSystemEvent fileSystemEvent)
	{
		super(output);
		this.fileSystemEvent = fileSystemEvent;
	}


	@Override
	public void run() 
	{
		Document doc = new Document();
		doc.append("command", Messages.FILE_DELETE_REQUEST);
		doc.append("fileDescriptor", fileSystemEvent.fileDescriptor.toDoc());
		doc.append("pathName", fileSystemEvent.pathName);
		
        sendMessage(doc.toJson());
		
	}
	
	
	
	
	
	
	
	
	
	
	

}
