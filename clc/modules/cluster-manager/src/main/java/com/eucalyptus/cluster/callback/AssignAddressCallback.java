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

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.AddressCategory;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.address.Address.Transition;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.vm.VmState;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.msgs.AssignAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AssignAddressType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;

public class AssignAddressCallback extends QueuedEventCallback<AssignAddressType,AssignAddressResponseType> {
  private static Logger LOG = Logger.getLogger( AssignAddressCallback.class );
  
  private Address       address;
  
  public AssignAddressCallback( Address address ) {
    this.address = address;
    super.setRequest( new AssignAddressType( address.getName( ), address.getInstanceAddress( ), address.getInstanceId( ) ) );
  }
  
  @Override
  public void prepare( AssignAddressType msg ) throws Exception {
    if( !this.checkVmState( ) ) {
      address.clearPending( );
      try {
        address.unassign( );
        if( address.isSystemOwned( ) ) {
          Addresses.release( address );
        }
      } catch ( IllegalStateException e ) {
      }
      throw new IllegalStateException( "Ignoring assignment to a vm which is not running: " + this.getRequest( ) );      
    } else {
      EventRecord.here( AssignAddressCallback.class, EventType.ADDRESS_ASSIGNING, Transition.assigning.toString( ), address.toString( ) ).debug( );
    }
  }
  
  @Override
  public void verify( BaseMessage msg ) throws Exception {
    try {
      this.updateState( );
      this.address.clearPending( );
    } catch ( Exception e1 ) {
      LOG.debug( e1, e1 );
      AddressCategory.unassign( address ).dispatch( address.getCluster( ) );
    }
  }
  
  @Override
  public void fail( Throwable e ) {
    LOG.debug( e, e );
    this.cleanupState( );
  }

  private boolean checkVmState( ) {
    try {
      VmInstance vm = VmInstances.getInstance( ).lookup( super.getRequest().getInstanceId( ) );
      VmState vmState = vm.getState( );
      String dnsDomain = "dns-disabled";
      try {
        dnsDomain = edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration.getSystemConfiguration( ).getDnsDomain( );
      } catch ( Exception e ) {
      }
      if ( !VmState.RUNNING.equals( vmState ) && !VmState.PENDING.equals( vmState ) ) {
        vm.getNetworkConfig( ).setIgnoredPublicIp( VmInstance.DEFAULT_IP );
        vm.getNetworkConfig( ).updateDns( dnsDomain );
        return false;
      } else {
        vm.getNetworkConfig( ).setIgnoredPublicIp( this.address.getName( ) );
        vm.getNetworkConfig( ).updateDns( dnsDomain );
        return true;
      }
    } catch ( NoSuchElementException e ) {
      return false;
    }
  }
  
  private void updateState( ) {
    if( !this.checkVmState( ) ) {
      throw new IllegalStateException( "Failed to find the vm for this assignment: " + this.getRequest( ) );
    } else {
      EventRecord.here( AssignAddressCallback.class, EventType.ADDRESS_ASSIGNED, Address.State.assigned.toString( ), LogUtil.dumpObject( address ) ).info( );
    }
  }

  private void cleanupState( ) {
    EventRecord.here( AssignAddressCallback.class, EventType.ADDRESS_ASSIGNING, Transition.assigning.toString( ), LogUtil.FAIL, address.toString( ) ).debug( );
    LOG.debug( LogUtil.subheader( this.getRequest( ).toString( ) ) );
    if( this.address.isPending( ) ) {
      this.address.clearPending( );
    } else if( this.address.isSystemOwned( ) ) {
      Addresses.release( address );
    } else if( this.address.isAssigned( ) ) {
      AddressCategory.unassign( address );
    }
  }

}
