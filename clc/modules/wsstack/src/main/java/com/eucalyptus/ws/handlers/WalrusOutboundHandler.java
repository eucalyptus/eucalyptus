package com.eucalyptus.ws.handlers;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import com.eucalyptus.util.WalrusUtil;
import com.eucalyptus.ws.MappingHttpResponse;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.cloud.AccessDeniedException;
import edu.ucsb.eucalyptus.cloud.BucketAlreadyExistsException;
import edu.ucsb.eucalyptus.cloud.BucketAlreadyOwnedByYouException;
import edu.ucsb.eucalyptus.cloud.BucketNotEmptyException;
import edu.ucsb.eucalyptus.cloud.DecryptionFailedException;
import edu.ucsb.eucalyptus.cloud.EntityTooLargeException;
import edu.ucsb.eucalyptus.cloud.ImageAlreadyExistsException;
import edu.ucsb.eucalyptus.cloud.InvalidRangeException;
import edu.ucsb.eucalyptus.cloud.NoSuchBucketException;
import edu.ucsb.eucalyptus.cloud.NoSuchEntityException;
import edu.ucsb.eucalyptus.cloud.NotAuthorizedException;
import edu.ucsb.eucalyptus.cloud.NotImplementedException;
import edu.ucsb.eucalyptus.cloud.NotModifiedException;
import edu.ucsb.eucalyptus.cloud.PreconditionFailedException;
import edu.ucsb.eucalyptus.cloud.TooManyBucketsException;
import edu.ucsb.eucalyptus.msgs.WalrusBucketErrorMessageType;

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
			EucalyptusMessage msg = (EucalyptusMessage) httpResponse.getMessage( );

			if(msg instanceof PutObjectResponseType) {
				PutObjectResponseType putObjectResponse = (PutObjectResponseType) msg;
				httpResponse.addHeader(HttpHeaders.Names.ETAG, '\"' + putObjectResponse.getEtag() + '\"');
				httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, putObjectResponse.getLastModified());
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

			} else if(msg instanceof EucalyptusErrorMessageType) {
				EucalyptusErrorMessageType errorMessage = (EucalyptusErrorMessageType) msg;
				EucalyptusMessage errMsg = WalrusUtil.convertErrorMessage(errorMessage);
				if(errMsg instanceof WalrusBucketErrorMessageType) {
					WalrusBucketErrorMessageType walrusErrorMsg = (WalrusBucketErrorMessageType) errMsg;
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
