package com.eucalyptus.ws.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.message.ExceptionMessage;

import com.eucalyptus.ws.binding.BindingManager;

import edu.ucsb.eucalyptus.cloud.RequestTransactionScript;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public class ReplyQueue {

	private static Logger           LOG     = Logger.getLogger( ReplyQueue.class );
	private static ReplyCoordinator replies = new ReplyCoordinator( );
	private static String ipAddress;
	private static int SC_DECRYPTION_FAILED = 566;

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

	public void handle( EucalyptusMessage msg ) {
		if ( msg.getCorrelationId( ) != null && msg.getCorrelationId( ).length( ) != 0 ) replies.putMessage( msg );
	}

	public static EucalyptusMessage getReply( String msgId ) {
		EucalyptusMessage msg = null;
		msg = replies.getMessage( msgId );
		return msg;
	}

	public void handle( ExceptionMessage exMsg ) {
		Throwable exception = exMsg.getException( );
		Object payload = null;
		EucalyptusMessage msg = null;
		if ( exception instanceof MessagingException ) {
			MessagingException ex = ( MessagingException ) exception;
			MuleMessage muleMsg = ex.getUmoMessage( );

			if ( payload instanceof RequestTransactionScript ) {
				msg = ( ( RequestTransactionScript ) payload ).getRequestMessage( );
			} else {
				try {
					msg = parsePayload( muleMsg.getPayload( ) );
				} catch ( Exception e ) {
					LOG.error( "Bailing out of error handling: don't have the correlationId for the caller!" );
					LOG.error( e, e );
					return;
				}
			}
			EucalyptusErrorMessageType errMsg = getErrorMessageType( exMsg, msg );
			replies.putMessage( errMsg );
			return;
		}

		Object requestMsg = exMsg.getPayload();
		String requestString = requestMsg.toString();
		try {
			msg = ( EucalyptusMessage ) BindingManager.getBinding( "msgs_eucalyptus_ucsb_edu" ).fromOM( requestString );
		} catch ( Exception e ) {
			LOG.error( e, e );
			return;
		}

		Throwable ex = exMsg.getException().getCause();
		EucalyptusMessage errMsg;

		if ( ex instanceof NoSuchBucketException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( NoSuchBucketException ) ex ).getBucketName(), "NoSuchBucket", "The specified bucket was not found", HttpStatus.SC_NOT_FOUND, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		}
		else if ( ex instanceof AccessDeniedException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( AccessDeniedException ) ex ).getBucketName(), "AccessDenied", "No U", HttpStatus.SC_FORBIDDEN, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		}
		else if ( ex instanceof NotAuthorizedException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( NotAuthorizedException ) ex ).getValue(), "Unauthorized", "No U", HttpStatus.SC_UNAUTHORIZED, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		}
		else if ( ex instanceof BucketAlreadyOwnedByYouException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( BucketAlreadyOwnedByYouException ) ex ).getBucketName(), "BucketAlreadyOwnedByYou", "Your previous request to create the named bucket succeeded and you already own it.", HttpStatus.SC_CONFLICT, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		}
		else if ( ex instanceof BucketAlreadyExistsException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( BucketAlreadyExistsException ) ex ).getBucketName(), "BucketAlreadyExists", "The requested bucket name is not available. The bucket namespace is shared by all users of the system. Please select a different name and try again.", HttpStatus.SC_CONFLICT, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		}
		else if ( ex instanceof BucketNotEmptyException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( BucketNotEmptyException ) ex ).getBucketName(), "BucketNotEmpty", "The bucket you tried to delete is not empty.", HttpStatus.SC_CONFLICT, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		}
		else if ( ex instanceof PreconditionFailedException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( PreconditionFailedException ) ex ).getPrecondition(), "PreconditionFailed", "At least one of the pre-conditions you specified did not hold.", HttpStatus.SC_PRECONDITION_FAILED, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		}
		else if ( ex instanceof NotModifiedException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( NotModifiedException ) ex ).getPrecondition(), "NotModified", "Object Not Modified", HttpStatus.SC_NOT_MODIFIED, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		}
		else if ( ex instanceof TooManyBucketsException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( TooManyBucketsException ) ex ).getBucketName(), "TooManyBuckets", "You have attempted to create more buckets than allowed.", HttpStatus.SC_BAD_REQUEST, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		}
		else if ( ex instanceof EntityTooLargeException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( EntityTooLargeException ) ex ).getEntityName(), "EntityTooLarge", "Your proposed upload exceeds the maximum allowed object size.", HttpStatus.SC_BAD_REQUEST, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		}
		else if ( ex instanceof NoSuchEntityException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( NoSuchEntityException ) ex ).getBucketName(), "NoSuchEntity", "The specified entity was not found", HttpStatus.SC_NOT_FOUND, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		}
		else if ( ex instanceof DecryptionFailedException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( DecryptionFailedException ) ex ).getValue(), "Decryption Failed", "Fail", SC_DECRYPTION_FAILED, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		}
		else if ( ex instanceof ImageAlreadyExistsException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( ImageAlreadyExistsException ) ex ).getValue(), "Image Already Exists", "Fail", HttpStatus.SC_CONFLICT, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		}
		else if ( ex instanceof NotImplementedException )
		{
			errMsg = new WalrusBucketErrorMessageType( ( ( NotImplementedException ) ex ).getValue(), "Not Implemented", "NA", HttpStatus.SC_NOT_IMPLEMENTED, msg.getCorrelationId(), ipAddress);
			errMsg.setCorrelationId( msg.getCorrelationId() );
		} else
		{
			errMsg = new EucalyptusErrorMessageType( exMsg.getComponentName() , msg, ex.getMessage());
		}
		replies.putMessage( errMsg );

	}

	private EucalyptusErrorMessageType getErrorMessageType( final ExceptionMessage exMsg, final EucalyptusMessage msg ) {
		Throwable exception = exMsg.getException( );
		EucalyptusErrorMessageType errMsg = null;
		if ( exception != null ) {
			Throwable e = exMsg.getException( ).getCause( );
			if ( e != null ) {
				errMsg = new EucalyptusErrorMessageType( exMsg.getComponentName( ), msg, e.getMessage( ) );
			}
		}
		if ( errMsg == null ) {
			ByteArrayOutputStream exStream = new ByteArrayOutputStream( );
			exception.printStackTrace( new PrintStream( exStream ) );
			errMsg = new EucalyptusErrorMessageType( exMsg.getComponentName( ), msg, "Internal Error: \n" + exStream.toString( ) );
		}
		return errMsg;
	}

	private EucalyptusMessage parsePayload( Object payload ) throws Exception {
		if ( payload instanceof EucalyptusMessage ) {
			return ( EucalyptusMessage ) payload;
		} else if ( payload instanceof VmAllocationInfo ) {
			return ( ( VmAllocationInfo ) payload ).getRequest( );
		} else {
			return ( EucalyptusMessage ) BindingManager.getBinding( "msgs_eucalyptus_ucsb_edu" ).fromOM( ( String ) payload );
		}
	}

}
