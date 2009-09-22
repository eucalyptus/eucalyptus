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
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

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
import edu.ucsb.eucalyptus.msgs.StopNetworkType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;
import edu.ucsb.eucalyptus.util.Admin;

public class ClusterAllocator extends Thread {

  private static Logger LOG = Logger.getLogger( ClusterAllocator.class );

  private State state;
  private AtomicBoolean rollback;
  Multimap<State, QueuedEvent> msgMap;
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

  @SuppressWarnings( "unchecked" )
  public void setupNetworkMessages( NetworkToken networkToken ) {
    if ( networkToken != null ) {
      StartNetworkType msg = new StartNetworkType( this.vmAllocInfo.getRequest(), networkToken.getVlan(), networkToken.getNetworkName() );
      this.msgMap.put( State.CREATE_NETWORK, QueuedEvent.make( new StartNetworkCallback( networkToken ), msg ) );
      this.msgMap.put( State.ROLLBACK, QueuedEvent.make( new StopNetworkCallback( networkToken ), new StopNetworkType( msg ) ) );
    }
    try {
      RunInstancesType request = this.vmAllocInfo.getRequest();
      Network network = Networks.getInstance().lookup( networkToken.getName() );
      LOG.info( "Setting up rules for: " + network.getName() );
      LOG.debug( network );
      ConfigureNetworkType msg = new ConfigureNetworkType( network.getRules() );
      msg.setUserId( networkToken.getUserName( ) );
      msg.setEffectiveUserId( networkToken.getUserName( ) );
      if ( !network.getRules().isEmpty() ) {
        this.msgMap.put( State.CREATE_NETWORK_RULES, QueuedEvent.make( ConfigureNetworkCallback.CALLBACK, msg ) );
      }
      //:: need to refresh the rules on the backend for all active networks which point to this network :://
      for( Network otherNetwork : Networks.getInstance().listValues() ) {
        if( otherNetwork.isPeer( network.getUserName(), network.getNetworkName() ) ) {
          LOG.warn( "Need to refresh rules for incoming named network ingress on: " + otherNetwork.getName() );
          LOG.debug( otherNetwork );
          ConfigureNetworkType omsg = new ConfigureNetworkType( otherNetwork.getRules() );
          omsg.setUserId( otherNetwork.getUserName() );
          omsg.setEffectiveUserId( Component.eucalyptus.name() );
          this.msgMap.put( State.CREATE_NETWORK_RULES, QueuedEvent.make( ConfigureNetworkCallback.CALLBACK, omsg ) );
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

    VmRunType run = new VmRunType( request, rsvId, request.getUserData(), token.getAmount(), imgInfo, vmInfo, keyInfo, token.getInstanceIds(), macs, vlan, networkNames, token.getNetworkIndexes( ) );
    this.msgMap.put( State.CREATE_VMS, QueuedEvent.make( new VmRunCallback( this, token ), run ) );
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
      try {
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
      } catch ( Throwable e ) {
        LOG.error( e, e );
      }
    }
  }

  public void clearQueue() {
    QueuedEvent event = null;
    while ( ( event = this.pendingEvents.poll() ) != null ) {
      Object o = null;
      QueuedEventCallback queuedCallback = null;
      try {
        LOG.debug( "-> Waiting for: " + LogUtil.lineObject( event.getCallback( ) ) );
        o = event.getCallback().getResponse( );
      } catch( Throwable t ) {
        LOG.debug( t, t );
      }
    }
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
    ROLLBACK;
  }

}
