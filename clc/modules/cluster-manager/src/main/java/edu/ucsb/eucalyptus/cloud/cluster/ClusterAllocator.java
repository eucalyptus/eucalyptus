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
package edu.ucsb.eucalyptus.cloud.cluster;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.cloud.VmImageInfo;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.cloud.VmRunType;
import edu.ucsb.eucalyptus.cloud.ws.AddressManager;
import edu.ucsb.eucalyptus.msgs.AssociateAddressType;
import edu.ucsb.eucalyptus.msgs.ConfigureNetworkType;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.StartNetworkType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;
import edu.ucsb.eucalyptus.util.Admin;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

class ClusterAllocator extends Thread {

  private static Logger LOG = Logger.getLogger( ClusterAllocator.class );

  private State state;
  private AtomicBoolean rollback;
  protected Multimap<State, QueuedEvent> msgMap;
  private Cluster cluster;
  private ConcurrentLinkedQueue<QueuedEvent> pendingEvents;
  private VmAllocationInfo vmAllocInfo;

  public ClusterAllocator( ResourceToken vmToken, VmAllocationInfo vmAllocInfo ) {
    this.msgMap = Multimaps.newHashMultimap();
    this.vmAllocInfo = vmAllocInfo;
    this.pendingEvents = new ConcurrentLinkedQueue<QueuedEvent>();
    this.cluster = Clusters.getInstance().lookup( vmToken.getCluster() );
    this.state = State.START;
    this.rollback = new AtomicBoolean( false );
    for ( NetworkToken networkToken : vmToken.getNetworkTokens() )
      this.setupNetworkMessages( networkToken );
    this.setupVmMessages( vmToken );
  }

  public void setupAddressMessages( List<String> addresses, List<VmInfo> runningVms ) {
    if ( EucalyptusProperties.disableNetworking ) {
      return;
    } else if ( addresses.size() < runningVms.size() ) {
      LOG.error( "Number of running VMs is greater than number of assigned addresses!" );
    } else {
      AddressManager.updateAddressingMode();
      for ( VmInfo vm : runningVms ) {
        String addr = addresses.remove( 0 );
        try {
          vm.getNetParams().setIgnoredPublicIp( addr );
          AssociateAddressType msg = new AssociateAddressType( addr, vm.getInstanceId() );
          msg.setUserId( vm.getOwnerId() );
          msg.setEffectiveUserId( Component.eucalyptus.name() );
          new AddressManager().AssociateAddress( msg );
        } catch ( Exception e ) {
          LOG.error( e );
        }
      }
    }
    for( String addr : addresses ) {
      try {
        new AddressManager().ReleaseAddress( Admin.makeMsg( ReleaseAddressType.class, addr ) );
        LOG.warn( "Released unused public address: " + addr );
      } catch ( EucalyptusCloudException e ) {}
    }

  }

  public void setupNetworkMessages( NetworkToken networkToken ) {
    if ( networkToken != null ) {
      StartNetworkType msg = new StartNetworkType( this.vmAllocInfo.getRequest(), networkToken.getVlan(), networkToken.getNetworkName() );
      StartNetworkCallback callback = new StartNetworkCallback( this.cluster.getConfiguration( ), this, networkToken );
      QueuedEvent<StartNetworkType> event = new QueuedEvent<StartNetworkType>( callback, msg );
      this.msgMap.put( State.CREATE_NETWORK, event );
    }
    try {
      Network network = Networks.getInstance().lookup( networkToken.getName() );
      LOG.warn( "Setting up rules for: " + network.getName() );
      LOG.debug( network );
      if ( !network.getRules().isEmpty() ) {
        QueuedEvent event = new QueuedEvent<ConfigureNetworkType>( new ConfigureNetworkCallback(this.cluster.getConfiguration( )), new ConfigureNetworkType( this.vmAllocInfo.getRequest(), network.getRules() ) );
        this.msgMap.put( State.CREATE_NETWORK_RULES, event );
      }
      //:: need to refresh the rules on the backend for all active networks which point to this network :://
      for( Network otherNetwork : Networks.getInstance().listValues() ) {
        if( otherNetwork.isPeer( network.getUserName(), network.getNetworkName() ) ) {
          LOG.warn( "Need to refresh rules for incoming named network ingress on: " + otherNetwork.getName() );
          LOG.debug( otherNetwork );
          ConfigureNetworkType msg = new ConfigureNetworkType( otherNetwork.getRules() );
          msg.setUserId( otherNetwork.getUserName() );
          msg.setEffectiveUserId( Component.eucalyptus.name() );
          this.msgMap.put( State.CREATE_NETWORK_RULES, new QueuedEvent<ConfigureNetworkType>( new ConfigureNetworkCallback(this.cluster.getConfiguration( )), msg ) );
        }
      }
    } catch ( NoSuchElementException e ) {}/* just added this network, shouldn't happen, if so just smile and nod */
  }

  public void setupVmMessages( ResourceToken token ) {
    List<String> macs = new ArrayList<String>();
    List<String> networkNames = new ArrayList<String>();

    for ( String instanceId : token.getInstanceIds() )
      macs.add( VmInstances.getAsMAC( instanceId ) );

    int vlan = -1;
    for ( Network net : vmAllocInfo.getNetworks() ) {
      networkNames.add( net.getNetworkName() );
      if ( vlan < 0 ) vlan = Networks.getInstance().lookup( net.getName() ).getToken( token.getCluster() ).getVlan();
    }
    if ( vlan < 0 ) vlan = 9;

    RunInstancesType request = this.vmAllocInfo.getRequest();
    VmImageInfo imgInfo = this.vmAllocInfo.getImageInfo();
    VmTypeInfo vmInfo = this.vmAllocInfo.getVmTypeInfo();
    String rsvId = this.vmAllocInfo.getReservationId();
    VmKeyInfo keyInfo = this.vmAllocInfo.getKeyInfo();

    VmRunType run = new VmRunType( request, rsvId, request.getUserData(), token.getAmount(), imgInfo, vmInfo, keyInfo, token.getInstanceIds(), macs, vlan, networkNames );
    this.msgMap.put( State.CREATE_VMS, new QueuedEvent<VmRunType>( new VmRunCallback( this.cluster.getConfiguration( ), this, token ), run ) );
  }

  public void setState( final State state ) {
    this.clearQueue();
    if ( this.rollback.get() && !State.ROLLBACK.equals( this.state ) )
      this.state = State.ROLLBACK;
    else if ( this.rollback.get() && State.ROLLBACK.equals( this.state ) )
      this.state = State.FINISHED;
    else
      this.state = state;
  }

  public void run() {
    this.state = State.CREATE_NETWORK;
    while ( !this.state.equals( State.FINISHED ) ) {
      this.queueEvents();
      switch ( this.state ) {
        case CREATE_NETWORK:
          this.setState( State.CREATE_NETWORK_RULES );
          break;
        case CREATE_NETWORK_RULES:
          this.setState( State.CREATE_VMS );
          break;
        case CREATE_VMS:
          this.setState( State.ASSIGN_ADDRESSES );
          break;
        case ASSIGN_ADDRESSES:
          this.setState( State.FINISHED );
          break;
        case ROLLBACK:
          this.setState( State.FINISHED );
          break;
      }
      this.clearQueue();
    }
  }

  public void clearQueue() {
    QueuedEvent event = null;
    while ( ( event = this.pendingEvents.poll() ) != null )
      event.getCallback().waitForEvent();
  }

  private void queueEvents() {
    for ( QueuedEvent event : this.msgMap.get( this.state ) ) {
      this.pendingEvents.add( event );
      this.cluster.getMessageQueue().enqueue( event );
    }
  }

  public void returnAllocationTokens() {
    for( ResourceToken token : this.vmAllocInfo.getAllocationTokens() ) {
      try {
        Clusters.getInstance().lookup( token.getCluster() ).getNodeState().redeemToken( token );
      } catch ( NoSuchTokenException e ) {
      }
    }
  }


  public AtomicBoolean getRollback() {
    return rollback;
  }

  enum State {

    START,
    CREATE_NETWORK,
    CREATE_NETWORK_RULES,
    CREATE_VMS,
    ASSIGN_ADDRESSES,
    FINISHED,
    ROLLBACK
  }

}
