/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage.pipeline;

import java.nio.charset.Charset;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;
import com.eucalyptus.ws.WebServicesException;

public class ObjectStorageRESTExceptionHandler extends SimpleChannelUpstreamHandler {
	private static Logger LOG = Logger.getLogger(ObjectStorageRESTExceptionHandler.class);
	private static String CODE_UNKNOWN = "UNKNOWN";

	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception {
		final Channel ch = e.getChannel();
		Throwable cause = e.getCause();
		if (cause.getCause() != null) {
			//wrapped exception
			cause = cause.getCause();
		}
		HttpResponseStatus status = null;
		String code = null;
		String resource = null;
		String message = null;
		String requestId = null;

		// Get the request ID from the context and clear the context. If you cant log an exception and move on
		try {
			if (ch != null) {
				Context context = Contexts.lookup(ch);
				requestId = context.getCorrelationId();
				Contexts.clear(context);
			}
		} catch (Exception ex) {
			LOG.trace("Error getting request ID or clearing context", ex);
		}

		// Populate the error response fields
		if (cause instanceof ObjectStorageException) {
			ObjectStorageException walrusEx = (ObjectStorageException) cause;
			status = walrusEx.getStatus();
			code = walrusEx.getCode();
			resource = walrusEx.getResource();
		} else if (cause instanceof WebServicesException) {
			WebServicesException webEx = (WebServicesException) cause;
			status = webEx.getStatus();
			code = CODE_UNKNOWN;
		} else {
			status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
			code = CODE_UNKNOWN;
		}
		message = cause.getMessage();

		StringBuilder error = new StringBuilder().append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><Code>").append(code != null ? code : new String())
				.append("</Code><Message>").append(message != null ? message : new String()).append("</Message><Resource>")
				.append(resource != null ? resource : new String()).append("</Resource><RequestId>").append(requestId != null ? requestId : new String())
				.append("</RequestId></Error>");

		ChannelBuffer buffer = ChannelBuffers.copiedBuffer(error, Charset.forName("UTF-8"));
		final HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
		response.addHeader(HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=UTF-8");
		response.addHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(buffer.readableBytes()));
		response.setContent(buffer);

		ChannelFuture writeFuture = Channels.future(ctx.getChannel());
		writeFuture.addListener(ChannelFutureListener.CLOSE);
		response.addHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
		if (ctx.getChannel().isConnected()) {
			Channels.write(ctx, writeFuture, response);
		}
		ctx.sendDownstream(e);
	}
}
