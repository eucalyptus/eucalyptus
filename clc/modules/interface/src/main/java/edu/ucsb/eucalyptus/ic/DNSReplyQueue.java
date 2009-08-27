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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 *
 * Author: Neil Soman neil@eucalyptus.com
 */

package edu.ucsb.eucalyptus.ic;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.StorageErrorMessageType;
import edu.ucsb.eucalyptus.util.ReplyCoordinator;
import org.apache.log4j.Logger;
import org.mule.message.ExceptionMessage;

import com.eucalyptus.ws.binding.BindingManager;

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
