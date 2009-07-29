package com.eucalyptus.ws.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.MappingHttpRequest;

@ChannelPipelineCoverage( "all" )
public class SoapMarshallingHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( SoapMarshallingHandler.class );

  @Override
  public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
      String content = httpMessage.getContent( ).toString( "UTF-8" );
      httpMessage.setMessageString( content );
      ByteArrayInputStream byteIn = new ByteArrayInputStream( content.getBytes( ) );
      XMLStreamReader xmlStreamReader = XMLInputFactory.newInstance( ).createXMLStreamReader( byteIn );
      StAXSOAPModelBuilder soapBuilder = new StAXSOAPModelBuilder( xmlStreamReader, SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI );
      SOAPEnvelope env = ( SOAPEnvelope ) soapBuilder.getDocumentElement( );
      httpMessage.setSoapEnvelope( env );
    }
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream( );
      httpMessage.getSoapEnvelope( ).serialize( byteOut );
      ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( byteOut.toByteArray( ) );
      httpMessage.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
      httpMessage.addHeader( HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=UTF-8" );
      httpMessage.setContent( buffer );
    }
  }

}
