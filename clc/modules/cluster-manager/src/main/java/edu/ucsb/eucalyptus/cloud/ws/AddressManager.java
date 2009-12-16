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
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.cloud.ws;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.msgs.AllocateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AllocateAddressType;
import edu.ucsb.eucalyptus.msgs.AssociateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AssociateAddressType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseItemType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressType;
import edu.ucsb.eucalyptus.msgs.EventRecord;
import edu.ucsb.eucalyptus.msgs.NetworkConfigType;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressResponseType;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressType;

public class AddressManager {

  public enum Events {
    ALLOCATE, ASSOCIATE, DISASSOCIATE, RELEASE, UNASSIGNFROMVM;
  }
  public static Logger LOG = Logger.getLogger( AddressManager.class );
  
  public AllocateAddressResponseType AllocateAddress( AllocateAddressType request ) throws EucalyptusCloudException {
    AllocateAddressResponseType reply = ( AllocateAddressResponseType ) request.getReply( );
        
    String userId = request.getUserId( );
    Address address;
    try {
      address = Addresses.allocate( userId, request.isAdministrator( ) );
      LOG.info( EventRecord.here( AddressManager.class, Events.ALLOCATE, address.toString( ) ) );
    } catch ( NotEnoughResourcesAvailable e ) {
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e );
    }
    reply.setPublicIp( address.getName( ) );
    return reply;
  }
  
  public ReleaseAddressResponseType ReleaseAddress( ReleaseAddressType request ) throws EucalyptusCloudException {
    ReleaseAddressResponseType reply = ( ReleaseAddressResponseType ) request.getReply( );
    reply.set_return( false );
    
    Addresses.updateAddressingMode( );
    
    Address address = Addresses.restrictedLookup( request.getUserId( ), request.isAdministrator( ), request.getPublicIp( ) );
    LOG.info( EventRecord.here( AddressManager.class, Events.RELEASE, address.toString( ) ) );
    Addresses.release( address );
    reply.set_return( true );
    return reply;
  }
  
  public DescribeAddressesResponseType DescribeAddresses( DescribeAddressesType request ) throws EucalyptusCloudException {
    DescribeAddressesResponseType reply = ( DescribeAddressesResponseType ) request.getReply( );
    
    Addresses.updateAddressingMode( );
    
    boolean isAdmin = request.isAdministrator( );
    for ( Address address : Addresses.getInstance( ).listValues( ) ) {
      if ( isAdmin || address.getUserId( ).equals( request.getUserId( ) ) ) {
        reply.getAddressesSet( ).add( address.getDescription( isAdmin ) );
      }
    }
    if ( isAdmin ) {
      for ( Address address : Addresses.getInstance( ).listDisabledValues( ) ) {
        reply.getAddressesSet( ).add( new DescribeAddressesResponseItemType( address.getName( ), Address.UNALLOCATED_USERID ) );
      }
    }
    return reply;
  }
  
  public AssociateAddressResponseType AssociateAddress( AssociateAddressType request ) throws Exception {
    AssociateAddressResponseType reply = ( AssociateAddressResponseType ) request.getReply( );
    reply.set_return( false );
    
    Addresses.updateAddressingMode( );
    
    Address address = Addresses.restrictedLookup( request.getUserId( ), request.isAdministrator( ), request.getPublicIp( ) );//TODO: test should throw error.
    VmInstance vm = VmInstances.restrictedLookup( request.getUserId( ), request.isAdministrator( ), request.getInstanceId( ) );
    LOG.info( EventRecord.here( AddressManager.class, Events.ASSOCIATE, address.toString( ), vm.toString( ) ) );
    
    NetworkConfigType netConfig = vm.getNetworkConfig( );
    reply.set_return( true );
    
    if ( vm.hasPublicAddress( ) ) {
      try {
        Address currentAddr = Addresses.getInstance( ).lookup( vm.getNetworkConfig( ).getIgnoredPublicIp( ) );
        LOG.info( EventRecord.here( AddressManager.class, Events.UNASSIGNFROMVM, currentAddr.toString( ), vm.toString( ) ) );
        if ( currentAddr.isAssigned( ) ) {
          Addresses.unassign( address );
        }
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
    }
    
    if ( address.isAssigned( ) && address.getUserId( ).equals( request.getUserId( ) ) && !address.isPending( ) ) {
      try {
        VmInstance oldVm = VmInstances.getInstance( ).lookup( address.getInstanceId( ) );
        LOG.info( EventRecord.here( AddressManager.class, Events.UNASSIGNFROMVM, address.toString( ), oldVm.toString( ) ) );
        Addresses.unassign( address );
      } catch ( Exception e ) {
        LOG.error( e, e );
      }
    }
    LOG.info( EventRecord.here( AddressManager.class, Events.ASSOCIATE, address.toString( ), vm.toString( ) ) );
    Addresses.assign( address, vm );    
    return reply;
  }
  
  public DisassociateAddressResponseType DisassociateAddress( DisassociateAddressType request ) throws EucalyptusCloudException {
    DisassociateAddressResponseType reply = ( DisassociateAddressResponseType ) request.getReply( );
    reply.set_return( false );
    
    Addresses.updateAddressingMode( );
    Address address = Addresses.restrictedLookup( request.getUserId( ), request.isAdministrator( ), request.getPublicIp( ) );    
    reply.set_return( true );
    LOG.info( EventRecord.here( AddressManager.class, Events.DISASSOCIATE, address.toString( ) ) );
    Addresses.unassign( address );
    return reply;
  }
  
}
