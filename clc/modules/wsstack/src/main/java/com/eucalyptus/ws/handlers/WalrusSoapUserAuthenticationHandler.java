package com.eucalyptus.ws.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.URLDecoder;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.encoders.Base64;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.eucalyptus.auth.Credentials;
import com.eucalyptus.auth.Hashes;
import com.eucalyptus.auth.util.AbstractKeyStore;
import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.ws.AuthenticationException;
import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.binding.Binding;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.auth.User;

import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;

@ChannelPipelineCoverage("one")
public class WalrusSoapUserAuthenticationHandler extends MessageStackHandler {
	private static Logger LOG = Logger.getLogger( WalrusSoapUserAuthenticationHandler.class );
	private final SOAPFactory soapFactory                      = OMAbstractFactory.getSOAP11Factory( );

	@Override
	public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpRequest ) {
			MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
			String content = httpRequest.getContent( ).toString( "UTF-8" );
			ByteArrayInputStream byteIn = new ByteArrayInputStream( content.getBytes( ) );
			XMLStreamReader xmlStreamReader = XMLInputFactory.newInstance( ).createXMLStreamReader( byteIn );
			StAXSOAPModelBuilder soapBuilder = new StAXSOAPModelBuilder( xmlStreamReader, SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI );
			SOAPEnvelope envelope = ( SOAPEnvelope ) soapBuilder.getDocumentElement( );
			httpRequest.setMessageString( content );
			httpRequest.setSoapEnvelope( envelope );
			if(!envelope.hasFault())
				httpRequest.setOmMessage( envelope.getBody( ).getFirstElement( ) );
			SOAPBody body = envelope.getBody();
			final StAXOMBuilder doomBuilder = new StAXOMBuilder( DOOMAbstractFactory.getOMFactory( ), body.getXMLStreamReader( ) );
			final OMElement elem = doomBuilder.getDocumentElement( );
			elem.build( );
			final Document doc = ( ( Element ) elem ).getOwnerDocument();
			handle(httpRequest, doc);
		}
	}

	public void handle(MappingHttpRequest httpRequest, Document doc) throws AuthenticationException
	{
		NodeList childNodes = doc.getChildNodes();
		Element bodyElem = doc.getDocumentElement();
		Node operationElem = bodyElem.getFirstChild();
		String operationName = operationElem.getNodeName();
		if(operationName.length() > 0) {
			NodeList authNodes = operationElem.getChildNodes();
			String queryId = null, timestamp = null, signature = null;
			for(int i = 0; i < authNodes.getLength(); ++i) {
				Node node = authNodes.item(i);
				if(node.getNodeName().equals(WalrusProperties.RequiredSOAPTags.AWSAccessKeyId.toString())) {
					queryId = node.getFirstChild().getNodeValue().trim();
				} else if(node.getNodeName().equals(WalrusProperties.RequiredSOAPTags.Timestamp.toString())) {
					timestamp = node.getFirstChild().getNodeValue().trim();
				} else if(node.getNodeName().equals(WalrusProperties.RequiredSOAPTags.Signature.toString())) {
					signature = node.getFirstChild().getNodeValue().trim();
				}
			}
			if(queryId == null) 
				throw new AuthenticationException("Unable to parse access key id");				
			if(signature == null)
				throw new AuthenticationException("Unable to parse signature");
			if(timestamp == null)
				throw new AuthenticationException("Unable to parse timestamp");
			String data = "AmazonS3" + operationName + timestamp;
			authenticate(httpRequest, queryId, signature, data);
		} else {
			throw new AuthenticationException("Invalid operation specified");
		}
	}

	private void authenticate(MappingHttpRequest httpRequest, String accessKeyID, String signature, String data) throws AuthenticationException {
		signature = signature.replaceAll("=", "");
		try {
			String queryKey = Credentials.Users.getSecretKey(accessKeyID);
			String authSig = checkSignature( queryKey, data );
			if (!authSig.equals(signature))
				throw new AuthenticationException( "User authentication failed. Could not verify signature" );
			String userName = Credentials.Users.getUserName( accessKeyID );
			User user = Credentials.getUser( userName );  
			httpRequest.setUser( user );
		} catch(Exception ex) {
			throw new AuthenticationException( "User authentication failed. Unable to obtain query key" );
		}
	}

	private String[] getSigInfo (String auth_part) {
		int index = auth_part.lastIndexOf(" ");
		String sigString = auth_part.substring(index + 1);
		return sigString.split(":");
	}

	protected String checkSignature( final String queryKey, final String subject ) throws AuthenticationException
	{
		SecretKeySpec signingKey = new SecretKeySpec( queryKey.getBytes(), Hashes.Mac.HmacSHA1.toString() );
		try
		{
			Mac mac = Mac.getInstance( Hashes.Mac.HmacSHA1.toString() );
			mac.init( signingKey );
			byte[] rawHmac = mac.doFinal( subject.getBytes() );
			return new String(Base64.encode( rawHmac )).replaceAll( "=", "" );
		}
		catch ( Exception e )
		{
			LOG.error( e, e );
			throw new AuthenticationException( "Failed to compute signature" );
		}
	}

	@Override
	public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpMessage ) {
			final MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
			if( httpMessage.getMessage( ) instanceof EucalyptusErrorMessageType ) {
				EucalyptusErrorMessageType errMsg = (EucalyptusErrorMessageType) httpMessage.getMessage( );
				httpMessage.setSoapEnvelope( Binding.createFault( errMsg.getSource( ), errMsg.getMessage( ), errMsg.getStatusMessage( ) ) );
			} else {
				// :: assert sourceElem != null :://
				httpMessage.setSoapEnvelope( this.soapFactory.getDefaultEnvelope( ) );
				httpMessage.getSoapEnvelope( ).getBody( ).addChild( httpMessage.getOmMessage( ) );
			}
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream( );
			httpMessage.getSoapEnvelope( ).serialize( byteOut );
			ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( byteOut.toByteArray( ) );
			httpMessage.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
			httpMessage.addHeader( HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=UTF-8" );
			httpMessage.setContent( buffer );
		}
	}
}
