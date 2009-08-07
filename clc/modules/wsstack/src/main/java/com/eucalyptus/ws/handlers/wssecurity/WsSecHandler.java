package com.eucalyptus.ws.handlers.wssecurity;

import java.util.Collection;
import java.util.Vector;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.log4j.Logger;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.util.CredentialProxy;

@ChannelPipelineCoverage( "all" )
public abstract class WsSecHandler extends MessageStackHandler {
  private static Logger         LOG = Logger.getLogger( WsSecHandler.class );
  private final CredentialProxy credentials;

  public WsSecHandler( final CredentialProxy credentials ) {
    this.credentials = credentials;
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    final Object o = event.getMessage( );
    if ( o instanceof MappingHttpMessage ) {
      final MappingHttpMessage httpRequest = ( MappingHttpMessage ) o;
      final StAXOMBuilder doomBuilder = new StAXOMBuilder( DOOMAbstractFactory.getOMFactory( ), httpRequest.getSoapEnvelope( ).getXMLStreamReader( ) );
      final OMElement elem = doomBuilder.getDocumentElement( );
      elem.build( );
      final Document doc = ( ( Element ) elem ).getOwnerDocument( );

      final Vector v = new Vector( );
      final WSSecHeader wsheader = new WSSecHeader( "", false );
      wsheader.insertSecurityHeader( doc );

      final WSSecSignature signer = new WSSecSignature( );
      signer.setKeyIdentifierType( WSConstants.BST_DIRECT_REFERENCE );
      signer.setSigCanonicalization( WSConstants.C14N_EXCL_OMIT_COMMENTS );
      signer.prepare( doc, this.credentials, wsheader );
      
      if ( this.shouldTimeStamp( ) ) {
        final WSSecTimestamp ts = new WSSecTimestamp( );
        ts.setTimeToLive( 300 );
        ts.prepare( doc );
        ts.prependToHeader( wsheader );
      }
      v.addAll( this.getSignatureParts( ) );
      signer.appendBSTElementToHeader( wsheader );
      signer.appendToHeader( wsheader );
      signer.addReferencesToSign( v, wsheader );

      signer.computeSignature( );

      final StAXSOAPModelBuilder stAXSOAPModelBuilder = new StAXSOAPModelBuilder( elem.getXMLStreamReader( ), null );
      final SOAPEnvelope envelope = stAXSOAPModelBuilder.getSOAPEnvelope( );
      envelope.build( );

      httpRequest.setSoapEnvelope( envelope );
    }
  }

  public abstract Collection<WSEncryptionPart> getSignatureParts( );

  public abstract boolean shouldTimeStamp( );

  @Override
  public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
  }

}
