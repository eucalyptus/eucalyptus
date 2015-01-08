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

package com.eucalyptus.objectstorage.pipeline.handlers;

import com.eucalyptus.auth.login.SecurityContext;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidAccessKeyIdException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidAddressingHeaderException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidSecurityException;
import com.eucalyptus.objectstorage.exceptions.s3.MissingSecurityHeaderException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.exceptions.s3.SignatureDoesNotMatchException;
import com.eucalyptus.objectstorage.pipeline.auth.ObjectStorageWrappedCredentials;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.server.Statistics;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;


@ChannelPipelineCoverage("one")
public class ObjectStorageAuthenticationHandler extends MessageStackHandler {
    private static Logger LOG = Logger.getLogger(ObjectStorageAuthenticationHandler.class);
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
     * <p/>
     * TODO: Also, should convert all headers to lower-case for consistent processing later. This is okay since headers are case-insensitive.
     * <p/>
     * in HTTP
     *
     * @param httpRequest
     */
    private static void canonicalizeHeaders(MappingHttpRequest httpRequest) {
        //Iterate through headers and find duplicates, concatenate their values together and remove from
        // request as we find them.
        TreeMap<String, String> headerMap = new TreeMap<String, String>();
        String value = null;

        //Construct a map of the normalized headers, cannot modify in-place since
        // conconcurrent-modify exception may result
        for (String header : httpRequest.getHeaderNames()) {
            //TODO: zhill, put in the map in lower-case form.
            headerMap.put(header, Joiner.on(',').join(httpRequest.getHeaders(header)));
        }

        //Remove *all* headers
        httpRequest.clearHeaders();

        //Add the normalized headers back into the request
        for (String foundHeader : headerMap.keySet()) {
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
        for (String header : httpRequest.getHeaderNames()) {
            hdrList = httpRequest.getHeaders(header);

            //Only address the specific case where there is exactly one identical copy of the header
            if (hdrList != null && hdrList.size() == 2 && hdrList.get(0).equals(hdrList.get(1))) {
                foundDup = true;
                fixedHeaders.put(header, Lists.newArrayList(hdrList.get(0)));
            } else {
                fixedHeaders.put(header, hdrList);
            }
        }

        if (foundDup) {
            LOG.debug("Found duplicate headers in: " + httpRequest.logMessage());
            httpRequest.clearHeaders();

            for (Map.Entry<String, List<String>> e : fixedHeaders.entrySet()) {
                for (String v : e.getValue()) {
                    httpRequest.addHeader(e.getKey(), v);
                }
            }
        }
    }

	@Override
	public void incomingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
		if (event.getMessage() instanceof MappingHttpRequest) {
			MappingHttpRequest httpRequest = (MappingHttpRequest) event.getMessage();

			removeDuplicateHeaderValues(httpRequest);
			// Consolidate duplicates, etc.

			canonicalizeHeaders(httpRequest);
			handle(httpRequest);
		}
	}

	// Overriding this method to ensure that the message is passed to the next stage in the pipeline only if it passes authentication.
	@Override
	public void handleUpstream(final ChannelHandlerContext ctx, final ChannelEvent channelEvent) throws Exception {
		if (channelEvent instanceof MessageEvent) {
			try {
				final MessageEvent msgEvent = (MessageEvent) channelEvent;
				Callable<Long> stat = Statistics.startUpstream(ctx.getChannel(), this);
				this.incomingMessage(ctx, msgEvent);
				stat.call();
				ctx.sendUpstream(channelEvent);
			} catch (Throwable e) {
				Channels.fireExceptionCaught(ctx, e);
			}
		} else {
			ctx.sendUpstream(channelEvent);
		}
	}


    /**
     * Process the authorization header
     *
     * @param authorization
     * @return
     * @throws AccessDeniedException
     */
    public static Map<AuthorizationField, String> processAuthorizationHeader(String authorization) throws AccessDeniedException {
        if (Strings.isNullOrEmpty(authorization)) {
            return null;
        }

        HashMap<AuthorizationField, String> authMap = new HashMap<AuthorizationField, String>();
        String[] components = authorization.split(" ");

        if (components.length < 2) {
            throw new AccessDeniedException("Invalid authoriztion header");
        }

        if (AWS_AUTH_TYPE.equals(components[0]) && components.length == 2) {
            //Expect: components[1] = <AccessKeyId>:<Signature>
            authMap.put(AuthorizationField.Type, AWS_AUTH_TYPE);
            String[] signatureElements = components[1].split(":");
            authMap.put(AuthorizationField.AccessKeyId, signatureElements[0]);
            authMap.put(AuthorizationField.Signature, signatureElements[1]);
        } else if (EUCA_AUTH_TYPE.equals(components[0]) && components.length == 4) {
            //Expect: components[0] = EUCA2-RSA-SHA256 components[1] = <fingerprint of signing certificate> components[2] = <list of signed headers> components[3] = <Signature>
            authMap.put(AuthorizationField.Type, EUCA_AUTH_TYPE);
            authMap.put(AuthorizationField.CertFingerPrint, components[1].trim());
            authMap.put(AuthorizationField.SignedHeaders, components[2].trim());
            authMap.put(AuthorizationField.Signature, components[3].trim());
        } else if (EUCA_OLD_AUTH_TYPE.equals(components[0]) && components.length == 1) {
            authMap.put(AuthorizationField.Type, EUCA_OLD_AUTH_TYPE);
        } else {
            throw new AccessDeniedException("Invalid authorization header");
        }

        return authMap;
    }

    static class S3Authentication {
        /**
         * Authenticate using S3-spec REST authentication
         *
         * @param httpRequest
         * @param authMap
         * @throws AccessDeniedException
         */

        static void authenticate(MappingHttpRequest httpRequest, Map<AuthorizationField, String> authMap) throws S3Exception {
            if (!authMap.get(AuthorizationField.Type).equals(AWS_AUTH_TYPE)) {
                throw new InvalidSecurityException("Mismatch between expected and found authentication types");
            }

            //Standard S3 authentication signed by SecretKeyID
            String verb = httpRequest.getMethod().getName();
            String date = getDate(httpRequest);
            String addrString = getS3AddressString(httpRequest, true);
            String content_md5 = httpRequest.getHeader("Content-MD5");
            content_md5 = content_md5 == null ? "" : content_md5;
            String content_type = httpRequest.getHeader(HttpHeaders.Names.CONTENT_TYPE);
            content_type = content_type == null ? "" : content_type;
            String securityToken = httpRequest.getHeader(ObjectStorageProperties.X_AMZ_SECURITY_TOKEN);
            String canonicalizedAmzHeaders = getCanonicalizedAmzHeaders(httpRequest);
            String data = verb + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n" + canonicalizedAmzHeaders + addrString;
            String accessKeyId = authMap.get(AuthorizationField.AccessKeyId);
            String signature = authMap.get(AuthorizationField.Signature);

            try {
                SecurityContext.getLoginContext(new ObjectStorageWrappedCredentials(httpRequest.getCorrelationId(), data, accessKeyId, signature, securityToken)).login();
            } catch (LoginException ex) {
                if (ex.getMessage().contains("The AWS Access Key Id you provided does not exist in our records")) {
                    throw new InvalidAccessKeyIdException(accessKeyId);
                }

                //Try using the '/services/ObjectStorage' portion of the addrString and retry the signature calc
                if (httpRequest.getUri().startsWith(ComponentIds.lookup(ObjectStorage.class).getServicePath()) || 
                		httpRequest.getUri().startsWith(ObjectStorageProperties.LEGACY_WALRUS_SERVICE_PATH)) {
                    try {
                        String modifiedAddrString = getS3AddressString(httpRequest, false);
                        data = verb + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n" + canonicalizedAmzHeaders + modifiedAddrString;
                        SecurityContext.getLoginContext(new ObjectStorageWrappedCredentials(httpRequest.getCorrelationId(), data, accessKeyId, signature, securityToken)).login();
                    } catch (S3Exception ex2) {
                        LOG.debug("CorrelationId: " + httpRequest.getCorrelationId() + " Authentication failed due to signature match issue:", ex2);
                        throw ex2;
                    } catch (Exception ex2) {
                        LOG.debug("CorrelationId: " + httpRequest.getCorrelationId() + " Authentication failed due to signature match issue:", ex2);
                        throw new SignatureDoesNotMatchException(data);
                    }
                } else {
                    LOG.debug("CorrelationId: " + httpRequest.getCorrelationId() + " Authentication failed due to signature mismatch:", ex);
                    throw new SignatureDoesNotMatchException(data);
                }
            } catch (Exception e) {
                LOG.warn("CorrelationId: " + httpRequest.getCorrelationId() + " Unexpected failure trying to authenticate request", e);
                throw new InternalErrorException(e);
            }
        }

        /**
         * Authenticate using S3-spec query string authentication
         *
         * @param httpRequest
         * @throws AccessDeniedException
         */
        static void authenticateQueryString(MappingHttpRequest httpRequest) throws S3Exception {
            //Standard S3 query string authentication
            Map<String, String> parameters = httpRequest.getParameters();
            String verb = httpRequest.getMethod().getName();
            String content_md5 = httpRequest.getHeader("Content-MD5");
            content_md5 = content_md5 == null ? "" : content_md5;
            String content_type = httpRequest.getHeader(HttpHeaders.Names.CONTENT_TYPE);
            content_type = content_type == null ? "" : content_type;
            String addrString = getS3AddressString(httpRequest, true);
            String accesskeyid = parameters.remove(SecurityParameter.AWSAccessKeyId.toString());

            try {
                //Parameter url decode happens during MappingHttpRequest construction.
                String signature = parameters.remove(SecurityParameter.Signature.toString());
                if (signature == null) {
                    throw new InvalidSecurityException("No signature found");
                }
                String expires = parameters.remove(SecurityParameter.Expires.toString());
                if (expires == null) {
                    throw new InvalidSecurityException("Expiration parameter must be specified.");
                }
                String securityToken = parameters.get(SecurityParameter.SecurityToken.toString());

                if (checkExpires(expires)) {
                    String canonicalizedAmzHeaders = getCanonicalizedAmzHeaders(httpRequest);
                    String stringToSign = verb + "\n" + content_md5 + "\n" + content_type + "\n" + Long.parseLong(expires) + "\n" + canonicalizedAmzHeaders + addrString;
                    try {
                        SecurityContext.getLoginContext(new ObjectStorageWrappedCredentials(httpRequest.getCorrelationId(), stringToSign, accesskeyid, signature, securityToken)).login();
                    } catch (Exception ex) {
                        //Try adding back the '/services/objectStorage' portion of the addrString and retry the signature calc
                        if (httpRequest.getUri().startsWith(ComponentIds.lookup(ObjectStorage.class).getServicePath()) || 
                        		httpRequest.getUri().startsWith(ObjectStorageProperties.LEGACY_WALRUS_SERVICE_PATH)) {
                            try {
                                String modifiedAddrString = getS3AddressString(httpRequest, false);
                                stringToSign = verb + "\n" + content_md5 + "\n" + content_type + "\n" + Long.parseLong(expires) + "\n" + canonicalizedAmzHeaders + modifiedAddrString;
                                SecurityContext.getLoginContext(new ObjectStorageWrappedCredentials(httpRequest.getCorrelationId(), stringToSign, accesskeyid, signature, securityToken)).login();
                            } catch (Exception ex2) {
                                LOG.error("CorrelationId: " + httpRequest.getCorrelationId() + " authentication failed due to signature mismatch:", ex2);
                                throw new SignatureDoesNotMatchException(stringToSign);
                            }
                        } else {
                            LOG.error("CorrelationId: " + httpRequest.getCorrelationId() + " authentication failed due to signature mismatch:", ex);
                            throw new SignatureDoesNotMatchException(stringToSign);
                        }
                    }
                } else {
                    throw new AccessDeniedException("Cannot process request. Expired.");
                }
            } catch (Exception ex) {
                throw new AccessDeniedException("Could not verify request " + ex.getMessage());
            }
        }

        /**
         * See if the expires string indicates the message is expired.
         *
         * @param expires
         * @return
         */
        static boolean checkExpires(String expires) {
            Long expireTime = Long.parseLong(expires);
            Long currentTime = new Date().getTime() / 1000;
            if (currentTime > expireTime)
                return false;
            return true;
        }

        /**
         * Gets the date for S3-spec authentication
         *
         * @param httpRequest
         * @return
         * @throws AccessDeniedException
         */
        static String getDate(MappingHttpRequest httpRequest) throws AccessDeniedException {
            String date;
            String verifyDate;
            if (httpRequest.containsHeader("x-amz-date")) {
                date = "";
                verifyDate = httpRequest.getHeader("x-amz-date");
            } else {
                date = httpRequest.getAndRemoveHeader(SecurityParameter.Date.toString());
                verifyDate = date;
                if (date == null || date.length() <= 0)
                    throw new AccessDeniedException("User authentication failed. Date must be specified.");
            }

            try {
                Date dateToVerify = DateUtil.parseDate(verifyDate);
                Date currentDate = new Date();
                if (Math.abs(currentDate.getTime() - dateToVerify.getTime()) > ObjectStorageProperties.EXPIRATION_LIMIT) {
                    LOG.error("Incoming ObjectStorage message is expired. Current date: " + currentDate.toString() + " Message's Verification Date: " + dateToVerify.toString());
                    throw new AccessDeniedException("Message expired. Sorry.");
                }
            } catch (Exception ex) {
                LOG.error("Cannot parse date: " + verifyDate);
                throw new AccessDeniedException("Unable to parse date.");
            }

            return date;
        }

        private static String getCanonicalizedAmzHeaders(MappingHttpRequest httpRequest) {
            String result = "";
            Set<String> headerNames = httpRequest.getHeaderNames();

            TreeMap<String, String> amzHeaders = new TreeMap<String, String>();
            for (String headerName : headerNames) {
                String headerNameString = headerName.toLowerCase().trim();
                if (headerNameString.startsWith("x-amz-")) {
                    String value = httpRequest.getHeader(headerName).trim();
                    String[] parts = value.split("\n");
                    value = "";
                    for (String part : parts) {
                        part = part.trim();
                        value += part + " ";
                    }
                    value = value.trim();
                    if (amzHeaders.containsKey(headerNameString)) {
                        String oldValue = (String) amzHeaders.remove(headerNameString);
                        oldValue += "," + value;
                        amzHeaders.put(headerNameString, oldValue);
                    } else {
                        amzHeaders.put(headerNameString, value);
                    }
                }
            }

            Iterator<String> iterator = amzHeaders.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = (String) amzHeaders.get(key);
                result += key + ":" + value + "\n";
            }
            return result;
        }

        //Old method for getting signature info from Auth header
        static String[] getSigInfo(String auth_part) {
            int index = auth_part.lastIndexOf(" ");
            String sigString = auth_part.substring(index + 1);
            return sigString.split(":");
        }

        /**
         * AWS S3-spec address string, which includes the query parameters
         *
         * @param httpRequest
         * @param removeServicePath if true, removes the service path from the address string if found and if the request is path-style
         * @return
         * @throws AccessDeniedException
         */
        static String getS3AddressString(MappingHttpRequest httpRequest, boolean removeServicePath) throws S3Exception {
            /*
             * There are two modes: dns-style and path-style.
             * dns-style has the bucket name in the HOST header
             * path-style has the bucket name in the request path.
             *
             * If using DNS-style, we assume the key is the path, no service path necessary or allowed
             * If using path-style, there may be service path as well that prefixes the bucket name (e.g. /services/objectstorage/bucket/key)
             */
            try {
                String addr = httpRequest.getUri();
                String targetHost = httpRequest.getHeader(HttpHeaders.Names.HOST);
                String osgServicePath = ComponentIds.lookup(ObjectStorage.class).getServicePath();
                String bucket, key;

                StringBuilder addrString = new StringBuilder();

                //Normalize the URI
                if (targetHost.contains(".objectstorage")) {
                    //dns-style request
                    String hostBucket = targetHost.substring(0, targetHost.indexOf(".objectstorage"));
                    if (hostBucket.length() == 0) {
                        throw new InvalidAddressingHeaderException("Invalid Host header: " + targetHost);
                    } else {
                        addrString.append("/" + hostBucket);
                    }
                } else if (targetHost.contains(".walrus")) { 
                	 //dns-style request with walrus
                    String hostBucket = targetHost.substring(0, targetHost.indexOf(".walrus"));
                    if (hostBucket.length() == 0) {
                        throw new InvalidAddressingHeaderException("Invalid Host header: " + targetHost);
                    } else {
                        addrString.append("/" + hostBucket);
                    }
                } else {
                    //path-style request (or service request that won't have a bucket anyway)
                    if (removeServicePath) {
                    	if (addr.startsWith(osgServicePath)) {
                    		addr = addr.substring(osgServicePath.length(), addr.length());
                    	} else if (addr.startsWith(ObjectStorageProperties.LEGACY_WALRUS_SERVICE_PATH)) {
                    		addr = addr.substring(ObjectStorageProperties.LEGACY_WALRUS_SERVICE_PATH.length(), addr.length());
                    	}
                    }
                }

                //Get the path part, up to the ?
                key = addr.split("\\?", 2)[0];
                if (!Strings.isNullOrEmpty(key)) {
                    addrString.append(key);
                } else {
                    addrString.append("/");
                }

                List<String> canonicalSubresources = new ArrayList<>();
                String resource;
                for (String queryParam : httpRequest.getParameters().keySet()) {
                    try {
                        if (ObjectStorageProperties.SubResource.valueOf(queryParam) != null) {
                            canonicalSubresources.add(queryParam);
                        }
                    } catch (IllegalArgumentException e) {
                        //Skip. Not in the set.
                    }
                }

                if (canonicalSubresources.size() > 0) {
                    Collections.sort(canonicalSubresources);
                    String value;
                    addrString.append("?");
                    //Add resources to canonical string
                    for (String subResource : canonicalSubresources) {
                        value = httpRequest.getParameters().get(subResource);
                        addrString.append(subResource);
                        //Query values are not URL-decoded, the signature should have them exactly as in the URI
                        if (!Strings.isNullOrEmpty(value)) {
                            addrString.append("=").append(value);
                        }
                        addrString.append("&");
                    }

                    //Remove trailng '&' if found
                    if (addrString.charAt(addrString.length() - 1) == '&') {
                        addrString.deleteCharAt(addrString.length() - 1);
                    }
                }

                return addrString.toString();
            } catch (S3Exception e) {
                throw e;
            } catch (Exception e) {
                //Anything unexpected...
                throw new InternalErrorException(e);
            }
        }

    } //End class S3Authentication

    /**
     * Authentication Handler for ObjectStorage REST requests (POST method and SOAP are processed using different handlers)
     *
     * @param httpRequest
     * @throws AccessDeniedException
     */
    public void handle(MappingHttpRequest httpRequest) throws S3Exception {
        //Clean up the headers such that no duplicates may exist etc.
        //sanitizeHeaders(httpRequest);
        Map<String, String> parameters = httpRequest.getParameters();

        if (httpRequest.containsHeader(SecurityParameter.Authorization.toString())) {
            String authHeader = httpRequest.getAndRemoveHeader(SecurityParameter.Authorization.toString());
            Map<AuthorizationField, String> authMap = processAuthorizationHeader(authHeader);

           if(AWS_AUTH_TYPE.equals(authMap.get(AuthorizationField.Type))) {
                //Normally signed request using AccessKeyId/SecretKeyId pair
                S3Authentication.authenticate(httpRequest, authMap);
            } else {
                throw new MissingSecurityHeaderException("Malformed or unexpected format for Authentication header");
            }
        } else {
            if (parameters.containsKey(SecurityParameter.AWSAccessKeyId.toString())) {
                //Query String Auth
                S3Authentication.authenticateQueryString(httpRequest);
            } else {
                //Anonymous request, no query string, no Authorization header
                try {
                    Context ctx = Contexts.lookup(httpRequest.getCorrelationId());
                    ctx.setUser(Principals.nobodyUser());
                } catch (NoSuchContextException e) {
                    LOG.error(e, e);
                    throw new AccessDeniedException();
                }
            }
        }
    }
}
