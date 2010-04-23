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
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.system;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.hibernate.exception.GenericJDBCException;

public class EucaLayout extends PatternLayout {
  public static int LINE_BYTES = 100;
  static {
    try {
      LINE_BYTES = Integer.parseInt( System.getenv( "COLUMNS" ) );
    } catch ( NumberFormatException e ) {}
  }
  public static String PATTERN = "%d{HH:mm:ss} %5p "+(LogLevels.DEBUG?"%-4.4L %-23.23c{1}":"%-23.23c{1}")+"| %m%n";
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
    return null;
  }

  @Override
  public boolean ignoresThrowable( ) {
    return false;
  }
}
