package com.eucalyptus.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class ExceptionUtil {

  public static String getStackTrace( Throwable t ) {
    if ( t != null ) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream( );
      t.printStackTrace( new PrintWriter( baos ) );
      try {
        return baos.toString( "UTF-8" );
      } catch ( UnsupportedEncodingException e ) {}
    }
    return "";
  }
  
}
