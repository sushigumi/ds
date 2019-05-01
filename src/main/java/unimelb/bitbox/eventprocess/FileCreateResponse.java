package unimelb.bitbox.eventprocess;
import java.io.BufferedWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

/**
 * @author yanli
 *
 */
public class FileCreateResponse extends BaseRunnable
{
	private Document received;
	private FileSystemManager fileSystemManager;

	
	public FileCreateResponse(BufferedWriter output, Document received, FileSystemManager fileSystemManager)
	{
		super(output);
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
			doc.append("message", "pathname already exists");
			doc.append("status", false);
			sendMessage(doc.toJson());
		}
		else
		{
			try {
				fileSystemManager.cancelFileLoader(pathName);

			} catch (IOException e) {
				System.out.println("Error with accessing file system");
				doc.append("message", "error accessing file system. could not create file");
				doc.append("status", false);
				e.printStackTrace();
				return;
			}
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
							sendMessage(message);
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
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		
	}
}
