package unimelb.bitbox.eventprocess;
import java.io.BufferedWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

/**
 * @author yanli
 *
 */
public class FileDeleteResponse extends EventProcess
{
	private Document received;
	private FileSystemManager fileSystemManager;

	
	public FileDeleteResponse(BufferedWriter output, Document received,
							  FileSystemManager fileSystemManager)
	{
		super(output);
		this.received = received;
		this.fileSystemManager = fileSystemManager;
	}

	public FileDeleteResponse(DatagramSocket socket, HostPort hostPort, Document received,
							  FileSystemManager fileSystemManager)
	{
		super(socket, hostPort);
		this.received = received;
		this.fileSystemManager = fileSystemManager;
	}
	
	@Override
	public void run() 
	{
		String pathName = received.getString("pathName");
		Document fileDescriptor = (Document)received.get("fileDescriptor");

		Document doc = new Document();
		doc.append("command", Messages.FILE_DELETE_RESPONSE);
		doc.append("fileDescriptor", fileDescriptor);
		doc.append("pathName", pathName);

		if(!fileSystemManager.isSafePathName(pathName))
		{
			doc.append("message", "unsafe pathname given");
			doc.append("status", false);
		}
		else 
		{
			boolean ifdelete = fileSystemManager.deleteFile(pathName, 
					fileDescriptor.getLong("lastModified"), 
					fileDescriptor.getString("md5"));
			if(ifdelete == true)
			{
			    doc.append("message", "file deleted");
			    doc.append("status", true);
			}
			else
			{
				doc.append("message", "there was a problem deleting the file");
				doc.append("status", false);
				//The file doesn't exist, or the last modified timestamp 
				//larger than that supplied, or their md5 are different
			}
		}
		sendMessage(doc.toJson());

	}
	
	
	
	
	
	
	
	
	
	
	

}
