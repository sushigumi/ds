package unimelb.bitbox;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Base64;

public class Client {

 public static void main(String[] args) throws Exception {
  
  //Object that will store the parsed command line arguments
  CmdLineArgs argsCommand = new CmdLineArgs();
  
  //Parser provided by args4j
  CmdLineParser parser = new CmdLineParser(argsCommand);
  try {
   
   //Parse the arguments
   parser.parseArgument(args);
   
   //what if the input argument is invalid, e.g., it should be ip:port, actually it is a bunch of characters
   HostPort serverHostPort = new HostPort(argsCommand.getServerHostPort());
   
   try (Socket socket = new Socket(serverHostPort.host, serverHostPort.port);){

    
    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
    BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

       
       //ask to be authorized
    String authRequest = ClientServerMessages.genAuthRequest(argsCommand.getIdentity());
    System.out.print("1.authRequest sent: " + authRequest + "\n");
          output.write(authRequest + "\n");
       output.flush();
       
       //read auth_response from server and get secret key
       String response = input.readLine();
       System.out.println("2.authResponse received: " + response + "\n");
       Document authResponse = Document.parse(response);
       
       if(authResponse.getBoolean("status")) {
        
        String message = authResponse.getString("AES128");
        byte[] secretKey = DecryptSecretKey(message);
        
        //Execute the specified command
        ParseCommand(output, argsCommand, secretKey);
        
        //As long as receiving a message,close the socket
        String commandResponse = input.readLine();
     System.out.println("3. commandResponse Received: " + commandResponse);
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
    String encryptListPeerRequest = encryption(secretKey, listPeersRequest);
    String payloadListPeerRequest = ClientServerMessages.genPayload(encryptListPeerRequest);
    output.write(payloadListPeerRequest + "\n");
    System.out.println("3.send list peer request: " + payloadListPeerRequest);
       output.flush();
       
    break;
    
   case "connect_peer":
    HostPort connectPeerHostPort = new HostPort(argsCommand.getPeerHostPort());
    String connectPeerRequest = ClientServerMessages.genConnectPeerRequest(connectPeerHostPort);   
    String encryptConnectPeerRequest = encryption(secretKey, connectPeerRequest);
    String payloadConnectPeerRequest = ClientServerMessages.genPayload(encryptConnectPeerRequest);
    System.out.println("3. connect request:" + payloadConnectPeerRequest + "\n");
    output.write(payloadConnectPeerRequest + "\n");
       output.flush();

    break;
    
   case "disconnect_peer":
    HostPort disconPeerHostPort = new HostPort(argsCommand.getPeerHostPort());
    String disconnectPeerRequest = ClientServerMessages.genDisconnectPeerRequest(disconPeerHostPort);   
    String encryptdisconnectPeerRequest = encryption(secretKey, disconnectPeerRequest);
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
    byte[] privateKey = getPrivateKey("bitboxclient_rsa");
    if (privateKey == null) {
     System.out.println("error getting private key");
     System.exit(2);
   }
         PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKey));
         Cipher cipher = Cipher.getInstance("RSA");
         cipher.init(Cipher.DECRYPT_MODE, key);
         byte[] decryptedBytes = cipher.doFinal(secretKey);

         return decryptedBytes;
     }
  
  
  private static byte[] getPrivateKey(String filename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException { 
//  File file = new File(filename);
//  FileInputStream fis = new FileInputStream(file);
//  DataInputStream dis = new DataInputStream(fis);
//  byte[] keyBytes = new byte[(int) file.length()];
//  dis.readFully(keyBytes);
//  dis.close();
//
//  PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
//  KeyFactory kf = KeyFactory.getInstance("RSA");
//
//  return kf.generatePrivate(keySpec).getEncoded();
   Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

   File privateKeyFile = new File(filename); // private key file in PEM format
   PEMParser pemParser = new PEMParser(new FileReader(privateKeyFile));
   Object object = pemParser.readObject();
   PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build("".toCharArray());
   JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
   KeyPair kp;
   if (object instanceof PEMEncryptedKeyPair) {
    System.out.println("Encrypted key - we will use provided password");
    kp = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
   } else {
    System.out.println("Unencrypted key - no password needed");
    kp = converter.getKeyPair((PEMKeyPair) object);
   }

   return kp.getPrivate().getEncoded();
  }


 public static String encryption(byte[] keyBytes, String plainText) {

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


}