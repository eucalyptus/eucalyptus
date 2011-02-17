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
package com.eucalyptus.address;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicMarkableReference;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.cluster.callback.AssignAddressCallback;
import com.eucalyptus.cluster.callback.UnassignAddressCallback;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Cluster;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.HasOwner;
import com.eucalyptus.util.async.NOOP;
import com.eucalyptus.util.async.RemoteCallback;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseItemType;

@Entity
@PersistenceContext( name = "eucalyptus_general" )
@Table( name = "addresses" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class Address implements HasName<Address>, HasOwner<Address> {
  public enum State {
    broken, unallocated, allocated, assigned, impending;
  }
  
  public enum Transition {
    allocating {
      public Class getCallback( ) {
        return NOOP.class;
      }
    },
    unallocating {
      public Class getCallback( ) {
        return NOOP.class;
      }
    },
    assigning {
      @Override
      public Class getCallback( ) {
        return AssignAddressCallback.class;
      }
    },
    unassigning {
      @Override
      public Class getCallback( ) {
        return UnassignAddressCallback.class;
      }
    },
    system {
      public Class getCallback( ) {
        return NOOP.class;
      }
    },
    quiescent {
      public Class getCallback( ) {
        return NOOP.class;
      }
    };
    public abstract Class getCallback( );
  }
  
  private static Logger                   LOG                     = Logger.getLogger( Address.class );
  @Id
  @GeneratedValue
  @Column( name = "address_id" )
  private Long                            id                      = -1l;
  @Column( name = "address_name" )
  private String                          name;
  @Column( name = "address_cluster" )
  private String                          cluster;
  @Column( name = "address_owner_id" )
  private String                          userId;
  @Transient
  private String                          instanceId;
  @Transient
  private String                          instanceAddress;
  @Transient
  public static String                    UNALLOCATED_USERID      = "nobody";
  @Transient
  public static String                    SYSTEM_ALLOCATED_USERID = "eucalyptus";
  @Transient
  public static String                    UNASSIGNED_INSTANCEID   = "available";
  @Transient
  public static String                    UNASSIGNED_INSTANCEADDR = "0.0.0.0";
  @Transient
  public static String                    PENDING_ASSIGNMENT      = "pending";
  @Transient
  private AtomicMarkableReference<State>  state;
  @Transient
  private String                          stateUuid;
  @Transient
  private transient final SplitTransition QUIESCENT               = new SplitTransition( Transition.quiescent ) {
                                                                    public void bottom( ) {}
                                                                    
                                                                    public void top( ) {}
                                                                    
                                                                    public String toString( ) {
                                                                      return "";
                                                                    }
                                                                  };
  @Transient
  private volatile SplitTransition        transition;
  @Transient
  private FullName                        owner;
  
  public Address( ) {}
  
  public Address( final String name ) {
    this( );
    this.name = name;
  }
  
  public Address( String address, String cluster ) {
    this( address );
    this.setUserId( UNALLOCATED_USERID );
    this.instanceId = UNASSIGNED_INSTANCEID;
    this.instanceAddress = UNASSIGNED_INSTANCEADDR;
    this.cluster = cluster;
    this.transition = this.QUIESCENT;
    this.state = new AtomicMarkableReference<State>( State.unallocated, false );
    this.init( );
  }
  
  public Address( String address, String cluster, String userId, String instanceId, String instanceAddress ) {
    this( address );
    this.cluster = cluster;
    this.setUserId( userId );
    this.instanceId = instanceId;
    this.instanceAddress = instanceAddress;
    this.transition = this.QUIESCENT;
    this.state = new AtomicMarkableReference<State>( State.unallocated, false );
    this.init( );
  }
  
  public void init( ) {//Should only EVER be called externally after loading from the db
    this.state = new AtomicMarkableReference<State>( State.unallocated, false );
    this.transition = this.QUIESCENT;
    if ( this.userId == null ) {
      this.setUserId(  UNALLOCATED_USERID );
    }
    if ( this.instanceAddress == null || this.instanceId == null ) {
      this.instanceAddress = UNASSIGNED_INSTANCEADDR;
      this.instanceId = UNASSIGNED_INSTANCEID;
    }
    if ( UNALLOCATED_USERID.equals( this.userId ) ) {
      this.state.set( State.unallocated, true );
      this.instanceAddress = UNASSIGNED_INSTANCEADDR;
      this.instanceId = UNASSIGNED_INSTANCEID;
      Addresses.getInstance( ).registerDisabled( this );
      this.state.set( State.unallocated, false );
    } else if ( !this.instanceId.equals( UNASSIGNED_INSTANCEID ) ) {
      this.state.set( State.assigned, true );
      Addresses.getInstance( ).register( this );
      this.state.set( State.assigned, false );
    } else {
      this.state.set( State.allocated, true );
      if ( this.isSystemOwned( ) ) {
        Addresses.getInstance( ).registerDisabled( this );
        this.setUserId(  UNALLOCATED_USERID );
        this.instanceAddress = UNASSIGNED_INSTANCEADDR;
        this.instanceId = UNASSIGNED_INSTANCEID;
        Address.removeAddress( this.name );
        this.state.set( State.unallocated, false );
      } else {
        Addresses.getInstance( ).register( this );
        this.state.set( State.allocated, false );
      }
    }
    LOG.debug( "Initialized address: " + this.toString( ) );
  }
  
  private boolean transition( State expectedState, State newState, boolean expectedMark, boolean newMark, SplitTransition transition ) {
    this.transition = transition;
    EventRecord.caller( this.getClass( ), EventType.ADDRESS_STATE, this.state.getReference( ), this.toString( ) ).debug( );
    if ( !this.state.compareAndSet( expectedState, newState, expectedMark, newMark ) ) {
      throw new IllegalStateException( String.format( "Cannot mark address as %s[%s.%s->%s.%s] when it is %s.%s: %s", transition.getName( ), expectedState,
                                                      expectedMark, newState, newMark, this.state.getReference( ), this.state.isMarked( ), this.toString( ) ) );
    }
    EventRecord.caller( this.getClass( ), EventType.ADDRESS_STATE, this.state.getReference( ), "TOP", this.transition.getName( ).name( ), this.toString( ) )
               .debug( );
    this.transition.top( );
    return true;
  }
  
  public Address allocate( final String userId ) {
    this.transition( State.unallocated, State.allocated, false, true, new SplitTransition( Transition.allocating ) {
      public void top( ) {
        Address.this.instanceId = UNASSIGNED_INSTANCEID;
        Address.this.instanceAddress = UNASSIGNED_INSTANCEADDR;
        Address.this.setUserId( userId );
        Address.addAddress( Address.this );
        try {
          Addresses.getInstance( ).register( Address.this );
        } catch ( NoSuchElementException e ) {
          LOG.debug( e );
        }
        EventRecord.here( Address.class, EventClass.ADDRESS, EventType.ADDRESS_ALLOCATE ).withDetails( Address.this.userId, Address.this.name, "type",
                                                                                                       Address.this.isSystemOwned( )
                                                                                                         ? "SYSTEM"
                                                                                                         : "USER" ).info( );
        Address.this.stateUuid = UUID.randomUUID( ).toString( );
        Address.this.state.attemptMark( State.allocated, false );
      }
      
      public void bottom( ) {}
    } );
    return this;
  }
  
  public Address release( ) {
    SplitTransition release = new SplitTransition( Transition.unallocating ) {
      public void top( ) {
        EventRecord.here( Address.class, EventClass.ADDRESS, EventType.ADDRESS_RELEASE ).withDetails( Address.this.userId, Address.this.name, "type",
                                                                                                      Address.this.isSystemOwned( )
                                                                                                        ? "SYSTEM"
                                                                                                        : "USER" ).info( );
        Address.this.instanceId = UNASSIGNED_INSTANCEID;
        Address.this.instanceAddress = UNASSIGNED_INSTANCEADDR;
        Address.this.setUserId( UNALLOCATED_USERID );
        Address.removeAddress( Address.this.name );
        Address.this.stateUuid = UUID.randomUUID( ).toString( );
        Address.this.state.attemptMark( State.unallocated, false );
      }
      
      public void bottom( ) {}
    };
    if ( State.impending.equals( this.state.getReference( ) ) ) {
      this.transition( State.impending, State.unallocated, true, true, release );
    } else {
      this.transition( State.allocated, State.unallocated, false, true, release );
    }
    return this;
  }
  
  private static void removeAddress( String name ) {
    try {
      Addresses.getInstance( ).disable( name );
    } catch ( NoSuchElementException e1 ) {
      LOG.debug( e1 );
    }
    EntityWrapper<Address> db = new EntityWrapper<Address>( );
    try {
      Address dbAddr = db.getUnique( new Address( name ) );
      db.delete( dbAddr );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
    }
  }
  
  public Address unassign( ) {
    SplitTransition unassign = new SplitTransition( Transition.unassigning ) {
      public void top( ) {
        String userId = Address.this.userId;
        try {
          VmInstance vm = VmInstances.getInstance( ).lookup( Address.this.getInstanceId( ) );
          EventRecord.here( Address.class, EventClass.ADDRESS, EventType.ADDRESS_UNASSIGNING )
                     .withDetails( vm.getOwnerId( ), Address.this.getName( ), "instanceid", vm.getInstanceId( ) )
                     .withDetails( "type", Address.this.isSystemOwned( )
                       ? "SYSTEM"
                       : "USER" )
                     .withDetails( "cluster", Address.this.getCluster( ) ).info( );
        } catch ( NoSuchElementException e ) {}
        EventRecord.here( Address.class, EventClass.ADDRESS, EventType.ADDRESS_ASSIGN )
                   .withDetails( userId, Address.this.name, "instance", Address.this.instanceId )
                   .withDetails( "instance-address", Address.this.instanceAddress ).withDetails( "type", Address.this.isSystemOwned( )
                     ? "SYSTEM"
                     : "USER" ).info( );
      }
      
      public void bottom( ) {
        Address.this.stateUuid = UUID.randomUUID( ).toString( );
        Address.this.instanceId = UNASSIGNED_INSTANCEID;
        Address.this.instanceAddress = UNASSIGNED_INSTANCEADDR;
      }
    };
    if ( State.impending.equals( this.state.getReference( ) ) ) {
      this.transition( State.impending, State.allocated, true, true, unassign );
    } else {
      this.transition( State.assigned, State.allocated, false, true, unassign );
    }
    return this;
  }
  
  public Address pendingAssignment( ) {
    this.transition( State.unallocated, State.impending, false, true, //
                     new SplitTransition( Transition.system ) {
                       public void top( ) {
                         Address.this.instanceId = PENDING_ASSIGNMENT;
                         Address.this.instanceAddress = UNASSIGNED_INSTANCEADDR;
                         Address.this.setUserId( SYSTEM_ALLOCATED_USERID );
                         Address.this.stateUuid = UUID.randomUUID( ).toString( );
                         try {
                           Addresses.getInstance( ).register( Address.this );
                         } catch ( NoSuchElementException e ) {
                           LOG.debug( e );
                         }
                         EventRecord.here( Address.class, EventClass.ADDRESS, EventType.ADDRESS_ALLOCATE ).withDetails( Address.this.userId, Address.this.name,
                                                                                                                        "type", Address.this.isSystemOwned( )
                                                                                                                          ? "SYSTEM"
                                                                                                                          : "USER" ).info( );
                       }
                       
                       public void bottom( ) {}
                     } );
    return this;
  }
  
  public Address assign( final VmInstance vm ) {
    SplitTransition assign = new SplitTransition( Transition.assigning ) {
      public void top( ) {
        EventRecord.here( Address.class, EventClass.ADDRESS, EventType.ADDRESS_ASSIGNING )
                   .withDetails( vm.getOwnerId( ), Address.this.getName( ), "instanceid", vm.getInstanceId( ) )
                   .withDetails( "type", Address.this.isSystemOwned( )
                     ? "SYSTEM"
                     : "USER" )
                   .withDetails( "cluster", Address.this.getCluster( ) ).info( );
        Address.this.setInstanceId( vm.getInstanceId( ) );
        Address.this.setInstanceAddress( vm.getPrivateAddress( ) );
        Address.this.stateUuid = UUID.randomUUID( ).toString( );
      }
      
      public void bottom( ) {
        String userId = Address.this.userId;
        EventRecord.here( Address.class, EventClass.ADDRESS, EventType.ADDRESS_ASSIGN )
                   .withDetails( userId, Address.this.name, "instance", Address.this.instanceId )
                   .withDetails( "instance-address", Address.this.instanceAddress ).withDetails( "type", Address.this.isSystemOwned( )
                     ? "SYSTEM"
                     : "USER" ).info( );
      }
    };
    if ( State.impending.equals( this.state.getReference( ) ) ) {
      this.transition( State.impending, State.assigned, true, true, assign );
    } else {
      this.transition( State.allocated, State.assigned, false, true, assign );
    }
    return this;
  }
  
  public Transition getTransition( ) {
    return this.transition.getName( );
  }
  
  public RemoteCallback getCallback( ) {
    try {
      Class cbClass = this.transition.getName( ).getCallback( );
      Constructor cbCons = cbClass.getConstructor( Address.class );
      return ( RemoteCallback ) cbCons.newInstance( this );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      return new NOOP( );
    }
  }
  
  public Address clearPending( ) {
    if ( !this.state.isMarked( ) ) {
      throw new IllegalStateException( "Trying to clear an address which is not currently pending." );
    } else {
      EventRecord
                 .caller( this.getClass( ), EventType.ADDRESS_STATE, this.state.getReference( ), "BOTTOM", this.transition.getName( ).name( ), this.toString( ) )
                 .debug( );
      try {
        this.transition.bottom( );
      } finally {
        this.transition = this.QUIESCENT;
        this.state.set( this.state.getReference( ), false );
      }
    }
    return this;
  }
  
  public boolean isAllocated( ) {
    return this.state.getReference( ).ordinal( ) > State.unallocated.ordinal( );
  }
  
  public boolean isSystemOwned( ) {
    return SYSTEM_ALLOCATED_USERID.equals( this.getUserId( ) );
  }
  
  public boolean isAssigned( ) {
    return this.state.getReference( ).ordinal( ) > State.allocated.ordinal( );
  }
  
  public boolean isPending( ) {
    return this.state.isMarked( );
  }
  
  private static void addAddress( Address address ) {
    Address addr = address;
    EntityWrapper<Address> db = new EntityWrapper<Address>( );
    try {
      addr = db.getUnique( new Address( address.getName( ) ) );
      addr.setUserId( address.getUserId( ) );
      db.commit( );
    } catch ( RuntimeException e ) {
      db.rollback( );
      LOG.error( e, e );
    } catch ( EucalyptusCloudException e ) {
      addr = new Address( address.getName( ), address.getCluster( ) );
      try {
        db.add( addr );
        db.commit( );
      } catch ( Throwable e1 ) {
        db.rollback( );
      }
    }
  }
  
  public String getInstanceId( ) {
    return this.instanceId;
  }
  
  public String getName( ) {
    return this.name;
  }
  
  public String getCluster( ) {
    return this.cluster;
  }
  
  public String getUserId( ) {
    return this.userId;
  }
  
  public String getInstanceAddress( ) {
    return this.instanceAddress;
  }
  
  private void setInstanceAddress( String instanceAddress ) {
    this.instanceAddress = instanceAddress;
  }
  
  public void setUserId( final String userId ) {
    this.userId = userId;
    if ( UNALLOCATED_USERID.equals( this.userId ) ) {
      this.owner = FullName.create.vendor( "euca" ).region( ComponentIds.lookup( Eucalyptus.class ).name( ) ).namespace( FullName.EMPTY ).end( );
    } else if ( UNALLOCATED_USERID.equals( this.userId ) ) {
      this.owner = UserFullName.get( User.SYSTEM );
    } else {
      this.owner = Accounts.lookupUserFullNameById( userId );
    }
  }

  @Override
  public FullName getOwner( ) {
    if( this.owner == null ) {
      this.setUserId( this.userId );
    }
    return this.owner;
  }
  
  public String getStateUuid( ) {
    return this.stateUuid;
  }
  
  public Long getId( ) {
    return this.id;
  }
  
  public void setId( final Long id ) {
    this.id = id;
  }
  
  public void setCluster( final String cluster ) {
    this.cluster = cluster;
  }
  
  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }
  
  @Override
  public String toString( ) {
    return "Address " + this.name + " " + this.cluster + " " + ( this.isAllocated( )
      ? this.userId + " "
      : "" ) + ( this.isAssigned( )
      ? this.instanceId + " " + this.instanceAddress + " "
      : "" ) + " " + this.transition;
  }
  
  public int compareTo( final Address that ) {
    return this.getName( ).compareTo( that.getName( ) );
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof Address ) ) return false;
    Address address = ( Address ) o;
    if ( !this.name.equals( address.name ) ) return false;
    return true;
  }
  
  @Override
  public int hashCode( ) {
    return this.name.hashCode( );
  }
  
  public DescribeAddressesResponseItemType getAdminDescription( ) {
    String name = this.getName( );
    String desc = String.format( "%s (%s)", this.getInstanceId( ), this.getOwner( ) );
    return new DescribeAddressesResponseItemType( name, desc );
  }
  
  public DescribeAddressesResponseItemType getDescription( ) {
    String name = this.getName( );
    String desc = UNASSIGNED_INSTANCEID.equals( this.getInstanceId( ) )
        ? null
        : this.getInstanceId( );
    return new DescribeAddressesResponseItemType( name, desc );
  }
  
  public abstract class SplitTransition {
    private Transition t;
    private State      previous;
    
    public SplitTransition( Transition t ) {
      this.t = t;
      this.previous = Address.this.state != null
        ? Address.this.state.getReference( )
        : State.unallocated;
    }
    
    private Transition getName( ) {
      return this.t;
    }
    
    public abstract void top( );
    
    public abstract void bottom( );
    
    @Override
    public String toString( ) {
      return String.format( "[SplitTransition previous=%s, transition=%s, next=%s, pending=%s]", this.previous, this.t, Address.this.state.getReference( ),
                            Address.this.state.isMarked( ) );
    }
  }
  
  @Override
  public String getPartition( ) {
    return this.cluster;//GRZE:BUG:BUG:TODO:
  }
  
  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" ).region( ComponentIds.lookup( Cluster.class ).name( ) ).namespace( this.getCluster( ) ).relativeId( "public-address",
                                                                                                                                        this.getName( ) );
  }
  
}
