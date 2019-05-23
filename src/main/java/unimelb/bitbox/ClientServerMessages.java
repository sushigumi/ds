package main.java.unimelb.bitbox;

import main.java.unimelb.bitbox.util.Document;
import main.java.unimelb.bitbox.util.HostPort;

public class ClientServerMessages {
	
	public static final String AUTH_REQUEST = "AUTH_REQUEST";
	public static final String AUTH_RESPONSE = "AUTH_RESPONSE";
	public static final String LIST_PEERS_REQUEST = "LIST_PEERS_REQUEST";
	public static final String LIST_PEERS_RESPONSE = "LIST_PEERS_RESPONSE";
	public static final String CONNECT_PEER_REQUEST = "CONNECT_PEER_REQUEST";
	public static final String CONNECT_PEER_RESPONSE = "CONNECT_PEER_RESPONSE";
	public static final String DISCONNECT_PEER_REQUEST = "DISCONNECT_PEER_REQUEST";
	public static final String DISCONNECT_PEER_RESPONSE = "DISCONNECT_PEER_RESPONSE";
	
	
	private ClientServerMessages() {
	}
	
	public static String genPayload(String payload) {	
		Document doc = new Document();
		doc.append("payload", payload);		
		return doc.toJson();	
	}
	
	
	public static String genAuthRequest() {
		Document doc = new Document();
        doc.append("command", AUTH_REQUEST);
        doc.append("identity", "yanli@Yans-MacBook-Pro.local");
        return doc.toJson();
    }
	
	public static String genAuthResponse(String message) {
		Document doc = new Document();
		doc.append("command", AUTH_RESPONSE);
		doc.append("AES128", message);
		doc.append("status", true);
		doc.append("message", "public key found");		
		return doc.toJson();
	}

	public static String genAuthResponseFalse() {
		Document doc = new Document();
		doc.append("command", AUTH_RESPONSE);
		doc.append("status", false);
		doc.append("message", "public key not found");
        return doc.toJson();
    }
	
	public static String genListPeersRequest() {
		Document doc = new Document();
		doc.append("command", LIST_PEERS_REQUEST);
        return doc.toJson();
    }
	
	//arguments?
	public static String genListPeersResponse() {
		Document doc = new Document();
		doc.append("command", LIST_PEERS_RESPONSE);
		//DO STH
        return doc.toJson();
    }
	
	public static String genConnectPeerRequest(HostPort hostPort) {
		Document doc = new Document();
		doc.append("command", CONNECT_PEER_REQUEST);
		doc.append("host", hostPort.host);
		doc.append("port", hostPort.port);//Not sure if it's gonna work
        return doc.toJson();
    }
	
	public static String genConnectPeerResponse() {
		Document doc = new Document();
		doc.append("command", CONNECT_PEER_RESPONSE);
		//DO STH
		
		doc.append("host", "");
		doc.append("port", 8111);//should be variable
		doc.append("status", true);
		doc.append("message","connected to peer");
		
		//connection failed
		
        return doc.toJson();
    }
	
	public static String genDisconnectPeerRequest(HostPort hostPort) {
		Document doc = new Document();
		doc.append("command", DISCONNECT_PEER_REQUEST);
		doc.append("host", hostPort.host);
		doc.append("port", hostPort.port);
        return doc.toJson();
    }
	
	public static String genDisconnectPeerResponse() {
		Document doc = new Document();
		doc.append("command", DISCONNECT_PEER_RESPONSE);
		//DO STH
		
		doc.append("host", "");
		doc.append("port", 8111);//should be variable
		doc.append("status", true);
		doc.append("message","disconnected from peer");
		
		//connection not active
		
        return doc.toJson();
    }

}
