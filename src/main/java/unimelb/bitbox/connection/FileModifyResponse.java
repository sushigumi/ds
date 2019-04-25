package unimelb.bitbox.connection;

import unimelb.bitbox.messages.MessageGenerator;
import unimelb.bitbox.runnables.BaseRunnable;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;


public class FileModifyResponse extends BaseRunnable {

	private DataOutputStream output;
	private FileSystemManager fileSystemManager;
	private Document doc;
	private FileSystemManager.FileSystemEvent fileSystemEvent;

	public FileModifyResponse(DataOutputStream output, Document doc, FileSystemManager fileSystemManager) {
		super(output);
		this.doc = doc;
		this.fileSystemManager = fileSystemManager;
	}

	public void run() {
		String pathName = doc.getString("pathName");
		Document fileDescriptor = (Document) doc.get("fileDescriptor");
		//if pathName is not a safePathName then print a notification and stop the loop
		if (!(fileSystemManager.isSafePathName(pathName))) {
			doc.append("message", "unsafe pathname given");
			doc.append("status", "false");
			sendMessage(doc.toJson());
		}
		//if pathName does not exist then print a notification and stop the loop
		else if (!(fileSystemManager.dirNameExists(pathName))) {
			doc.append("message", "pathname does not exist");
			doc.append("status", "false");
			sendMessage(doc.toJson());
		}
		//if file content already exist then print a notification and stop the loop
		else if (fileSystemManager.fileNameExists(pathName, fileDescriptor.getString("md5"))) {
			doc.append("message", "unsafe pathname given");
			doc.append("status", "false");
			sendMessage(doc.toJson());
		} else {
			try {
				if (fileSystemManager.modifyFileLoader(pathName,
						fileDescriptor.getString("md5"),
						fileDescriptor.getLong("lastModified"))) {
					doc.append("message", "file loader ready");
					doc.append("status", "true");
					sendMessage(doc.toJson());
					if (!fileSystemManager.checkShortcut(pathName)) {
						ArrayList<String> messages = MessageGenerator.genFileBytesRequests(fileDescriptor, pathName);

						for (String message : messages) {
							sendMessage(message);
						}

					}


				}
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
