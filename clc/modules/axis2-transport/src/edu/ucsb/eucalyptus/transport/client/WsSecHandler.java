package edu.ucsb.eucalyptus.transport.client;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.log4j.Logger;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;
import org.bouncycastle.openssl.PEMReader;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Vector;

@ChannelPipelineCoverage("all")
public class WsSecHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {
  private static Logger LOG = Logger.getLogger( WsSecHandler.class );

  public void handleUpstream( final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent ) throws Exception {
    channelHandlerContext.sendUpstream( channelEvent );        
  }

  public void handleDownstream( final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent ) throws Exception {
    LOG.fatal(this.getClass().getSimpleName() + ": " + channelEvent);
    if( channelEvent instanceof MessageEvent ) {
      Object o = ((MessageEvent) channelEvent).getMessage();
      if( o instanceof MappingHttpRequest ) {
        MappingHttpRequest httpRequest = (MappingHttpRequest) o;
        StAXOMBuilder doomBuilder = new StAXOMBuilder( DOOMAbstractFactory.getOMFactory(), httpRequest.getEnvelope().getXMLStreamReader() );
        OMElement elem = doomBuilder.getDocumentElement();
        elem.build();
        Document doc = ( ( Element ) elem ).getOwnerDocument();

        Vector v = new Vector();
        WSSecHeader wsheader = new WSSecHeader( "", false );
        wsheader.insertSecurityHeader( doc );

        WSSecSignature signer = new WSSecSignature();
        signer.setKeyIdentifierType( WSConstants.BST_DIRECT_REFERENCE );
        signer.setSigCanonicalization( WSConstants.C14N_EXCL_OMIT_COMMENTS );
        signer.prepare( doc, new CryptoProxy(), wsheader );

        WSSecTimestamp ts = new WSSecTimestamp();
        ts.setTimeToLive( 300 );
        ts.prepare( doc );
        ts.prependToHeader( wsheader );
        v.add( new WSEncryptionPart( WSConstants.TIMESTAMP_TOKEN_LN, WSConstants.WSU_NS, "Content" ) );

        v.add( new WSEncryptionPart( SOAP11Constants.BODY_LOCAL_NAME, SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI, "Content" ) );
        signer.appendBSTElementToHeader( wsheader );
        signer.appendToHeader( wsheader );
        signer.addReferencesToSign( v, wsheader );

        signer.computeSignature();

        StAXSOAPModelBuilder stAXSOAPModelBuilder = new StAXSOAPModelBuilder(elem.getXMLStreamReader(), null);
        SOAPEnvelope envelope = stAXSOAPModelBuilder.getSOAPEnvelope();
        envelope.build();

        httpRequest.setEnvelope( envelope );
      }
    }
    channelHandlerContext.sendDownstream( channelEvent );
  }
  static class CryptoProxy implements Crypto {
    public X509Certificate[] getCertificates( String arg0 ) throws WSSecurityException {
      X509Certificate cert;
      try {
        cert = getCertByName();
      }
      catch ( Exception e ) {
        throw new WSSecurityException( 0, null, null, e );
      }
      return ( new X509Certificate[]{
          cert
      } );
    }

    public PrivateKey getPrivateKey( String alias, String password ) throws Exception {
      return getPrivateKeyByName();
    }

    X509Certificate getCertByName() throws Exception {

      if ( cert == null ) {
        CertificateFactory factory = CertificateFactory.getInstance("X509");
        cert = (X509Certificate)factory.generateCertificate(new FileInputStream(certfile));
      }
      return cert;
    }

    private PrivateKey getPrivateKeyByName()
        throws Exception {

      if ( privkey == null ) {
        PEMReader reader = new PEMReader( new FileReader( keyfile ) );
        KeyPair kp = ( KeyPair ) reader.readObject();
        privkey = kp.getPrivate();
      }
      return privkey;
    }

    private String keyfile = System.getenv( "EC2_PRIVATE_KEY" );
    private String certfile = System.getenv( "EC2_CERT" );
    private X509Certificate cert;
    private PrivateKey privkey;

    public String[] getAliasesForDN( String arg0 ) throws WSSecurityException { throw new WSSecurityException( 0 ); }

    public String getAliasForX509Cert( byte arg0[] ) throws WSSecurityException { throw new WSSecurityException( 0 ); }

    public String getAliasForX509Cert( Certificate arg0 ) throws WSSecurityException { throw new WSSecurityException( 0 ); }

    public String getAliasForX509Cert( String arg0, BigInteger arg1 ) throws WSSecurityException { throw new WSSecurityException( 0 ); }

    public String getAliasForX509Cert( String arg0 ) throws WSSecurityException { throw new WSSecurityException( 0 ); }

    public byte[] getCertificateData( boolean arg0, X509Certificate arg1[] ) throws WSSecurityException { throw new WSSecurityException( 0 ); }

    public CertificateFactory getCertificateFactory() throws WSSecurityException { throw new WSSecurityException( 0 ); }

    public String getDefaultX509Alias() { throw new RuntimeException( "Unimplemented" ); }

    public KeyStore getKeyStore() { throw new RuntimeException( "Unimplemented" ); }

    public byte[] getSKIBytesFromCert( X509Certificate arg0 ) throws WSSecurityException { throw new WSSecurityException( 0 ); }

    public X509Certificate[] getX509Certificates( byte arg0[], boolean arg1 ) throws WSSecurityException { throw new WSSecurityException( 0 ); }

    public X509Certificate loadCertificate( InputStream arg0 ) throws WSSecurityException { throw new WSSecurityException( 0 ); }

    public boolean validateCertPath( X509Certificate arg0[] ) throws WSSecurityException { throw new WSSecurityException( 0 ); }

    public String getAliasForX509CertThumb( byte bytes[] ) throws WSSecurityException { throw new RuntimeException( "Unimplemented" ); }

  }

}
