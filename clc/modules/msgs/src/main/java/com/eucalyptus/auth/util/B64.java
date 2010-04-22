package com.eucalyptus.auth.util;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.UrlBase64;

public class B64 {
  public static class url {
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
  
  public static String encString( byte[] data ) {
    return new String( enc( data ) );
  }
  
  public static String encString( String data ) {
    return new String( enc( data ) );
  }
  
  public static byte[] enc( String data ) {
    return Base64.encode( data.getBytes( ) );
  }
  
  public static byte[] enc( byte[] data ) {
    return Base64.encode( data );
  }
  
  public static String decString( byte[] data ) {
    return new String( dec( data ) );
  }
  
  public static String decString( String data ) {
    return new String( dec( data ) );
  }
  
  public static byte[] dec( String data ) {
    return Base64.decode( data );
  }
  
  public static byte[] dec( byte[] data ) {
    return Base64.decode( data );
  }
}
