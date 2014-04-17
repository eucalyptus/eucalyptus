/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cluster.callback;

import static com.eucalyptus.cloud.VmInstanceLifecycleHelpers.NetworkResourceVmInstanceLifecycleHelper;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.address.AddressingDispatcher;
import com.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.cloud.VmRunType;
import com.eucalyptus.cluster.ResourceState.NoSuchTokenException;
import com.eucalyptus.compute.common.network.PublicIPResource;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.MessageCallback;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmRunResponseType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class VmRunCallback extends MessageCallback<VmRunType, VmRunResponseType> {
  
  private static Logger       LOG = Logger.getLogger( VmRunCallback.class );
  
  private final ResourceToken token;
  
  public VmRunCallback( final VmRunType msg, final ResourceToken token ) {
    super( msg );
    this.token = token;
    LOG.debug( this.token );
  }
  
  @Override
  public void initialize( final VmRunType msg ) {
    LOG.debug( this.token + ":" + msg );

    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      final VmInstance vm = VmInstances.lookup( msg.getInstanceId( ) );
      msg.setUserId( vm.getOwnerUserId( ) );
      msg.setOwnerId( vm.getOwnerUserId( ) );
      msg.setAccountId( vm.getOwnerAccountNumber( ) );
      if ( !VmState.PENDING.apply( vm) ) {
        throw new EucalyptusClusterException( "Intercepted a RunInstances request for an instance with unexpected state: " + vm.getState( ) );
      }
      db.rollback( );
    } catch ( final Exception e ) {
      LOG.error( e );
      Logs.extreme( ).error( e, e );
      db.rollback( );
      try {
        this.token.abort( );
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      }
      throw new EucalyptusClusterException( "Error while initializing request state: " + this.getRequest( ), e );
    } finally {
      if ( db.isActive( ) ) db.rollback( );
    }

    try {
      this.token.submit( );
    } catch ( final NoSuchTokenException e2 ) {
      LOG.error( e2 );
      Logs.extreme( ).error( e2, e2 );
    }
  }
  
  @Override
  public void fire( final VmRunResponseType reply ) {
    Logs.extreme( ).error( reply );
    try {
      this.token.redeem( );
    } catch ( Exception ex ) {
      LOG.error( this.token + ": " + ex );
      Logs.extreme( ).error( this.token + ": " + ex, ex );
    }
    final Function<VmInfo, Boolean> updateInstance = new Function<VmInfo, Boolean>( ) {
      @Override
      public Boolean apply( final VmInfo input ) {
        final VmInstance vm = VmInstances.lookup( input.getInstanceId( ) );
        vm.updateAddresses( input.getNetParams( ).getIpAddress( ), input.getNetParams( ).getIgnoredPublicIp( ) );
        try {
          vm.updateMacAddress( input.getNetParams( ).getMacAddress( ) );
          vm.setServiceTag( input.getServiceTag( ) );
        } catch ( Exception ex ) {
          LOG.error( VmRunCallback.this.token + ": " + ex );
          Logs.extreme( ).error( VmRunCallback.this.token + ": " + ex, ex );
        }
        final Address addr = getAddress( );
        if ( addr != null ) {
            AddressingDispatcher.dispatch(
                AsyncRequests.newRequest( addr.assign( vm ).getCallback( ) ).then(
                    new Callback.Success<BaseMessage>( ) {
                      @Override
                      public void fire( final BaseMessage response ) {
                        Addresses.updatePublicIpByInstanceId( vm.getInstanceId(), addr.getName() );
                      }
                    }
                ),
                vm.getPartition( ) );
        }
        return true;
      }
    };
    for ( final VmInfo vmInfo : reply.getVms( ) ) {
      if ( this.token.getInstanceId( ).equals( vmInfo.getInstanceId( ) ) ) {
        try {
          Entities.asTransaction( VmInstance.class, updateInstance, 10 ).apply( vmInfo );
          break;
        } catch ( RuntimeException ex ) {
          LOG.error( "Failed: " + this.token + " because of " + ex.getMessage( ), ex );
          throw ex;
        }
      }
      throw new EucalyptusClusterException( "ccRunInstancesResponse: does not contain requested instance information for: "
                                            + this.token.getInstanceId( )
                                            + " but return status is "
                                            + reply.get_return( )
                                            + "\nccRunInstancesResponse:vms="
                                            + reply.getVms( ) );
    }
  }
  
  @Override
  public void fireException( final Throwable e ) {
    LOG.debug( LogUtil.header( "Failing run instances because of: " + e.getMessage( ) ), e );
    LOG.debug( LogUtil.subheader( VmRunCallback.this.getRequest( ).toString( ) ) );
    Predicate<Throwable> rollbackToken = new Predicate<Throwable>( ) {
      
      @Override
      public boolean apply( Throwable input ) {
        LOG.debug( "-> Release resource tokens for unused resources." );
        try {
          VmRunCallback.this.token.abort( );
        } catch ( final Exception ex ) {
          LOG.error( ex.getMessage( ) );
          Logs.extreme( ).error( ex, ex );
        }
        return true;
      }
    };
    try {
      Entities.asTransaction( VmInstance.class, Functions.forPredicate( rollbackToken ) ).apply( e );
    } catch ( Exception ex ) {
      Logs.extreme( ).error( ex, ex );
    }
  }

  private Address getAddress( ) {
    final PublicIPResource publicIPResource = (PublicIPResource) Iterables.find(
        VmRunCallback.this.token.getAttribute( NetworkResourceVmInstanceLifecycleHelper.NetworkResourcesKey ),
        Predicates.instanceOf( PublicIPResource.class ),
        null );
    return publicIPResource!=null && publicIPResource.getValue()!=null ?
        Addresses.getInstance().lookup( publicIPResource.getValue() ) :
        null;
  }

  @Override
  public String toString( ) {
    return "VmRunCallback " + this.token;
  }
}
