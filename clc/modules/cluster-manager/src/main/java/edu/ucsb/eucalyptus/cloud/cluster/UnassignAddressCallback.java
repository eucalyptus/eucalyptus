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

import org.apache.log4j.Logger;

import com.eucalyptus.net.Addresses;
import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.util.LogUtil;

import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.EventRecord;
import edu.ucsb.eucalyptus.msgs.NetworkConfigType;
import edu.ucsb.eucalyptus.msgs.UnassignAddressType;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;

public class UnassignAddressCallback extends QueuedEventCallback<UnassignAddressType> {

  private static Logger LOG = Logger.getLogger( UnassignAddressCallback.class );
  private Address       parentAddr;
  private VmInstance    parentVm;
  public UnassignAddressCallback( final Address address ) {
    this.parentAddr = address;
    super.setRequest( new UnassignAddressType( parentAddr.getName( ), parentAddr.getInstanceAddress( ) ) );
  }

  public UnassignAddressCallback( final Address address,final VmInstance vm ) {
    this.parentAddr = address;
    this.parentVm = vm;
    super.setRequest( new UnassignAddressType( parentAddr.getName( ), vm.getNetworkConfig( ).getIpAddress( ) ) );
  }

  @Override
  public void prepare( UnassignAddressType msg ) throws Exception {
    LOG.debug( EventRecord.here( UnassignAddressCallback.class, Address.State.unassigning, parentAddr.toString( ) ) );
//    if( !this.parent.isAssigned( ) || this.parent.isPending( ) ) {
//      throw new EucalyptusClusterException( "Received request to unassign an address which is either not assigned or has an assignment pending: " + this.parent.toString( ) );
//    }
  }

  @Override
  public void verify( EucalyptusMessage msg ) {
    LOG.info( String.format( EucalyptusProperties.DEBUG_FSTRING, EucalyptusProperties.TokenState.unassigned, LogUtil.subheader( this.getRequest( ).toString( "eucalyptus_ucsb_edu" ) ) ) );
    String ipAddress = this.getRequest( ).getSource( );
    if( this.parentVm != null ) {
      this.clearVm( ipAddress, this.parentVm );
    } else {
      for( VmInstance vm : VmInstances.getInstance( ).listValues( ) ) {
        this.clearVm( ipAddress, vm );
      }
    }
    this.parentAddr.clearPending( );
  }

  private void clearVm( String ipAddress, VmInstance vm ) {
    NetworkConfigType netConfig = vm.getNetworkConfig( );  
    if( netConfig.getIpAddress( ).equals( this.getRequest( ).getDestination( ) ) && ( netConfig.getIgnoredPublicIp( ).equals( ipAddress ) ) ) {
      netConfig.setIgnoredPublicIp( netConfig.getIpAddress( ) );
    }
  }

  @Override
  public void fail( Throwable e ) {
    LOG.debug( LogUtil.subheader( this.getRequest( ).toString( "eucalyptus_ucsb_edu" ) ) );
    LOG.debug( e, e );
    //FIXME: unassign fails clean up state.
    this.parentAddr.clearPending( );
  }

}
