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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.ws.handlers;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Arrays;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
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
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.login.SecurityContext;
import com.eucalyptus.auth.login.WalrusWrappedComponentCredentials;
import com.eucalyptus.auth.login.WalrusWrappedCredentials;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.util.WalrusUtil;
import com.google.common.collect.Lists;

@ChannelPipelineCoverage("one")
public class WalrusAuthenticationHandler extends MessageStackHandler {
	private static Logger LOG = Logger.getLogger( WalrusAuthenticationHandler.class );
	public enum SecurityParameter {
		AWSAccessKeyId,
		Timestamp,
		Expires,
		Signature,
		Authorization,
		Date,
		Content_MD5,
		Content_Type,
		SecurityToken,
	}
	
	/**
	 * This method exists to clean up a problem encountered periodically where the HTTP
	 * headers are duplicated
	 * 
	 * @param httpRequest
	 */
	private static void removeDuplicateHeaderValues(MappingHttpRequest httpRequest) {
		List<String> hdrList = null;
		HashMap<String, List<String>> fixedHeaders = new HashMap<String, List<String>>();
		boolean foundDup = false;
		for(String header : httpRequest.getHeaderNames()) {
			hdrList = httpRequest.getHeaders(header);
			
			//Only address the specific case where there is exactly one identical copy of the header
			if(hdrList != null && hdrList.size() == 2 && hdrList.get(0).equals(hdrList.get(1))) {
				foundDup = true;
				fixedHeaders.put(header, Lists.newArrayList(hdrList.get(0)));
			} else {
				fixedHeaders.put(header, hdrList);
			}
		}
		
		if(foundDup) {
			LOG.debug("Found duplicate headers in: " + httpRequest.logMessage());		
			httpRequest.clearHeaders();
		
			for(Map.Entry<String,List<String>> e : fixedHeaders.entrySet()) {
				for(String v : e.getValue()) {
					httpRequest.addHeader(e.getKey(), v);
				}
			}			
		}
		
	}

	@Override
	public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpRequest ) {
			MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
			
			removeDuplicateHeaderValues(httpRequest);
			
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
			String content_md5 = httpRequest.getHeader("Content-MD5");
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
				//Split into individual parameter=value strings
				String[] params = addrStrings[1].split("&");

				//Sort the query parameters before adding them to the canonical string
				Arrays.sort(params);
				String[] pair = null;
				boolean first = true;
				try {
					for(String qparam : params) {
						pair = qparam.split("="); //pair[0] = param name, pair[1] = param value if it is present
					
						for(WalrusProperties.SubResource subResource : WalrusProperties.SubResource.values()) {
							if(pair[0].equals(subResource.toString())) {
								if(first) {
									addrString += "?";
									first = false;
								}
								else {
									addrString += "&";
								}
								addrString += subResource.toString() + (pair.length > 1 ? "=" + WalrusUtil.URLdecode(pair[1]) : "");							
							}
						}
					}					
				} catch(UnsupportedEncodingException e) {
					throw new AuthenticationException("Could not verify request. Failed url decoding query parameters: " + e.getMessage());
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
					if(Math.abs(currentDate.getTime() - dateToVerify.getTime()) > WalrusProperties.EXPIRATION_LIMIT)
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
				String securityToken = httpRequest.getHeader(WalrusProperties.X_AMZ_SECURITY_TOKEN);
				try {
					SecurityContext.getLoginContext(new WalrusWrappedCredentials(httpRequest.getCorrelationId(), data, accessKeyId, signature, securityToken)).login();
				} catch(Exception ex) {
					LOG.error(ex);
					throw new AuthenticationException(ex);
				}
			} else if(parameters.containsKey(SecurityParameter.AWSAccessKeyId.toString())) {
				//query string authentication
				String accesskeyid = parameters.remove(SecurityParameter.AWSAccessKeyId.toString());
				try {
					//No need to decode the parameter, that is done during HTTP message creation
					String signature = parameters.remove(SecurityParameter.Signature.toString());
					if(signature == null) {
						throw new AuthenticationException("User authentication failed. Null signature.");
					}
					String expires = parameters.remove(SecurityParameter.Expires.toString());
					if(expires == null) {
						throw new AuthenticationException("Authentication failed. Expires must be specified.");
					}
					if(checkExpires(expires)) {
						String stringToSign = verb + "\n" + content_md5 + "\n" + content_type + "\n" + Long.parseLong(expires) + "\n" + getCanonicalizedAmzHeaders(httpRequest) + addrString;
						String securityToken = parameters.get(SecurityParameter.SecurityToken.toString());
						try {
							SecurityContext.getLoginContext(new WalrusWrappedCredentials(httpRequest.getCorrelationId(), stringToSign, accesskeyid, signature, securityToken)).login();
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
				try {
					Context ctx = Contexts.lookup(httpRequest.getCorrelationId());
					ctx.setUser(Principals.nobodyUser());
				} catch (NoSuchContextException e) {
					LOG.error(e, e);
					throw new AuthenticationException(e);
				}
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

	private String combineMultilineHeader(String header) {
		StringBuilder result = new StringBuilder();
		String[] parts = header.trim().split("\n");

		for(String part: parts) {
			result.append(part.trim());
			result.append(" ");
		}
		// Delete the final space
		if (values.length() > 0)
			result.deleteCharAt(values.length() -1);

		return result.toString();
	}

	private String getCanonicalizedAmzHeaders(MappingHttpRequest httpRequest) {
		Set<String> headerNames = httpRequest.getHeaderNames();

		TreeMap amzHeaders = new TreeMap<String, String>();
		for(String headerName : headerNames) {
			String headerNameLcase = headerName.toLowerCase().trim();
			if (!headerNameLcase.startsWith("x-amz-"))
				continue;

			StringBuilder values = new StringBuilder();
			for (String headerValue: httpRequest.getHeaders(headerName)) {
				values.append(combineMultilineHeader(headerValue));
				values.append(",");
			}
			// Remove the last comma
			if (values.length() > 0)
				values.deleteCharAt(values.length() -1);

			amzHeaders.put(headerNameLcase, values.toString());
		}

		StringBuilder result = new StringBuilder();
		Iterator<String> iterator = amzHeaders.keySet().iterator();
		while(iterator.hasNext()) {
			String key = iterator.next();
			String value = (String) amzHeaders.get(key);
			result.append(key).append(":");
			result.append(value).append("\n");
		}
		return result.toString();
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
		String securityToken = httpRequest.getHeader(WalrusProperties.X_AMZ_SECURITY_TOKEN);
		if(auth_part != null) {
			String sigString[] = getSigInfo(auth_part);
		 	if(sigString.length < 2) {
				throw new AuthenticationException("Invalid authentication header");
			}
			String accessKeyId = sigString[0];
			String signature = sigString[1];
			try {
				SecurityContext.getLoginContext(new WalrusWrappedCredentials(httpRequest.getCorrelationId(), data, accessKeyId, signature, securityToken)).login();
			} catch(Exception ex) {
				LOG.error(ex);
				throw new AuthenticationException(ex);
			}
		} else {
			throw new AuthenticationException("User authentication failed. Invalid policy signature.");
		}

	}

	public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent exceptionEvent ) throws Exception {
		LOG.info("[exception " + exceptionEvent + "]");
		final HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR );
		DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), ctx.getChannel().getCloseFuture(), response, null );
		ctx.sendDownstream( newEvent );
		newEvent.getFuture( ).addListener( ChannelFutureListener.CLOSE );
	}
}
