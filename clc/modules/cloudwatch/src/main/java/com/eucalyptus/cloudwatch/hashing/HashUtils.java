package com.eucalyptus.cloudwatch.hashing;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import com.eucalyptus.crypto.Digest;

public class HashUtils {

  public static String hash(String input) {
    if (input == null) throw new IllegalArgumentException("input is null");
    byte[] inputBytes;
    try {
      // using .getBytes() with no argument is platform dependent.  
      // Using a known encoding instead
      inputBytes = input.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      throw new UnsupportedOperationException("UTF-8 is not installed");
    } 
    MessageDigest md = Digest.SHA1.get();
    return toHexString(md.digest(inputBytes));
  }
  
  private static String toHexString(byte[] b) {
    StringWriter s = new StringWriter();
    PrintWriter out = new PrintWriter(s);
    for (int i=0;i<b.length; i++) {
      out.printf("%02x", b[i]);
    }
    out.flush();
    return s.toString();
  }
  
}