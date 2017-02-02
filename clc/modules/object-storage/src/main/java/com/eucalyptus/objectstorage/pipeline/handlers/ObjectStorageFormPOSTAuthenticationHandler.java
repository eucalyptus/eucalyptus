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

import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.auth.login.SecurityContext;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidAccessKeyIdException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidPolicyDocumentException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.exceptions.s3.SignatureDoesNotMatchException;
import com.eucalyptus.objectstorage.pipeline.auth.ObjectStorageWrappedCredentials;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.google.common.base.Strings;

/**
 * Performs POST form authentication using the POST Policy and form fields. Works exclusively off the formFields collection in the httpRequest. That
 * map must be populated prior to this handler's execution
 */
@ChannelPipelineCoverage("one")
public class ObjectStorageFormPOSTAuthenticationHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger(ObjectStorageFormPOSTAuthenticationHandler.class);

  @Override
  public void handleUpstream(final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent) throws Exception {
    LOG.trace(this.getClass().getSimpleName() + "[incoming]: " + channelEvent);
    try {
      if (channelEvent instanceof MessageEvent) {
        final MessageEvent msgEvent = (MessageEvent) channelEvent;
        this.incomingMessage(channelHandlerContext, msgEvent);
      }
      channelHandlerContext.sendUpstream(channelEvent);
    } catch (S3Exception e) {
      LOG.trace("Caught exception in POST form authentication.", e);
      Channels.fireExceptionCaught(channelHandlerContext, e);
    }
  }

  @Override
  public void incomingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    if (event.getMessage() instanceof MappingHttpRequest) {
      MappingHttpRequest httpRequest = (MappingHttpRequest) event.getMessage();

      // Validate the policy as well-formed
      UploadPolicyChecker.checkPolicy(httpRequest.getFormFields());

      // Authenticate the request
      handle(httpRequest);
    }
  }

  /**
   * Uses the form fields in the request to validate signature
   * 
   * @param httpRequest
   * @throws S3Exception
   */
  public void handle(MappingHttpRequest httpRequest) throws S3Exception {
    String accessKey = (String) httpRequest.getFormFields().get(ObjectStorageProperties.FormField.AWSAccessKeyId.toString());
    String signature = (String) httpRequest.getFormFields().get(ObjectStorageProperties.FormField.Signature.toString());
    String policy = (String) httpRequest.getFormFields().get(ObjectStorageProperties.FormField.Policy.toString());
    String securityToken = (String) httpRequest.getFormFields().get(ObjectStorageProperties.FormField.x_amz_security_token.toString());

    if (!Strings.isNullOrEmpty(policy)) {
      if (Strings.isNullOrEmpty(signature)) {
        throw new InvalidPolicyDocumentException(httpRequest.getUri(), "Policy specified, but no signature field found.");
      } else if (Strings.isNullOrEmpty(accessKey)) {
        throw new InvalidPolicyDocumentException(httpRequest.getUri(), "Policy specified, but no AWSAccessKeyId field found");
      } else {
        try {
          SecurityContext.getLoginContext(
              new ObjectStorageWrappedCredentials(httpRequest.getCorrelationId(), policy, accessKey, signature, securityToken)).login();
        } catch (LoginException ex) {
          if (ex.getMessage().contains("The AWS Access Key Id you provided does not exist in our records")) {
            throw new InvalidAccessKeyIdException();
          } else {
            LOG.debug("CorrelationId: " + httpRequest.getCorrelationId() + " Authentication failed due to signature mismatch:", ex);
            throw new SignatureDoesNotMatchException();
          }
        } catch (Exception e) {
          LOG.warn("CorrelationId: " + httpRequest.getCorrelationId() + " Unexpected failure trying to authenticate request", e);
          throw new InternalErrorException(e);
        }
      }
    } else {
      // anonymous request, no policy included
      try {
        Context ctx = Contexts.lookup(httpRequest.getCorrelationId());
        ctx.setUser(Principals.nobodyUser());
      } catch (NoSuchContextException e) {
        LOG.error("Could not find context for anonymous request. Returning internal error.", e);
        throw new InternalErrorException(httpRequest.getUri(), e);
      }
    }
  }
}
