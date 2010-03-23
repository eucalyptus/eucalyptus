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
package com.eucalyptus.sla;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.AddressCategory;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.Networks;
import com.eucalyptus.cluster.SuccessCallback;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.cloud.VmImageInfo;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.cloud.VmRunResponseType;
import edu.ucsb.eucalyptus.cloud.VmRunType;
import edu.ucsb.eucalyptus.cloud.cluster.ConfigureNetworkCallback;
import edu.ucsb.eucalyptus.cloud.cluster.MultiClusterCallback;
import edu.ucsb.eucalyptus.cloud.cluster.NoSuchTokenException;
import edu.ucsb.eucalyptus.cloud.cluster.QueuedEventCallback;
import edu.ucsb.eucalyptus.cloud.cluster.StartNetworkCallback;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.cloud.cluster.VmRunCallback;
import edu.ucsb.eucalyptus.msgs.ConfigureNetworkType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class ClusterAllocator extends Thread {
  
  private static Logger                              LOG            = Logger.getLogger( ClusterAllocator.class );
  public static Boolean                              SPLIT_REQUESTS = true;
  private State                                      state;
  private AtomicBoolean                              rollback;
  private Multimap<State, QueuedEventCallback>       msgMap;
  private Cluster                                    cluster;
  private ConcurrentLinkedQueue<QueuedEventCallback> pendingEvents;
  private VmAllocationInfo                           vmAllocInfo;
  
  public ClusterAllocator( ResourceToken vmToken, VmAllocationInfo vmAllocInfo ) {
    this.msgMap = Multimaps.newHashMultimap( );
    this.pendingEvents = new ConcurrentLinkedQueue<QueuedEventCallback>( );
    this.state = State.START;
    this.rollback = new AtomicBoolean( false );
    this.vmAllocInfo = vmAllocInfo;
    if ( vmToken != null ) {
      try {
        this.cluster = Clusters.getInstance( ).lookup( vmToken.getCluster( ) );
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
      }
    }
  }
  
  private void addRequest( State state, QueuedEventCallback callback ) {
    this.msgMap.put( State.CREATE_NETWORK, callback );
  }
  
  @SuppressWarnings( "unchecked" )
  public void setupNetworkMessages( NetworkToken networkToken ) {
    if ( networkToken != null ) {
      QueuedEventCallback callback = new StartNetworkCallback( networkToken ).regarding( vmAllocInfo.getRequest( ) );
      this.msgMap.put( State.CREATE_NETWORK, callback );
    }
    try {
      RunInstancesType request = this.vmAllocInfo.getRequest( );
      if ( networkToken != null ) {
        Network network = Networks.getInstance( ).lookup( networkToken.getName( ) );
        LOG.debug( LogUtil.header( "Setting up rules for: " + network.getName( ) ) );
        LOG.debug( LogUtil.subheader( network.toString( ) ) );
        ConfigureNetworkType msg = new ConfigureNetworkType( network.getRules( ) );
        msg.setUserId( networkToken.getUserName( ) );
        msg.setEffectiveUserId( networkToken.getUserName( ) );
        if ( !network.getRules( ).isEmpty( ) ) {
          this.addRequest( State.CREATE_NETWORK_RULES, new ConfigureNetworkCallback( msg ) );
        }
        //:: need to refresh the rules on the backend for all active networks which point to this network :://
        for ( Network otherNetwork : Networks.getInstance( ).listValues( ) ) {
          if ( otherNetwork.isPeer( network.getUserName( ), network.getNetworkName( ) ) ) {
            LOG.warn( "Need to refresh rules for incoming named network ingress on: " + otherNetwork.getName( ) );
            LOG.debug( otherNetwork );
            ConfigureNetworkType omsg = new ConfigureNetworkType( otherNetwork.getRules( ) );
            omsg.setUserId( otherNetwork.getUserName( ) );
            omsg.setEffectiveUserId( Component.eucalyptus.name( ) );
            if ( !otherNetwork.getRules( ).isEmpty( ) ) {
              this.addRequest( State.CREATE_NETWORK_RULES, new ConfigureNetworkCallback( omsg ) );
            }
          }
        }
      }
    } catch ( NoSuchElementException e ) {}/* just added this network, shouldn't happen, if so just smile and nod */
  }
  
  public void setupVmMessages( final ResourceToken token ) {
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
      networkNames = Lists.newArrayList( "default" );
      networkIndexes = Lists.newArrayList( "-1" );
    }
    
    final List<String> addresses = Lists.newArrayList( token.getAddresses( ) );
    RunInstancesType request = this.vmAllocInfo.getRequest( );
    String rsvId = this.vmAllocInfo.getReservationId( );
    VmImageInfo imgInfo = this.vmAllocInfo.getImageInfo( );
    VmKeyInfo keyInfo = this.vmAllocInfo.getKeyInfo( );
    VmTypeInfo vmInfo = this.vmAllocInfo.getVmTypeInfo( );
    String userData = this.vmAllocInfo.getUserData( );
    QueuedEventCallback cb = null;
    try {
      int index = 0;
      for ( ResourceToken childToken : this.cluster.getNodeState( ).splitToken( token ) ) {
        List<String> instanceIds = Lists.newArrayList( token.getInstanceIds( ).get( index ) );
        List<String> netIndexes = Lists.newArrayList( networkIndexes.get( index ) );
        List<String> addrList = Lists.newArrayList( );
        if ( !addresses.isEmpty( ) ) {
          addrList.add( addresses.get( index ) );
        }
        cb = makeRunRequest( childToken, rsvId, instanceIds, imgInfo, keyInfo, vmInfo, vlan, networkNames, netIndexes, addrList, userData );
        this.addRequest( State.CREATE_VMS, cb );
        index++;
      }
    } catch ( NoSuchTokenException e ) {
      cb = makeRunRequest( token, rsvId, token.getInstanceIds( ), imgInfo, keyInfo, vmInfo, vlan, networkNames, networkIndexes, addresses, userData );
    }
    this.addRequest( State.CREATE_VMS, cb );
  }
  
  private QueuedEventCallback makeRunRequest( ResourceToken childToken, String rsvId, List<String> instanceIds, 
                                                         VmImageInfo imgInfo, VmKeyInfo keyInfo, VmTypeInfo vmInfo, 
                                                         Integer vlan, List<String> networkNames, List<String> netIndexes, 
                                                         final List<String> addrList, String userData ) {
    List<String> macs = Lists.transform( instanceIds, new Function<String, String>( ) {
      @Override
      public String apply( String instanceId ) {
        return VmInstances.getAsMAC( instanceId );
      }
    } );
    VmRunType run = new VmRunType( rsvId, userData, childToken.getAmount( ), imgInfo, vmInfo, keyInfo, instanceIds, macs, vlan, networkNames, netIndexes );
    VmRunCallback cb = new VmRunCallback( run, this, childToken );
    if ( !addrList.isEmpty( ) ) {
      cb.then( new SuccessCallback<VmRunResponseType>( ) {
        @Override
        public void apply( VmRunResponseType response ) {
          Iterator<String> addrs = addrList.iterator( );
          for ( VmInfo vmInfo : response.getVms( ) ) {//TODO: this will have some funny failure characteristics
            Address addr = Addresses.getInstance( ).lookup( addrs.next( ) );
            VmInstance vm = VmInstances.getInstance( ).lookup( vmInfo.getInstanceId( ) );
            AddressCategory.assign( addr, vm ).dispatch( addr.getCluster( ) );
          }
        }
      } );
    }
    return cb;
  }
  public void setState( final State state ) {
    this.clearQueue( );
    if ( this.rollback.get( ) && !State.ROLLBACK.equals( this.state ) )
      this.state = State.ROLLBACK;
    else if ( this.rollback.get( ) && State.ROLLBACK.equals( this.state ) )
      this.state = State.FINISHED;
    else this.state = state;
  }
  
  public void run( ) {
    this.state = State.CREATE_NETWORK;
    while ( !this.state.equals( State.FINISHED ) ) {
      try {
        this.queueEvents( );
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
        this.clearQueue( );
      } catch ( Throwable e ) {
        LOG.error( e, e );
      }
    }
  }
  
  public void clearQueue( ) {
    QueuedEventCallback event = null;
    while ( ( event = this.pendingEvents.poll( ) ) != null ) {
      Object o = null;
      try {
        o = event.getResponse( );
      } catch ( Throwable t ) {
        LOG.debug( t, t );
        this.rollback.lazySet( true );
        this.state = State.ROLLBACK;
      }
    }
  }
  
  @SuppressWarnings( "unchecked" )
  private void queueEvents( ) {
    for ( QueuedEventCallback event : this.msgMap.get( this.state ) ) {
      if ( event instanceof MultiClusterCallback ) {
        MultiClusterCallback callback = ( MultiClusterCallback ) event;
        for( Cluster c : Clusters.getInstance( ).listValues( ) ) {
          QueuedEventCallback subEvent = callback.newInstance( );
          this.pendingEvents.add( subEvent );
          LOG.info( "Enqueing event for cluster " + cluster.getName( ) + " of type: " + event );
          subEvent.dispatch( c );
        }
      } else {
        LOG.info( "Enqueing event for cluster " + cluster.getName( ) + " of type: " + event );
        this.pendingEvents.add( event );
        event.dispatch( cluster );
      }
    }
  }
  
  public AtomicBoolean getRollback( ) {
    return rollback;
  }
  
  enum State {
    START, CREATE_NETWORK, CREATE_NETWORK_RULES, CREATE_VMS, ASSIGN_ADDRESSES, FINISHED, ROLLBACK;
  }
  
}
