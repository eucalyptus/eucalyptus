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
package com.eucalyptus.ws.util;

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.HttpMethod;

import com.eucalyptus.ws.HttpException;
import com.google.common.collect.Lists;


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
  private static List<String> httpVerbPrefix = Lists.newArrayList( HttpMethod.CONNECT.getName( ).substring( 0, 3 ),
                                                                   HttpMethod.GET.getName( ).substring( 0, 3 ),
                                                                   HttpMethod.PUT.getName( ).substring( 0, 3 ),
                                                                   HttpMethod.POST.getName( ).substring( 0, 3 ),
                                                                   HttpMethod.HEAD.getName( ).substring( 0, 3 ),
                                                                   HttpMethod.OPTIONS.getName( ).substring( 0, 3 ),
                                                                   HttpMethod.DELETE.getName( ).substring( 0, 3 ),
                                                                   HttpMethod.TRACE.getName( ).substring( 0, 3 ) );
                                                                   
                                                                   
  public static boolean maybeSsl( ChannelBuffer buffer ) throws HttpException {
    buffer.markReaderIndex( );
    StringBuffer sb = new StringBuffer( );
    for( int lineLength = 0; lineLength++ < 3; sb.append( (char) buffer.readByte() ) );
    buffer.resetReaderIndex( );
    return !httpVerbPrefix.contains( sb.toString( ) );
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
