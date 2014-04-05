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

import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.walrus.msgs.WalrusDataGetResponseType;
import com.eucalyptus.walrus.msgs.WalrusDataResponseType;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.walrus.exceptions.HeadExceptionInterface;
import com.eucalyptus.walrus.msgs.CopyObjectResponseType;
import com.eucalyptus.walrus.msgs.CreateBucketResponseType;
import com.eucalyptus.walrus.msgs.PostObjectResponseType;
import com.eucalyptus.walrus.msgs.PutObjectResponseType;
import com.eucalyptus.walrus.msgs.WalrusDeleteResponseType;
import com.eucalyptus.walrus.msgs.WalrusErrorMessageType;
import com.eucalyptus.walrus.msgs.WalrusHeadResponseType;
import com.eucalyptus.walrus.util.WalrusProperties;
import com.eucalyptus.walrus.util.WalrusUtil;
import com.eucalyptus.ws.handlers.MessageStackHandler;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import com.eucalyptus.walrus.msgs.AbortMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.AbortMultipartUploadType;
import com.eucalyptus.walrus.msgs.CompleteMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.CompleteMultipartUploadType;
import com.eucalyptus.walrus.msgs.InitiateMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.InitiateMultipartUploadType;
import com.eucalyptus.walrus.msgs.UploadPartResponseType;
import com.eucalyptus.walrus.msgs.UploadPartType;

public class WalrusOutboundHandler extends MessageStackHandler {
	private static Logger LOG = Logger.getLogger( WalrusOutboundHandler.class );

	@Override
	public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpResponse ) {
			MappingHttpResponse httpResponse = ( MappingHttpResponse ) event.getMessage( );
			BaseMessage msg = (BaseMessage) httpResponse.getMessage( );

			if(msg instanceof PutObjectResponseType) {
				PutObjectResponseType putObjectResponse = (PutObjectResponseType) msg;
				httpResponse.addHeader(HttpHeaders.Names.ETAG, '\"' + putObjectResponse.getEtag() + '\"');
				httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, DateFormatter.dateToHeaderFormattedString(putObjectResponse.getLastModified()));
				if(putObjectResponse.getVersionId() != null) {
					httpResponse.addHeader(WalrusProperties.X_AMZ_VERSION_ID, putObjectResponse.getVersionId());
				}
			} else if(msg instanceof WalrusDataResponseType) {
                WalrusDataResponseType response = (WalrusDataResponseType) msg;
                httpResponse.addHeader(HttpHeaders.Names.ETAG, '\"' + response.getEtag() + '\"');
                httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, DateFormatter.dateToHeaderFormattedString(response.getLastModified()));
                if(response.getVersionId() != null) {
                    httpResponse.addHeader(WalrusProperties.X_AMZ_VERSION_ID, response.getVersionId());
                }
            } else if (msg instanceof PostObjectResponseType) {
				PostObjectResponseType postObjectResponse = (PostObjectResponseType) msg;
				String redirectUrl = postObjectResponse.getRedirectUrl();
				if ( redirectUrl != null ) {
					httpResponse.addHeader(HttpHeaders.Names.LOCATION, redirectUrl);
					httpResponse.setStatus(HttpResponseStatus.SEE_OTHER);
					httpResponse.setMessage(null);
				} else {
					Integer successCode = postObjectResponse.getSuccessCode();
					if ( successCode != null ) {
						if(successCode != 201) {
							httpResponse.setMessage(null);
							httpResponse.setStatus(new HttpResponseStatus(successCode, "OK"));
						} else {
							httpResponse.setStatus(new HttpResponseStatus(successCode, "Created"));
						}
					}
				}
				//have to force a close for browsers
				event.getFuture().addListener(ChannelFutureListener.CLOSE);
			} else if(msg instanceof CopyObjectResponseType) {
				CopyObjectResponseType copyResponse = (CopyObjectResponseType) msg;
				if(copyResponse.getVersionId() != null)
					httpResponse.addHeader("x-amz-version-id", copyResponse.getVersionId());
				if(copyResponse.getCopySourceVersionId() != null)
					httpResponse.addHeader("x-amz-copy-source-version-id", copyResponse.getCopySourceVersionId());
			} else if(msg instanceof EucalyptusErrorMessageType) {      
				EucalyptusErrorMessageType errorMessage = (EucalyptusErrorMessageType) msg;
				BaseMessage errMsg = WalrusUtil.convertErrorMessage(errorMessage);
				if(errMsg instanceof WalrusErrorMessageType) {
					WalrusErrorMessageType walrusErrorMsg = (WalrusErrorMessageType) errMsg;
					httpResponse.setStatus(walrusErrorMsg.getStatus());
				}
				httpResponse.setMessage(errMsg);
			} else if(msg instanceof ExceptionResponseType) {      
				ExceptionResponseType errorMessage = (ExceptionResponseType) msg;
				BaseMessage errMsg = WalrusUtil.convertErrorMessage(errorMessage);
				if(errMsg instanceof WalrusErrorMessageType) {
					WalrusErrorMessageType walrusErrorMsg = (WalrusErrorMessageType) errMsg;
					httpResponse.setStatus(walrusErrorMsg.getStatus());
				}
				// Fix for EUCA-2782. If the exception occurred on HEAD request, http response body should be empty
				if(errorMessage.getException() instanceof HeadExceptionInterface) {
					httpResponse.setMessage(null);
				} else {
					httpResponse.setMessage(errMsg);	
				}
			} else if (msg instanceof WalrusDeleteResponseType) {
				httpResponse.setStatus(HttpResponseStatus.NO_CONTENT);
				httpResponse.setMessage(null);
			} else if (msg instanceof WalrusHeadResponseType) {
				//This is a HEAD request, don't put a body
				httpResponse.setStatus(HttpResponseStatus.OK);
				httpResponse.setMessage(null);
			} else if (msg instanceof CreateBucketResponseType) {
				httpResponse.setStatus(HttpResponseStatus.OK);
				httpResponse.setMessage(null);
				event.getFuture().addListener(ChannelFutureListener.CLOSE);
			} 
		}
	}

}
