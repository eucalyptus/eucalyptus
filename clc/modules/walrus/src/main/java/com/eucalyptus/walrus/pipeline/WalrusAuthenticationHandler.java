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

package com.eucalyptus.walrus.pipeline;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.TreeSet;

import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.walrus.WalrusBackend;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
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
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.walrus.exceptions.AccessDeniedException;
import com.eucalyptus.walrus.auth.WalrusWrappedComponentCredentials;
import com.eucalyptus.walrus.auth.WalrusWrappedCredentials;
import com.eucalyptus.walrus.util.WalrusProperties;
import com.eucalyptus.walrus.util.WalrusUtil;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.collect.Lists;


public class WalrusAuthenticationHandler extends MessageStackHandler {
	private static Logger LOG = Logger.getLogger( WalrusAuthenticationHandler.class );
	private static final String AWS_AUTH_TYPE = "AWS";
	private static final String EUCA_AUTH_TYPE = "EUCA2-RSA-SHA256";
	private static final String EUCA_OLD_AUTH_TYPE = "Euca";
	protected static final String ISO_8601_FORMAT = "yyyyMMdd'T'HHmmss'Z'"; //Use the ISO8601 format
	
	public static enum SecurityParameter {
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
	
	/* The possible fields in an authorization header */
	private static enum AuthorizationField {
		Type,
		AccessKeyId,
		CertFingerPrint,
		SignedHeaders,
		Signature
	}
	
	/**
	 * Ensure that only one header for each name exists (i.e. not 2 Authorization headers)
	 * Accomplish this by comma-delimited concatenating any duplicates found as per HTTP 1.1 RFC 2616 section 4.2
	 * 
	 * TODO: Also, should convert all headers to lower-case for consistent processing later. This is okay since headers are case-insensitive.
	 * 
	 * in HTTP
	 * @param httpRequest
	 */
	private static void canonicalizeHeaders(MappingHttpRequest httpRequest) {
		//Iterate through headers and find duplicates, concatenate their values together and remove from
		// request as we find them.
		TreeMap<String, String> headerMap = new TreeMap<String, String>();
		String value = null;
		
		//Construct a map of the normalized headers, cannot modify in-place since
		// conconcurrent-modify exception may result
		for(String header : httpRequest.getHeaderNames()) {
			//TODO: zhill, put in the map in lower-case form.
			headerMap.put(header, Joiner.on(',').join(httpRequest.getHeaders(header)));
		}
		
		//Remove *all* headers
		httpRequest.clearHeaders();

		//Add the normalized headers back into the request
		for(String foundHeader : headerMap.keySet()) {
			httpRequest.addHeader(foundHeader, headerMap.get(foundHeader).toString());
		}
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
			try {
				MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
	
				removeDuplicateHeaderValues(httpRequest);			
				//Consolidate duplicates, etc.
				
				canonicalizeHeaders(httpRequest);			
				if(httpRequest.containsHeader(WalrusProperties.Headers.S3UploadPolicy.toString())) {
					checkUploadPolicy(httpRequest);
				}
				handle(httpRequest);
			} catch (Exception ex) {
				Channels.fireExceptionCaught(ctx, ex);
			}
		}
	}
	
	/**
	 * Process the authorization header
	 * @param authorization
	 * @return
	 * @throws AccessDeniedException
	 */
	public static Map<AuthorizationField,String> processAuthorizationHeader(String authorization) throws AccessDeniedException { 
		if(Strings.isNullOrEmpty(authorization)) {
			return null;
		}
		
		HashMap<AuthorizationField, String> authMap = new HashMap<AuthorizationField,String>();		
		String[] components = authorization.split(" ");
		
		if(components.length < 2) {
			throw new AccessDeniedException("Invalid authoriztion header");
		}
		
		if(AWS_AUTH_TYPE.equals(components[0]) && (components.length == 2 || (components.length == 3 && components[2].startsWith(":")))) {
			//Expect: components[1] = <AccessKeyId>:<Signature>
			authMap.put(AuthorizationField.Type, AWS_AUTH_TYPE);
			if(components.length == 2) {
				String[] signatureElements = components[1].split(":");			
				authMap.put(AuthorizationField.AccessKeyId, signatureElements[0]);
				authMap.put(AuthorizationField.Signature, signatureElements[1]);
			} else {
				authMap.put(AuthorizationField.AccessKeyId, components[1].trim());
				authMap.put(AuthorizationField.Signature, components[2].substring(1).trim());
			}
		}
		else if(EUCA_AUTH_TYPE.equals(components[0]) && components.length == 4){
			//Expect: components[0] = EUCA2-RSA-SHA256 components[1] = <fingerprint of signing certificate> components[2] = <list of signed headers> components[3] = <Signature>
			authMap.put(AuthorizationField.Type, EUCA_AUTH_TYPE);			
			authMap.put(AuthorizationField.CertFingerPrint, components[1].trim());
			authMap.put(AuthorizationField.SignedHeaders, components[2].trim());
			authMap.put(AuthorizationField.Signature, components[3].trim());			
		}
		else if(EUCA_OLD_AUTH_TYPE.equals(components[0]) && components.length == 1){
			authMap.put(AuthorizationField.Type, EUCA_OLD_AUTH_TYPE);
		}
		else {
			throw new AccessDeniedException("Invalid authorization header");
		}
		
		return authMap;
	}
	
	/*
	 * Handle S3UploadPolicy optionally sent as headers for bundle-upload calls.
	 * Simply verifies the policy and signature of the policy.
	 */
	private static void checkUploadPolicy(MappingHttpRequest httpRequest) throws AccessDeniedException {
		Map<String, String> fields = new HashMap<String, String>();
		String policy = httpRequest.getHeader(WalrusProperties.Headers.S3UploadPolicy.toString());
		fields.put(WalrusProperties.FormField.policy.toString(), policy);
		String policySignature = httpRequest.getHeader(WalrusProperties.Headers.S3UploadPolicySignature.toString());
		if(policySignature == null)
			throw new AccessDeniedException("Policy signature must be specified with policy.");
		String awsAccessKeyId = httpRequest.getHeader(SecurityParameter.AWSAccessKeyId.toString());
		if(awsAccessKeyId == null)
			throw new AccessDeniedException("AWSAccessKeyID must be specified.");
		fields.put(WalrusProperties.FormField.signature.toString(), policySignature);
		fields.put(SecurityParameter.AWSAccessKeyId.toString(), awsAccessKeyId);
		String acl = httpRequest.getHeader(WalrusProperties.AMZ_ACL.toString());
		if(acl != null)
			fields.put(WalrusProperties.FormField.acl.toString(), acl);
		String operationPath = httpRequest.getServicePath().replaceAll(ComponentIds.lookup(WalrusBackend.class).getServicePath(), "");
		String[] target = WalrusUtil.getTarget(operationPath);
		if(target != null) {
			fields.put(WalrusProperties.FormField.bucket.toString(), target[0]);
			if(target.length > 1)
				fields.put(WalrusProperties.FormField.key.toString(), target[1]);
		}
		try {
			UploadPolicyChecker.checkPolicy(httpRequest, fields);
		} catch (AuthenticationException aex) {
			throw new AccessDeniedException(aex.getMessage());
		}
	
	}	
	
	/**
	 * Class contains methods for implementing EucaRSA-V2 Authentication.
	 * @author zhill
	 *
	 */
	public static class EucaAuthentication {
		private static final Set<String> SAFE_HEADER_SET = Sets.newHashSet("transfer-encoding"); 
		
		/**
		 * Implements the Euca2 auth method
		 * 
		 * Add an Authorization HTTP header to the request that contains the following strings, separated by spaces:
		 * EUCA2-RSA-SHA256
		 * The lower-case hexadecimal encoding of the component's X.509 certificate's fingerprint
		 * The SignedHeaders list calculated in Task 1
		 * The Base64 encoding of the Signature calculated in Task 2
		 * 
		 * Signature = RSA(privkey, SHA256(CanonicalRequest))
		 * 
		 * CanonicalRequest =
		 * 	HTTPRequestMethod + '\n' +
		 * 	CanonicalURI + '\n' +
		 * 	CanonicalQueryString + '\n' +
		 *	CanonicalHeaders + '\n' +
		 *	SignedHeaders
			  	
		 * @param httpRequest
		 * @param authMap
		 * @throws AccessDeniedException
		 */
		public static void authenticate(MappingHttpRequest httpRequest, Map<AuthorizationField, String> authMap) throws AccessDeniedException {		
			if(authMap ==  null || !EUCA_AUTH_TYPE.equals(authMap.get(AuthorizationField.Type))) {
				throw new AccessDeniedException("Mismatch between expected and found authentication types");
			}
			
			//Remove unsigned headers so they are not consumed accidentally later
			cleanHeaders(httpRequest, authMap.get(AuthorizationField.SignedHeaders));
			
			//Must contain a date of some sort signed
			checkDate(httpRequest);
			
			//Must be certificate signed
			String certString = null;
			if(authMap.containsKey(AuthorizationField.CertFingerPrint)) {
				certString = authMap.get(AuthorizationField.CertFingerPrint);
			}
			else {
				throw new AccessDeniedException("Invalid Authorization Header");
			}
			
			String verb = httpRequest.getMethod().getName();
			String canonicalURI = getCanonicalURI(httpRequest);
			String canonicalQueryString = getCanonicalQueryString(httpRequest);
			String canonicalHeaders = getCanonicalHeaders(httpRequest, authMap.get(AuthorizationField.SignedHeaders));
			String signedHeaders = getSignedHeaders(httpRequest, authMap);
			
			String data = verb + "\n" + canonicalURI + "\n" + canonicalQueryString + "\n" + canonicalHeaders + "\n" + signedHeaders;
			String AWSAccessKeyID = httpRequest.getAndRemoveHeader(SecurityParameter.AWSAccessKeyId.toString());
			String signature = authMap.get(AuthorizationField.Signature);
			
			try {
				SecurityContext.getLoginContext(new WalrusWrappedComponentCredentials(httpRequest.getCorrelationId(), data, AWSAccessKeyID, signature, certString)).login();
			} catch(Exception ex) {
				LOG.error(ex);
				throw new AccessDeniedException(ex.getMessage());
			}
			
		}
		
		private static void checkDate(MappingHttpRequest httpRequest) throws AccessDeniedException {
			String date;
			String verifyDate;
			if(httpRequest.containsHeader("x-amz-date")) {
				date = "";
				verifyDate = httpRequest.getHeader("x-amz-date");
			} else {
				date =  httpRequest.getHeader(SecurityParameter.Date.toString());
				verifyDate = date;
				if(date == null || date.length() <= 0)
					throw new AccessDeniedException("User authentication failed. Date must be specified.");
			}

			try {				
				ArrayList<String> formats = new ArrayList<String>();
				formats.add(ISO_8601_FORMAT);
				Date dateToVerify = DateUtil.parseDate(verifyDate, formats);
				Date currentDate = new Date();
				if(Math.abs(currentDate.getTime() - dateToVerify.getTime()) > WalrusProperties.EXPIRATION_LIMIT) {
					LOG.error("Incoming WalrusBackend message is expired. Current date: " + currentDate.toString() + " Message's Verification Date: " + dateToVerify.toString());
					throw new AccessDeniedException("Message expired. Sorry.");
				}
			} catch(DateParseException ex) {
				LOG.error("WalrusBackend cannot parse date: " + verifyDate);
				throw new AccessDeniedException("Unable to parse date.");
			}
		}
		
		/**
		 * Gets the signed header string for Euca2 auth.
		 * @param httpRequest
		 * @param authMap
		 * @return
		 * @throws AccessDeniedException
		 */
		private static String getSignedHeaders(MappingHttpRequest httpRequest, Map<AuthorizationField, String> authMap) throws AccessDeniedException {
			String signedHeaders = authMap.get(AuthorizationField.SignedHeaders);
			if(signedHeaders != null) return signedHeaders.trim();
			return "";
		}
		/**
		 * Returns the canonical URI for euca2 auth, just the path from the end of the host header value to the first ?
		 * @param httpRequest
		 * @return
		 * @throws AccessDeniedException
		 */
		private static String getCanonicalURI(MappingHttpRequest httpRequest) throws AccessDeniedException {
			String addr = httpRequest.getUri();
			String targetHost = httpRequest.getHeader(HttpHeaders.Names.HOST);
			if(targetHost != null && targetHost.contains(".walrus")) {
				String bucket = targetHost.substring(0, targetHost.indexOf(".walrus"));
				addr = "/" + bucket + addr;
			}
			String[] addrStrings = addr.split("\\?");
			String addrString = addrStrings[0];
			return addrString;
		}
		
		/**
		 * Get the canonical headers list, a string composed of sorted headers and values, taken from the list of signed headers given by the request
		 * @param httpRequest
		 * @return
		 * @throws AccessDeniedException
		 */
		private static String getCanonicalHeaders(MappingHttpRequest httpRequest, String signedHeaders) throws AccessDeniedException {
			String[] signedHeaderArray = signedHeaders.split(";");
			StringBuilder canonHeader = new StringBuilder();
			boolean foundHost = false;
			for(String headerName : signedHeaderArray) {			
				String headerNameString = headerName.toLowerCase().trim();
				if("host".equals(headerNameString)) {
					foundHost = true;
				}
				String value =  httpRequest.getHeader(headerName);
				if(value != null) {
					value = value.trim();
					String[] parts = value.split("\n");
					value = "";
					for(String part: parts) {
						part = part.trim();
						value += part + " ";
					}
					value = value.trim();
				}
				else {
					value = "";
				}
				canonHeader.append(headerNameString).append(":").append(value).append('\n');
			}
			
			if(!foundHost) {
				throw new AccessDeniedException("Host header not found when canonicalizing headers");
			}
			
			return canonHeader.toString().trim();
		}		

		/**
		 * Gets Euca2 signing canonical query string.
		 * @param httpRequest
		 * @return
		 * @throws AccessDeniedException
		 */
		private static String getCanonicalQueryString(MappingHttpRequest httpRequest) throws AccessDeniedException {
			String addr = httpRequest.getUri();
			String[] addrStrings = addr.split("\\?");
			StringBuilder addrString = new StringBuilder();
			
			NavigableSet<String> sortedParams = new TreeSet<String>( );
			Map<String, String> params = httpRequest.getParameters();
			if(params == null) {
				return "";
			}
			
			sortedParams.addAll(params.keySet());
			
			String key = null;
			while((key = sortedParams.pollFirst()) != null) {
				addrString.append(key).append('=').append(params.get(key)).append('&');
			}
			
			if(addrString.length() > 0) {
				addrString.deleteCharAt(addrString.length() - 1); //delete trailing '&';
			}
			
			return addrString.toString();
		}
		
		/**
		 * Removes all headers that are not in the signed-headers list. This prevents potentially modified headers from being used by later stages.
		 * @param httpRequest
		 * @param signedHeaders - semicolon delimited list of header names
		 */
		private static void cleanHeaders(MappingHttpRequest httpRequest, String signedHeaders) {
			if(Strings.isNullOrEmpty(signedHeaders)) {
				//Remove all headers.
				signedHeaders = "";
			}
						
			//Remove ones not found in the list
			Set<String> signedNames = new TreeSet<String>();
			String[] names = signedHeaders.split(";");
			for(String n : names) {
				signedNames.add(n.toLowerCase());
			}
			
			signedNames.addAll(SAFE_HEADER_SET);
			
			Set<String> removeSet = new TreeSet<String>();
			for(String headerName : httpRequest.getHeaderNames()) {
				if(!signedNames.contains(headerName.toLowerCase())) {
					removeSet.add(headerName);
				}
			}
			
			for(String headerName : removeSet) {				
				httpRequest.removeHeader(headerName);
			}
		}
		
	} //End class EucaAuthentication
	
	private static class S3Authentication {
		/**
		 * Authenticate using S3-spec REST authentication
		 * @param httpRequest
		 * @param authMap
		 * @throws AccessDeniedException
		 */
		private static void authenticate(MappingHttpRequest httpRequest, Map<AuthorizationField, String> authMap) throws AccessDeniedException {
			if(!authMap.get(AuthorizationField.Type).equals(AWS_AUTH_TYPE)) {
				throw new AccessDeniedException("Mismatch between expected and found authentication types");
			}
			
			//Standard S3 authentication signed by SecretKeyID
			String verb = httpRequest.getMethod().getName();
			String date = getDate(httpRequest);
			String addrString = getS3AddressString(httpRequest);
			String content_md5 = httpRequest.getHeader("Content-MD5");
			content_md5 = content_md5 == null ? "" : content_md5;
			String content_type = httpRequest.getHeader(WalrusProperties.CONTENT_TYPE);
			content_type = content_type == null ? "" : content_type;
			String securityToken = httpRequest.getHeader(WalrusProperties.X_AMZ_SECURITY_TOKEN);
			String canonicalizedAmzHeaders = getCanonicalizedAmzHeaders(httpRequest);
			String data = verb + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n" + canonicalizedAmzHeaders + addrString;
			String accessKeyId = authMap.get(AuthorizationField.AccessKeyId);
			String signature = authMap.get(AuthorizationField.Signature);
			
			try {
				SecurityContext.getLoginContext(new WalrusWrappedCredentials(httpRequest.getCorrelationId(), data, accessKeyId, signature, securityToken)).login();
			} catch(Exception ex) {
				//Try stripping of the '/services/Walrus' portion of the addrString and retry the signature calc
                String servicePath = ComponentIds.lookup(WalrusBackend.class).getServicePath();
				if(addrString.startsWith(servicePath)) {
					try {
						String modifiedAddrString = addrString.replaceFirst(servicePath, "");
						data = verb + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n" + canonicalizedAmzHeaders + modifiedAddrString;
						SecurityContext.getLoginContext(new WalrusWrappedCredentials(httpRequest.getCorrelationId(), data, accessKeyId, signature, securityToken)).login();
					} catch(Exception ex2) {
						LOG.error(ex2);
						throw new AccessDeniedException(ex2.getMessage());					
					}
				} else {
					LOG.error(ex);
					throw new AccessDeniedException(ex.getMessage());
				}
			}
		}

		/**
		 * See if the expires string indicates the message is expired.
		 * @param expires
		 * @return
		 */
		private static boolean checkExpires(String expires) {
			Long expireTime = Long.parseLong(expires);
			Long currentTime = new Date().getTime() / 1000;
			if(currentTime > expireTime)
				return false;
			return true;
		}
		
		/**
		 * Gets the date for S3-spec authentication
		 * @param httpRequest
		 * @return
		 * @throws AccessDeniedException
		 */
		private static String getDate(MappingHttpRequest httpRequest) throws AccessDeniedException {
			String date;
			String verifyDate;
			if(httpRequest.containsHeader("x-amz-date")) {
				date = "";
				verifyDate = httpRequest.getHeader("x-amz-date");
			} else {
				date =  httpRequest.getAndRemoveHeader(SecurityParameter.Date.toString());
				verifyDate = date;
				if(date == null || date.length() <= 0)
					throw new AccessDeniedException("User authentication failed. Date must be specified.");
			}

			try {
				Date dateToVerify = DateUtil.parseDate(verifyDate);
				Date currentDate = new Date();
				if(Math.abs(currentDate.getTime() - dateToVerify.getTime()) > WalrusProperties.EXPIRATION_LIMIT) {
					LOG.error("Incoming WalrusBackend message is expired. Current date: " + currentDate.toString() + " Message's Verification Date: " + dateToVerify.toString());
					throw new AccessDeniedException("Message expired. Sorry.");
				}
			} catch(Exception ex) {
				LOG.error("Cannot parse date: " + verifyDate);
				throw new AccessDeniedException("Unable to parse date.");
			}

			return date;
		}
		
		private static String getCanonicalizedAmzHeaders(MappingHttpRequest httpRequest) {
			String result = "";
			Set<String> headerNames = httpRequest.getHeaderNames();

			TreeMap<String,String> amzHeaders = new TreeMap<String, String>();
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
		
		//Old method for getting signature info from Auth header
		private static String[] getSigInfo (String auth_part) {
			int index = auth_part.lastIndexOf(" ");
			String sigString = auth_part.substring(index + 1);
			return sigString.split(":");
		}
		
		/**
		 * AWS S3-spec address string, which includes the query parameters
		 * @param httpRequest
		 * @return
		 * @throws AccessDeniedException
		 */
		private static String getS3AddressString(MappingHttpRequest httpRequest) throws AccessDeniedException {
			String addr = httpRequest.getUri();
			String targetHost = httpRequest.getHeader(HttpHeaders.Names.HOST);
			if(targetHost.contains(".walrus")) {
				String bucket = targetHost.substring(0, targetHost.indexOf(".walrus"));
				addr = "/" + bucket + addr;
			}
			String[] addrStrings = addr.split("\\?");
			StringBuilder addrString = new StringBuilder(addrStrings[0]);
			
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
									addrString.append("?");
									first = false;
								}
								else {
									addrString.append("&");
								}
								addrString.append(subResource.toString()).append((pair.length > 1 ? "=" + WalrusUtil.URLdecode(pair[1]) : ""));							
							}
						}
					}					
				} catch(UnsupportedEncodingException e) {
					throw new AccessDeniedException("Could not verify request. Failed url decoding query parameters: " + e.getMessage());
				}
			}		
			return addrString.toString();
		}
				
	} //End class S3Authentication	
	
	/**
	 * Authentication Handler for Walrus REST requests (POST method and SOAP are processed using different handlers)
	 * @param httpRequest
	 * @throws AccessDeniedException
	 */
	public void handle(MappingHttpRequest httpRequest) throws AccessDeniedException {
		//Clean up the headers such that no duplicates may exist etc.
		//sanitizeHeaders(httpRequest);
		Map<String,String> parameters = httpRequest.getParameters();

		if(httpRequest.containsHeader(SecurityParameter.Authorization.toString())) {							
			String authHeader = httpRequest.getAndRemoveHeader(SecurityParameter.Authorization.toString());
			Map<AuthorizationField, String> authMap = processAuthorizationHeader(authHeader);

			if(AWS_AUTH_TYPE.equals(authMap.get(AuthorizationField.Type))) {
				//Normally signed request using AccessKeyId/SecretKeyId pair
				S3Authentication.authenticate(httpRequest, authMap);
                return;
			}
		}
        throw new AccessDeniedException("Invalid Authentication Scheme");
	}
	
	public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent exceptionEvent ) throws Exception {
		LOG.info("[exception " + exceptionEvent + "]");
		final HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR );
		DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), ctx.getChannel().getCloseFuture(), response, null );
		ctx.sendDownstream( newEvent );
		newEvent.getFuture( ).addListener( ChannelFutureListener.CLOSE );
	}
}
