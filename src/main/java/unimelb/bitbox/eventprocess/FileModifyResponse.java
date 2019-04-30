package unimelb.bitbox.eventprocess;

import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.eventprocess.BaseRunnable;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;


public class FileModifyResponse extends BaseRunnable {

	private FileSystemManager fileSystemManager;
	private Document received;

	public FileModifyResponse(BufferedWriter output, Document received, FileSystemManager fileSystemManager) {
		super(output);
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
			doc.append("message", "filepath does not exist");
			doc.append("status", false);
			sendMessage(doc.toJson());
		} else {
			try {
				if (fileSystemManager.modifyFileLoader(pathName,
						fileDescriptor.getString("md5"),
						fileDescriptor.getLong("lastModified"))) {
					doc.append("message", "file loader ready");
					doc.append("status", true);
					sendMessage(doc.toJson());
					if (!fileSystemManager.checkShortcut(pathName)) {
						ArrayList<String> messages = Messages.genFileBytesRequests(fileDescriptor, pathName);

						for (String message : messages) {
							sendMessage(message);
						}

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
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				doc.append("message", "error creating file loader");
				doc.append("status", false);
				sendMessage(doc.toJson());
				e.printStackTrace();
			}
		}
	}
}
