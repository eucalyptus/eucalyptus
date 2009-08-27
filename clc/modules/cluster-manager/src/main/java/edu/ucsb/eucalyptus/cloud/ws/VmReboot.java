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
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.cluster.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;

import java.util.*;

public class VmReboot extends RebootInstancesType implements Cloneable {

  private static Logger LOG = Logger.getLogger( VmReboot.class );

  private boolean isAdmin = false;
  private boolean result = false;

  public VmReboot( RebootInstancesType msg )
  {
    this.setCorrelationId( msg.getCorrelationId() );
    this.setEffectiveUserId( msg.getEffectiveUserId() );
    this.setUserId( msg.getUserId() );
    this.setInstancesSet( msg.getInstancesSet() );
  }

  public void transform() throws EucalyptusInvalidRequestException
  {
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = null;
    try
    {
      user = db.getUnique( new UserInfo( this.getUserId() ) );
      this.isAdmin = user.isAdministrator();
    }
    catch ( EucalyptusCloudException e )
    {
      db.rollback();
      throw new EucalyptusInvalidRequestException( e );
    }
    db.commit();
    if ( this.getInstancesSet() == null )
      this.setInstancesSet( new ArrayList<String>() );
  }

  public void apply() throws EucalyptusCloudException
  {
    if ( this.getInstancesSet().isEmpty() ) return;
    Map<String, RebootInstancesType> rebootMap = new HashMap<String, RebootInstancesType>();
    for ( String vmId : this.getInstancesSet() )
    {
      VmInstance vm = null;
      try
      {
        vm = VmInstances.getInstance().lookup( vmId );
        if ( vm.getOwnerId().equals( this.getUserId() ) || this.isAdmin )
        {
          if ( rebootMap.get( vm.getPlacement() ) == null )
          {
            RebootInstancesType request = new RebootInstancesType();
            request.setUserId( this.isAdmin ? Component.eucalyptus.name() : this.getUserId() );
            rebootMap.put( vm.getPlacement(), request );
          }
          rebootMap.get( vm.getPlacement() ).getInstancesSet().add( vm.getInstanceId() );
        }
        else
        {
          this.result = false;
          return;
        }
      }
      catch ( NoSuchElementException e )
      {
        this.result = false;
        return;
      }
    }
//    for ( String cluster : rebootMap.keySet() )
//      Clusters.getInstance().lookup( cluster ).getMessageQueue().enqueue( rebootMap.get( cluster ) );
    this.result = true;
    return;
  }

  public void rollback()
  {

  }

  public RebootInstancesResponseType getResult()
  {
    RebootInstancesResponseType reply = ( RebootInstancesResponseType ) this.getReply();
    reply.set_return( this.result );
    return reply;
  }

}
