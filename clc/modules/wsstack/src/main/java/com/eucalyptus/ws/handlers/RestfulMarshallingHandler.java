package com.eucalyptus.ws.handlers;

import java.io.ByteArrayOutputStream;

import org.apache.axiom.om.OMElement;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.MappingHttpResponse;
import com.eucalyptus.ws.binding.Binding;
import com.eucalyptus.ws.binding.BindingManager;
import com.eucalyptus.ws.server.EucalyptusQueryPipeline.RequiredQueryParams;

import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;

@ChannelPipelineCoverage("one")
public abstract class RestfulMarshallingHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( RestfulMarshallingHandler.class );
  protected String namespace;

  @Override
  public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      this.namespace = "http://ec2.amazonaws.com/doc/" + httpRequest.getParameters( ).remove( RequiredQueryParams.Version.toString( ) );
      LOG.error( "Setting namespace="+this.namespace);
      // TODO: get real user data here too
      httpRequest.setMessage( this.bind( "admin", true, httpRequest ) );
    }
  }

  public abstract Object bind( String user, boolean admin, MappingHttpRequest httpRequest ) throws BindingException;

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse httpResponse = ( MappingHttpResponse ) event.getMessage( );
      LOG.error( "Getting namespace="+this.namespace);
      Binding binding = BindingManager.getBinding( BindingManager.sanitizeNamespace( this.namespace ) );
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      if( httpResponse.getMessage( ) instanceof EucalyptusErrorMessageType ) {
        EucalyptusErrorMessageType errMsg = (EucalyptusErrorMessageType) httpResponse.getMessage( );
        Binding.createFault( errMsg.getSource( ), errMsg.getMessage( ), errMsg.getStatusMessage( ) ).serialize( byteOut );
      } else {
        OMElement omMsg = binding.toOM( httpResponse.getMessage( ), this.namespace );
        omMsg.serialize( byteOut );        
      }
      byte[] req = byteOut.toByteArray();
      ChannelBuffer buffer = ChannelBuffers.copiedBuffer( req );
      httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes() ) );
      httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, "application/xml; charset=UTF-8" );
      httpResponse.setContent( buffer );
    }
  }

}
