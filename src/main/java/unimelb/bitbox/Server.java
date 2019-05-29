package unimelb.bitbox;

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
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;

public class Server {

	public static void main(String[] args) {
		
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
	
	
	private static void serverClient(Socket client, ServerSocket server) {
		try(Socket clientSocket = client){
			BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
			byte[] secretKey = null;
			System.out.println(input.readLine());
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
	        				//maybe wrong?
	        				String[] keyElement = key.split(" ");
	        				//byte[] publicKey = keyElement[1].getBytes("UTF-8");
	        				System.out.println("Read public key: " + key);
	        				
	        				String keyTest = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAwyl1KXKUMS3+VnxL22Oq" + 
	        						"7RDrmCHzF4dZWWYeP6Np145bYLog+HkieFhDrjI5H0m0Gq8YpAjKvvsSGjrFwpUU" + 
	        						"+nukBEW/d2dgczYPL3tgR0fPiIEiJgsS2ComN1GFNHT3mNXc389CbxWnOVPSmP0Q" + 
	        						"Ze2SIloc1h+O74+rSQu1UbO3fn5XzCFYuDCmlLCjlcXUrdOxWVZo4UyN7LbO1coW" + 
	        						"qXWb/RbdrRjk4cSUR3LidiVM/RqfBORep8esB1hIG1kW5F4gnLXbt0fvSB3HsLwL" + 
	        						"PbK0byRSdG6jmgAioFgaPrM8Dhf7J9F6OeF4dIKl7X5tPq2WiSZDHBj8l0HqWtnF" + 
	        						"ZpfIcolMj2KNQDLDL+rTticX2dU831JiPt+ZNKUiji33H39mckJU3Z14QamBLmcT" + 
	        						"OHUrAgwA5/P5Ym6AXQFCEyl9+T2eBqQyKhGHEzPHv9fsPVBDAirzHICPq5QmKq/u" + 
	        						"LQN5S4Uiygi8xsIq4Ax+R1XW5MPj28PqI/NZUWGNl1mKusycZ1sa2gDacTN+TDhU" + 
	        						"wB6XY7k32g7Eg4Jvi3HrD9tSF72tcKQRRXzVqLKTTfqENBNQL3VJCETbD28sU01C" + 
	        						"1IqIWIULapzJmwB/38kTuKmotVk17vDMSMLI4W2a9IQJwPlcIwDgUtbS1x48ay0D" + 
	        						"W4cm8m1I09w2qjKzcwaaZPMCAwEAAQ==";
	        				
	        				byte[] publicKey = Base64.getDecoder().decode((keyTest.getBytes()));
	        				
	        				String message = EncryptSecretKey(publicKey, secretKey);
	            			String authResponse = ClientServerMessages.genAuthResponse(message);
	            			System.out.println("2. auth response sent: " + authResponse + "\n");
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
	            	System.out.println("3. command request received: " + requestString + "\n");
	            	Document request = Document.parse(requestString);
	            	ParseRequest(output, request, secretKey, server);
	            }	
            }
		}catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	
	private static void ParseRequest(BufferedWriter output, Document request, byte[] secretKey, ServerSocket server) throws IOException {
		
		switch(request.getString("command")){
		
			case ClientServerMessages.LIST_PEERS_REQUEST:
		    	//do sth;//There should be arguments here.	
				String listPeersResponse = ClientServerMessages.genListPeersResponse();	
				System.out.println("4.List peers response (raw): " + listPeersResponse + "\n");
				String encryptListPeersResponse = Encrypt(secretKey, listPeersResponse);	
				System.out.println("5.List peers response (encrypted): " + encryptListPeersResponse);
				String payloadListPeersResponse = ClientServerMessages.genPayload(encryptListPeersResponse);
				System.out.println("6.List peers response (payload): " + payloadListPeersResponse);
				output.write(payloadListPeersResponse + "\n");
		    	output.flush();
				break;
				
			case ClientServerMessages.CONNECT_PEER_REQUEST:
				String connectPeerResponse = ClientServerMessages.genConnectPeerResponse();
				System.out.println("4. connect peer response (raw): " + connectPeerResponse + "\n");
				String encryptConnectPeerResponse = Encrypt(secretKey, connectPeerResponse);
				System.out.println("5.connect peer response (encrypted): " + encryptConnectPeerResponse);
				String payloadConnectPeerResponse = ClientServerMessages.genPayload(encryptConnectPeerResponse);
				System.out.println("6.connect peer response (payload): " + payloadConnectPeerResponse);
				output.write(payloadConnectPeerResponse + "\n");
		    	output.flush();
				break;
				
			case ClientServerMessages.DISCONNECT_PEER_REQUEST:
				String disconnectPeerResponse = ClientServerMessages.genDisconnectPeerResponse();
				System.out.println("4.Disconnect peer response (raw): " + disconnectPeerResponse + "\n");
				String encryptDisconnectPeersResponse = Encrypt(secretKey, disconnectPeerResponse);
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
		server.close();
	}
	
	
	private static byte[] generateSecretKey() {
		/*try {
			  // Generate a 128bit key
			    final int outputKeyLength = 128;

			    //SecureRandom secureRandom = new SecureRandom();
			    SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			    // Do *not* seed secureRandom! Automatically seeded from system entropy.
			    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			    keyGen.init(outputKeyLength, secureRandom);
			    SecretKey secretKey = keyGen.generateKey();
			    System.out.println(secretKey);
			    return secretKey.getEncoded();
			    
		 } catch (NoSuchAlgorithmException e) {
			   e.printStackTrace();
			   return null;
		}
		KeyGenerator keyGenerator;*/
		
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
	
	
	 private static String EncryptSecretKey(byte[] publicKey, byte[] secretKey)
	            throws Exception {
	        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
	        Cipher cipher = Cipher.getInstance("RSA");
	        cipher.init(Cipher.ENCRYPT_MODE, key);
	        byte[] encryptedBytes = cipher.doFinal(secretKey);
	        return Base64.getEncoder().encodeToString(encryptedBytes);
	    }
	 
	 
	 private static String Decrypt(byte[] secretKey, String payload) {		 
		 byte[] message = Base64.getDecoder().decode(payload);
		 Key aesKey = new SecretKeySpec(secretKey, "AES");
			
		 try {
			 	Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
				//Cipher cipher = Cipher.getInstance("AES");			
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
			Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
			//Cipher cipher = Cipher.getInstance("AES");			
			cipher.init(Cipher.ENCRYPT_MODE, aesKey);			
			byte[] encrypted = cipher.doFinal(message.getBytes("UTF-8"));//LOWERCASE?"utf-8"
			return Base64.getEncoder().encodeToString(encrypted);	
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return null;
		} 
		 	
	 }

}


