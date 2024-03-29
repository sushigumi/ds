package unimelb.bitbox.eventprocess;

import java.io.BufferedWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;


import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.peer.UDPPeer;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

public class FileModifyRequest extends EventProcess {
	private FileSystemManager.FileSystemEvent fileSystemEvent;
	
	public FileModifyRequest (BufferedWriter output, FileSystemManager.FileSystemEvent fileSystemEvent) {
		super(output);
		this.fileSystemEvent=fileSystemEvent;
	}

	public FileModifyRequest (DatagramSocket socket, HostPort hostPort,
							  FileSystemManager.FileSystemEvent fileSystemEvent) {
		super(socket, hostPort);
		this.fileSystemEvent=fileSystemEvent;
	}

	public FileModifyRequest (DatagramSocket socket, HostPort hostPort,
							  FileSystemManager.FileSystemEvent fileSystemEvent, UDPPeer peer) {
		super(socket, hostPort, peer);
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
