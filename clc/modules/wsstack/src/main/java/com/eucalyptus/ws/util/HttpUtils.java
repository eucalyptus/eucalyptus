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
package com.eucalyptus.ws.util;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;

import com.eucalyptus.ws.HttpException;


public class HttpUtils {

  public static String readLine( ChannelBuffer buffer, int maxLineLength ) throws HttpException {
    StringBuilder sb = new StringBuilder( 64 );
    int lineLength = 0;
    while ( true ) {
      byte nextByte = buffer.readByte( );
      if ( nextByte == HttpUtils.CR ) {
        nextByte = buffer.readByte( );
        if ( nextByte == HttpUtils.LF ) { return sb.toString( ); }
      } else if ( nextByte == HttpUtils.LF ) {
        return sb.toString( );
      } else {
        if ( lineLength >= maxLineLength ) { throw new HttpException( "HTTP input line longer than " + maxLineLength + " bytes." ); }
        lineLength++;
        sb.append( ( char ) nextByte );
      }
    }
  }

  public static final byte SP = 32;
  public static final byte HT = 9;
  public static final byte CR = 13;
  public static final byte EQUALS = 61;
  public static final byte LF = 10;
  public static final byte[] CRLF = new byte[] { CR, LF };
  public static final byte COLON = 58;
  public static final byte SEMICOLON = 59;
  public static final byte COMMA = 44;
  public static final byte DOUBLE_QUOTE = '"';
  public static final String DEFAULT_CHARSET = "UTF-8";

}
