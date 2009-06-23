package edu.ucsb.eucalyptus.transport.client;

import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesType;
import edu.ucsb.eucalyptus.transport.binding.Binding;
import edu.ucsb.eucalyptus.transport.binding.BindingManager;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.log4j.Logger;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.Vector;

public class TestBasicHttpNioClient {
  private static Logger LOG = Logger.getLogger( TestBasicHttpNioClient.class );

  static {
    Security.addProvider( new BouncyCastleProvider() );
    WSSConfig.getDefaultWSConfig().addJceProvider( "BC", BouncyCastleProvider.class.getCanonicalName() );
    WSSConfig.getDefaultWSConfig().setTimeStampStrict( true );
    WSSConfig.getDefaultWSConfig().setEnableSignatureConfirmation( true );
  }

  static final String WSA_NAMESPACE = "http://www.w3.org/2005/08/addressing";
  static final String WSA_NAMESPACE_PREFIX = "wsa";
  static final String WSA_MESSAGE_ID = "MessageID";
  static final String WSA_RELATES_TO = "RelatesTo";
  static final String WSA_RELATES_TO_RELATIONSHIP_TYPE = "RelationshipType";
  static final String WSA_TO = "To";
  static final String WSA_REPLY_TO = "ReplyTo";
  static final String WSA_FROM = "From";
  static final String WSA_FAULT_TO = "FaultTo";
  static final String WSA_ACTION = "Action";
  static final String EPR_SERVICE_NAME = "ServiceName";
  static final String EPR_REFERENCE_PARAMETERS = "ReferenceParameters";

  public static void main( String[] args ) throws Throwable {
    String host = "10.0.0.2";
//    String servicePath = "/axis2/services/EucalyptusCC";
//    int port = 8774;
    String servicePath = "/services/Eucalyptus";
    int port = 8773;

    URI uri = new URI( "http://" + host + ":" + port + servicePath );

    DescribeAvailabilityZonesType testMsg = new DescribeAvailabilityZonesType();
//    DescribePublicAddressesType testMsg = new DescribePublicAddressesType();
    Binding binding = BindingManager.getBinding( "ec2_amazonaws_com_doc_2008_12_01" );
//    Binding binding = BindingManager.getBinding( "cc_eucalyptus_ucsb_edu" );

    NioClient client = new NioClient( host, port );

    HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, uri.toASCIIString(), testMsg );
    request.addHeader( HttpHeaders.Names.HOST, host + ":" + port );
    request.addHeader( HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=UTF-8" );


    ChannelFuture requestFuture = null;
    try {
      requestFuture = client.write( request );
    } catch ( Throwable throwable ) {
      client.cleanup();
    }
    requestFuture.awaitUninterruptibly();
    client.cleanup();
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
        CertificateFactory factory = CertificateFactory.getInstance( "X509" );
        cert = ( X509Certificate ) factory.generateCertificate( new FileInputStream( certfile ) );
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
