/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package com.eucalyptus.ws.handlers;

import java.io.ByteArrayInputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.eucalyptus.auth.Hashes;
import com.eucalyptus.auth.UserCredentialProvider;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.ws.AuthenticationException;
import com.eucalyptus.ws.MappingHttpRequest;

import com.eucalyptus.auth.User;

@ChannelPipelineCoverage("one")
public class WalrusSoapUserAuthenticationHandler extends MessageStackHandler {
	private static Logger LOG = Logger.getLogger( WalrusSoapUserAuthenticationHandler.class );

	@Override
	public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpRequest ) {
			MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
			SOAPEnvelope envelope = httpRequest.getSoapEnvelope();
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
			String queryKey = UserCredentialProvider.getSecretKey(accessKeyID);
			String authSig = checkSignature( queryKey, data );
			if (!authSig.equals(signature))
				throw new AuthenticationException( "User authentication failed. Could not verify signature" );
			String userName = UserCredentialProvider.getUserName( accessKeyID );
			User user = UserCredentialProvider.getUser( userName );  
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
	}
}
