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
package com.eucalyptus.ws.handlers;

import java.io.StringReader;
import java.net.URLDecoder;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.groovy.JsonSlurper;

import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.encoders.Base64;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.Credentials;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.CredentialProvider;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.util.AbstractKeyStore;
import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.ws.AuthenticationException;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.auth.User;
import com.eucalyptus.bootstrap.Component;

@ChannelPipelineCoverage("one")
public class WalrusAuthenticationHandler extends MessageStackHandler {
	private static Logger LOG = Logger.getLogger( WalrusAuthenticationHandler.class );
	private final static long EXPIRATION_LIMIT = 900000;

	public enum SecurityParameter {
		AWSAccessKeyId,
		Timestamp,
		Expires,
		Signature,
		Authorization,
		Date,
		Content_MD5,
		Content_Type
	}

	@Override
	public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpRequest ) {
			MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
			handle(httpRequest);
		}
	}

	public void handle(MappingHttpRequest httpRequest) throws AuthenticationException
	{
		Map<String,String> parameters = httpRequest.getParameters( );
		String verb = httpRequest.getMethod().getName();
		String addr = httpRequest.getUri();

		if(httpRequest.containsHeader(StorageProperties.StorageParameters.EucaSignature.toString())) {
			//possible internal request -- perform authentication using internal credentials
			String date = httpRequest.getAndRemoveHeader(SecurityParameter.Date.toString());
			String signature = httpRequest.getAndRemoveHeader(StorageProperties.StorageParameters.EucaSignature.toString());
			String certString = null;
			if( httpRequest.containsHeader( StorageProperties.StorageParameters.EucaCert.toString( ) ) ) {
				certString= httpRequest.getAndRemoveHeader(StorageProperties.StorageParameters.EucaCert.toString());
			}
			String data = verb + "\n" + date + "\n" + addr + "\n";

			Signature sig;
			boolean valid = false;
			try {
				try {
					PublicKey publicKey = SystemCredentialProvider.getCredentialProvider(Component.storage).getCertificate().getPublicKey();
					sig = Signature.getInstance("SHA1withRSA");
					sig.initVerify(publicKey);
					sig.update(data.getBytes());
					valid = sig.verify(Base64.decode(signature));
				} catch ( Exception e ) {
					LOG.warn ("Authentication: certificate not found in keystore");
				} finally {
					if( !valid && certString != null ) {
						try {
							X509Certificate nodeCert = Hashes.getPemCert( Base64.decode( certString ) );
							String alias = CredentialProvider.getCertificateAlias( nodeCert );
							PublicKey publicKey = nodeCert.getPublicKey( );
							sig = Signature.getInstance( "SHA1withRSA" );
							sig.initVerify( publicKey );
							sig.update( data.getBytes( ) );
							valid = sig.verify( Base64.decode( signature ) );
						} catch ( Exception e2 ) {
							LOG.warn ("Authentication exception: " + e2.getMessage());
						}            
					}
				}
			} catch (Exception ex) {
				LOG.warn ("Authentication exception: " + ex.getMessage());
				ex.printStackTrace();
			}

			if(!valid) {
				throw new AuthenticationException( "User authentication failed." );
			}
			try {
				User user = CredentialProvider.getUser( "admin" );
				user.setIsAdministrator(true);
				httpRequest.setUser( user );
			} catch (NoSuchUserException e) {
				throw new AuthenticationException( "User authentication failed." );
			}  
		}  else {
			//external user request
			String content_md5 = httpRequest.getAndRemoveHeader("Content-MD5");
			content_md5 = content_md5 == null ? "" : content_md5;
			String content_type = httpRequest.getHeader(WalrusProperties.CONTENT_TYPE);
			content_type = content_type == null ? "" : content_type;

			String targetHost = httpRequest.getHeader(HttpHeaders.Names.HOST);
			if(targetHost.contains(".walrus")) {
				String bucket = targetHost.substring(0, targetHost.indexOf(".walrus"));
				addr = "/" + bucket + addr;
			}
			String[] addrStrings = addr.split("\\?");
			String addrString = addrStrings[0];

			if(addrStrings.length > 1) {
				for(WalrusProperties.SubResource subResource : WalrusProperties.SubResource.values()) {
					if(addr.endsWith(subResource.toString().toLowerCase())) {
						addrString += "?" + subResource.toString().toLowerCase();
						break;
					}
				}
			}

			if(httpRequest.containsHeader(SecurityParameter.Authorization.toString())) {
				String date;
				String verifyDate;
				if(httpRequest.containsHeader("x-amz-date")) {
					date = "";
					verifyDate = httpRequest.getHeader("x-amz-date");
				} else {
					date =  httpRequest.getAndRemoveHeader(SecurityParameter.Date.toString());
					verifyDate = date;
					if(date == null || date.length() <= 0)
						throw new AuthenticationException("User authentication failed. Date must be specified.");
				}

				try {
					Date dateToVerify = DateUtil.parseDate(verifyDate);
					Date currentDate = new Date();
					if(Math.abs(currentDate.getTime() - dateToVerify.getTime()) > EXPIRATION_LIMIT)
						throw new AuthenticationException("Message expired. Sorry.");
				} catch(Exception ex) {
					throw new AuthenticationException("Unable to parse date.");
				}
				String data = verb + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n" +  getCanonicalizedAmzHeaders(httpRequest) + addrString;

				String auth_part = httpRequest.getAndRemoveHeader(SecurityParameter.Authorization.toString());
				String sigString[] = getSigInfo(auth_part);
				String signature = sigString[1];
				authenticate(httpRequest, sigString[0], signature, data);
			} else if(parameters.containsKey(SecurityParameter.AWSAccessKeyId.toString())) {
				//query string authentication
				String accesskeyid = parameters.remove(SecurityParameter.AWSAccessKeyId.toString());
				try {
					String signature = URLDecoder.decode(parameters.remove(SecurityParameter.Signature.toString()), "UTF-8");
					if(signature == null) {
						throw new AuthenticationException("User authentication failed. Null signature.");
					}
					String expires = parameters.remove(SecurityParameter.Expires.toString());
					if(expires == null) {
						throw new AuthenticationException("Authentication failed. Expires must be specified.");
					}
					if(checkExpires(expires)) {
						String stringToSign = verb + "\n" + content_md5 + "\n" + content_type + "\n" + Long.parseLong(expires) + "\n" + getCanonicalizedAmzHeaders(httpRequest) + addrString;
						authenticate(httpRequest, accesskeyid, signature, stringToSign);
					} else {
						throw new AuthenticationException("Cannot process request. Expired.");
					}
				} catch (Exception ex) {
					throw new AuthenticationException("Could not verify request " + ex.getMessage());
				}
			} else{
				//anonymous request              
			}
		}
	}

	private boolean checkExpires(String expires) {
		Long expireTime = Long.parseLong(expires);
		Long currentTime = new Date().getTime() / 1000;
		if(currentTime > expireTime)
			return false;
		return true;
	}

	private void authenticate(MappingHttpRequest httpRequest, String accessKeyID, String signature, String data) throws AuthenticationException {
		signature = signature.replaceAll("=", "");
		try {
			String queryKey = CredentialProvider.getSecretKey(accessKeyID);
			String authSig = checkSignature( queryKey, data );
			if (!authSig.equals(signature))
				throw new AuthenticationException( "User authentication failed. Could not verify signature" );
			String userName = CredentialProvider.getUserName( accessKeyID );
			User user = CredentialProvider.getUser( userName );  
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

	private String getCanonicalizedAmzHeaders(MappingHttpRequest httpRequest) {
		String result = "";
		Set<String> headerNames = httpRequest.getHeaderNames();

		TreeMap amzHeaders = new TreeMap<String, String>();
		for(String headerName : headerNames) {
			String headerNameString = headerName.toLowerCase().trim();
			if(headerNameString.startsWith("x-amz-")) {
				String value =  httpRequest.getHeader(headerName).trim();
				String[] parts = value.split("\n");
				value = "";
				for(String part: parts) {
					part = part.trim();
					value += part + " ";
				}
				value = value.trim();
				if(amzHeaders.containsKey(headerNameString)) {
					String oldValue = (String) amzHeaders.remove(headerNameString);
					oldValue += "," + value;
					amzHeaders.put(headerNameString, oldValue);
				} else {
					amzHeaders.put(headerNameString, value);
				}
			}
		}

		Iterator<String> iterator = amzHeaders.keySet().iterator();
		while(iterator.hasNext()) {
			String key = iterator.next();
			String value = (String) amzHeaders.get(key);
			result += key + ":" + value + "\n";
		}
		return result;
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
	public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent exceptionEvent ) throws Exception {
		LOG.info("[exception " + exceptionEvent + "]");
		final HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR );
		DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), ctx.getChannel().getCloseFuture(), response, null );
		ctx.sendDownstream( newEvent );
		newEvent.getFuture( ).addListener( ChannelFutureListener.CLOSE );
	}

	@Override
	public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
	}
}
