package com.eucalyptus.ws.handlers.soap;

import java.util.UUID;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.handlers.MessageStackHandler;

@ChannelPipelineCoverage("one")
public class AddressingHandler extends MessageStackHandler {
  
  private static Logger     LOG                              = Logger.getLogger( AddressingHandler.class );

  static final String       WSA_NAMESPACE                    = "http://www.w3.org/2005/08/addressing";
  static final String       WSA_NAMESPACE_PREFIX             = "wsa";
  static final String       WSA_MESSAGE_ID                   = "MessageID";
  static final String       WSA_RELATES_TO                   = "RelatesTo";
  static final String       WSA_RELATES_TO_RELATIONSHIP_TYPE = "RelationshipType";
  static final String       WSA_TO                           = "To";
  static final String       WSA_REPLY_TO                     = "ReplyTo";
  static final String       WSA_FROM                         = "From";
  static final String       WSA_FAULT_TO                     = "FaultTo";
  static final String       WSA_ACTION                       = "Action";

  private final SOAPFactory soapFactory                      = OMAbstractFactory.getSOAP11Factory( );

  private String prefix;
  
  public AddressingHandler( ) {
    this.prefix = "";
  }

  public AddressingHandler( String prefix ) {
    this.prefix = prefix;
  }

  @Override
  public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      final MappingHttpRequest httpMessage = ( MappingHttpRequest ) event.getMessage( );

      // :: set action :://
      final String action = prefix + httpMessage.getOmMessage( ).getLocalName( );
      httpMessage.addHeader( "SOAPAction", action );
      final SOAPHeader header = httpMessage.getSoapEnvelope( ).getHeader( );

      // :: set soap addressing info :://
      final OMNamespace wsaNs = OMAbstractFactory.getOMFactory( ).createOMNamespace( WSA_NAMESPACE, WSA_NAMESPACE_PREFIX );
      final SOAPHeaderBlock wsaToHeader = header.addHeaderBlock( WSA_TO, wsaNs );
      wsaToHeader.setText( httpMessage.getUri( ) );
      final SOAPHeaderBlock wsaActionHeader = header.addHeaderBlock( WSA_ACTION, wsaNs );
      wsaActionHeader.setText( action );
      final SOAPHeaderBlock wsaMsgId = header.addHeaderBlock( WSA_MESSAGE_ID, wsaNs );
      wsaMsgId.setText( "urn:uuid:" + UUID.randomUUID( ).toString( ).replaceAll( "-", "" ).toUpperCase( ) );

    }
  }
}
