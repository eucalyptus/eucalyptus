/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.ic;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.WalrusBucketErrorMessageType;
import edu.ucsb.eucalyptus.util.ReplyCoordinator;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.mule.message.ExceptionMessage;

import com.eucalyptus.ws.binding.BindingManager;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

public class WalrusReplyQueue {

    private static Logger LOG = Logger.getLogger( WalrusReplyQueue.class );

    private static ReplyCoordinator replies = new ReplyCoordinator( 3600000 );

    private static int SC_DECRYPTION_FAILED = 566;
    public void handle( EucalyptusMessage msg )
    {
        replies.putMessage( msg );
    }

    public void handle( ExceptionMessage muleMsg )
    {
        try
        {
            Object requestMsg = muleMsg.getPayload();
            String requestString = requestMsg.toString();
            EucalyptusMessage msg = ( EucalyptusMessage ) BindingManager.getBinding( "msgs_eucalyptus_ucsb_edu" ).fromOM( requestString );
            Throwable ex = muleMsg.getException().getCause();
            EucalyptusMessage errMsg;

            String ipAddr = "127.0.0.1";
            List<NetworkInterface> ifaces = null;
            try {
                ifaces = Collections.list( NetworkInterface.getNetworkInterfaces() );
            } catch ( SocketException e1 ) {}

            for ( NetworkInterface iface : ifaces ) {
                try {
                    if ( !iface.isLoopback() && !iface.isVirtual() && iface.isUp() ) {
                        for ( InetAddress iaddr : Collections.list( iface.getInetAddresses() ) ) {
                            if ( !iaddr.isSiteLocalAddress() && !( iaddr instanceof Inet6Address) ) {
                                ipAddr = iaddr.getHostAddress();
                                break;
                            }
                        }
                    }
                } catch ( SocketException e1 ) {}
            }

            if ( ex instanceof NoSuchBucketException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( NoSuchBucketException ) ex ).getBucketName(), "NoSuchBucket", "The specified bucket was not found", HttpStatus.SC_NOT_FOUND, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof AccessDeniedException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( AccessDeniedException ) ex ).getBucketName(), "AccessDenied", "No U", HttpStatus.SC_FORBIDDEN, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof NotAuthorizedException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( NotAuthorizedException ) ex ).getValue(), "Unauthorized", "No U", HttpStatus.SC_UNAUTHORIZED, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof BucketAlreadyOwnedByYouException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( BucketAlreadyOwnedByYouException ) ex ).getBucketName(), "BucketAlreadyOwnedByYou", "Your previous request to create the named bucket succeeded and you already own it.", HttpStatus.SC_CONFLICT, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof BucketAlreadyExistsException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( BucketAlreadyExistsException ) ex ).getBucketName(), "BucketAlreadyExists", "The requested bucket name is not available. The bucket namespace is shared by all users of the system. Please select a different name and try again.", HttpStatus.SC_CONFLICT, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof BucketNotEmptyException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( BucketNotEmptyException ) ex ).getBucketName(), "BucketNotEmpty", "The bucket you tried to delete is not empty.", HttpStatus.SC_CONFLICT, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof PreconditionFailedException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( PreconditionFailedException ) ex ).getPrecondition(), "PreconditionFailed", "At least one of the pre-conditions you specified did not hold.", HttpStatus.SC_PRECONDITION_FAILED, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof NotModifiedException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( NotModifiedException ) ex ).getPrecondition(), "NotModified", "Object Not Modified", HttpStatus.SC_NOT_MODIFIED, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof TooManyBucketsException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( TooManyBucketsException ) ex ).getBucketName(), "TooManyBuckets", "You have attempted to create more buckets than allowed.", HttpStatus.SC_BAD_REQUEST, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof EntityTooLargeException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( EntityTooLargeException ) ex ).getEntityName(), "EntityTooLarge", "Your proposed upload exceeds the maximum allowed object size.", HttpStatus.SC_BAD_REQUEST, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof NoSuchEntityException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( NoSuchEntityException ) ex ).getBucketName(), "NoSuchEntity", "The specified entity was not found", HttpStatus.SC_NOT_FOUND, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof DecryptionFailedException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( DecryptionFailedException ) ex ).getValue(), "Decryption Failed", "Fail", SC_DECRYPTION_FAILED, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof ImageAlreadyExistsException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( ImageAlreadyExistsException ) ex ).getValue(), "Image Already Exists", "Fail", HttpStatus.SC_CONFLICT, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof NotImplementedException )
            {
                errMsg = new WalrusBucketErrorMessageType( ( ( NotImplementedException ) ex ).getValue(), "Not Implemented", "NA", HttpStatus.SC_NOT_IMPLEMENTED, msg.getCorrelationId(), ipAddr);
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else
            {
                errMsg = new EucalyptusErrorMessageType( muleMsg.getComponentName() , msg, ex.getMessage());
            }
            replies.putMessage( errMsg );
        }
        catch ( Exception e )
        {
            LOG.error( e, e );
        }
    }

    public static EucalyptusMessage getReply( String msgId )
    {
        EucalyptusMessage msg = null;
        msg = replies.getMessage( msgId );
        LOG.info( "walrus got reply to: " + msgId);
        return msg;
    }
}
