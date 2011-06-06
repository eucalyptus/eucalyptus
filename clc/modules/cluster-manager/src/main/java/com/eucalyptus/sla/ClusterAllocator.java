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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.sla;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.blockstorage.Volumes;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.Networks;
import com.eucalyptus.cluster.NoSuchTokenException;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.cluster.callback.StartNetworkCallback;
import com.eucalyptus.cluster.callback.VmRunCallback;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.Request;
import com.eucalyptus.util.async.StatefulMessageSet;
import com.eucalyptus.vm.SystemState.Reason;
import com.eucalyptus.vm.VmState;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VirtualBootRecord;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.cloud.VmRunResponseType;
import edu.ucsb.eucalyptus.cloud.VmRunType;
import edu.ucsb.eucalyptus.msgs.AttachStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.AttachStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.StartNetworkResponseType;
import edu.ucsb.eucalyptus.msgs.StartNetworkType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class ClusterAllocator extends Thread {
  private static Logger LOG = Logger.getLogger( ClusterAllocator.class );
  
  enum State {
    START, CREATE_VOLS, CREATE_IGROUPS, CREATE_NETWORK, CREATE_NETWORK_RULES, CREATE_VMS, ATTACH_VOLS, ASSIGN_ADDRESSES, FINISHED, ROLLBACK;
  }
  
  public static Boolean             SPLIT_REQUESTS = true; //TODO:GRZE:@Configurable
  private StatefulMessageSet<State> messages;
  private Cluster                   cluster;
  private Allocation                allocInfo;
  private ServiceConfiguration      sc;
  
  public static void create( ResourceToken t, Allocation allocInfo ) {
    Clusters.getInstance( ).lookup( t.getCluster( ) ).getThreadFactory( ).newThread( new ClusterAllocator( t, allocInfo ) ).start( );
  }
  
  private ClusterAllocator( ResourceToken vmToken, Allocation allocInfo ) {
    this.allocInfo = allocInfo;
    if ( vmToken != null ) {
      try {
        this.cluster = Clusters.getInstance( ).lookup( vmToken.getCluster( ) );
        this.sc = Partitions.lookupService( Storage.class, this.cluster.getPartition( ) );
        this.messages = new StatefulMessageSet<State>( this.cluster, State.values( ) );
        
        if ( this.allocInfo.getBootSet( ).getMachine( ) instanceof BlockStorageImageInfo ) {
          VirtualBootRecord root = allocInfo.getVmTypeInfo( ).lookupRoot( );
          if ( root.isBlockStorage( ) ) {
            for ( int i = 0; i < vmToken.getAmount( ); i++ ) {
              BlockStorageImageInfo imgInfo = ( ( BlockStorageImageInfo ) this.allocInfo.getBootSet( ).getMachine( ) );
              int sizeGb = ( int ) Math.ceil( imgInfo.getImageSizeBytes( ) / ( 1024l * 1024l * 1024l ) );
              LOG.debug( "About to prepare root volume using bootable block storage: " + imgInfo + " and vbr: " + root );
              Volume vol = Volumes.createStorageVolume( this.sc, this.allocInfo.getOwnerFullName( ), imgInfo.getSnapshotId( ), sizeGb, allocInfo.getRequest( ) );
              if ( imgInfo.getDeleteOnTerminate( ) ) {
                this.allocInfo.getTransientVolumes( ).add( vol );
              } else {
                this.allocInfo.getPersistentVolumes( ).add( vol );
              }
            }
          }
        }
        
        for ( NetworkToken networkToken : vmToken.getNetworkTokens( ) )
          this.setupNetworkMessages( networkToken );
        this.setupVmMessages( vmToken );
      } catch ( Throwable e ) {
        LOG.debug( e, e );
        try {
          Clusters.getInstance( ).lookup( vmToken.getCluster( ) ).getNodeState( ).releaseToken( vmToken );
        } catch ( Throwable e1 ) {
          LOG.debug( e1 );
          LOG.trace( e1, e1 );
        }
        for ( String addr : vmToken.getAddresses( ) ) {
          try {
            Addresses.release( Addresses.getInstance( ).lookup( addr ) );
          } catch ( Throwable e1 ) {
            LOG.debug( e1 );
            LOG.trace( e1, e1 );
          }
        }
        try {
          if ( vmToken.getPrimaryNetwork( ) != null ) {
            Network net = Networks.getInstance( ).lookup( vmToken.getPrimaryNetwork( ).getName( ) );
            for ( Integer i : vmToken.getPrimaryNetwork( ).getIndexes( ) ) {
              net.returnNetworkIndex( i );
            }
          }
        } catch ( Throwable e1 ) {
          LOG.debug( e1 );
          LOG.trace( e1, e1 );
        }
        for ( String vmId : vmToken.getInstanceIds( ) ) {
          try {
            VmInstance vm = VmInstances.getInstance( ).lookup( vmId );
            vm.setState( VmState.TERMINATED, Reason.FAILED, e.getMessage( ) );
            VmInstances.getInstance( ).disable( vmId );
          } catch ( Exception e1 ) {
            LOG.debug( e1, e1 );
          }
        }
      }
    }
  }
  
  @SuppressWarnings( "unchecked" )
  private void setupNetworkMessages( NetworkToken networkToken ) {
    if ( networkToken != null ) {
      Request<StartNetworkType, StartNetworkResponseType> callback = AsyncRequests.newRequest( new StartNetworkCallback( networkToken ) );
      this.messages.addRequest( State.CREATE_NETWORK, callback );
      EventRecord.here( ClusterAllocator.class, EventType.VM_PREPARE, callback.getClass( ).getSimpleName( ), networkToken.toString( ) ).debug( );
    }
  }
  
  private void setupVmMessages( final ResourceToken token ) {
    Integer vlan = null;
    List<String> networkNames = null;
    ArrayList<String> networkIndexes = Lists.newArrayList( );
    if ( token.getPrimaryNetwork( ) != null ) {
      vlan = token.getPrimaryNetwork( ).getVlan( );
      if ( vlan < 0 ) vlan = 9;//FIXME: general vlan, should be min-1?
      networkNames = Lists.newArrayList( token.getPrimaryNetwork( ).getNetworkName( ) );
      for ( Integer index : token.getPrimaryNetwork( ).getIndexes( ) ) {
        networkIndexes.add( index.toString( ) );
      }
    } else {
      vlan = -1;
      networkNames = Lists.newArrayList( Collections.nCopies( token.getAmount( ), "default" ) );
      networkIndexes = Lists.newArrayList( Collections.nCopies( token.getAmount( ), "-1" ) );
    }
    
    final List<String> addresses = Lists.newArrayList( token.getAddresses( ) );
    RunInstancesType request = this.allocInfo.getRequest( );
    String rsvId = this.allocInfo.getReservationId( );
    VmKeyInfo keyInfo = this.allocInfo.getKeyInfo( );
    VmTypeInfo vmInfo = this.allocInfo.getVmTypeInfo( );
    String userData = this.allocInfo.getRequest( ).getUserData( );
    Request cb = null;
    int index = 0;
    try {
      for ( ResourceToken childToken : this.cluster.getNodeState( ).splitToken( token ) ) {
        VirtualBootRecord root = vmInfo.lookupRoot( );
        VmTypeInfo childVmInfo = vmInfo;
        if( root.isBlockStorage( ) ) {
          childVmInfo = vmInfo.child( );
          Volume vol = this.allocInfo.getPersistentVolumes( ).get( index );
          for( String nodeTag : this.cluster.getNodeTags( ) ) {
            try {
              AttachStorageVolumeResponseType scAttachResponse = ServiceDispatcher.lookup( sc ).send( new AttachStorageVolumeType( this.cluster.getNode( nodeTag ).getIqn( ), vol.getDisplayName( ) ) );
              childVmInfo.lookupRoot( ).setResourceLocation( "iqn://" + scAttachResponse.getRemoteDeviceString( ) );
            } catch ( EucalyptusCloudException ex ) {
              LOG.error( ex , ex );
            }
          }
        }//TODO:GRZE:OMGFIXME: move this for bfe to later stage.
        cb = makeRunRequest( request, childToken, this.allocInfo.getOwnerFullName( ), rsvId, keyInfo, childVmInfo,
                             this.allocInfo.getBootSet( ).getMachine( ).getPlatform( ).name( ), vlan, networkNames,
                             userData );
        this.messages.addRequest( State.CREATE_VMS, cb );
        index++;
      }
    } catch ( NoSuchTokenException ex ) {
      throw new RuntimeException( ex );
    }
  }
  
  private Request makeRunRequest( RunInstancesType request, final ResourceToken childToken, UserFullName userFullName, String rsvId,
                                  VmKeyInfo keyInfo, VmTypeInfo vmInfo, String platform, Integer vlan, List<String> networkNames, String userData ) {
    List<String> macs = Lists.transform( childToken.getInstanceIds( ), new Function<String, String>( ) {
      @Override
      public String apply( String instanceId ) {
        return VmInstances.getAsMAC( instanceId );
      }
    } );
    
    List<String> networkIndexes = ( childToken.getPrimaryNetwork( ) == null )
      ? new ArrayList<String>( )
      : Lists.newArrayList( Iterables.transform( childToken.getPrimaryNetwork( ).getIndexes( ), Functions.toStringFunction( ) ) );
    //TODO:GRZE:ASAP use ern here instead of string name -- see KeyPairManager.resolve()

    VmRunType run = new VmRunType( rsvId, userData, childToken.getAmount( ),
                                   vmInfo, keyInfo, platform != null
                                     ? platform
                                     : "linux", /** ASAP:FIXME:GRZE **/
                                   childToken.getInstanceIds( ), macs,
                                   vlan, networkNames, networkIndexes, childToken.getInstanceUuids( ) ).regarding( request );
    run.setUserId( userFullName.getUserId( ) );
    Request<VmRunType, VmRunResponseType> req = AsyncRequests.newRequest( new VmRunCallback( run, childToken ) );
    if ( !childToken.getAddresses( ).isEmpty( ) ) {
      req.then( new Callback.Success<VmRunResponseType>( ) {
        @Override
        public void fire( VmRunResponseType response ) {
          Iterator<String> addrs = childToken.getAddresses( ).iterator( );
          for ( VmInfo vmInfo : response.getVms( ) ) {//TODO: this will have some funny failure characteristics
            final Address addr = Addresses.getInstance( ).lookup( addrs.next( ) );
            final VmInstance vm = VmInstances.getInstance( ).lookup( vmInfo.getInstanceId( ) );
            AsyncRequests.newRequest( addr.assign( vm ).getCallback( ) ).then( new Callback.Success<BaseMessage>( ) {
              public void fire( BaseMessage response ) {
                vm.updatePublicAddress( addr.getName( ) );
              }
            } ).dispatch( addr.getCluster( ) );
          }
        }
      } );
    }
    return req;
  }
  
  public void run( ) {
    this.messages.run( );
  }
  
}
