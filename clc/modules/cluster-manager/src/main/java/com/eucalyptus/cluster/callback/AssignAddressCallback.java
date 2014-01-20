/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Address.Transition;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.address.AddressingDispatcher;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.MessageCallback;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstance.VmStateSet;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmInstances.TerminatedInstanceException;
import com.eucalyptus.vm.VmNetworkConfig;
import edu.ucsb.eucalyptus.msgs.AssignAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AssignAddressType;

public class AssignAddressCallback extends MessageCallback<AssignAddressType, AssignAddressResponseType> {
  private static Logger    LOG = Logger.getLogger( AssignAddressCallback.class );
  
  private final Address    address;
  private final VmInstance vm;
  
  public AssignAddressCallback( Address address ) {
    super( new AssignAddressType( address.getStateUuid( ), address.getName( ), address.getInstanceAddress( ), address.getInstanceId( ) ) );
    this.address = address;
    this.vm = lookupVm( );
  }

  private VmInstance lookupVm( ) {
    try {
      VmInstance foundVm = VmInstances.lookup( super.getRequest( ).getInstanceId( ) );
      if ( VmStateSet.RUN.apply( foundVm ) ) {
        return foundVm;
      } else {
        return null;
      }
    } catch ( TerminatedInstanceException ex ) {
      return null;
    } catch ( NoSuchElementException ex ) {
      return null;
    }
  }
  
  @Override
  public void initialize( AssignAddressType msg ) {
    LOG.debug( this.address.toString( ) );
  }
  
  @Override
  public void fire( AssignAddressResponseType msg ) {
    try {
      if ( !this.checkVmState( ) ) {
        this.clearState( );
      } else {
        this.address.clearPending( );
        EventRecord.here( AssignAddressCallback.class, EventType.ADDRESS_ASSIGNED, Address.State.assigned.toString( ), this.address.toString( ) ).info( );
      }
    } catch ( Exception e ) {
      LOG.debug( e, e );
      this.clearState( );
    }
  }
  
  @Override
  public void fireException( Throwable e ) {
    LOG.error( e );
    Logs.extreme( ).error( e, e );
    this.clearState( );
  }

  private void clearState( ) {
    EventRecord.here( AssignAddressCallback.class, EventType.ADDRESS_ASSIGNING, Transition.assigning.toString( ), "FAILED", this.address.toString( ) ).debug( );
    if ( this.address.isPending( ) ) {
      try {
        this.address.clearPending( );
      } catch ( Exception ex ) {}
    }
    if ( this.address.isSystemOwned( ) ) {
      Addresses.release( this.address );
    } else if ( this.address.isAssigned( ) && this.vm != null ) {
      AddressingDispatcher.dispatch( AsyncRequests.newRequest( this.address.unassign().getCallback() ), vm.getPartition() );
    } else if ( this.address.isAssigned( ) && this.vm == null ) {
      this.address.unassign( ).clearPending( );
    }
  }
  
  private boolean checkVmState( ) {
    try {
      if ( this.vm != null ) {
        VmInstance foundVm = VmInstances.lookup( this.vm.getInstanceId( ) );
        if ( !VmStateSet.RUN.apply( foundVm ) ) {
          return false;
        } else {
          return true;
        }
      } else {
        return false;
      }
    } catch ( NoSuchElementException e ) {
      LOG.debug( e, e );
      return false;
    }
  }
  
  @Override
  public String toString( ) {
    return "AssignAddressCallback " + this.address + " vm=" + ( this.vm != null ? this.vm.getInstanceId( ) : "none" );
  }

}
