package unimelb.bitbox.client;

import unimelb.bitbox.peer.TCPPeerManager;
import unimelb.bitbox.peer.UDPPeerManager;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.net.DatagramSocket;
import java.util.ArrayList;

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
	
	
	public static String genAuthRequest(String identity) {
		Document doc = new Document();
        doc.append("command", AUTH_REQUEST);
        //identity should be variable
        doc.append("identity", identity);
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

    public static String genConnectPeerResponseFail(String host, int port) {
		Document doc = new Document();
		doc.append("command", CONNECT_PEER_RESPONSE);
		doc.append("host", host);
		doc.append("port", port);
		doc.append("status", false);
		doc.append("message", "connection failed");

		return doc.toJson();
	}

	public static String genConnectPeerResponseSuccess(String host, int port) {
		Document doc = new Document();
		doc.append("command", CONNECT_PEER_RESPONSE);
		doc.append("host", host);
		doc.append("port", port);
		doc.append("status", true);
		doc.append("message", "connection success");

		return doc.toJson();
	}


	public static String genDisconnectPeerRequest(HostPort hostPort) {
		Document doc = new Document();
		doc.append("command", DISCONNECT_PEER_REQUEST);
		doc.append("host", hostPort.host);
		doc.append("port", hostPort.port);
        return doc.toJson();
    }


	public static String genDisconnectPeerResponse(String host, int port,DatagramSocket socket) {
		Document doc = new Document();
		doc.append("command", DISCONNECT_PEER_RESPONSE);
		doc.append("host",host);
		doc.append("port",port);
		HostPort hostPort = new HostPort(host,port);
		System.out.println(socket + ","+ UDPPeerManager.getInstance().getConnectedPeers().contains(hostPort));
		if( socket!=null && UDPPeerManager.getInstance().getConnectedPeers().contains(hostPort)) {
			// close connection
			UDPPeerManager.getInstance().disconnectPeer(hostPort);
			doc.append("status", true);
			doc.append("message", "disconnected from peer");
		}
		else if(socket == null && TCPPeerManager.getInstance().getPeersHostPorts().contains(hostPort)){
			TCPPeerManager.getInstance().closeConnection(hostPort,false);
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
