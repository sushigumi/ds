package unimelb.bitbox.eventprocess;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.peer.UDPPeer;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

/**
 * @author yanli
 *
 */
public class FileCreateResponse extends EventProcess
{
	private Document received;
	private FileSystemManager fileSystemManager;

	
	public FileCreateResponse(BufferedWriter output, Document received, FileSystemManager fileSystemManager)
	{
		super(output);
		this.received = received;
		this.fileSystemManager = fileSystemManager;
	}

	public FileCreateResponse(DatagramSocket socket, HostPort hostPort, UDPPeer peer,
							  Document received, FileSystemManager fileSystemManager)
	{
		super(socket, hostPort, peer);
		this.received = received;
		this.fileSystemManager = fileSystemManager;
	}
	
	@Override
	public void run() 
	{
		// Check for errors
		String pathName = received.getString("pathName");
		Document fileDescriptor = (Document)received.get("fileDescriptor");

		Document doc = new Document();
		doc.append("command", Messages.FILE_CREATE_RESPONSE);
		doc.append("fileDescriptor", fileDescriptor);
		doc.append("pathName", pathName);
		
		if(!fileSystemManager.isSafePathName(pathName))
		{   
			doc.append("message", "unsafe pathname given");
			doc.append("status", false);
			sendMessage(doc.toJson());
		}
		else if(fileSystemManager.fileNameExists(pathName)) 
		{
			try {
				// Modify a file if it is already there but it last modified is older
				if (fileSystemManager.modifyFileLoader(pathName, fileDescriptor.getString("md5"), fileDescriptor.getLong("lastModified"))) {
					doc.append("message", "file loader ready");
					doc.append("status", true);
					sendMessage(doc.toJson());

					ArrayList<String> messages = Messages.genFileBytesRequests(fileDescriptor, pathName);

					for (String message : messages) {
						sendMessageFileBytes(message);
					}
				}
				// Else dont send
				else {
					doc.append("message", "pathname already exists");
					doc.append("status", false);
					sendMessage(doc.toJson());
				}
			} catch (IOException e) {
				doc.append("message", e.getMessage());
				doc.append("status", false);
				sendMessage(doc.toJson());
			}
		}
		else
		{
//			try {
//				fileSystemManager.cancelFileLoader(pathName);
//
//			} catch (IOException e) {
//				System.out.println("Error with accessing file system");
//				doc.append("message", "error accessing file system. could not create file");
//				doc.append("status", false);
//				e.printStackTrace();
//				return;
//			}
			try {
				if(fileSystemManager.createFileLoader(pathName,
						fileDescriptor.getString("md5"),fileDescriptor.getLong("fileSize"),
						fileDescriptor.getLong("lastModified"))){

					// TODO check if right
					if(!fileSystemManager.checkShortcut(pathName))
					{
						doc.append("message", "file loader ready");
						doc.append("status", true);
						sendMessage(doc.toJson());

						ArrayList<String> messages = Messages.genFileBytesRequests(fileDescriptor, pathName);

						for (String message : messages) {
							sendMessageFileBytes(message);
						}
					}
					else {
						doc.append("message", "file created from local copy");
						doc.append("status", true);
						sendMessage(doc.toJson());
					}
				} else {
					doc.append("message", "file loader being modified");
					doc.append("status", false);
					sendMessage(doc.toJson());
				}
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				doc.append("message", "file is currently being modified");
				doc.append("status", false);
				sendMessage(doc.toJson());
				//e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}

		}
		
		
	}

	private void sendMessageFileBytes(String message) {
		if (ServerMain.getMode().equals(ServerMain.MODE_TCP)) {
			super.sendMessage(message);
		}
		// UDP is different because it should not send back for the file create response runnable
		// Instead submit the file bytes request
		else {
			// Convert the message to a bytes buffer before sending to the other peer
			message = message + "\n";
			byte[] buf;
			try {
				buf = message.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.severe("unsupported encoding");
				return;
			}
			//System.out.println(message);
			DatagramPacket packet;
			try {
				packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(hostPort.host), hostPort.port);
			} catch (UnknownHostException e) {
				log.severe(e.getMessage());
				return;
			}
//                System.out.println(packet.getLength());
//                System.out.println(new String(packet.getData()));
			//System.out.println(packet.getAddress() + "," + packet.getPort());
			try {
				socket.send(packet);
			} catch (IOException e) {
				log.severe("io error occurred.");
			}

			// Insert a retry only for requests sent
			if (peer != null) {
				Document doc = Document.parse(message);
				peer.queueRetry(new FileBytesRequest(socket, hostPort, doc, peer),
						doc);
			}
		}
	}
}
