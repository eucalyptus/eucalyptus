package com.eucalyptus.auth;

import java.util.List;
import org.apache.log4j.Logger;

public class Debugging {
  
  public static final boolean DEBUG = true;
  
  public static String getListString( List list ) {
    StringBuilder sb = new StringBuilder( );
    for ( Object o : list ) {
      sb.append( o ).append( " " );
    }
    return sb.toString( );
  }
  
  public static String getEucaStackTraceString( int start ) {
    StringBuilder sb = ( new StringBuilder( ) ).append( " STACK || ");
    StackTraceElement[] stes = ( new Throwable( ) ).getStackTrace( );
    for ( int i = start; i < stes.length; i++ ) {
      String steStr = stes[i].toString( );
      if ( steStr.contains( "eucalyptus" ) ) {
        sb.append( steStr ).append( " || " );
      }
    }
    return sb.toString( );
  }
  
  /**
   * Log with trace stack.
   * 
   * @param logger
   */
  public static void logWT( Logger logger, Object... objs ) {
    if ( !DEBUG ) return;
    StringBuilder sb = new StringBuilder( );
    for ( Object obj : objs ) {
      if ( obj != null ) {
        sb.append( obj.toString( ) ).append( " " );
      }
    }
    logger.debug( sb.toString( ) + " @ " +  getEucaStackTraceString( 2 ) );
  }
}
