/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.ws.handlers;

import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Hmac;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.WalrusProperties;

@ChannelPipelineCoverage("one")
public class WalrusSoapUserAuthenticationHandler extends MessageStackHandler {
	private static Logger LOG = Logger.getLogger( WalrusSoapUserAuthenticationHandler.class );

	@Override
	public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpRequest ) {
			MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
			SOAPEnvelope envelope = httpRequest.getSoapEnvelope();
			SOAPBody body = envelope.getBody();
			HoldMe.canHas.lock();
			try {
				final StAXOMBuilder doomBuilder = HoldMe.getStAXOMBuilder( HoldMe.getDOOMFactory( ), body.getXMLStreamReader( ) );
				final OMElement elem = doomBuilder.getDocumentElement( );
				elem.build( );
				final Document doc = ( ( Element ) elem ).getOwnerDocument();
				handle(httpRequest, doc);
			} finally {
				HoldMe.canHas.unlock( );
			}
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
			//check timestamp
			verifyTimestamp(timestamp);
			String data = "AmazonS3" + operationName + timestamp;
			//check signature
			authenticate(httpRequest, queryId, signature, data);
		} else {
			throw new AuthenticationException("Invalid operation specified");
		}
	}

	private void authenticate(MappingHttpRequest httpRequest, String accessKeyID, String signature, String data) throws AuthenticationException {
		signature = signature.replaceAll("=", "");
		try {
			User user = Accounts.lookupUserByAccessKeyId( accessKeyID );  
			String queryKey = user.getKey( accessKeyID ).getSecretKey( );
			String authSig = checkSignature( queryKey, data );
			if (!authSig.equals(signature))
				throw new AuthenticationException( "User authentication failed. Could not verify signature" );
			Contexts.lookup( httpRequest.getCorrelationId( ) ).setUser( user );
		} catch(Exception ex) {
			throw new AuthenticationException( "User authentication failed. Unable to obtain query key" );
		}
	}

	private void verifyTimestamp(String timestamp) throws AuthenticationException {
		try {
			Date dateToVerify = DateUtil.parseDate(timestamp);
			Date currentDate = new Date();
			if(Math.abs(currentDate.getTime() - dateToVerify.getTime()) > WalrusProperties.EXPIRATION_LIMIT)
				throw new AuthenticationException("Message expired. Sorry.");
		} catch(Exception ex) {
			throw new AuthenticationException("Unable to parse date.");
		}		
	}

	private String[] getSigInfo (String auth_part) {
		int index = auth_part.lastIndexOf(" ");
		String sigString = auth_part.substring(index + 1);
		return sigString.split(":");
	}

	protected String checkSignature( final String queryKey, final String subject ) throws AuthenticationException
	{
		SecretKeySpec signingKey = new SecretKeySpec( queryKey.getBytes(), Hmac.HmacSHA1.toString() );
		try
		{
			Mac mac = Hmac.HmacSHA1.getInstance();
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
