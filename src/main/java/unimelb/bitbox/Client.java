package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

public class Client {

	public static void main(String[] args) throws Exception {
		
		//Object that will store the parsed command line arguments
		CmdLineArgs argsCommand = new CmdLineArgs();
		
		//Parser provided by args4j
		CmdLineParser parser = new CmdLineParser(argsCommand);
		try {
			
			//Parse the arguments
			parser.parseArgument(args);
			
			//After parsing, the fields in argsBean have been updated with the given
			//command line arguments
			System.out.println("Command Name: " + argsCommand.getCommandName());
			System.out.println("Server Host Port: " + argsCommand.getServerHostPort());
			System.out.println("Peer Host Port: " + argsCommand.getPeerHostPort());
			
			//what if the input argument is invalid, e.g., it should be ip:port, actually it is a bunch of characters
			HostPort serverHostPort = new HostPort(argsCommand.getServerHostPort());
			
			try (Socket socket = new Socket(serverHostPort.host, serverHostPort.port);){
				
				BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
				BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

			    
		    	//ask to be authorized
				String authRequest = ClientServerMessages.genAuthRequest();
				System.out.print("1.authRequest sent: " + authRequest + "\n");
		        output.write(authRequest + "\n");
		    	output.flush();
		    	
		    	//read auth_response from server and get secret key
		    	String response = input.readLine();
		    	System.out.println("2.authResponse received: " + response + "\n");
		    	Document authResponse = Document.parse(response);
		    	
		    	if(authResponse.getBoolean("status") == true) {
		    		
		    		String message = authResponse.getString("AES128");
		    		byte[] secretKey = DecryptSecretKey(message);
		    		
		    		//Execute the specified command
		    		ParseCommand(output, argsCommand, secretKey);
		    		
		    		//As long as receiving a message,close the socket???
		    		String commandResponse = input.readLine();
		    		if(commandResponse != null && !commandResponse.isEmpty()) {
		    			
		    			socket.close();
		    			
		    		}
		    	}	 	
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			//Print the usage to help the user understand the arguments expected
			//by the program
			parser.printUsage(System.err);
		}
	}
		
	private static void ParseCommand(BufferedWriter output, CmdLineArgs argsCommand, byte[] secretKey) throws IOException {
		
		switch(argsCommand.getCommandName()){
		
			case "list_peers":	
				String listPeersRequest = ClientServerMessages.genListPeersRequest();			
				String encryptListPeerRequest = Encrypt(secretKey, listPeersRequest);			
				String payloadListPeerRequest = ClientServerMessages.genPayload(encryptListPeerRequest);
				output.write(payloadListPeerRequest + "\n");
		    	output.flush();
							
				break;
				
			case "connect_peer":
				HostPort connectPeerHostPort = new HostPort(argsCommand.getPeerHostPort());
				String connectPeerRequest = ClientServerMessages.genConnectPeerRequest(connectPeerHostPort);			
				String encryptConnectPeerRequest = Encrypt(secretKey, connectPeerRequest);			
				String payloadConnectPeerRequest = ClientServerMessages.genPayload(encryptConnectPeerRequest);
				System.out.println("3. connect request:" + payloadConnectPeerRequest + "\n");
				output.write(payloadConnectPeerRequest + "\n");
		    	output.flush();

				break;
				
			case "disconnect_peer":
				HostPort disconPeerHostPort = new HostPort(argsCommand.getPeerHostPort());
				String disconnectPeerRequest = ClientServerMessages.genDisconnectPeerRequest(disconPeerHostPort);			
				String encryptdisconnectPeerRequest = Encrypt(secretKey, disconnectPeerRequest);			
				String payloaddisconnectPeerRequest = ClientServerMessages.genPayload(encryptdisconnectPeerRequest);				
				output.write(payloaddisconnectPeerRequest + "\n");
		    	output.flush();
				
				break;
				
			default:
				System.out.println("Invalid command!");
				break;
				
		}
	}
	
	 private static byte[] DecryptSecretKey(String message)
	            throws Exception {
		 
		 	byte[] secretKey = Base64.getDecoder().decode(message);	 	
		 	byte[] privateKey = getPrivateKey("bitboxclient_rsa.der");
	        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKey));
	        Cipher cipher = Cipher.getInstance("RSA");
	        cipher.init(Cipher.DECRYPT_MODE, key);
	        byte[] decryptedBytes = cipher.doFinal(secretKey);

	        return decryptedBytes;
	    }
	 
	 
	 private static byte[] getPrivateKey(String filename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException { 
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);
		DataInputStream dis = new DataInputStream(fis);
		byte[] keyBytes = new byte[(int) file.length()];
		dis.readFully(keyBytes);
		dis.close();
		 		 
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		
		return kf.generatePrivate(keySpec).getEncoded();		 			 
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
			e.printStackTrace();
			return null;
		} 
		 	
	 }
	 
}
