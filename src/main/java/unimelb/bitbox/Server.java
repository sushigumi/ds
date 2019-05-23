package main.java.unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import main.java.unimelb.bitbox.util.Document;
import main.java.unimelb.bitbox.util.Configuration;

public class Server {

	public static void main(String[] args) {
		
		int portNumber = Integer.parseInt(Configuration.getConfigurationValue("clientPort"));
		byte[] secretKey = null;
		try {
			ServerSocket serverSocket = new ServerSocket(portNumber);
			while(true) {
				Socket clientSocket = serverSocket.accept();
				
				BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
				BufferedWriter output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
				
				//Read the message from the client and reply
				String clientMsg = null;
				while((clientMsg = input.readLine()) != null) {
					Document doc = Document.parse(clientMsg);					
					
		            if(doc.containsKey("command") && doc.getString("command").equals(ClientServerMessages.AUTH_REQUEST)) {	
	            		String identity = doc.getString("identity");
	            		String[] keys = Configuration.getConfigurationValue("authorized_keys").split(",");
	            		boolean ifFound = false;
	            		
	            		for(String key:keys) {
	            			if(key.contains(identity)){
	            				ifFound = true;
	            				secretKey = generateSecretKey();
	            				//maybe wrong?
	            				byte[] publicKey = key.getBytes("UTF-8");
	            				String message = EncryptSecretKey(publicKey, secretKey);
	                			String authResponse = ClientServerMessages.genAuthResponse(message);
	            		        output.write(authResponse + "\n");
	            		    	output.flush();
	            				
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
		            	String requestString = Decrypt(secretKey, payload);
		            	Document request = Document.parse(requestString);
		            	ParseRequest(output, request, secretKey);
		            	
		            }
				}

			}	
			
		} catch (IOException e) {	
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}	

	}
	
	private static void ParseRequest(BufferedWriter output, Document request, byte[] secretKey) throws IOException {
		
		switch(request.getString("command")){
		
			case ClientServerMessages.LIST_PEERS_REQUEST:
		    	//do sth;//There should be arguments here.	
				String listPeersResponse = ClientServerMessages.genListPeersResponse();	
				String encryptListPeersResponse = Encrypt(secretKey, listPeersResponse);			
				String payloadListPeersResponse = ClientServerMessages.genPayload(encryptListPeersResponse);									
				output.write(payloadListPeersResponse + "\n");
		    	output.flush();		
				break;
				
			case ClientServerMessages.CONNECT_PEER_REQUEST:
				String connectPeerResponse = ClientServerMessages.genListPeersResponse();	
				String encryptConnectPeersResponse = Encrypt(secretKey, connectPeerResponse);			
				String payloadConnectPeerResponse = ClientServerMessages.genPayload(encryptConnectPeersResponse);							
				output.write(payloadConnectPeerResponse + "\n");
		    	output.flush();		
				break;
				
			case ClientServerMessages.DISCONNECT_PEER_REQUEST:
				String disconnectPeerResponse = ClientServerMessages.genListPeersResponse();	
				String encryptdisconnectPeersResponse = Encrypt(secretKey, disconnectPeerResponse);			
				String payloaddisconnectPeerResponse = ClientServerMessages.genPayload(encryptdisconnectPeersResponse);									
				output.write(payloaddisconnectPeerResponse + "\n");
		    	output.flush();		
				break;
				
			default:
				//wrong message?
				break;
			}
	}
	
	private static byte[] generateSecretKey() {
		  KeyGenerator keyGen = null;
		  try {
		    /* Get KeyGenerator object that generates secret keys for the
		    * specified algorithm.
		    */
		   keyGen = KeyGenerator.getInstance("AES");
		  } catch (NoSuchAlgorithmException e) {
		   e.printStackTrace();
		  }
		  /* Initializes this key generator for key size to 128. */
		  keyGen.init(128);
		  /* Generates a secret key */
		  SecretKey secretKey = keyGen.generateKey();
		  return secretKey.getEncoded();
		 }
	
	
	 private static String EncryptSecretKey(byte[] publicKey, byte[] secretKey)
	            throws Exception {
	        PublicKey key = KeyFactory.getInstance("RSA")
	        		.generatePublic(new X509EncodedKeySpec(publicKey));
	        Cipher cipher = Cipher.getInstance("RSA");
	        cipher.init(Cipher.ENCRYPT_MODE, key);
	        byte[] encryptedBytes = cipher.doFinal(secretKey);
	        return Base64.getEncoder().encodeToString(encryptedBytes);
	    }
	 
	 private static String Decrypt(byte[] secretKey, String payload) {		 
		 byte[] message = Base64.getDecoder().decode(payload);		 
		 Key aesKey = new SecretKeySpec(secretKey, "AES");
			//Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		 try {
				Cipher cipher = Cipher.getInstance("AES");			
				cipher.init(Cipher.DECRYPT_MODE, aesKey);			
				byte[] decrypted = cipher.doFinal(message);	
				String originalMessage = new String(decrypted, "UTF-8");//LOWERCASE?"utf-8"
			    return originalMessage;
			 } catch (Exception e) {
		         System.out.println(e.toString());
		         return null;
		     }
	 }
	 
	 private static String Encrypt(byte[] secretKey, String message) {		 
		 Key aesKey = new SecretKeySpec(secretKey, "AES");
		 try {
			//Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			Cipher cipher = Cipher.getInstance("AES");			
			cipher.init(Cipher.ENCRYPT_MODE, aesKey);			
			byte[] encrypted = cipher.doFinal(message.getBytes("UTF-8"));//LOWERCASE?"utf-8"
			return Base64.getEncoder().encodeToString(encrypted);	
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return null;
		} 
		 	
	 }

}


