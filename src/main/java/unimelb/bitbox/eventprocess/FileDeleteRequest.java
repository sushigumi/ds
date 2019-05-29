/**
 * 
 */
package unimelb.bitbox.eventprocess;
import java.io.BufferedWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.peer.UDPPeer;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

/**
 * @author yanli
 *
 */
public class FileDeleteRequest extends EventProcess
{
	private FileSystemEvent fileSystemEvent;

	
	public FileDeleteRequest(BufferedWriter output, FileSystemEvent fileSystemEvent)
	{
		super(output);
		this.fileSystemEvent = fileSystemEvent;
	}

	public FileDeleteRequest(DatagramSocket socket, HostPort hostPort, FileSystemEvent fileSystemEvent)
	{
		super(socket, hostPort);
		this.fileSystemEvent = fileSystemEvent;
	}

	public FileDeleteRequest(DatagramSocket socket, HostPort hostPort, FileSystemEvent fileSystemEvent, UDPPeer peer)
	{
		super(socket, hostPort, peer);
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
