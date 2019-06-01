package unimelb.bitbox.messages;

import unimelb.bitbox.util.Document;

public class MessageValidator {
    private static MessageValidator ourInstance = new MessageValidator();

    public static MessageValidator getInstance() {
        return ourInstance;
    }

    private MessageValidator() {
    }

    public String validateFileChangeRequest(Document doc) {
    	Document fileDescriptor = (Document)doc.get("fileDescriptor");

        if (doc.get("fileDescriptor") == null) {
        	return "fileDescriptor";       	
        }
        if (fileDescriptor.getString("md5") == null) {
        	return "md5";
        }
        //not so sure
        if (fileDescriptor.get("fileSize") == null) {
        	return "fileSize";	
        }
        
        if (fileDescriptor.get("lastModified") == null) {
        	return "lastModified";	
        }
        
        if (doc.getString("pathName") == null) {
        	return "pathName";       	
        }

        //return error message
        return null;
    }

    public String validateFileChangeResponse(Document doc) {
    	
    	Document fileDescriptor = (Document)doc.get("fileDescriptor");
    	
        if (doc.get("fileDescriptor") == null) {
        	return "fileDescriptor";       	
        }
        else if (fileDescriptor.getString("md5") == null) {
        	return "md5";
        }
        //not so sure
        else if (fileDescriptor.get("fileSize") == null) {
        	return "fileSize";	
        }
        
        else if (fileDescriptor.get("lastModified") == null) {
        	return "lastModified";	
        }
        
        if (doc.getString("pathName") == null) {
        	return "pathName";       	
        }
        
        if (doc.getString("message") == null) {
        	return "message";       	
        }
        
        if (doc.get("status") == null) {
        	return "status";     	
        }
   	
		return null;
		
    }
    
    public String validateFileBytesRequest(Document doc) {
    	
        Document fileDescriptor = (Document)doc.get("fileDescriptor");
    	
        if (doc.get("fileDescriptor") == null) {
        	return "fileDescriptor";       	
        }
        else if (fileDescriptor.getString("md5") == null) {
        	return "md5";
        }
        //not so sure
        else if (fileDescriptor.get("fileSize") == null) {
        	return "fileSize";	
        }
        
        else if (fileDescriptor.get("lastModified") == null) {
        	return "lastModified";	
        }
        
        if (doc.getString("pathName") == null) {
        	return "pathName";       	
        }
    	
        if (doc.get("position") == null) {
        	return "position";	
        }
        
        if (doc.get("length") == null) {
        	return "length";	
        }
        
        return null;	
    	
    }
    
    public String validateFileBytesResponse(Document doc) {
    	
        Document fileDescriptor = (Document)doc.get("fileDescriptor");
    	
        if (doc.get("fileDescriptor") == null) {
        	return "fileDescriptor";       	
        }
        else if (fileDescriptor.getString("md5") == null) {
        	return "md5";
        }
        //not so sure
        else if (fileDescriptor.get("fileSize") == null) {
        	return "fileSize";	
        }
        
        else if (fileDescriptor.get("lastModified") == null) {
        	return "lastModified";	
        }
        
        if (doc.getString("pathName") == null) {
        	return "pathName";
        }

        if (doc.get("position") == null) {
        	return "position";
        }

        if (doc.get("length") == null) {
        	return "length";
        }

        if (doc.getString("content") == null) {
        	return "content";
        }

        if (doc.getString("message") == null) {
        	return "message";
        }

        if (doc.get("status") == null) {
        	return "status";
        }
        return null;

    }


    public String validateDirectoryChangeRequest(Document doc) {

    	 if (doc.getString("pathName") == null) {
         	return "pathName";
         }
    	 else {
    		 return null;
    	 }

    }
    public String validateDirectoryChangeResponse(Document doc) {

        if (doc.getString("pathName") == null) {
            return "pathName";
        }

        if (doc.getString("message") == null) {
            return "message";
        }

        if (doc.get("status") == null) {
            return "status";
        }
        return null;
    }
}
