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

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataResponseType;
import com.eucalyptus.objectstorage.msgs.PreflightCheckCorsResponseType;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.msgs.s3.CorsHeader;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.storage.msgs.s3.PreflightResponse;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.google.common.base.Strings;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ChannelPipelineCoverage("one")
public class ObjectStorageOPTIONSOutboundHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger(ObjectStorageOPTIONSOutboundHandler.class);

  @Override
  public void outgoingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    LOG.debug("LPT: Here I am in ObjectStorageOPTIONSOutboundHandler.outgoingMessage()");

    LOG.debug("LPT: event's message is of class " + event.getMessage().getClass());
    if (event.getMessage() instanceof MappingHttpResponse) {
      MappingHttpResponse httpResponse = (MappingHttpResponse) event.getMessage();
      BaseMessage msg = (BaseMessage) httpResponse.getMessage();
      httpResponse.setHeader(ObjectStorageProperties.AMZ_REQUEST_ID, msg.getCorrelationId());
      httpResponse.setHeader(HttpHeaders.Names.DATE, DateFormatter.dateToHeaderFormattedString(new Date()));

      //Exception e = new Exception(); //LPT
      LOG.debug("LPT: msg is of class " + msg.getClass());
      if (msg instanceof PreflightCheckCorsResponseType) {
        PreflightCheckCorsResponseType preflightResponseMsg = (PreflightCheckCorsResponseType) msg;
        PreflightResponse preflightResponseFields = preflightResponseMsg.getPreflightResponse();
        httpResponse.setStatus(preflightResponseMsg.getStatus());
        //LPT e = new Exception();
        LOG.debug("LPT: Yes, I am a PreflightCheckCorsResponseType, and my HTTP status code is " + 
            httpResponse.getStatus());

        httpResponse.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, preflightResponseFields.getOrigin());
        
        List<String> methodList = preflightResponseFields.getMethods();
        if (methodList != null) {
          // Convert list into "[method1, method2, ...]"
          String methods = methodList.toString();
          if (methods.length() > 2) {
            // Chop off brackets
            methods = methods.substring(1, methods.length()-1);
            httpResponse.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS, methods);
          }
        }
        
        List<CorsHeader> allowHeadersList = preflightResponseFields.getAllowedHeaders();
        if (allowHeadersList != null) {
          // Convert list into "[allowHeader1, allowHeader2, ...]"
          String allowHeaders = allowHeadersList.toString();
          if (allowHeaders.length() > 2) {
            // Chop off brackets
            allowHeaders = allowHeaders.substring(1, allowHeaders.length()-1);
            httpResponse.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
          }
        }
        
        List<CorsHeader> exposeHeadersList = preflightResponseFields.getExposeHeaders();
        if (exposeHeadersList != null) {
          // Convert list into "[exposeHeader1, exposeHeader2, ...]"
          String exposeHeaders = exposeHeadersList.toString();
          if (exposeHeaders.length() > 2) {
            // Chop off brackets
            exposeHeaders = exposeHeaders.substring(1, exposeHeaders.length()-1);
            httpResponse.setHeader(HttpHeaders.Names.ACCESS_CONTROL_EXPOSE_HEADERS, exposeHeaders);
          }
        }
        
        if (preflightResponseFields.getMaxAgeSeconds() > 0) {
          httpResponse.setHeader(HttpHeaders.Names.ACCESS_CONTROL_MAX_AGE, preflightResponseFields.getMaxAgeSeconds());
        }
        // Set the "allow credentials" header to true only if the matching 
        // CORS rule is NOT "any origin", otherwise don't set the header.
        if (preflightResponseFields.getOrigin() != null && 
            !preflightResponseFields.getOrigin().equals("*")) {
          httpResponse.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        // Match AWS behavior. Always contains these 3 header names. 
        // It tells the user agent: If you cache this request+response, only
        // give the cached response to a future request if all these headers 
        // match the ones in the cached request. Otherwise, send the request 
        // to the server, don't use the cached response.
        httpResponse.setHeader(HttpHeaders.Names.VARY, 
            HttpHeaders.Names.ORIGIN + ", " +
                HttpHeaders.Names.ACCESS_CONTROL_REQUEST_HEADERS + ", " +
                HttpHeaders.Names.ACCESS_CONTROL_REQUEST_METHOD);

        // No body in this preflight response that has no error.
        // Error responses are handled separately as S3Exception objects.
        httpResponse.setMessage(null);
        // Need to set this to zero to prevent client from waiting for more.
        httpResponse.setHeader(HttpHeaders.Names.CONTENT_LENGTH, 0);
      }
    }
  }
}
