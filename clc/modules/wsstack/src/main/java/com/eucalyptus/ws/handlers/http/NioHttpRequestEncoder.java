package com.eucalyptus.ws.handlers.http;

import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.util.HttpUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMessageEncoder;
import org.jboss.netty.handler.codec.http.HttpRequest;

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
