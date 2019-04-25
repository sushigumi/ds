package unimelb.bitbox.eventprocess;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import unimelb.bitbox.messages.Command;
import unimelb.bitbox.messages.MessageGenerator;
import unimelb.bitbox.runnables.BaseRunnable;
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

	
	public FileCreateResponse(DataOutputStream output, Document received, FileSystemManager fileSystemManager)
	{
		super(output);
		this.received = received;
		this.fileSystemManager = fileSystemManager;
	}
	
	@Override
	public void run() 
	{
		String pathName = received.getString("pathName");
		Document fileDescriptor = (Document)received.get("fileDescriptor");

		Document doc = new Document();
		doc.append("command", Command.FILE_CREATE_RESPONSE.toString());
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
			System.out.println(3);
			try {
				if(fileSystemManager.createFileLoader(pathName, 
						fileDescriptor.getString("md5"),fileDescriptor.getLong("fileSize"),
						fileDescriptor.getLong("lastModified"))){
				    doc.append("message", "file loader ready");
				    doc.append("status", true);
				    sendMessage(doc.toJson());
				    if(!fileSystemManager.checkShortcut(pathName))
				    {
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
