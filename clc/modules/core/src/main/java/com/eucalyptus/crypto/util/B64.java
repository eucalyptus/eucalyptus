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

package com.eucalyptus.crypto.util;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.UrlBase64;
import com.eucalyptus.crypto.util.B64.standard;

public class B64 implements Base64Handler {
  public static class url implements Base64Handler {
    public static String encString( byte[] data ) {
      return new String( enc( data ) );
    }
    
    public static String encString( String data ) {
      return new String( enc( data ) );
    }
    
    public static byte[] enc( String data ) {
      return UrlBase64.encode( data.getBytes( ) );
    }
    
    public static byte[] enc( byte[] data ) {
      return UrlBase64.encode( data );
    }
    
    public static String decString( byte[] data ) {
      return new String( dec( data ) );
    }
    
    public static String decString( String data ) {
      return new String( dec( data ) );
    }
    
    public static byte[] dec( String data ) {
      return UrlBase64.decode( data );
    }
    
    public static byte[] dec( byte[] data ) {
      return UrlBase64.decode( data );
    }
  }

  public static class standard implements Base64Handler {

    public static String encString( byte[] data ) {
      return new String( standard.enc( data ) );
    }

    public static String encString( String data ) {
      return new String( standard.enc( data ) );
    }

    public static byte[] enc( String data ) {
      return Base64.encode( data.getBytes( ) );
    }

    public static byte[] enc( byte[] data ) {
      return Base64.encode( data );
    }

    public static String decString( byte[] data ) {
      return new String( standard.dec( data ) );
    }

    public static String decString( String data ) {
      return new String( standard.dec( data ) );
    }

    public static byte[] dec( String data ) {
      return Base64.decode( data );
    }

    public static byte[] dec( byte[] data ) {
      return Base64.decode( data );
    }
  
  }
  
  /**
   * @deprecated Use {@link standard#encString(byte[])} instead
   */
  public static String encString( byte[] data ) {
    return standard.encString( data );
  }
  
  /**
   * @deprecated Use {@link standard#encString(String)} instead
   */
  public static String encString( String data ) {
    return standard.encString( data );
  }
  
  /**
   * @deprecated Use {@link standard#enc(String)} instead
   */
  public static byte[] enc( String data ) {
    return standard.enc( data );
  }
  
  /**
   * @deprecated Use {@link standard#enc(byte[])} instead
   */
  public static byte[] enc( byte[] data ) {
    return standard.enc( data );
  }
  
  /**
   * @deprecated Use {@link standard#decString(byte[])} instead
   */
  public static String decString( byte[] data ) {
    return standard.decString( data );
  }
  
  /**
   * @deprecated Use {@link standard#decString(String)} instead
   */
  public static String decString( String data ) {
    return standard.decString( data );
  }
  
  /**
   * @deprecated Use {@link standard#dec(String)} instead
   */
  public static byte[] dec( String data ) {
    return standard.dec( data );
  }
  
  /**
   * @deprecated Use {@link standard#dec(byte[])} instead
   */
  public static byte[] dec( byte[] data ) {
    return standard.dec( data );
  }
}
