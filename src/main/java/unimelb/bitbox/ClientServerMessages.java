package unimelb.bitbox;

import unimelb.bitbox.peer.Connection;
import unimelb.bitbox.peer.TCPPeerManager;
import unimelb.bitbox.peer.UDPPeerManager;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.util.ArrayList;
import java.util.Arrays;

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
        //identity should be variable
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
	public static String genListPeersResponse(ArrayList<HostPort> hostPorts) {
		Document doc = new Document();
		doc.append("command", LIST_PEERS_RESPONSE);
		doc.append("peers",hostPorts);
        return doc.toJson();
    }
	
	public static String genConnectPeerRequest(HostPort hostPort) {
		Document doc = new Document();
		doc.append("command", CONNECT_PEER_REQUEST);
		doc.append("host", hostPort.host);
		doc.append("port", hostPort.port);
        return doc.toJson();
    }
	
	public static String genConnectPeerResponse(String host, int port, String mode) {
		Document doc = new Document();
		doc.append("command", CONNECT_PEER_RESPONSE);
		doc.append("host", host);
		doc.append("port", port);
		HostPort hostPort = new HostPort(host,port);

		/*
		if(mode.equals("udp")){
			//TODO: if connection already exists, true or false
			if(Arrays.asList(UDPPeerManager.getInstance().getConnectedPeers()).contains(hostPort)){
				doc.append("status", false);
				doc.append("message", "connection failed");
			}
			//TODO: how to check the connection succeeds or fails
/*
			if()
			else if(){
				//outgoing connection?
				//UDPPeerManager.getInstance().addPeer(serverSocket,);
				doc.append("status", true);
				doc.append("message", "connected to peer");
			}
			*/
		//}
		/*
		else if(mode.equals("tcp")){
			//TODO: if connection already exists, true or false
			if(Arrays.asList(TCPPeerManager.getInstance().getPeersHostPorts()).contains(hostPort)){
				doc.append("status", false);
				doc.append("message", "connection failed");
			}
			//TODO
			/*
			else if(){
				TCPPeerManager.getInstance().connect(fileSystemManager, hostPort);
				doc.append("status", true);
				doc.append("message", "connected to peer");
			}
			*/
	//	}

		
        return doc.toJson();
    }
	
	public static String genDisconnectPeerRequest(HostPort hostPort) {
		Document doc = new Document();
		doc.append("command", DISCONNECT_PEER_REQUEST);
		doc.append("host", hostPort.host);
		doc.append("port", hostPort.port);
        return doc.toJson();
    }


	public static String genDisconnectPeerResponse(String host, int port, String mode) {
		Document doc = new Document();
		doc.append("command", DISCONNECT_PEER_RESPONSE);
		doc.append("host",host);
		doc.append("port",port);
		//DO STH

		HostPort hostPort = new HostPort(host,port);

		if( mode.equals("udp") && Arrays.asList(UDPPeerManager.getInstance().getConnectedPeers()).contains(hostPort)) {
			// close connection
			UDPPeerManager.getInstance().disconnectPeer(hostPort);
			doc.append("status", true);
			doc.append("message", "disconnected from peer");
		}
		else if(mode.equals("tcp") && Arrays.asList(TCPPeerManager.getInstance().getPeersHostPorts()).contains(hostPort)){
			//TODO: closeConnection arguments
			//TCPPeerManager.getInstance().closeConnection();
			doc.append("status", true);
			doc.append("message", "disconnected from peer");
		}
		// connection not active
		else{
			doc.append("status", false);
			doc.append("message","connection not active");
		}

        return doc.toJson();
    }

}
