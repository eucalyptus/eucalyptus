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
import java.util.UUID;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.ws.util.WalrusBucketLogger;

import edu.ucsb.eucalyptus.cloud.BucketLogData;
import edu.ucsb.eucalyptus.msgs.WalrusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.WalrusRequestType;
import edu.ucsb.eucalyptus.msgs.WalrusResponseType;

@ChannelPipelineCoverage("one")
public class WalrusRESTLoggerOutbound extends MessageStackHandler {
	@Override
	public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
	}

	@Override
	public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpResponse ) {
			MappingHttpResponse httpResponse = ( MappingHttpResponse ) event.getMessage( );
			if(httpResponse.getMessage() instanceof WalrusResponseType) {
				WalrusResponseType response = (WalrusResponseType) httpResponse.getMessage();
				BucketLogData logData = response.getLogData();
				if(logData != null) {
					computeStats(logData, httpResponse);
					response.setLogData(null);
				}
			} else if(httpResponse.getMessage() instanceof WalrusErrorMessageType) {
				WalrusErrorMessageType errorMessage = (WalrusErrorMessageType) httpResponse.getMessage();
				BucketLogData logData = errorMessage.getLogData();
				if(logData != null) {
					computeStats(logData, httpResponse);
					logData.setError(errorMessage.getCode());
					errorMessage.setLogData(null);
				}
			}
		}
	}

	private void computeStats(BucketLogData logData, MappingHttpResponse httpResponse) {
		logData.setBytesSent(httpResponse.getContent().readableBytes());
		long startTime = logData.getTotalTime();
		long currentTime = System.currentTimeMillis();
		logData.setTotalTime(currentTime - startTime);
		long startTurnAroundTime = logData.getTurnAroundTime();
		logData.setTurnAroundTime(Math.min((currentTime - startTurnAroundTime), logData.getTotalTime()));
		HttpResponseStatus status = httpResponse.getStatus();
		if(status != null)
			logData.setStatus(Integer.toString(status.getCode()));
		WalrusBucketLogger.getInstance().addLogEntry(logData);					
	}
}
