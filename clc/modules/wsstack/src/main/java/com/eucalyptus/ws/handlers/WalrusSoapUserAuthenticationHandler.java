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
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.ws.handlers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.crypto.Hmac;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.context.Contexts;
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
			String data = "AmazonS3" + operationName + timestamp;
			authenticate(httpRequest, queryId, signature, data);
		} else {
			throw new AuthenticationException("Invalid operation specified");
		}
	}

	private void authenticate(MappingHttpRequest httpRequest, String accessKeyID, String signature, String data) throws AuthenticationException {
		signature = signature.replaceAll("=", "");
		try {
      User user = Users.lookupQueryId( accessKeyID );  
      String queryKey = user.getSecretKey( );
			String authSig = checkSignature( queryKey, data );
			if (!authSig.equals(signature))
				throw new AuthenticationException( "User authentication failed. Could not verify signature" );
      Contexts.lookup( httpRequest.getCorrelationId( ) ).setUser( user );
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
