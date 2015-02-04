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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException;
import com.eucalyptus.objectstorage.exceptions.s3.MissingSecurityHeaderException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.pipeline.handlers.S3Authentication.AuthorizationField;
import com.eucalyptus.objectstorage.pipeline.handlers.S3Authentication.SecurityParameter;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.server.MessageStatistics;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@ChannelPipelineCoverage("one")
public class ObjectStorageAuthenticationHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger(ObjectStorageAuthenticationHandler.class);
  private static final String AWS_AUTH_TYPE = "AWS";
  protected static final String ISO_8601_FORMAT = "yyyyMMdd'T'HHmmss'Z'"; // Use the ISO8601 format

  /**
   * Ensure that only one header for each name exists (i.e. not 2 Authorization headers) Accomplish this by comma-delimited concatenating any
   * duplicates found as per HTTP 1.1 RFC 2616 section 4.2
   *
   * @param httpRequest
   */
  private static void canonicalizeHeaders(MappingHttpRequest httpRequest) {
    // Iterate through headers and find duplicates, concatenate their values together and remove from
    // request as we find them.
    TreeMap<String, String> headerMap = new TreeMap<String, String>();
    String value = null;

    for (String header : httpRequest.getHeaderNames()) {
      headerMap.put(header, Joiner.on(',').join(httpRequest.getHeaders(header)));
    }

    // Remove *all* headers
    httpRequest.clearHeaders();

    // Add the normalized headers back into the request
    for (String foundHeader : headerMap.keySet()) {
      httpRequest.addHeader(foundHeader, headerMap.get(foundHeader));
    }
  }

  /**
   * This method exists to clean up a problem encountered periodically where the HTTP headers are duplicated
   *
   * @param httpRequest
   */
  private static void removeDuplicateHeaderValues(MappingHttpRequest httpRequest) {
    List<String> hdrList = null;
    HashMap<String, List<String>> fixedHeaders = new HashMap<String, List<String>>();
    boolean foundDup = false;
    for (String header : httpRequest.getHeaderNames()) {
      hdrList = httpRequest.getHeaders(header);

      // Only address the specific case where there is exactly one identical copy of the header
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
        Callable<Long> stat = MessageStatistics.startUpstream(ctx.getChannel(), this);
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
      throw new AccessDeniedException("Invalid authorization header");
    }

    if (AWS_AUTH_TYPE.equals(components[0]) && components.length == 2) {
      // Expect: components[1] = <AccessKeyId>:<Signature>
      authMap.put(AuthorizationField.Type, AWS_AUTH_TYPE);
      String[] signatureElements = components[1].split(":");
      authMap.put(AuthorizationField.AccessKeyId, signatureElements[0]);
      authMap.put(AuthorizationField.Signature, signatureElements[1]);
    } else {
      throw new AccessDeniedException("Invalid authorization header");
    }

    return authMap;
  }

  /**
   * Authentication Handler for ObjectStorage REST requests (POST method and SOAP are processed using different handlers)
   *
   * @param httpRequest
   * @throws AccessDeniedException
   */
  public void handle(MappingHttpRequest httpRequest) throws S3Exception {
    // Clean up the headers such that no duplicates may exist etc.
    // sanitizeHeaders(httpRequest);
    Map<String, String> parameters = httpRequest.getParameters();

    if (httpRequest.containsHeader(SecurityParameter.Authorization.toString())) {
      String authHeader = httpRequest.getAndRemoveHeader(SecurityParameter.Authorization.toString());
      Map<AuthorizationField, String> authMap = processAuthorizationHeader(authHeader);

      if (AWS_AUTH_TYPE.equals(authMap.get(AuthorizationField.Type))) {
        // Normally signed request using AccessKeyId/SecretKeyId pair
        S3Authentication.authenticateVersion2(httpRequest, authMap);
      } else {
        throw new MissingSecurityHeaderException("Malformed or unexpected format for Authentication header");
      }
    } else {
      if (parameters.containsKey(SecurityParameter.AWSAccessKeyId.toString())) {
        // Query String Auth
        S3Authentication.authenticateQueryString(httpRequest);
      } else {
        // Anonymous request, no query string, no Authorization header
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
