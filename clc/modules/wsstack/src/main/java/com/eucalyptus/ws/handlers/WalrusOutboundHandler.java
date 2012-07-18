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
 ************************************************************************/

package com.eucalyptus.ws.handlers;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.util.WalrusUtil;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.CopyObjectResponseType;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import edu.ucsb.eucalyptus.msgs.PostObjectResponseType;
import edu.ucsb.eucalyptus.msgs.PutObjectResponseType;
import edu.ucsb.eucalyptus.msgs.WalrusDeleteResponseType;
import edu.ucsb.eucalyptus.msgs.WalrusErrorMessageType;

@ChannelPipelineCoverage("one")
public class WalrusOutboundHandler extends MessageStackHandler {
	private static Logger LOG = Logger.getLogger( WalrusOutboundHandler.class );

	@Override
	public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {

	}

	@Override
	public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpResponse ) {
			MappingHttpResponse httpResponse = ( MappingHttpResponse ) event.getMessage( );
			BaseMessage msg = (BaseMessage) httpResponse.getMessage( );

			if(msg instanceof PutObjectResponseType) {
				PutObjectResponseType putObjectResponse = (PutObjectResponseType) msg;
				httpResponse.addHeader(HttpHeaders.Names.ETAG, '\"' + putObjectResponse.getEtag() + '\"');
				httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, putObjectResponse.getLastModified());
				if(putObjectResponse.getVersionId() != null) {
					httpResponse.addHeader(WalrusProperties.X_AMZ_VERSION_ID, putObjectResponse.getVersionId());
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
				httpResponse.setMessage(errMsg);
			} else if(msg instanceof WalrusDeleteResponseType) {
				httpResponse.setStatus(HttpResponseStatus.NO_CONTENT);
				httpResponse.setMessage(null);
			}
		}
	}

}
