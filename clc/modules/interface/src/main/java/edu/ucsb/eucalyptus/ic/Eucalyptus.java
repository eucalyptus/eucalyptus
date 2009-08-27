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
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.ic;

import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.constants.EventType;
import edu.ucsb.eucalyptus.msgs.DescribeBundleTasksType;
import edu.ucsb.eucalyptus.msgs.DescribeRegionsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeRegionsType;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.EventRecord;
import edu.ucsb.eucalyptus.msgs.RegionInfoType;
import edu.ucsb.eucalyptus.msgs.UnimplementedMessage;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.ws.util.Messaging;
import org.apache.log4j.Logger;

public class Eucalyptus {

  private static Logger LOG = Logger.getLogger( Eucalyptus.class );

//  public EucalyptusMessage handle( EucalyptusMessage msg )
//  {
//    if( msg instanceof UnimplementedMessage ) {
//      return msg.getReply();
//    }
//    if( msg instanceof DescribeRegionsType ) {
//      DescribeRegionsResponseType reply = ( DescribeRegionsResponseType ) msg.getReply();
//      try {
//        SystemConfiguration config = EucalyptusProperties.getSystemConfiguration();
//        reply.getRegionInfo().add(new RegionInfoType( "Eucalyptus", config.getStorageUrl().replaceAll( "Walrus", "Eucalyptus" )));
//        reply.getRegionInfo().add(new RegionInfoType( "Walrus", config.getStorageUrl()));
//      } catch ( EucalyptusCloudException e ) {}
//      return reply;
//    } else if ( msg instanceof DescribeBundleTasksType ) {
//      return msg.getReply();
//    }
//    LOG.info( EventRecord.create( this.getClass().getSimpleName(), msg.getUserId(), msg.getCorrelationId(), EventType.MSG_RECEIVED, msg.getClass().getSimpleName() ) );
//    long startTime = System.currentTimeMillis();
//    Messaging.dispatch( "vm://RequestQueue", msg );
//    EucalyptusMessage reply = null;
//    reply = ReplyQueue.getReply( msg.getCorrelationId() );
//    LOG.info( EventRecord.create( this.getClass().getSimpleName(), msg.getUserId(), msg.getCorrelationId(), EventType.MSG_SERVICED, ( System.currentTimeMillis() - startTime ) ) );
//    if ( reply == null )
//      return new EucalyptusErrorMessageType( this.getClass().getSimpleName(), msg, "Received a NULL reply" );
//    return reply;
//  }

}
