/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2009, Eucalyptus Systems, Inc.
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Neil Soman neil@eucalyptus.com
 */

package edu.ucsb.eucalyptus.ic;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.StorageErrorMessageType;
import edu.ucsb.eucalyptus.transport.binding.BindingManager;
import edu.ucsb.eucalyptus.util.ReplyCoordinator;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.mule.message.ExceptionMessage;

public class DNSReplyQueue {

    private static Logger LOG = Logger.getLogger( DNSReplyQueue.class );

    private static ReplyCoordinator replies = new ReplyCoordinator( 3600000 );

    public void handle( EucalyptusMessage msg )
    {
        Logger.getLogger( DNSReplyQueue.class ).warn( "storage queueing reply to " + msg.getCorrelationId() );
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


            errMsg = new EucalyptusErrorMessageType( muleMsg.getComponentName() , msg, ex.getMessage());

            replies.putMessage( errMsg );
        }
        catch ( Exception e )
        {
            LOG.error(e);
        }
    }

    public static EucalyptusMessage getReply( String msgId )
    {
        Logger.getLogger( DNSReplyQueue.class ).warn( "dns request for reply to " + msgId );
        EucalyptusMessage msg = replies.getMessage( msgId );
        Logger.getLogger( DNSReplyQueue.class ).warn( "dns obtained reply to " + msgId );
        return msg;
    }
}