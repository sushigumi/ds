package unimelb.bitbox;

import unimelb.bitbox.util.Document;

public class TestJson {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Document doc = new Document();
		
		doc.append("host", "");
		doc.append("port", 8111);//should be variable
		doc.append("status", true);
		doc.append("message","disconnected from peer");
		
		
		System.out.println("the json is: " + doc.toJson());

	}

}
