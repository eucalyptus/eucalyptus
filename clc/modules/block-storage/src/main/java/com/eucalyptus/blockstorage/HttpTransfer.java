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

package com.eucalyptus.blockstorage;

import java.net.URL;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.auth.SystemCredentials.Credentials;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Strings;

import java.util.Arrays;


//All HttpTransfer operations should be called asynchronously. The operations themselves are synchronous.
public class HttpTransfer {	
	private static Logger LOG = Logger.getLogger(HttpTransfer.class);
	protected HttpClient httpClient;
	protected HttpMethodBase method;
		
	protected static final String EUCA2_AUTH_ID = "EUCA2-RSA-SHA256";
	protected static final String EUCA2_AUTH_HEADER_NAME = "Authorization";
	protected static final String ISO_8601_FORMAT = "yyyyMMdd'T'HHmmss'Z'"; //Use the ISO8601 format

	/**
	 * Calculates and sets the Authorization header value for the request using the EucaRSA-V2 signing algorithm
	 * Algorithm Overview:
	 * 
	 * 1. Generate the canonical Request
	 *  a.) CanonicalRequest =
	 * 			HTTPRequestMethod + '\n' +
	 * 			CanonicalURI + '\n' +
	 * 			CanonicalQueryString + '\n' +
	 * 			CanonicalHeaders + '\n' +
	 * 			SignedHeaders
	 * 	b.) Where CanonicalURI = 
	 * 	c.) Where CanonicalQueryString = 
	 *	d.) Where CanonicalHeaders =  sorted (by lowercased header name) ';' delimited list of <lowercase(headername)>:<value> items
	 *	e.) Where SignedHeaders = sorted, ';' delimited list of headers in CanonicalHeaders
	 * 
	 * 2. Signature = RSA(privkey, SHA256(CanonicalRequest))
	 * 
	 * 3. Add an Authorization HTTP header to the request that contains the following strings, separated by spaces:
	 * EUCA2-RSA-SHA256
	 * The lower-case hexadecimal encoding of the component's X.509 certificate's md5 fingerprint
	 * The SignedHeaders list calculated in Task 1
	 * The Base64 encoding of the Signature calculated in Task 2
	 * 
	 * @param httpBaseRequest -- the request, the 'Authorization' header will be added to the request
	 */
	public static void signEucaInternal(HttpMethodBase httpBaseRequest) {
		StringBuilder canonicalRequest = new StringBuilder();
		String canonicalURI = null;		
		String verb = httpBaseRequest.getName();
		canonicalURI = httpBaseRequest.getPath();

		String canonicalQuery = calcCanonicalQuery(httpBaseRequest);
		String[] processedHeaders = getCanonicalAndSignedHeaders(httpBaseRequest);
		String canonicalHeaders = processedHeaders[0];
		String signedHeaders = processedHeaders[1];
		
		canonicalRequest.append(verb).append('\n');
		canonicalRequest.append(canonicalURI).append('\n');
		canonicalRequest.append(canonicalQuery).append('\n');
		canonicalRequest.append(canonicalHeaders).append('\n');
		canonicalRequest.append(signedHeaders);

		StringBuilder authHeader = new StringBuilder(EUCA2_AUTH_ID);
		String signature = null;
		String fingerprint = null;
		try {
			Credentials ccCreds = SystemCredentials.lookup(Storage.class);
			PrivateKey ccPrivateKey = ccCreds.getPrivateKey();
			fingerprint = ccCreds.getCertFingerprint();
			Signature sign = Signature.getInstance("SHA256withRSA");
			sign.initSign(ccPrivateKey);
			LOG.debug("Signing canonical request: " + canonicalRequest.toString());
			sign.update(canonicalRequest.toString().getBytes());
			byte[] sig = sign.sign();
			signature = new String(Base64.encode(sig));
		} catch(Exception ex) {
			LOG.error("Signing error while signing request", ex);
		}

		authHeader.append(" ").append(fingerprint.toLowerCase()).append(" ").append(signedHeaders.toString()).append(" ").append(signature);		
		httpBaseRequest.addRequestHeader(EUCA2_AUTH_HEADER_NAME, authHeader.toString());
	}

	/**
	 * Constructs and returns the canonicalQuery string per EucaRSA-V2 specs.
	 * @param httpBaseRequest
	 * @return
	 */
	private static String calcCanonicalQuery(HttpMethodBase httpBaseRequest) {
			StringBuilder canonicalQuery = new StringBuilder();
			//Sort query elements, assume all values are already URL encoded
			String tmpQuery = httpBaseRequest.getQueryString();
			HashMap<String, String> parameters = new HashMap<String,String>();
			if(!Strings.isNullOrEmpty(tmpQuery)) {
				String[] rawQueryParams = tmpQuery.split("&");
				String[] queryParamNames = new String[rawQueryParams.length];
				String[] tmpKV = null;
				int i = 0;		
				for(String paramKV : rawQueryParams) {
					tmpKV = paramKV.split("=");
					queryParamNames[i++] = tmpKV[0];
					if(tmpKV.length == 2) {				
						parameters.put(tmpKV[0],tmpKV[1]);
					}
					else {
						parameters.put(tmpKV[0], "");
					}			
				}
				
				Arrays.sort(queryParamNames);
				for(String paramName : queryParamNames) {
					canonicalQuery.append(paramName).append('=');
					if(parameters.get(paramName) != null) {
						canonicalQuery.append(parameters.get(paramName));
					} else {
						//Single key, no value
						canonicalQuery.append("");
					}
					canonicalQuery.append('&');
				}
				
				if(canonicalQuery.length() > 0) {
					canonicalQuery.deleteCharAt(canonicalQuery.length() -1); //Delete the trailing '&'
				}		
			}
			return canonicalQuery.toString();
	}
	
	/**
	 * Calculates the canonical and signed header strings in a single pass, done in one pass for efficiency
	 * @param httpBaseRequest
	 * @return Array of 2 elements, first element is the canonicalHeader string, second element is the signedHeaders string
	 */
	private static String[] getCanonicalAndSignedHeaders(HttpMethodBase httpBaseRequest) {
		/*
		 * The host header is required for EucaV2 signing, but it is not constructed by the HttpClient until the method is executed.
		 * So, here we add a header with the same value and name so that we can do the proper sigining, but know that this value will
		 * be overwritten when HttpMethodBase is executed to send the request.
		 * 
		 * This code is specific to the jakarta commons httpclient because that client will set the host header to hostname:port rather than
		 * just hostname.
		 * 
		 * Supposedly you can force the value of the Host header with: httpBaseRequest.getParams().setVirtualHost("hostname"), but that was not successful
		 */
		try {
			httpBaseRequest.addRequestHeader("Host", httpBaseRequest.getURI().getHost() + ":" + httpBaseRequest.getURI().getPort());
		} catch(URIException e) {
			LOG.error("Could not add Host header for canonical headers during authorization header creation in HTTP client: ",e);
			return null;
		}
		
		Header[] headers = httpBaseRequest.getRequestHeaders();
		StringBuilder signedHeaders = new StringBuilder();
		StringBuilder canonicalHeaders = new StringBuilder(); 
		
		if(headers != null) {
			Arrays.sort(headers, new Comparator<Header>() {
				@Override
				public int compare(Header arg0, Header arg1) {
					return arg0.getName().toLowerCase().compareTo(arg1.getName().toLowerCase());
				}
				
			});
			
			for(Header header : headers) {
				//Add to the signed headers
				signedHeaders.append(header.getName().toLowerCase()).append(';');
				//Add the name and value to the canonical header
				canonicalHeaders.append(header.getName().toLowerCase()).append(':').append(header.getValue().trim()).append('\n');
			}
			
			if(signedHeaders.length() > 0) {
				signedHeaders.deleteCharAt(signedHeaders.length() - 1); //Delete the trailing semi-colon
			}
			
			if(canonicalHeaders.length() > 0) {
				canonicalHeaders.deleteCharAt(canonicalHeaders.length() -1); //Delete the trialing '\n' just to make things clear and consistent
			}			
		}
		String[] result = new String[2];
		result[0] = canonicalHeaders.toString();
		result[1] = signedHeaders.toString();
		return result;
	}
	
	/**
	 * Constructs the requested method, optionally signing the request via EucaRSA-V2 signing method if signRequest=true
	 * Signing the request can be done later as well by explicitly calling signEucaInternal() and passing it the output of this method.
	 * That case is useful for constructing the request and then adding headers explicitly before signing takes place.
	 * @param verb - The HTTP verb GET|PUT|POST|DELETE|UPDATE
	 * @param addr - THe destination address for the request
	 * @param eucaOperation - The EucaOperation, if any (e.g. StoreSnapshot, GetWalrusSnapshot, or other values from WalrusProperties.StorageOperations)
	 * @param eucaHeader - The Euca Header value, if any. This is not typically used.
	 * @param signRequest - Determines if the request is signed at construction time or must be done explicitly later (boolean)
	 * @return
	 */
	public HttpMethodBase constructHttpMethod(String verb, String addr, String eucaOperation, String eucaHeader, boolean signRequest) {
		String date = DateUtil.formatDate(new Date(),ISO_8601_FORMAT);
		//String date = new Date().toString();
		String httpVerb = verb;
		String addrPath = null;
		java.net.URI addrUri = null;
		try {
			addrUri = new URL(addr).toURI();
			addrPath = addrUri.getPath().toString();
			String query = addrUri.getQuery();
			if(query != null) {
				addrPath += "?" + query;
			}
		} catch(Exception ex) {
			LOG.error(ex, ex);
			return null;
		}

		HttpMethodBase method = null;
		if(httpVerb.equals("PUT")) {
			method = new  PutMethodWithProgress(addr);
		} else if(httpVerb.equals("DELETE")) {
			method = new DeleteMethod(addr);
		} else  {
			method = new GetMethod(addr);
		} 
		
		method.setRequestHeader("Date", date);
		//method.setRequestHeader("Expect", "100-continue");

		method.setRequestHeader(StorageProperties.EUCALYPTUS_OPERATION, eucaOperation);
		if(eucaHeader != null) {
			method.setRequestHeader(StorageProperties.EUCALYPTUS_HEADER, eucaHeader);
		}
		
		if(signRequest) {
			signEucaInternal(method);
		}
		
		return method;
	}
	
	public void abortTransfer() throws EucalyptusCloudException {
		if(httpClient != null && method != null) {
			method.abort();
			method.releaseConnection();
		}
	}

	public HttpTransfer() {}
}
