package edu.ucsb.eucalyptus.transport.client;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import java.io.ByteArrayOutputStream;

@ChannelPipelineCoverage("all")
public class SerializingHandler extends MessageStackHandler {
    private static Logger LOG = Logger.getLogger( SerializingHandler.class );

  public void incomingMessage( final MessageEvent event ) throws Exception {
  }

  public void outgoingMessage( final MessageEvent event ) throws Exception {
    Object o = event.getMessage();
    if( o instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = (MappingHttpRequest) o;
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      httpRequest.getEnvelope().serialize( byteOut );
      byte[] req = byteOut.toByteArray();
      ChannelBuffer buffer = ChannelBuffers.copiedBuffer( req );
      httpRequest.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes() ) );
      httpRequest.setContent( buffer );
    }
  }

}
