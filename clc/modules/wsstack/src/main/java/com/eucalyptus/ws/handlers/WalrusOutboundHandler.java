package com.eucalyptus.ws.handlers;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

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
	private static String ipAddress;

	static {
		ipAddress = "127.0.0.1";
		List<NetworkInterface> ifaces = null;
		try {
			ifaces = Collections.list( NetworkInterface.getNetworkInterfaces() );
		} catch ( SocketException e1 ) {}

		for ( NetworkInterface iface : ifaces ) {
			try {
				if ( !iface.isLoopback() && !iface.isVirtual() && iface.isUp() ) {
					for ( InetAddress iaddr : Collections.list( iface.getInetAddresses() ) ) {
						if ( !iaddr.isSiteLocalAddress() && !( iaddr instanceof Inet6Address) ) {
							ipAddress = iaddr.getHostAddress();
							break;
						}
					}
				}
			} catch ( SocketException e1 ) {}
		}
	}


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
				EucalyptusMessage errMsg;
				Throwable ex = errorMessage.getException();
				if ( ex instanceof NoSuchBucketException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( NoSuchBucketException ) ex ).getBucketName(), "NoSuchBucket", "The specified bucket was not found", HttpResponseStatus.NOT_FOUND, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				}
				else if ( ex instanceof AccessDeniedException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( AccessDeniedException ) ex ).getBucketName(), "AccessDenied", "No U", HttpResponseStatus.FORBIDDEN, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				}
				else if ( ex instanceof NotAuthorizedException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( NotAuthorizedException ) ex ).getValue(), "Unauthorized", "No U", HttpResponseStatus.UNUATHORIZED, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				}
				else if ( ex instanceof BucketAlreadyOwnedByYouException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( BucketAlreadyOwnedByYouException ) ex ).getBucketName(), "BucketAlreadyOwnedByYou", "Your previous request to create the named bucket succeeded and you already own it.", HttpResponseStatus.CONFLICT, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				}
				else if ( ex instanceof BucketAlreadyExistsException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( BucketAlreadyExistsException ) ex ).getBucketName(), "BucketAlreadyExists", "The requested bucket name is not available. The bucket namespace is shared by all users of the system. Please select a different name and try again.", HttpResponseStatus.CONFLICT, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				}
				else if ( ex instanceof BucketNotEmptyException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( BucketNotEmptyException ) ex ).getBucketName(), "BucketNotEmpty", "The bucket you tried to delete is not empty.", HttpResponseStatus.CONFLICT, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				}
				else if ( ex instanceof PreconditionFailedException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( PreconditionFailedException ) ex ).getPrecondition(), "PreconditionFailed", "At least one of the pre-conditions you specified did not hold.", HttpResponseStatus.PRECONDITION_FAILED, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				}
				else if ( ex instanceof NotModifiedException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( NotModifiedException ) ex ).getPrecondition(), "NotModified", "Object Not Modified", HttpResponseStatus.NOT_MODIFIED, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				}
				else if ( ex instanceof TooManyBucketsException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( TooManyBucketsException ) ex ).getBucketName(), "TooManyBuckets", "You have attempted to create more buckets than allowed.", HttpResponseStatus.BAD_REQUEST, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				}
				else if ( ex instanceof EntityTooLargeException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( EntityTooLargeException ) ex ).getEntityName(), "EntityTooLarge", "Your proposed upload exceeds the maximum allowed object size.", HttpResponseStatus.BAD_REQUEST, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				}
				else if ( ex instanceof NoSuchEntityException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( NoSuchEntityException ) ex ).getBucketName(), "NoSuchEntity", "The specified entity was not found", HttpResponseStatus.NOT_FOUND, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				}
				else if ( ex instanceof DecryptionFailedException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( DecryptionFailedException ) ex ).getValue(), "Decryption Failed", "Fail", HttpResponseStatus.EXPECTATION_FAILED, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				}
				else if ( ex instanceof ImageAlreadyExistsException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( ImageAlreadyExistsException ) ex ).getValue(), "Image Already Exists", "Fail", HttpResponseStatus.CONFLICT, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				}
				else if ( ex instanceof NotImplementedException )
				{
					errMsg = new WalrusBucketErrorMessageType( ( ( NotImplementedException ) ex ).getValue(), "Not Implemented", "NA", HttpResponseStatus.NOT_IMPLEMENTED, msg.getCorrelationId(), ipAddress);
					errMsg.setCorrelationId( msg.getCorrelationId() );
				} else {
					errMsg = errorMessage;
				}
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
