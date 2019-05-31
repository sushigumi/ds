package unimelb.bitbox;

import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import unimelb.bitbox.peer.Connection;
import unimelb.bitbox.peer.TCPPeerManager;
import unimelb.bitbox.peer.UDPPeer;
import unimelb.bitbox.peer.UDPPeerManager;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

public class Server {
	private DatagramSocket datagramSocket = null;
	private FileSystemManager fileSystemManager;

	public Server(DatagramSocket datagramSocket, FileSystemManager fileSystemManager) {
		this.fileSystemManager = fileSystemManager;
		this.datagramSocket = datagramSocket;

		startServer();
	}


	public Server(FileSystemManager fileSystemManager){
		this.fileSystemManager = fileSystemManager;

		startServer();
	}

	private void startServer() {
		int portNumber = Integer.parseInt(Configuration.getConfigurationValue("clientPort"));
		try {
			ServerSocket server = new ServerSocket(portNumber);
			while(true) {
				Socket client = server.accept();

				//Start a new thread for a connection
				Thread t = new Thread(() -> serverClient(client, server));
				t.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void serverClient(Socket client, ServerSocket server) {
		try(Socket clientSocket = client){
			BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
			byte[] secretKey = null;

			//Read the message from the client and reply
			String clientMsg = null;
			while((clientMsg = input.readLine()) != null) {
				System.out.println("1. AuthRequest received:" + clientMsg);
				Document doc = Document.parse(clientMsg);					
				
	            if(doc.containsKey("command") && doc.getString("command").equals(ClientServerMessages.AUTH_REQUEST)) {	
	        		String identity = doc.getString("identity");
	        		System.out.println("identity: " + identity);
	        		String[] keys = Configuration.getConfigurationValue("authorized_keys").split(",");
	        		boolean ifFound = false;
	        		
	        		for(String key:keys) {
	        			if(key.contains(identity)){
	        				ifFound = true;
	        				secretKey = generateSecretKey();

	        				byte[] publicKeyBytes = key.getBytes();
	        				RSAPublicKeySpec publicKey =  KeyTransformation.decodeOpenSSH(publicKeyBytes);
	        				String message = EncryptSecretKey(publicKey, secretKey);
	        				
	            			String authResponse = ClientServerMessages.genAuthResponse(message);
	            			System.out.println("2. auth response sent: " + authResponse + "\n");
	        		        output.write(authResponse + "\n");
	        		    	output.flush();
	        		    	break;
	        				
	        			}
	        		}
	 
	        		if(ifFound == false) {
	        			String falseResponse = ClientServerMessages.genAuthResponseFalse();
	    		        output.write(falseResponse + "\n");
	    		    	output.flush();
	        		}
	        		
	            }	
	            if (doc.containsKey("payload")) {
	            	
	            	String payload = doc.getString("payload");
					String requestString = decryption(secretKey,payload);
	            	System.out.println("3. command request received: " + requestString + "\n");
	            	Document request = Document.parse(requestString);
	            	ParseRequest(output, request, secretKey, server);

	            }	
            }
		}catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	
	private void ParseRequest(BufferedWriter output, Document request, byte[] secretKey, ServerSocket server){
		try {
			switch(request.getString("command")){
			
				case ClientServerMessages.LIST_PEERS_REQUEST:
			    	//do sth;//There should be arguments here.
					ArrayList<HostPort> hostPorts;
					if( Configuration.getConfigurationValue("mode").equals("tcp")){
						hostPorts =  TCPPeerManager.getInstance().getPeersHostPorts();
					} else{
						hostPorts = UDPPeerManager.getInstance().getConnectedPeers();
					}
					String listPeersResponse = ClientServerMessages.genListPeersResponse(hostPorts);
					System.out.println("4.List peers response (raw): " + listPeersResponse + "\n");
					String encryptListPeersResponse =encryption(secretKey, listPeersResponse);
					System.out.println("5.List peers response (encrypted): " + encryptListPeersResponse);
					String payloadListPeersResponse = ClientServerMessages.genPayload(encryptListPeersResponse);
					System.out.println("6.List peers response (payload): " + payloadListPeersResponse);
					output.write(payloadListPeersResponse + "\n");
			    	output.flush();
					break;
					
				case ClientServerMessages.CONNECT_PEER_REQUEST:
					int port = (int)request.getLong("port");
					String hostname = request.getString("host");

					HostPort remoteHostPort = new HostPort(hostname, port);

					String connectPeerResponse;
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						 System.out.println(e.getMessage());
					}
					while (true) {
						if (ServerMain.getMode().equals(ServerMain.MODE_UDP)) {
							UDPPeerManager.getInstance().addPeer(datagramSocket,remoteHostPort.toString(),fileSystemManager);
							UDPPeer.STATE state = UDPPeerManager.getInstance().getStateByAdvertisedHostPort(remoteHostPort);

							if (state == UDPPeer.STATE.OK) {
								connectPeerResponse = ClientServerMessages.genConnectPeerResponseSuccess(hostname, port);
								break;
							}
							else if (state == null) {
								connectPeerResponse = ClientServerMessages.genConnectPeerResponseFail(hostname, port);
								break;
							}
						}
						else if (ServerMain.getMode().equals(ServerMain.MODE_TCP)) {
							TCPPeerManager.getInstance().connect(fileSystemManager,remoteHostPort.toString());
							Connection.STATE state = TCPPeerManager.getInstance().getPeerState(remoteHostPort);
							if (state == Connection.STATE.OK) {
								connectPeerResponse = ClientServerMessages.genConnectPeerResponseSuccess(hostname, port);
								break;
							}
							else if (state == null) {
								connectPeerResponse = ClientServerMessages.genConnectPeerResponseFail(hostname, port);
								break;
							}
						}
					}
					System.out.println("4. connect peer response (raw): " + connectPeerResponse + "\n");
					String encryptConnectPeerResponse = encryption(secretKey, connectPeerResponse);
					System.out.println("5.connect peer response (encrypted): " + encryptConnectPeerResponse);
					String payloadConnectPeerResponse = ClientServerMessages.genPayload(encryptConnectPeerResponse);
					System.out.println("6.connect peer response (payload): " + payloadConnectPeerResponse);
					output.write(payloadConnectPeerResponse + "\n");
			    	output.flush();
					break;
					
				case ClientServerMessages.DISCONNECT_PEER_REQUEST:
					port = (int) request.getLong("port");
					String disconnectPeerResponse = ClientServerMessages.genDisconnectPeerResponse(request.getString("host"),
							port, datagramSocket);
					System.out.println("4.Disconnect peer response (raw): " + disconnectPeerResponse + "\n");
					String encryptDisconnectPeersResponse = encryption(secretKey, disconnectPeerResponse);
					System.out.println("5.Disconnect peer response (encrypted): " + encryptDisconnectPeersResponse);
					String payloadDisconnectPeerResponse = ClientServerMessages.genPayload(encryptDisconnectPeersResponse);
					System.out.println("6.Disconnect peer response (payload): " + payloadDisconnectPeerResponse);
					output.write(payloadDisconnectPeerResponse + "\n");
			    	output.flush();
					break;
					
				default:
					output.write("Invalid command!");
					output.flush();
					break;
				}
			}catch(IOException e) {
				System.out.println("Socket closed");
				
			}
	}
	
	
	private byte[] generateSecretKey() {
		
		KeyGenerator keyGen = null;
		  try {
		    /* Get KeyGenerator object that generates secret keys for the
		    * specified algorithm.
		    */
			  keyGen = KeyGenerator.getInstance("AES");
			  /* Initializes this key generator for key size to 128. */
			  keyGen.init(128);
			  /* Generates a secret key */
			  SecretKey secretKey = keyGen.generateKey();
			  return secretKey.getEncoded();
			  
		 } catch (NoSuchAlgorithmException e) {
			   e.printStackTrace();
			   return null;
		}
	} 
	
	
	 private String EncryptSecretKey(RSAPublicKeySpec publicKey, byte[] secretKey)
	            throws Exception {
		 	PublicKey key = KeyFactory.getInstance("RSA").generatePublic(publicKey);
	        Cipher cipher = Cipher.getInstance("RSA");
	        cipher.init(Cipher.ENCRYPT_MODE, key);
	        byte[] encryptedBytes = cipher.doFinal(secretKey);
	        return Base64.getEncoder().encodeToString(encryptedBytes);
	    }
	 
	 
	public String encryption(byte[] keyBytes, String plainText) {

		byte[] plaintTextByteArray = new byte[0];
		try {
			plaintTextByteArray = plainText.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// generate IV
		SecureRandom secureRandom = new SecureRandom();
		byte[] iv = new byte[16]; //NEVER REUSE THIS IV WITH SAME KEY
		secureRandom.nextBytes(iv);

		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		}

		try {
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(iv));
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}

		byte[] cipherText = new byte[0];
		try {
			cipherText = cipher.doFinal(plaintTextByteArray);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}

		ByteBuffer byteBuffer = ByteBuffer.allocate(1 + iv.length + cipherText.length);
		byteBuffer.put((byte) iv.length);
		byteBuffer.put(iv);
		byteBuffer.put(cipherText);
		return Base64.getEncoder().encodeToString(byteBuffer.array());
	}

	public String decryption(byte[] keyBytes, String ciphermessage) {

		byte[] cipherMessage = Base64.getDecoder().decode(ciphermessage);
		try {
			ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
			int ivLength = (byteBuffer.get() & 0xFF);
			byte[] iv = new byte[ivLength];
			byteBuffer.get(iv);

			byte[] cipherText = new byte[byteBuffer.remaining()];
			byteBuffer.get(cipherText);

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(iv));
			byte[] plainText = cipher.doFinal(cipherText);
			//System.out.println(Base64.getEncoder().encodeToString(plainText));
			System.out.println(new String(plainText));
			return new String(plainText);
		} catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
			System.out.println("Error while decrypting: " + e.toString());
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return null;
	}
}


