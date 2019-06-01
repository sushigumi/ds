package unimelb.bitbox.eventprocess;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.peer.UDPPeer;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;


public class FileModifyResponse extends EventProcess {

	private FileSystemManager fileSystemManager;
	private Document received;

	public FileModifyResponse(BufferedWriter output, Document received, FileSystemManager fileSystemManager) {
		super(output);
		this.received = received;
		this.fileSystemManager = fileSystemManager;
	}

	public FileModifyResponse(DatagramSocket socket, HostPort hostPort, UDPPeer peer,
							  Document received, FileSystemManager fileSystemManager) {
		super(socket, hostPort, peer);
		this.received = received;
		this.fileSystemManager = fileSystemManager;
	}

	public void run() {
		String pathName = received.getString("pathName");
		Document fileDescriptor = (Document) received.get("fileDescriptor");

		Document doc = new Document();
		doc.append("command", Messages.FILE_MODIFY_RESPONSE);
		doc.append("fileDescriptor", fileDescriptor);
		doc.append("pathName", pathName);
		//if pathName is not a safePathName then print a notification and stop the loop
		if (!(fileSystemManager.isSafePathName(pathName))) {
			doc.append("message", "unsafe pathname given");
			doc.append("status", false);
			sendMessage(doc.toJson());
		}
		//if file content does not already exist then print a notification and stop the loop
		else if (fileSystemManager.fileNameExists(pathName, fileDescriptor.getString("md5"))) {
			doc.append("message", "exact file already exists");
			doc.append("status", false);
			sendMessage(doc.toJson());
		} else {
			try {
				if (fileSystemManager.modifyFileLoader(pathName,
						fileDescriptor.getString("md5"),
						fileDescriptor.getLong("lastModified"))) {

					if (!fileSystemManager.checkShortcut(pathName)) {
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


				} else{
				    doc.append("message", "filePath being modified");
				    doc.append("status", false);
				    sendMessage(doc.toJson());
                }
			} catch (NoSuchAlgorithmException e) {
				doc.append("message", "file loader already in progress");
				doc.append("status", false);
				sendMessage(doc.toJson());
			} catch (IOException e) {
				doc.append("message", "error creating file loader");
				doc.append("status", false);
				sendMessage(doc.toJson());
			}
		}
	}

	public void sendMessageFileBytes(String message) {
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
