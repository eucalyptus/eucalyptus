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
package com.eucalyptus.cluster.callback;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.Networks;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.vm.SystemState.Reason;
import com.eucalyptus.vm.VmState;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmRunResponseType;
import edu.ucsb.eucalyptus.cloud.VmRunType;

public class VmRunCallback extends QueuedEventCallback<VmRunType,VmRunResponseType> {

  private static Logger LOG = Logger.getLogger( VmRunCallback.class );

  private ResourceToken token;
  
  public VmRunCallback( final VmRunType msg, final ResourceToken token ) {
    this.token = token;
    this.setRequest( msg );
  }

  public void prepare( final VmRunType msg ) throws Exception {
//    LOG.trace( LogUtil.subheader( msg.toString( ) ) );
    for( String vmId : msg.getInstanceIds( ) ) {
      try {
        VmInstance vm = VmInstances.getInstance( ).lookup( vmId );
        if( !VmState.PENDING.equals( vm.getState( ) ) ) {
          throw new EucalyptusClusterException("Intercepted a RunInstances request for an instance which has meanwhile been terminated." );
        }
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
    }
    try {
      Clusters.getInstance().lookup( token.getCluster() ).getNodeState().submitToken( token );
    } catch ( Exception e2 ) {
      LOG.debug( e2, e2 );
      throw e2;
    }
  }

  @Override
  public void verify( VmRunResponseType reply ) throws Exception {
    try {
      Clusters.getInstance().lookup( token.getCluster() ).getNodeState().redeemToken( token );
    } catch ( Throwable e ) {
      this.fail( e );
      LOG.debug( e, e );
      return;
    } 
    try {
      if ( reply != null && reply.getVms( ).isEmpty( ) ) {
        this.fail( new EucalyptusClusterException( "Failed to run the requested instances because the request was rejected: " + token.toString( ) ) );
      } else if ( reply != null && !reply.getVms( ).isEmpty( ) ) {
        Set<String> liveIds = Sets.newHashSet( );
        for ( VmInfo vmInfo : reply.getVms() ) {
          liveIds.add( vmInfo.getInstanceId( ) );
          try {
            VmInstance vm = VmInstances.getInstance().lookup( vmInfo.getInstanceId() );
            vm.updateAddresses( vmInfo.getNetParams().getIpAddress(), vmInfo.getNetParams().getIgnoredPublicIp() );
            vm.clearPending( );
          } catch ( Throwable ex ) {
            Exceptions.eat( "UNIDENTIFIED VM: " + vmInfo.toString( ) );
          }
        }
        Set<String> requestIds = Sets.newHashSet( token.getInstanceIds( ) );
        requestIds.removeAll( liveIds );
        for( String id : requestIds ) {
          int idx = token.getInstanceIds( ).indexOf( id );
          try {
            VmInstance vm = VmInstances.getInstance().lookup( id );
            vm.clearPending( );
            vm.setState( VmState.TERMINATED, Reason.FAILED, "This instance failed to be run by the CC." );
            LOG.error( "Failed virtual machine: " + vm.toString( ) );
          } catch ( Throwable ex ) {
            LOG.trace( "Failed virtual machine: " + id );
          }          
          if( idx > 0 && token.getAddresses( ).size( ) > idx ) {
            try {
              String addr = token.getAddresses( ).get( idx );
              LOG.warn( "Failed virtual machine: " + id + " releasing address: " + addr ); 
              Addresses.getInstance( ).lookup( addr ).release( );
            } catch ( Throwable ex ) {
              LOG.error( ex , ex );
            }
          }
        }
      } else {
        this.fail( new EucalyptusClusterException( "RunInstances returned false." + this.getRequest( ) ) );
      }
    } catch ( Exception e ) { 
      LOG.debug( e, e );
      throw e; 
    }
  }


  @Override
  public void fail( Throwable e ) {
    LOG.debug( LogUtil.header( "Failing run instances because of: " + e.getMessage( ) ), e );
    LOG.debug( LogUtil.subheader( this.getRequest( ).toString( ) ) );
    for( String addr : this.token.getAddresses() ) {
      try {
        LOG.debug( "-> Release addresses from failed vm run allocation: " + addr );
        Addresses.getInstance().lookup( addr ).release( );
      } catch ( NoSuchElementException e1 ) {
        LOG.debug( "-> Failed to release addresses from failed vm run allocation: " + addr );
      }
    }
    LOG.debug( "-> Release network index allocation." );
    if( this.token.getPrimaryNetwork( ) != null ) {
      try {
        NetworkToken net = this.token.getPrimaryNetwork( );
        Network network = Networks.getInstance( ).lookup( net.getName( ) );
        for( Integer index : this.token.getPrimaryNetwork( ).getIndexes( ) ) {
          network.returnNetworkIndex( index );
        }
      } catch ( Throwable e2 ) {
        LOG.debug( e2, e2 );
      }
    }
    LOG.debug( "-> Release resource tokens for unused resources." );
    try {
      Clusters.getInstance().lookup( token.getCluster() ).getNodeState().releaseToken( token );
    } catch ( Throwable e2 ) {
      LOG.debug( e2, e2 );
    }
  }


}
