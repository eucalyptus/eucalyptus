/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
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
