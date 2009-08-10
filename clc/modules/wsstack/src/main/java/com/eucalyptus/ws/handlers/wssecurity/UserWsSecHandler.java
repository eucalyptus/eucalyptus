package com.eucalyptus.ws.handlers.wssecurity;

import java.io.ByteArrayInputStream;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.log4j.Logger;
import org.apache.ws.security.SOAPConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSDocInfoStore;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.message.EnvelopeIdResolver;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.Reference;
import org.apache.ws.security.message.token.SecurityTokenReference;
import org.apache.ws.security.message.token.X509Security;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.UrlBase64;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.eucalyptus.auth.Credentials;
import com.eucalyptus.auth.Hashes;
import com.eucalyptus.auth.User;
import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.google.common.collect.Lists;

@ChannelPipelineCoverage( "one" )
public class UserWsSecHandler extends MessageStackHandler implements ChannelHandler {
  private static Logger             LOG = Logger.getLogger( UserWsSecHandler.class );
  private static CertificateFactory factory;
  static {
    Credentials.init( );
  }

  @Override
  public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    final Object o = event.getMessage( );
    if ( o instanceof MappingHttpMessage ) {
      final MappingHttpMessage httpRequest = ( MappingHttpMessage ) o;
      SOAPEnvelope envelope = httpRequest.getSoapEnvelope( );
      final StAXOMBuilder doomBuilder = new StAXOMBuilder( DOOMAbstractFactory.getOMFactory( ), httpRequest.getSoapEnvelope( ).getXMLStreamReader( ) );
      final OMElement elem = doomBuilder.getDocumentElement( );
      elem.build( );
      final Document doc = ( ( Element ) elem ).getOwnerDocument( );
      final Element env = ( ( Element ) elem );
      final SOAPConstants soapConstants = WSSecurityUtil.getSOAPConstants( env );
      final Element soapHeaderElement = ( Element ) WSSecurityUtil.getDirectChildElement( doc.getFirstChild( ), soapConstants.getHeaderQName( ).getLocalPart( ), soapConstants.getEnvelopeURI( ) );
      final Element securityNode = ( Element ) WSSecurityUtil.getDirectChildElement( soapHeaderElement, WSConstants.WSSE_LN, WSConstants.WSSE_NS );
      final Element signatureNode = ( Element ) WSSecurityUtil.getDirectChildElement( securityNode, WSConstants.SIG_LN, WSConstants.SIG_NS );

      XMLSignature sig = null;
      try {
        sig = new XMLSignature( ( Element ) signatureNode, null );
      } catch ( XMLSecurityException e2 ) {
        throw new WSSecurityException( WSSecurityException.FAILED_CHECK, "noXMLSig", null, e2 );
      }
      sig.addResourceResolver( EnvelopeIdResolver.getInstance( ) );
      KeyInfo info = sig.getKeyInfo( );
      if ( info != null ) {
        Node node = WSSecurityUtil.getDirectChild( info.getElement( ), SecurityTokenReference.SECURITY_TOKEN_REFERENCE, WSConstants.WSSE_NS );
        if ( node == null ) { throw new WSSecurityException( WSSecurityException.INVALID_SECURITY, "unsupportedKeyInfo" ); }

        SecurityTokenReference secRef = new SecurityTokenReference( ( Element ) node );
        if ( secRef.containsReference( ) ) {
          Reference tokenRef = secRef.getReference( );
          Element bstDirect = WSSecurityUtil.getElementByWsuId( securityNode.getOwnerDocument( ), tokenRef.getURI( ) );
          if ( bstDirect == null ) {
            bstDirect = WSSecurityUtil.getElementByGenId( securityNode.getOwnerDocument( ), tokenRef.getURI( ) );
            if ( bstDirect == null ) { throw new WSSecurityException( WSSecurityException.INVALID_SECURITY, "noCert" ); }
          }
          BinarySecurity token = new BinarySecurity( bstDirect );
          String type = token.getValueType( );
          X509Security x509 = null;
          X509Certificate cert = null;
          try {
            x509 = new X509Security( bstDirect );
            byte[] bstToken = x509.getToken( );
            CertificateFactory factory = getCertificateFactory( );
            cert = ( X509Certificate ) factory.generateCertificate( new ByteArrayInputStream( bstToken ) );
          } catch ( Exception e ) {
            LOG.error( e, e );
            throw new WSSecurityException( WSSecurityException.UNSUPPORTED_SECURITY_TOKEN, "unsupportedBinaryTokenType", new Object[] { type } );
          }
          if( !sig.checkSignatureValue( cert ) ) {
            throw new WSSecurityException( WSSecurityException.FAILED_CHECK );
          }
          String userName = Credentials.Users.getUserName( cert );
          User user = Credentials.getUser( userName );
          httpRequest.setUser( user );
        }
      }
    }
  }

  private static CertificateFactory getCertificateFactory( ) {
    if ( factory == null ) {
      try {
        factory = CertificateFactory.getInstance( "X.509", "BC" );
      } catch ( CertificateException e ) {
        LOG.error( e, e );
      } catch ( NoSuchProviderException e ) {
        LOG.error( e, e );
      }
    }
    return factory;
  }

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {

  }

}
