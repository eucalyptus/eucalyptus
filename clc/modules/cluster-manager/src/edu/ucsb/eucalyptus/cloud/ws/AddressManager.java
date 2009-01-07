/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.cloud.cluster.*;
import edu.ucsb.eucalyptus.cloud.exceptions.ExceptionList;
import edu.ucsb.eucalyptus.cloud.net.*;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.util.*;
import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Startable;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;

public class AddressManager implements Startable {

  private static Logger LOG = Logger.getLogger( AddressManager.class );

  public void start() throws MuleException {
    EntityWrapper<Address> db = new EntityWrapper<Address>();
    List<Address> addrList = db.query( new Address() );
    for ( Address addr : addrList )
      try {
        Addresses.getInstance().replace( addr.getName(), addr );
      } catch ( NoSuchElementException e ) {
        Addresses.getInstance().register( addr );
      }
    db.commit();
  }

  public AllocateAddressResponseType AllocateAddress( AllocateAddressType request ) throws EucalyptusCloudException {
    int addrCount = 0;
    for( Address a : Addresses.getInstance().listValues() ) {
      if( request.getUserId().equals( a.getUserId() ) ) addrCount++;
    }
    if( addrCount > 5 && !request.isAdministrator() )
      throw new EucalyptusCloudException( ExceptionList.ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );

    ConcurrentNavigableMap<String, Address> unusedAddresses = Addresses.getInstance().getDisabledMap();
    Map.Entry<String, Address> addressEntry = unusedAddresses.pollFirstEntry();

    //:: address is null -- disabled map is empty :://
    if ( addressEntry == null ) throw new EucalyptusCloudException( ExceptionList.ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );

    Address address = addressEntry.getValue();
    address.allocate( request.getUserId() );
    EntityWrapper<Address> db = new EntityWrapper<Address>();
    try {
      Address addr = db.getUnique( new Address( address.getName() ) );
      addr.allocate( request.getUserId() );
    } catch ( EucalyptusCloudException e ) {
      db.merge( address );
    }
    db.commit();

    try {
      Addresses.getInstance().register( address );
    } catch ( Exception e ) {
    }

    AllocateAddressResponseType reply = ( AllocateAddressResponseType ) request.getReply();
    reply.setPublicIp( address.getName() );
    return reply;
  }

  public ReleaseAddressResponseType ReleaseAddress( ReleaseAddressType request ) throws EucalyptusCloudException {
    ReleaseAddressResponseType reply = ( ReleaseAddressResponseType ) request.getReply();
    reply.set_return( false );

    Address address = null;
    try {
      //:: find the addr :://
      address = Addresses.getInstance().lookup( request.getPublicIp() );
      if ( !request.isAdministrator() && !address.getUserId().equals( request.getUserId() ) )
        return reply;
      //:: dispatch the unassign if needed :://
      if ( address.isAssigned() ) {
        UnassignAddressType unassignMsg = new UnassignAddressType( request, address.getName(), address.getInstanceAddress() );
        UnassignAddressCallback unassignHandler = new UnassignAddressCallback( address );
        QueuedEvent<UnassignAddressType> event = new QueuedEvent<UnassignAddressType>( unassignHandler, unassignMsg );
        Messaging.dispatch( EucalyptusProperties.CLUSTERSINK_REF, new ClusterEnvelope( address.getCluster(), event ) );
      }


      EntityWrapper<Address> db = new EntityWrapper<Address>();
      try {
        Address dbAddr = db.getUnique( address );
        db.delete( dbAddr );
        db.commit();
      } catch ( EucalyptusCloudException e ) {
        db.rollback();
      }

      address.release();
      Addresses.getInstance().disable( address.getName() );

      reply.set_return( true );
    }
    catch ( NoSuchElementException e ) {
      return reply;
    }

    return reply;
  }

  public DescribeAddressesResponseType DescribeAddresses( DescribeAddressesType request ) throws EucalyptusCloudException {
    DescribeAddressesResponseType reply = ( DescribeAddressesResponseType ) request.getReply();


    boolean isAdmin = request.isAdministrator();
    for ( Address address : Addresses.getInstance().listValues() ) {
      try {
        VmInstances.getInstance().lookup( address.getInstanceId() );
      } catch ( NoSuchElementException e ) {
        address.unassign();
      }
      if ( isAdmin || address.getUserId().equals( request.getUserId() ) ) {
        reply.getAddressesSet().add( address.getDescription( isAdmin ) );
      }
    }
    if ( request.isAdministrator() )
      for ( Address address : Addresses.getInstance().listDisabledValues() )
        reply.getAddressesSet().add( new DescribeAddressesResponseItemType( address.getName(), Address.UNALLOCATED_USERID ) );

    return reply;
  }

  public AssociateAddressResponseType AssociateAddress( AssociateAddressType request ) throws AxisFault {
    AssociateAddressResponseType reply = ( AssociateAddressResponseType ) request.getReply();
    reply.set_return( false );


    LOG.debug( "Associate: " + request.getPublicIp() + " => " + request.getInstanceId() );
    Address address = null;
    try {
      address = Addresses.getInstance().lookup( request.getPublicIp() );
    } catch ( NoSuchElementException e ) {
      return reply;
    }
    LOG.debug( "Found address: " + address );

    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance().lookup( request.getInstanceId() );
    } catch ( NoSuchElementException e ) {
      return reply;
    }
    LOG.debug( "Found vm: " + vm );

    if ( !request.isAdministrator() && !( request.getUserId().equals( address.getUserId() ) && request.getUserId().equals( vm.getOwnerId() ) ) )
      return reply;

    if( VmInstance.DEFAULT_IP.equals( vm.getInstanceId() ) )
        return reply;

    //:: operation should be idempotent; request is legitimate so return true :://
    reply.set_return( true );

    //:: made it here, means it looks legitimate :://
    if ( address.isAssigned() ) {
      LOG.debug( "Dispatching unassign message for: " + address );
      UnassignAddressType unassignMsg = Admin.makeMsg( UnassignAddressType.class, address.getName(), address.getInstanceAddress() );
      ClusterEnvelope.dispatch( address.getCluster(), QueuedEvent.make( new UnassignAddressCallback( address ), unassignMsg ) );
    }
    //:: record the assignment :://
    address.unassign();
    address.assign( vm.getInstanceId(), vm.getNetworkConfig().getIpAddress() );
    EntityWrapper<Address> db = new EntityWrapper<Address>();
    try {
      Address addr = db.getUnique( new Address( address.getName() ) );
      addr.unassign();
      addr.assign( vm.getInstanceId(), vm.getNetworkConfig().getIpAddress() );
      db.commit();
    } catch ( EucalyptusCloudException e ) {
      db.rollback();
    }

    //:: dispatch the request to the cluster that owns the address :://
    AssignAddressType assignMsg = Admin.makeMsg( AssignAddressType.class, address.getName(), address.getInstanceAddress() );
    ClusterEnvelope.dispatch( address.getCluster(), QueuedEvent.make( new AssignAddressCallback( vm ), assignMsg ) );

    return reply;
  }

  public DisassociateAddressResponseType DisassociateAddress( DisassociateAddressType request ) throws EucalyptusCloudException {
    DisassociateAddressResponseType reply = ( DisassociateAddressResponseType ) request.getReply();
    reply.set_return( false );

    LOG.debug( "Disassociate: " + request.getPublicIp()  );
    Address address = null;
    try {
      address = Addresses.getInstance().lookup( request.getPublicIp() );
    } catch ( NoSuchElementException e ) {
      return reply;
    }
    LOG.debug( "Found address: " + address );

    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance().lookup( address.getInstanceId() );
    } catch ( NoSuchElementException e ) {
      return reply;
    }
    LOG.debug( "Found vm: " + vm );

    if ( !request.isAdministrator() && !( request.getUserId().equals( address.getUserId() ) && request.getUserId().equals( vm.getOwnerId() ) ) )
      return reply;

    if( VmInstance.DEFAULT_IP.equals( vm.getInstanceId() ) )
        return reply;

    reply.set_return( true );
    //:: made it here, means it looks legitimate :://
    UnassignAddressType unassignMsg = Admin.makeMsg( UnassignAddressType.class, address.getName(), address.getInstanceAddress() );
    ClusterEnvelope.dispatch( address.getCluster(), QueuedEvent.make( new UnassignAddressCallback( address ), unassignMsg ) );
    address.unassign();
    EntityWrapper<Address> db = new EntityWrapper<Address>();
    try {
      Address addr = db.getUnique( new Address( address.getName() ) );
      addr.unassign();
      db.commit();
    } catch ( EucalyptusCloudException e ) {
      db.rollback();
    }

    return reply;
  }

}
