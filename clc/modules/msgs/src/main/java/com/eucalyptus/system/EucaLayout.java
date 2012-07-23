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

package com.eucalyptus.system;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.hibernate.exception.GenericJDBCException;
import com.eucalyptus.records.Logs;

public class EucaLayout extends PatternLayout {
  public static int LINE_BYTES = 100;
  static {
    try {
      LINE_BYTES = Integer.parseInt( System.getenv( "COLUMNS" ) );
    } catch ( NumberFormatException e ) {}
  }
  public static String PATTERN = "%d{EEE MMM d HH:mm:ss yyyy} %5p "+(Logs.isExtrrreeeme()?"%C{1}.%M(%F):%L":"%-23.23c{1}")+" | %m%n";
  private String CONTINUATION = "%m%n";
  private PatternLayout continuation = null;
  

  public EucaLayout( ) {
    super( PATTERN );
    
  }

  public EucaLayout( String pattern ) {
    super( PATTERN );
    
  }

  
  
  @Override
  public String format( LoggingEvent event ) {
    try {
      if( event.getThrowableInformation( ) != null ) {
        Throwable t = event.getThrowableInformation( ).getThrowable( );
        if( t != null && t instanceof GenericJDBCException ) {
          return "";
        }
      } else if ( event.getFQNOfLoggerClass( ).matches(".*JDBCExceptionReporter.*") ) {
        return "";
      }
      String renderedMessage = event.getRenderedMessage( );
      if(renderedMessage != null) {
String[] messages = renderedMessage.split( "\n" );
      StringBuffer sb = new StringBuffer( );
      boolean con = false;
      for( int i = 0; i < messages.length; i++ ) {
//      String message= messages[i];
        String substring= messages[i];
//      while ( message.length( ) > 0 ) {
//        int rb = LINE_BYTES>message.length( )?message.length( ):LINE_BYTES;
//        String substring = message.substring( 0, rb );
//        message = message.substring( rb );
          LoggingEvent n = new LoggingEvent( event.getFQNOfLoggerClass( ), event.getLogger( ), 
                                             event.getTimeStamp( ), event.getLevel( ), 
                                             substring, event.getThreadName( ), 
                                             event.getThrowableInformation( ), null, null, null );
          sb.append( (!con)?super.format( n ):continuation.format( n ) );
          if(continuation==null) {
            continuation = new PatternLayout(sb.toString( ).split( "\\|" )[0].replaceAll( ".", " " )+"| "+CONTINUATION);
          }
          con = true;        
//      }      
      }    
      return sb.toString( );
      }
    } catch ( Exception ex ) {
      ex.printStackTrace( );
    }
    return null;
  }

  @Override
  public boolean ignoresThrowable( ) {
    return true;
  }
}
