/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.google.common.base.Charsets;

public class X509CertHelper {
  
  public static String fromCertificate( X509Certificate x509 ) {
    return B64.url.encString( PEMFiles.getBytes( x509 ) );
  }
  
  public static X509Certificate toCertificate( String x509PemString ) {
    return PEMFiles.getCert( B64.url.dec( x509PemString ) );
  }
  
  public static String certificateToPem( X509Certificate x509 ) {
    try {
      return new String( PEMFiles.getBytes( x509 ), "UTF-8" );
    } catch ( UnsupportedEncodingException e ) {
      return new String( PEMFiles.getBytes( x509 ) );
    }
  }
  
  public static X509Certificate pemToCertificate( String pem ) {
    return PEMFiles.getCert( pem.getBytes( Charsets.UTF_8 ) );
  }
  
  public static String privateKeyToPem( PrivateKey pk ) {
    return new String( PEMFiles.getBytes(
        "RSA PRIVATE KEY",
        Crypto.getCertificateProvider().getEncoded( pk )
    ), Charsets.UTF_8 );
  }
  
  public static String calcFingerprint(X509Certificate cert) {
      try {
      	MessageDigest md = MessageDigest.getInstance("MD5");
      	byte[] der = cert.getEncoded();        
      	md.update(der);
      	byte[] digest = md.digest();
      	return hexify(digest);
      } catch(Exception e) {
      	return null;
      }
  }

  public static String hexify (byte bytes[]) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);      
    for (byte b : bytes) {
    	builder.append(Integer.toHexString((b & 0xf0) >> 4));
    	builder.append(Integer.toHexString(b & 0x0f));
    }

    return builder.toString();
  }
  
}
