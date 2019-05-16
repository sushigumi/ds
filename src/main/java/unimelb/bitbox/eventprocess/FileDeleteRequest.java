/**
 * 
 */
package unimelb.bitbox.eventprocess;
import java.io.BufferedWriter;
import java.net.DatagramSocket;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

/**
 * @author yanli
 *
 */
public class FileDeleteRequest extends EventProcess
{
	private FileSystemEvent fileSystemEvent;

	
	public FileDeleteRequest(BufferedWriter output, FileSystemEvent fileSystemEvent)
	{
		super(output, ServerMain.MODE_TCP);
		this.fileSystemEvent = fileSystemEvent;
	}

	public FileDeleteRequest(DatagramSocket socket, FileSystemEvent fileSystemEvent)
	{
		super(socket, ServerMain.MODE_UDP);
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
