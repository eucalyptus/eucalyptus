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

package com.eucalyptus.objectstorage;

import com.eucalyptus.context.Context;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class OSGChannelWriter {

    private static Logger LOG = Logger.getLogger(OSGChannelWriter.class);

    public static void writeResponse(final Context ctx, final OSGMessageResponse response) throws InternalErrorException {
        Channel channel = ctx.getChannel();

        if(channel == null || (!channel.isConnected())) {
            throw new InternalErrorException("Response: " + response + " requested, but no channel to write to.");
        }

        final HttpResponseStatus status = response.getHttpResponseStatus();
        if (status != null) {
            HttpResponse httpResponse = new DefaultHttpResponse( HttpVersion.HTTP_1_1, status );
            channel.write(httpResponse).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    //no post processing here, but for debugging
                    LOG.debug("Wrote response status: " + status + " for request: " + ctx.getCorrelationId());
                }
            });
        }
        final String responseMessage = response.getHttpResponseBody();
        if (responseMessage != null) {
            channel.write(responseMessage).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    //no post processing here, but for debugging
                    LOG.debug("Wrote response body: " + responseMessage + " for request: " + ctx.getCorrelationId());
                }
            });
        }
    }

}
