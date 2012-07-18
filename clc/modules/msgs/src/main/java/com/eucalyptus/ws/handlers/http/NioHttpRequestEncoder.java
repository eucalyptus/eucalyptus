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

package com.eucalyptus.ws.handlers.http;

import com.eucalyptus.http.MappingHttpRequest;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMessageEncoder;
import org.jboss.netty.handler.codec.http.HttpRequest;

@Deprecated
public class NioHttpRequestEncoder extends HttpMessageEncoder {

  public NioHttpRequestEncoder( ) {
    super( );
  }

  @Override
  protected void encodeInitialLine( ChannelBuffer buf, HttpMessage message ) throws Exception {
    MappingHttpRequest request = ( MappingHttpRequest ) message;
    buf.writeBytes( request.getMethod( ).toString( ).getBytes( "ASCII" ) );
    buf.writeByte( HttpUtils.SP );
    buf.writeBytes( request.getServicePath( ).getBytes( "ASCII" ) );
    buf.writeByte( HttpUtils.SP );
    buf.writeBytes( request.getProtocolVersion( ).toString( ).getBytes( "ASCII" ) );
    buf.writeBytes( HttpUtils.CRLF );
  }
}
