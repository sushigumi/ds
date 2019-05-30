package unimelb.bitbox;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


public class GenSecretKey {
    /*
    public static void main(String args[]) {
        String originalString = "howtodoinjava.com";
        //GenSecretKey secretKey = new GenSecretKey();
        //byte[] key = secretKey.genkey();
        byte[] key = genkey();
        byte[] cipherMessage = encryption(originalString, key);
        decryption(cipherMessage, key);
    }
    */


    public static byte[] genkey() {
        KeyGenerator keyGenerator;

        try {
            keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            SecretKey secretKey = keyGenerator.generateKey();
            byte[] keyBytes = secretKey.getEncoded();
            //
            String encodedKey = Base64.getEncoder().encodeToString(keyBytes);
            System.out.println("key");
            System.out.println(encodedKey);
            return keyBytes;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

            //SecretKey key = new SecretKeySpec(keyBytes, "AES");

        return null;
    }


    public static String encryptSecretKey(byte[] publicKey, byte[] secretKey){
        PublicKey key = null;
        try {
            key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        byte[] encryptedBytes = new byte[0];
        try {
            encryptedBytes = cipher.doFinal(secretKey);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static byte[] encryption(String plainText, byte[] keyBytes) {

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

        //
        System.out.println("Original data:");
        System.out.println(plainText);
        System.out.println("Encrypted data:");
        System.out.println(Base64.getEncoder().encodeToString(cipherText));

        ByteBuffer byteBuffer = ByteBuffer.allocate(1 + iv.length + cipherText.length);
        byteBuffer.put((byte) iv.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);
        return byteBuffer.array();
    }



    public static byte[] decryption(byte[] cipherMessage, byte[] keyBytes) {

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
            //
            System.out.println("Decrypted data:");
            //System.out.println(Base64.getEncoder().encodeToString(plainText));
            System.out.println(new String(plainText));
            return plainText;
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            System.out.println("Error while decrypting: " + e.toString());
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
        }
}





