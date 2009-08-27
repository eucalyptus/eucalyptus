/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.util;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

public class EucaLayout extends PatternLayout {
  public static int LINE_BYTES = 100;
  static {
    try {
      LINE_BYTES = Integer.parseInt( System.getenv( "COLUMNS" ) );
    } catch ( NumberFormatException e ) {}
  }
  public static String PATTERN = "%d{HH:mm:ss} %5p %-20.20c{1} | %m%n";
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
    String[] messages = event.getRenderedMessage( ).split( "\n" );
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
}
