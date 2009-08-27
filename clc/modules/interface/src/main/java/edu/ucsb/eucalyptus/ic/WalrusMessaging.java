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
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.ic;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;
import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;

public class WalrusMessaging {

    private static Logger LOG = Logger.getLogger( WalrusMessaging.class );
    private static MuleClient client = null;

    private static MuleClient getClient() throws MuleException
    {
        synchronized ( WalrusMessaging.class )
        {
            if ( client == null )
                client = new MuleClient();
        }
        return client;
    }

    private static boolean first = true;
    public static void enqueue( EucalyptusMessage msg ) throws EucalyptusCloudException
    {
        try
        {
            if( first )
            {
                first = false;
                DescribeAvailabilityZonesType descAZMsg = new DescribeAvailabilityZonesType();
                descAZMsg.setUserId( Component.eucalyptus.name( ) );
                descAZMsg.setEffectiveUserId( Component.eucalyptus.name( ) );
                getClient().dispatch( "vm://Request", descAZMsg, null );
            }
            getClient().dispatch( "vm://WalrusRequestQueue", msg, null );
        }
        catch ( MuleException e )
        {
            LOG.error( e );
            throw new EucalyptusCloudException( e );
        }
    }

    public static EucalyptusMessage dequeue( String msgId )
    {
        return WalrusReplyQueue.getReply( msgId );
    }


}
