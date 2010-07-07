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
 *
 * Author: Neil Soman neil@eucalyptus.com
 */
package com.eucalyptus.ws.handlers;

import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
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
import com.eucalyptus.auth.Authentication;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.crypto.Hmac;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.login.SecurityContext;
import com.eucalyptus.auth.login.WalrusWrappedComponentCredentials;
import com.eucalyptus.auth.login.WalrusWrappedCredentials;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.AbstractKeyStore;
import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.util.WalrusUtil;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.http.MappingHttpRequest;

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
			if(httpRequest.containsHeader(WalrusProperties.Headers.S3UploadPolicy.toString())) {
				checkUploadPolicy(httpRequest);
			}
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
			String effectiveUserID = httpRequest.getAndRemoveHeader(StorageProperties.StorageParameters.EucaEffectiveUserId.toString());
			try {
				SecurityContext.getLoginContext(new WalrusWrappedComponentCredentials(httpRequest.getCorrelationId(), data, effectiveUserID, signature, certString)).login();
			} catch(Exception ex) {
				LOG.error(ex);
				throw new AuthenticationException(ex);
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
				String[] subResourcesCandidates = addrStrings[1].split("&");
				for(String subResourceCandidate : subResourcesCandidates) {
					for(WalrusProperties.SubResource subResource : WalrusProperties.SubResource.values()) {
						if(subResourceCandidate.equals(subResource.toString().toLowerCase())) {
							addrString += "?" + subResource.toString().toLowerCase();
							break;
						}
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
				String authPart = httpRequest.getAndRemoveHeader(SecurityParameter.Authorization.toString());
				String sigString[] = getSigInfo(authPart);
				if(sigString.length < 2) {
					throw new AuthenticationException("Invalid authentication header");
				}
				String accessKeyId = sigString[0];
				String signature = sigString[1];

				try {
					SecurityContext.getLoginContext(new WalrusWrappedCredentials(httpRequest.getCorrelationId(), data, accessKeyId, signature)).login();
				} catch(Exception ex) {
					LOG.error(ex);
					throw new AuthenticationException(ex);
				}
			} else if(parameters.containsKey(SecurityParameter.AWSAccessKeyId.toString())) {
				//query string authentication
				String accesskeyid = parameters.remove(SecurityParameter.AWSAccessKeyId.toString());
				try {
					String signature = WalrusUtil.URLdecode(parameters.remove(SecurityParameter.Signature.toString()));
					if(signature == null) {
						throw new AuthenticationException("User authentication failed. Null signature.");
					}
					String expires = parameters.remove(SecurityParameter.Expires.toString());
					if(expires == null) {
						throw new AuthenticationException("Authentication failed. Expires must be specified.");
					}
					if(checkExpires(expires)) {
						String stringToSign = verb + "\n" + content_md5 + "\n" + content_type + "\n" + Long.parseLong(expires) + "\n" + getCanonicalizedAmzHeaders(httpRequest) + addrString;
						try {
							SecurityContext.getLoginContext(new WalrusWrappedCredentials(httpRequest.getCorrelationId(), stringToSign, accesskeyid, signature)).login();
						} catch(Exception ex) {
							LOG.error(ex);
							throw new AuthenticationException(ex);
						}
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

	private void checkUploadPolicy(MappingHttpRequest httpRequest) throws AuthenticationException {
		Map<String, String> fields = new HashMap<String, String>();
		String policy = httpRequest.getAndRemoveHeader(WalrusProperties.Headers.S3UploadPolicy.toString());
		fields.put(WalrusProperties.FormField.policy.toString(), policy);
		String policySignature = httpRequest.getAndRemoveHeader(WalrusProperties.Headers.S3UploadPolicySignature.toString());
		if(policySignature == null)
			throw new AuthenticationException("Policy signature must be specified with policy.");
		String awsAccessKeyId = httpRequest.getAndRemoveHeader(SecurityParameter.AWSAccessKeyId.toString());
		if(awsAccessKeyId == null)
			throw new AuthenticationException("AWSAccessKeyID must be specified.");
		fields.put(WalrusProperties.FormField.signature.toString(), policySignature);
		fields.put(SecurityParameter.AWSAccessKeyId.toString(), awsAccessKeyId);
		String acl = httpRequest.getAndRemoveHeader(WalrusProperties.AMZ_ACL.toString());
		if(acl != null)
			fields.put(WalrusProperties.FormField.acl.toString(), acl);
		String operationPath = httpRequest.getServicePath().replaceAll(WalrusProperties.walrusServicePath, "");
		String[] target = WalrusUtil.getTarget(operationPath);
		if(target != null) {
			fields.put(WalrusProperties.FormField.bucket.toString(), target[0]);
			if(target.length > 1)
				fields.put(WalrusProperties.FormField.key.toString(), target[1]);
		}
		UploadPolicyChecker.checkPolicy(httpRequest, fields);

		String data = httpRequest.getAndRemoveHeader(WalrusProperties.FormField.FormUploadPolicyData.toString());
		String auth_part = httpRequest.getAndRemoveHeader(SecurityParameter.Authorization.toString());
		if(auth_part != null) {
			String sigString[] = getSigInfo(auth_part);
			if(sigString.length < 2) {
				throw new AuthenticationException("Invalid authentication header");
			}
			String accessKeyId = sigString[0];
			String signature = sigString[1];
			try {
				SecurityContext.getLoginContext(new WalrusWrappedCredentials(httpRequest.getCorrelationId(), data, accessKeyId, signature)).login();
			} catch(Exception ex) {
				LOG.error(ex);
				throw new AuthenticationException(ex);
			}
		} else {
			throw new AuthenticationException("User authentication failed. Invalid policy signature.");
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
