/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import org.apache.log4j.spi.LoggingEvent;
import org.hibernate.exception.GenericJDBCException;

import com.eucalyptus.context.Contexts;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.log.EucaLoggingEvent;
import com.eucalyptus.system.log.EucaPatternLayout;

/**
 * @author Sang-Min Park
 *
 */
public class RequestTrackingLayout extends EucaPatternLayout {
  private static final String DEFAULT_LOG_PATTERN     = "%d{yyyy-MM-dd HH:mm:ss} %5.5p | [%o] %m%n";
  private static final String DEBUG_LOG_PATTERN       = "%d{yyyy-MM-dd HH:mm:ss} %5.5p %-24.24c{1} | [%o] %m%n";
  private static final String EXTREME_LOG_PATTERN     = "%d{yyyy-MM-dd HH:mm:ss} %5.9p %-24.24c{1} %-33.33f | [%o] %m%n";
 
  private String CONTINUATION = "[%o] %m%n";
  private EucaPatternLayout continuation = null;
  private final EucaPatternLayout extremeLayout;
  private final EucaPatternLayout debugLayout;
  
  public RequestTrackingLayout() {
    super( DEFAULT_LOG_PATTERN );
    this.debugLayout = new EucaPatternLayout(DEBUG_LOG_PATTERN);
    this.extremeLayout = new EucaPatternLayout(EXTREME_LOG_PATTERN);
  }
  
  private String mainFormat(LoggingEvent e) {
    if (Logs.isExtrrreeeme()) 
      return extremeLayout.format(e);
    else if (Logs.isDebug())
      return debugLayout.format(e);
    else 
      return super.format(e);
  }

  @Override
  public String format( LoggingEvent event ) {
    try {
      String corrId = null;
      try {
        corrId = Contexts.lookup().getCorrelationId();
      }catch(final Exception ex){
        corrId = Threads.getCorrelationId();
      }         
      if(corrId==null || corrId.length()<36)
        return "";
      
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
          String substring= messages[i];
          LoggingEvent n = new EucaLoggingEvent( event.getFQNOfLoggerClass( ), event.getLogger( ), 
              event.getTimeStamp( ), event.getLevel( ), 
              substring, event.getThreadName( ), 
              event.getThrowableInformation( ), null, null, null );
          n.setProperty("correlation-id", corrId);
          sb.append( (!con)?mainFormat( n ):continuation.format( n ) );
          if(continuation==null) {
            continuation = new EucaPatternLayout(sb.toString( ).split( "\\|" )[0].replaceAll( ".", " " )+"| "+CONTINUATION);
          }
          con = true;        
        }    
        return sb.toString( );
      }
    } catch ( Exception ex ) {
      ;
    }
    return null;
  }

  @Override
  public boolean ignoresThrowable( ) {
    return true;
  }
}
