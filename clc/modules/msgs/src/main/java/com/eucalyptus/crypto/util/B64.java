/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
