package edu.ucsb.eucalyptus.admin.server.extensions.store;

import java.io.UnsupportedEncodingException;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;


public class SignatureGenerator {

    private final TreeMap<String,List<String>> parameters = new TreeMap<String,List<String>>();
    private final String method;
    private final String host;
    private final String path;

    private final String ALGORITHM = "HMacSHA256";

    public SignatureGenerator(String method, String host, int port, String path) {
        this.method = method.toUpperCase();
        if (port == 80) {
            this.host = host.toLowerCase();
        } else {
            this.host = host.toLowerCase() + ":" + port;
        }
        this.path = path;
    }

    public void addParameter(String name, String value) {
        List<String> values = parameters.get(name);
        if (values == null) {
            values = new ArrayList<String>();
            parameters.put(name, values);
        }
        values.add(value);
    }

    public String getSignature(String secretKey) {
        Mac mac;
        try {
            mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(), ALGORITHM));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        mac.update(method.getBytes());
        mac.update((byte)'\n');
        mac.update(host.getBytes());
        mac.update((byte)'\n');
        mac.update(path.getBytes());
        mac.update((byte)'\n');

        boolean addAmpersand = false;
        for (Map.Entry<String,List<String>> entry : parameters.entrySet()) {
            byte[] nameBytes = encodeString(entry.getKey());
            List<String> values = entry.getValue();
            Collections.sort(values);
            for (String value : values) {
                if (addAmpersand) {
                    mac.update((byte)'&');
                } else {
                    addAmpersand = true;
                }
                byte[] valueBytes = encodeString(value);
                mac.update(nameBytes);
                mac.update((byte)'=');
                mac.update(valueBytes);
            }
        }

        byte[] digest = mac.doFinal();
        return new String(Base64.encodeBase64(digest));
    }

    private final static String RFC3986_UNRESERVED =
                                           "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                           "abcdefghijklmnopqrstuvwxyz" +
                                           "01234567890-_.~";
    private final static char[] HEX_MAP = {'0','1','2','3','4','5','6','7',
                                           '8','9','A','B','C','D','E','F'};

    private byte[] encodeString(String value) {
        // Will be at most six times as large (U => %AB%CD,
        // where U is a unicode character).
        byte[] valueBytes;
        try {
            valueBytes = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        byte[] result = new byte[valueBytes.length * 6];
        int i = 0;
        for (byte c : valueBytes) {
            if (RFC3986_UNRESERVED.indexOf(c) != -1) {
                result[i++] = c;
            } else {
                result[i++] = (byte)'%';
                result[i++] = (byte)HEX_MAP[(c & 0xf0) >>> 4];
                result[i++] = (byte)HEX_MAP[(c & 0x0f)];
            }
        }
        byte[] trimmedResult = new byte[i];
        System.arraycopy(result, 0, trimmedResult, 0, i);
        return trimmedResult;
    }

}
