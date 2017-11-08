/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.pipeline.handlers;

import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.exceptions.s3.*;
import com.eucalyptus.objectstorage.pipeline.auth.S3Authentication;
import com.eucalyptus.objectstorage.pipeline.auth.S3Authentication.S3Authenticator;
import com.eucalyptus.objectstorage.pipeline.handlers.AwsChunkStream.AwsChunk;
import com.eucalyptus.objectstorage.pipeline.handlers.AwsChunkStream.StreamingHttpRequest;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.server.MessageStatistics;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

public class ObjectStorageAuthenticationHandler extends MessageStackHandler {
  private static final Logger LOG = Logger.getLogger(ObjectStorageAuthenticationHandler.class);

  private boolean authenticatedInitialRequest;

  /**
   * Note: Overriding to ensure that the message is passed to the next stage in the pipeline only if it
   * passes authentication.
   */
  @Override
  public void handleUpstream(final ChannelHandlerContext ctx, final ChannelEvent channelEvent) throws Exception {
    if (channelEvent instanceof MessageEvent) {
      Callable<Long> stat = MessageStatistics.startUpstream(ctx.getChannel(), this);

      try {
        MessageEvent event = (MessageEvent) channelEvent;

        // Handle V4 streaming requests
        if (event.getMessage() instanceof StreamingHttpRequest) {
          StreamingHttpRequest request = (StreamingHttpRequest) event.getMessage();

          // Authenticate initial request if necessary
          if (!authenticatedInitialRequest) {
            authenticate(request.initialRequest);
            ctx.sendUpstream(new UpstreamMessageEvent(ctx.getChannel(), request.initialRequest, event.getRemoteAddress()));
          }

          // Authenticate chunks
          authenticate(request);
          for (AwsChunk chunk : request.awsChunks) {
            ctx.sendUpstream(new UpstreamMessageEvent(ctx.getChannel(), chunk.toHttpChunk(), event.getRemoteAddress()));
          }
        } else {
          if (event.getMessage() instanceof MappingHttpRequest)
            authenticate((MappingHttpRequest) event.getMessage());
          ctx.sendUpstream(channelEvent);
        }
      } catch (AccessDeniedException | SignatureDoesNotMatchException | InvalidAccessKeyIdException | MissingSecurityHeaderException ex) {
        LOG.debug(ex.getMessage());
        Channels.fireExceptionCaught(ctx, ex);
      } catch (Throwable e) {
        LOG.error(e, e);
        Channels.fireExceptionCaught(ctx, e);
      } finally {
        stat.call();
      }
    } else {
      ctx.sendUpstream(channelEvent);
    }
  }

  /**
   * Authentication Handler for ObjectStorage REST requests (POST method and SOAP are processed using different
   * handlers).
   *
   * @throws AccessDeniedException          if the auth header is invalid
   * @throws SignatureDoesNotMatchException if the signature is invalid
   * @throws InvalidAccessKeyIdException    if the contextual AWS key is invalid
   * @throws InternalErrorException         if something unexpected occurs
   * @throws MissingSecurityHeaderException is the auth header is invalid
   */
  private void authenticate(MappingHttpRequest request) throws S3Exception {
    removeDuplicateHeaderValues(request);
    joinDuplicateHeaders(request);
    Map<String, String> lowercaseParams = lowercaseKeys(request.getParameters());
    S3Authenticator.of(request, lowercaseParams).authenticate(request, lowercaseParams);
    authenticatedInitialRequest = true;
  }

  /**
   * Authenticate streaming V4 chunks.
   */
  private void authenticate(StreamingHttpRequest request) throws S3Exception {
    removeDuplicateHeaderValues(request.initialRequest);
    joinDuplicateHeaders(request.initialRequest);
    Map<String, String> lowercaseParams = lowercaseKeys(request.initialRequest.getParameters());

    if (request.awsChunks.isEmpty())
      S3Authenticator.of(request.initialRequest, lowercaseParams).authenticate(request.initialRequest, lowercaseParams);
    else
      S3Authentication.authenticateV4Streaming(request.initialRequest, request.awsChunks);
  }

  /**
   * This method exists to clean up a problem encountered periodically where the HTTP headers are identically duplicated.
   * <p>
   * TODO Move to somewhere common outside of object-storage
   */
  private static void removeDuplicateHeaderValues(MappingHttpRequest request) {
    List<String> hdrList;
    Map<String, List<String>> fixedHeaders = new HashMap<>();
    boolean foundDup = false;
    for (String header : request.getHeaderNames()) {
      hdrList = request.getHeaders(header);

      // Only address the specific case where there is exactly one identical copy of the header
      if (hdrList != null && hdrList.size() == 2 && hdrList.get(0).equals(hdrList.get(1))) {
        foundDup = true;
        fixedHeaders.put(header, Lists.newArrayList(hdrList.get(0)));
      } else {
        fixedHeaders.put(header, hdrList);
      }
    }

    if (foundDup) {
      LOG.debug("Found duplicate headers in: " + request.logMessage());
      request.clearHeaders();

      for (Map.Entry<String, List<String>> e : fixedHeaders.entrySet()) {
        for (String v : e.getValue()) {
          request.addHeader(e.getKey(), v);
        }
      }
    }
  }

  /**
   * Ensure that only one header for each name exists (i.e. not 2 Authorization headers) Accomplish this by
   * comma-delimited concatenating any duplicates found as per HTTP 1.1 RFC 2616 section 4.2.
   * <p>
   * TODO Move to somewhere common outside of object-storage
   */
  private static void joinDuplicateHeaders(MappingHttpRequest request) {
    // Join headers
    Map<String, String> joined = new TreeMap<>();
    for (String header : request.getHeaderNames())
      joined.put(header, Joiner.on(',').join(request.getHeaders(header)));

    // Remove all headers
    request.clearHeaders();

    // Add joined headers
    for (Map.Entry<String, String> entry : joined.entrySet())
      request.addHeader(entry.getKey(), entry.getValue());
  }

  /**
   * Returns a representation of the {@code map} with lowercase keys.
   */
  static Map<String, String> lowercaseKeys(Map<String, String> map) {
    Map<String, String> result = new HashMap<>();
    map.entrySet().forEach(e -> result.put(e.getKey().toLowerCase(), e.getValue()));
    return result;
  }
}
