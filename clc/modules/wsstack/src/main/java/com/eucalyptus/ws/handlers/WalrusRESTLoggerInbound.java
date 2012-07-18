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

import java.net.InetSocketAddress;
import java.util.Calendar;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpRequest;

import edu.ucsb.eucalyptus.cloud.BucketLogData;
import edu.ucsb.eucalyptus.msgs.WalrusRequestType;

@ChannelPipelineCoverage("one")
public class WalrusRESTLoggerInbound extends MessageStackHandler {
	@Override
	public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpRequest ) {
			MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage();
			if(httpRequest.getMessage() instanceof WalrusRequestType) {
				WalrusRequestType request = (WalrusRequestType) httpRequest.getMessage();
				BucketLogData logData = request.getLogData();
				if(logData != null) {
					long currentTime = System.currentTimeMillis();
					logData.setTotalTime(currentTime);
					logData.setTurnAroundTime(currentTime);
					logData.setUri(httpRequest.getUri());
					String referrer = httpRequest.getHeader(HttpHeaders.Names.REFERER);
					if(referrer != null)
						logData.setReferrer(referrer);
					String userAgent = httpRequest.getHeader(HttpHeaders.Names.USER_AGENT);
					if(userAgent != null)
						logData.setUserAgent(userAgent);
					logData.setTimestamp(String.format("[%1$td/%1$tb/%1$tY:%1$tH:%1$tM:%1$tS %1$tz]", Calendar.getInstance()));
					User user = Contexts.lookup( httpRequest.getCorrelationId( ) ).getUser();
					if(user != null)
						logData.setAccessorId(user.getUserId());
					if(request.getBucket() != null)
						logData.setBucketName(request.getBucket());
					if(request.getKey() != null) 
						logData.setKey(request.getKey());
					if(ctx.getChannel().getRemoteAddress() instanceof InetSocketAddress) {
						InetSocketAddress sockAddress = (InetSocketAddress) ctx.getChannel().getRemoteAddress();
						logData.setSourceAddress(sockAddress.getAddress().getHostAddress());
					}
				}
			}			
		}
	}

	@Override
	public void outgoingMessage(ChannelHandlerContext ctx, MessageEvent event)
			throws Exception {
	}
}
