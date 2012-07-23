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

package com.eucalyptus.auth;

import java.util.List;
import org.apache.log4j.Logger;

public class Debugging {
  
  public static final boolean DEBUG = true;
  
  @SuppressWarnings( "rawtypes" )
  public static String getListString( List list ) {
    StringBuilder sb = new StringBuilder( );
    for ( Object o : list ) {
      sb.append( o ).append( " " );
    }
    return sb.toString( );
  }
  
  public static String getEucaStackTraceString( int start, Throwable t ) {
    StringBuilder sb = ( new StringBuilder( ) ).append( " | ");
    StackTraceElement[] stes = t.getStackTrace( );
    for ( int i = start; i < stes.length; i++ ) {
      String steStr = stes[i].toString( );
      if ( steStr.contains( "eucalyptus" ) ) {
        sb.append( steStr ).append( " | " );
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
    logger.debug( sb.toString( ) + " @ " +  getEucaStackTraceString( 1, new Throwable( ) ) );
  }
  
  public static void logError( Logger logger, Throwable t, String message ) {
    if ( t != null ) {
      logger.error( t, t );
      logger.debug( message + " with exception " + t + getEucaStackTraceString( 0, t ) );
    } else {
      logger.debug( message );
    }
  }
  
}
