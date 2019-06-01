package unimelb.bitbox;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public class KeyTransformation {


    public static RSAPublicKeySpec decodeOpenSSH(byte[] input) {
        String[] fields = new String(input, StandardCharsets.US_ASCII).split(" ");
        if ((fields.length < 2) || (!fields[0].equals("ssh-rsa"))) throw new IllegalArgumentException("Unsupported type");
        byte[] std = Base64.getDecoder().decode(fields[1]);
        return decodeRSAPublicSSH(std);
    }

    static RSAPublicKeySpec decodeRSAPublicSSH(byte[] encoded) {
        ByteBuffer input = ByteBuffer.wrap(encoded);
        String type = string(input);
        if (!"ssh-rsa".equals(type)) throw new IllegalArgumentException("Unsupported type");
        BigInteger exp = sshint(input);
        BigInteger mod = sshint(input);
        if (input.hasRemaining()) throw new IllegalArgumentException("Excess data");
        return new RSAPublicKeySpec(mod, exp);
    }


    private static String string(ByteBuffer buf) {
        return new String(lenval(buf), Charset.forName("US-ASCII"));
    }

    private static BigInteger sshint(ByteBuffer buf) {
        return new BigInteger(+1, lenval(buf));
    }

    private static byte[] lenval(ByteBuffer buf) {
        byte[] copy = new byte[buf.getInt()];
        buf.get(copy);
        return copy;
    }



}