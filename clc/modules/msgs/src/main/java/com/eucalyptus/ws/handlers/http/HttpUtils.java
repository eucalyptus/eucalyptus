/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.ws.handlers.http;

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.HttpMethod;

import com.google.common.collect.Lists;


class HttpUtils {

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
        if ( lineLength >= maxLineLength ) { throw new HttpException( "HTTP input line longer than " + maxLineLength + " bytes: " + sb.toString( ) ); }
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
