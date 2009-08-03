package com.eucalyptus.ws.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

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
			errMsg.setException(exception.getCause());
			replies.putMessage( errMsg );
		}
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
