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
package edu.ucsb.eucalyptus.cloud.entities;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.net.Addresses;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.HasName;

import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseItemType;
import edu.ucsb.eucalyptus.msgs.EventRecord;

@Entity
@Table( name = "addresses" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class Address implements HasName {
  
  private static Logger          LOG                     = Logger.getLogger( Address.class );
  
  @Id
  @GeneratedValue
  @Column( name = "address_id" )
  private Long                   id                      = -1l;
  @Column( name = "address_name" )
  private String                 name;
  @Column( name = "address_cluster" )
  private String                 cluster;
  @Column( name = "address_owner_id" )
  private String                 userId;
  @Column( name = "address_is_assigned" )
  private Boolean                assigned;
  @Column( name = "address_instance_id" )
  private String                 instanceId;
  @Column( name = "address_instance_addr" )
  private String                 instanceAddress;
  @Transient
  private final ReadWriteLock    canHas                  = new ReentrantReadWriteLock( true );
  public static String           UNALLOCATED_USERID      = "nobody";
  public static String           UNASSIGNED_INSTANCEID   = "available";
  public static String           UNASSIGNED_INSTANCEADDR = "0.0.0.0";
  public static String           PENDING_ASSIGNMENT      = "pending";
  @Transient
  private AtomicBoolean          pending                 = new AtomicBoolean( false );
  @Transient
  private AtomicReference<State> state                   = new AtomicReference<State>( State.unallocated );
  
  public Address( ) {}
  
  public Address( final String name ) {
    this.name = name;
  }
  
  public Address( String address, String cluster ) {
    this.name = address;
    this.cluster = cluster;
    this.state.set( State.unallocated );
    this.userId = UNALLOCATED_USERID;
    this.instanceId = UNASSIGNED_INSTANCEID;
    this.instanceAddress = UNASSIGNED_INSTANCEADDR;
  }
  
  public String getInstanceId( ) {
    return this.instanceId;
  }
  
  public String getName( ) {
    return this.name;
  }
  
  public DescribeAddressesResponseItemType getDescription( boolean isAdmin ) {
    return new DescribeAddressesResponseItemType(
      this.getName( ),
      isAdmin ? String.format( "%s (%s)", this.getInstanceId( ), this.getUserId( ) ) : UNASSIGNED_INSTANCEID.equals( this.getInstanceId( ) ) ? null : this.getInstanceId( ) );
  }
  
  public Boolean getAssigned( ) {
    return this.assigned;
  }
  
  public void setAssigned( ) {
    
  }
  public void setAssigned( final String instanceId, final String instanceAddress ) {
    this.instanceId = instanceId;
    this.instanceAddress = instanceAddress;
    this.state.set( State.assigned );
    addAddress( this );
  }
  
  public String getCluster( ) {
    return cluster;
  }
  
  public String getUserId( ) {
    return userId;
  }
  
  public void setUserId( final String userId ) {
    this.userId = userId;
  }
  
  public String getInstanceAddress( ) {
    return instanceAddress;
  }
  
  public void setInstanceAddress( final String instanceAddress ) {
    this.instanceAddress = instanceAddress;
  }
  
  public Long getId( ) {
    return id;
  }
  
  public void setId( final Long id ) {
    this.id = id;
  }
  
  public void setCluster( final String cluster ) {
    this.cluster = cluster;
  }
  
  public void init( ) {
    if ( UNALLOCATED_USERID.equals( this.userId ) ) {
      this.doUnassign( );
      this.state.set( State.unallocated );
      Addresses.getInstance( ).registerDisabled( this );
    } else if( !this.instanceId.equals( UNASSIGNED_INSTANCEID ) ) {
      this.state.set( State.assigned );      
      Addresses.getInstance( ).register( this );
    } else {
      this.state.set( State.allocated );
      Addresses.getInstance( ).register( this );
      if ( Component.eucalyptus.name( ).equals( this.userId ) && !this.isAssigned( ) ) {
        if ( !( this.system = !Addresses.doDynamicAddressing( ) ) ) {
          this.release( );
        }
      }
    }
    LOG.debug( "Initializing address: " + this.toString( ) );
  }
  
  @Override
  public String toString( ) {
    return String.format(
                          "Address [cluster=%s, instanceAddress=%s, instanceId=%s, name=%s, pending=%s, state=%s, userId=%s]",
                          this.cluster, this.instanceAddress, this.instanceId, this.name, this.pending, this.state,
                          this.userId );
  }
  
  public void clean() {
    this.canHas.writeLock().lock( );
    try {
      String user = this.userId;
      if( !this.isPending( ) ) {
        this.release( );
        if( !Component.eucalyptus.name().equals( user ) 
            || !Addresses.doDynamicAddressing( ) ) {
          this.allocate( userId );
        }
      }
    } finally {
      this.canHas.writeLock( ).unlock( );
    }
  }
  
  @Transient
  private volatile boolean system = false;
  
  public boolean isAllocated() {
    return UNALLOCATED_USERID.equals( this.userId );
  }
  
  public void allocate( String userId ) {
    if ( !this.pending.compareAndSet( false, false ) ) {
      throw new IllegalStateException( "Trying to allocate an address which is already pending: " + this );
    } else if ( !this.state.compareAndSet( State.unallocated, State.allocated ) ) {
      this.pending.set( false );
      throw new IllegalStateException( "Trying to allocate an address which is already allocated: " + this );
    } else {
      this.canHas.writeLock( ).lock( );
      try {
        this.doUnassign( );
        this.setUserId( userId );
        if ( Component.eucalyptus.name( ).equals( userId ) ) {
          system = true;
        }
        addAddress( this );
        LOG.debug( EventRecord.caller( this.getClass( ), this.state.get( ), this.toString( ) ) );
        try {
          Addresses.getInstance( ).register( this );
        } catch ( NoSuchElementException e ) {
          LOG.debug( e );
        }
      } finally {
        this.canHas.writeLock( ).unlock( );
      }
    }
  }
  
  public void release( ) {
    this.canHas.writeLock( ).lock( );
    try {
      this.pending.set( false );
      this.doUnassign( );
      this.userId = UNALLOCATED_USERID;
      this.system = false;
      this.state.set( State.unallocated );
      removeAddress( this.name );
      return;
    } finally {
      this.canHas.writeLock( ).unlock( );
      LOG.debug( EventRecord.caller( this.getClass( ), this.state.get( ), this.toString( ) ) );
    }
  }
  
  private static void removeAddress( String name ) {
    Addresses.getInstance( ).disable( name );
    EntityWrapper<Address> db = new EntityWrapper<Address>( );
    try {
      Address dbAddr = db.getUnique( new Address( name ) );
      db.delete( dbAddr );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
    }
  }
  
  private static void addAddress( Address address ) {
    Address addr = new Address( address.getName( ), address.getCluster( ) );
    EntityWrapper<Address> db = new EntityWrapper<Address>( );
    try {
      addr = db.getUnique( new Address( address.getName( ) ) );
      addr.setUserId( address.getUserId( ) );
      db.commit( );
    } catch ( Throwable e ) {
      try {
        db.add( address );
        db.commit( );
      } catch ( Throwable e1 ) {
        db.rollback( );
      }
    }
  }
  
  public void unassign( ) {
    if ( !this.pending.compareAndSet( false, true ) ) {
      throw new IllegalStateException( "Trying to unassign an address which is already pending: " + this );
    } else {
      try {
        if ( !this.state.compareAndSet( State.assigned, State.allocated ) ) {
          this.pending.set( false );
          throw new IllegalStateException( "Trying to unassign an address which is not currently assigned: " + this );
        }
      } finally {
        this.state.set( State.allocated );
        LOG.debug( EventRecord.caller( this.getClass( ), State.unassigning, this.toString( ) ) );
      }
    }
  }
  
  public void doUnassign( ) {
    this.canHas.writeLock( ).lock( );
    try {
      this.instanceId = UNASSIGNED_INSTANCEID;
      this.instanceAddress = UNASSIGNED_INSTANCEADDR;
    } finally {
      this.canHas.writeLock( ).unlock( );
      LOG.debug( EventRecord.caller( this.getClass( ), State.allocated, this.toString( ) ) );
    }
  }
  
  public boolean isSystemAllocated( ) {
    return Component.eucalyptus.name( ).equals( this.getUserId( ) );
  }
  
  public boolean isAssigned( ) {
    return State.assigned.equals( this.state.get( ) );
  }
  
  public boolean isPending( ) {
    return this.pending.get( );
  }
  
  public boolean clearPending( ) {
    boolean result = this.pending.compareAndSet( true, false );
    if ( result && State.allocated.equals( this.state.get( ) ) ) {
      if ( system && !edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).isDoDynamicPublicAddresses( ) ) {
        this.release( );
      } else {
        this.doUnassign( );
      }
    } else if ( State.assigned.equals( this.state.get( ) ) ) {
      LOG.debug( EventRecord.caller( this.getClass( ), this.state.get( ), this.toString( ) ) );
    }
    return result;
  }
  
  public boolean assign( String instanceId, String instanceAddr ) {//FIXME: have typed arguments here.
    if ( !this.pending.compareAndSet( false, true ) ) {
      throw new IllegalStateException( "Trying to assign an address which is currently pending: " + this );
    } else if ( !this.state.compareAndSet( State.allocated, State.assigned ) ) {
      this.pending.set( false );
      throw new IllegalStateException( "Address is not currently unassigned: " + this );
    } else {
      this.canHas.writeLock( ).lock( );
      try {
        this.instanceId = instanceId;
        this.instanceAddress = instanceAddr;
        return true;
      } finally {
        this.canHas.writeLock( ).unlock( );
        LOG.debug( EventRecord.caller( this.getClass( ), State.assigning, this.toString( ) ) );
      }
    }
  }
  
  public int compareTo( final Object o ) {
    Address that = ( Address ) o;
    return this.getName( ).compareTo( that.getName( ) );
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof Address ) ) return false;
    
    Address address = ( Address ) o;
    
    if ( !name.equals( address.name ) ) return false;
    
    return true;
  }
  
  @Override
  public int hashCode( ) {
    return name.hashCode( );
  }
  
  public enum State {
    unallocated, allocated, system, unassigning, assigning, assigned;//allocated => unassigned
  }
  
  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }
  
}
