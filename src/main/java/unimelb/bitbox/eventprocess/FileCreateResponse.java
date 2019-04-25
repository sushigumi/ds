package unimelb.bitbox.eventprocess;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

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
	private Document doc;
	private FileSystemManager fileSystemManager;

	
	public FileCreateResponse(DataOutputStream output, Document doc, FileSystemManager fileSystemManager) 
	{
		super(output);
		this.doc = doc;
		this.fileSystemManager = fileSystemManager;
	}
	
	@Override
	public void run() 
	{
		String pathName = doc.getString("pathName");
		Document fileDescriptor = (Document)doc.get("fileDescriptor");
		
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
				if(fileSystemManager.createFileLoader(pathName, 
						fileDescriptor.getString("md5"),fileDescriptor.getLong("filesize"),
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
