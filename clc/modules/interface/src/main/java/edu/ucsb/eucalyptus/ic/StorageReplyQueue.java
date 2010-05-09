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
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.StorageErrorMessageType;
import edu.ucsb.eucalyptus.util.ReplyCoordinator;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.mule.message.ExceptionMessage;

import com.eucalyptus.binding.BindingManager;

public class StorageReplyQueue {

    private static Logger LOG = Logger.getLogger( StorageReplyQueue.class );

    private static ReplyCoordinator replies = new ReplyCoordinator( 3600000 );

    public void handle( EucalyptusMessage msg )
    {
        Logger.getLogger( StorageReplyQueue.class ).warn( "storage queueing reply to " + msg.getCorrelationId() );
        replies.putMessage( msg );
    }

    public void handle( ExceptionMessage muleMsg )
    {
        try
        {
            Object requestMsg = muleMsg.getPayload();
            String requestString = requestMsg.toString();
            EucalyptusMessage msg = ( EucalyptusMessage ) BindingManager.getBinding( "msgs_eucalyptus_com" ).fromOM( requestString );
            Throwable ex = muleMsg.getException().getCause();
            EucalyptusMessage errMsg;

            if ( ex instanceof NoSuchVolumeException )
            {
                errMsg = new StorageErrorMessageType( "NoSuchVolume", "Volume not found", HttpStatus.SC_NOT_FOUND, msg.getCorrelationId());
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof VolumeInUseException )
            {
                errMsg = new StorageErrorMessageType( "VolumeInUse", "Volume in use", HttpStatus.SC_FORBIDDEN, msg.getCorrelationId());
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof NoSuchSnapshotException )
            {
                errMsg = new StorageErrorMessageType( "NoSuchSnapshot", "Snapshot not found", HttpStatus.SC_NOT_FOUND, msg.getCorrelationId());
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof VolumeAlreadyExistsException )
            {
                errMsg = new StorageErrorMessageType( "VolumeAlreadyExists", "Volume already exists", HttpStatus.SC_CONFLICT, msg.getCorrelationId());
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof VolumeNotReadyException )
            {
                errMsg = new StorageErrorMessageType( "VolumeNotReady", "Volume not ready yet", HttpStatus.SC_CONFLICT, msg.getCorrelationId());
                errMsg.setCorrelationId( msg.getCorrelationId() );
            }
            else if ( ex instanceof SnapshotInUseException )
            {
                errMsg = new StorageErrorMessageType( "SnapshotInUse", "Snapshot in use", HttpStatus.SC_CONFLICT, msg.getCorrelationId());
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
            LOG.error(e);
        }
    }

    public static BaseMessage getReply( String msgId )
    {
        Logger.getLogger( StorageReplyQueue.class ).warn( "storage request for reply to " + msgId );
        BaseMessage msg = replies.getMessage( msgId );
        Logger.getLogger( StorageReplyQueue.class ).warn( "storage obtained reply to " + msgId );
        return msg;
    }
}
