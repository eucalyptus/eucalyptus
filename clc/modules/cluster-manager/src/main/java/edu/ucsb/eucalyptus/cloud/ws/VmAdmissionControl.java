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

import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.FailScriptFailException;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.SLAs;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.cloud.cluster.NotEnoughResourcesAvailable;
import edu.ucsb.eucalyptus.cloud.entities.Counters;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import java.util.List;
import java.util.NavigableSet;

public class VmAdmissionControl {

  private static Logger LOG = Logger.getLogger( VmAdmissionControl.class );

  public VmAllocationInfo verify( RunInstancesType request ) throws EucalyptusCloudException {
    //:: encapsulate the request into a VmAllocationInfo object and forward it on :://
    VmAllocationInfo vmAllocInfo = new VmAllocationInfo( request );

    vmAllocInfo.setReservationIndex( Counters.getIdBlock( request.getMaxCount() ) );

    String userData = vmAllocInfo.getRequest().getUserData();
    if ( userData != null ) {
      try {
        userData = new String( Base64.decode( vmAllocInfo.getRequest().getUserData() ) );
      } catch ( Exception e ) {
        userData = "";
      }
    }
    else { userData = ""; }
    vmAllocInfo.setUserData( userData );
    vmAllocInfo.getRequest().setUserData( new String( Base64.encode( userData.getBytes() ) ) );
    return vmAllocInfo;
  }

  public VmAllocationInfo evaluate( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    SLAs sla = new SLAs();
    List<ResourceToken> allocTokeList = null;
    boolean failed = false;
    boolean hasVms = false;
    try {
      allocTokeList = sla.doVmAllocation( vmAllocInfo );
      int addrCount = 0;
      for ( ResourceToken token : allocTokeList ) {
        addrCount += token.getAmount();
      }
      if ( !EucalyptusProperties.disableNetworking && ( "public".equals( vmAllocInfo.getRequest().getAddressingType() ) || vmAllocInfo.getRequest().getAddressingType() == null ) ) {
        NavigableSet<String> addresses = AddressManager.allocateAddresses( addrCount );
        for ( ResourceToken token : allocTokeList ) {
          for ( int i = 0; i < token.getAmount(); i++ ) {
            token.getAddresses().add( addresses.pollFirst() );
          }
        }
      }
      vmAllocInfo.getAllocationTokens().addAll( allocTokeList );
      sla.doNetworkAllocation( vmAllocInfo.getRequest().getUserId(), vmAllocInfo.getAllocationTokens(), vmAllocInfo.getNetworks() );
    } catch ( FailScriptFailException e ) {
      failed = true;
      LOG.debug( e, e );
    } catch ( NotEnoughResourcesAvailable e ) {
      failed = true;
      LOG.debug( e, e );
    }
    if ( failed ) {
      if ( allocTokeList != null ) {
        for ( ResourceToken token : allocTokeList ) {
          Clusters.getInstance().lookup( token.getCluster() ).getNodeState().releaseToken( token );
        }
        throw new EucalyptusCloudException( "Not enough resources available: addresses (try --addressing private)");
      }
      throw new EucalyptusCloudException( "Not enough resources available: vm resources.");
    }

    return vmAllocInfo;
  }

}


