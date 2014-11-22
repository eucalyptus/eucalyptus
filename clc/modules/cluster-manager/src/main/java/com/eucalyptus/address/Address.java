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

package com.eucalyptus.address;

import static com.eucalyptus.reporting.event.AddressEvent.AddressAction;
import java.lang.reflect.Constructor;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicMarkableReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.AddressInfoType;
import com.eucalyptus.compute.common.CloudMetadata.AddressMetadata;
import com.eucalyptus.cluster.callback.AssignAddressCallback;
import com.eucalyptus.cluster.callback.UnassignAddressCallback;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.compute.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.vpc.NetworkInterface;
import com.eucalyptus.entities.AccountMetadata;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.AddressEvent;
import com.eucalyptus.reporting.event.EventActionInfo;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.NOOP;
import com.eucalyptus.util.async.RemoteCallback;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_addresses" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class Address extends UserMetadata<Address.State> implements AddressMetadata {

  public static final String ID_PREFIX_ALLOC = "eipalloc";
  public static final String ID_PREFIX_ASSOC = "eipassoc";

  public enum State {
    broken,
    unallocated,
    allocated,
    impending,
    assigned,
    started,
  }

  public enum Domain {
    standard,
    vpc
  }
  
  public enum Transition {
    allocating {
      @Override
      public Class<? extends RemoteCallback<? extends BaseMessage,? extends BaseMessage>> getCallback( ) {
        return NOOP.class;
      }
    },
    unallocating {
      @Override
      public Class<? extends RemoteCallback<? extends BaseMessage,? extends BaseMessage>> getCallback( ) {
        return NOOP.class;
      }
    },
    assigning {
      @Override
      public Class<? extends RemoteCallback<? extends BaseMessage,? extends BaseMessage>> getCallback( ) {
        return AssignAddressCallback.class;
      }
    },
    unassigning {
      @Override
      public Class<? extends RemoteCallback<? extends BaseMessage,? extends BaseMessage>> getCallback( ) {
        return UnassignAddressCallback.class;
      }
    },
    starting {
      @Override
      public Class<? extends RemoteCallback<? extends BaseMessage,? extends BaseMessage>> getCallback( ) {
        return NOOP.class;
      }
    },
    stopping {
      @Override
      public Class<? extends RemoteCallback<? extends BaseMessage,? extends BaseMessage>> getCallback( ) {
        return NOOP.class;
      }
    },
    system {
      @Override
      public Class<? extends RemoteCallback<? extends BaseMessage,? extends BaseMessage>> getCallback( ) {
        return NOOP.class;
      }
    },
    quiescent {
      @Override
      public Class<? extends RemoteCallback<? extends BaseMessage,? extends BaseMessage>> getCallback( ) {
        return NOOP.class;
      }
    };
    public abstract Class<? extends RemoteCallback<? extends BaseMessage,? extends BaseMessage>> getCallback( );
    
    @Override
    public String toString( ) {
      return this.name( );
    }
  }
  
  private static Logger                   LOG                     = Logger.getLogger( Address.class );

  private static final long               serialVersionUID        = 1L;

  @Transient
  private String                          instanceUuid;
  @Transient
  private String                          instanceId;
  @Transient
  private String                          instanceAddress;

  /**
   * EC2 VPC domain. Null unless allocated for use in VPC.
   */
  @Enumerated( EnumType.STRING )
  @Column( name = "metadata_domain" )
  private Domain                          domain;

  /**
   * EC2 VPC allocation identifier. Null unless allocated for use in VPC.
   */
  @Column( name = "metadata_allocation_id" )
  private String                          allocationId;

  /**
   * EC2 VPC association identifier. Null unless associated and allocated for use in VPC.
   */
  @Column( name = "metadata_association_id" )
  private String                          associationId;

  /**
   * EC2 VPC network interface identifier. Null unless associated and allocated for use in VPC.
   */
  @Column( name = "metadata_association_eni_id" )
  private String                          networkInterfaceId;

  /**
   * EC2 VPC network interface owner identifier. Null unless associated and allocated for use in VPC.
   */
  @Column( name = "metadata_association_eni_owner_id" )
  private String                          networkInterfaceOwnerId;

  /**
   * EC2 VPC private address. Null unless associated and allocated for use in VPC.
   */
  @Column( name = "metadata_association_private_address" )
  private String privateAddress;

  public static String                    UNASSIGNED_INSTANCEUUID = "";
  public static String                    UNASSIGNED_INSTANCEID   = "available";
  public static String                    UNASSIGNED_INSTANCEADDR = "0.0.0.0";
  public static String                    PENDING_ASSIGNMENT      = "pending";
  public static String                    PENDING_ASSIGNMENTUUID  = "";
  public static String                    ASSIGNED_UNKNOWN_INSTANCEUUID  = "";
  public static String                    ASSIGNED_UNKNOWN_INSTANCEID    = "assigned";
  public static String                    ASSIGNED_UNKNOWN_INSTANCEADDR  = "0.0.0.0";
  @Transient
  private AtomicMarkableReference<State>  atomicState;
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
  
  public Address( ) {}
  
  public Address( String ipAddress ) {
    super( Principals.nobodyFullName( ), ipAddress );
    this.instanceUuid = UNASSIGNED_INSTANCEUUID;
    this.instanceId = UNASSIGNED_INSTANCEID;
    this.instanceAddress = UNASSIGNED_INSTANCEADDR;
    this.transition = this.QUIESCENT;
    this.atomicState = new AtomicMarkableReference<State>( State.unallocated, false );
    this.init( );
  }
  
  public Address( OwnerFullName ownerFullName, String address, String instanceUuid, String instanceId, String instanceAddress ) {
    this( address );
    this.setOwner( ownerFullName );
    this.instanceUuid = instanceUuid;
    this.instanceId = instanceId;
    this.instanceAddress = instanceAddress;
    this.transition = this.QUIESCENT;
    this.atomicState = new AtomicMarkableReference<State>( State.allocated, false );
    this.init( );
  }
  
  public void init( ) {//Should only EVER be called externally after loading from the db
    this.resetPersistence();
    this.atomicState = new AtomicMarkableReference<State>( State.unallocated, false );
    this.transition = this.QUIESCENT;
    this.getOwner( );//ensure to initialize
    if ( this.instanceAddress == null || this.instanceId == null ) {
      this.instanceAddress = UNASSIGNED_INSTANCEADDR;
      this.instanceUuid = UNASSIGNED_INSTANCEUUID;
      this.instanceId = UNASSIGNED_INSTANCEID;
    }
    if ( Principals.nobodyFullName( ).equals( super.getOwner( ) ) ) {
      this.atomicState.set( State.unallocated, true );
      this.instanceAddress = UNASSIGNED_INSTANCEADDR;
      this.instanceUuid = UNASSIGNED_INSTANCEUUID;
      this.instanceId = UNASSIGNED_INSTANCEID;
      this.associationId = null;
      this.networkInterfaceId = null;
      this.networkInterfaceOwnerId = null;
      this.privateAddress = null;
      Addresses.getInstance( ).registerDisabled( this );
      this.atomicState.set( State.unallocated, false );
    } else if ( !this.instanceId.equals( UNASSIGNED_INSTANCEID ) ) {
      final State addressState = this.networkInterfaceId != null ?
          State.started :
          State.assigned;
      this.atomicState.set( addressState, true );
      Addresses.getInstance().register( this );
      this.atomicState.set( addressState, false );
    } else if ( this.networkInterfaceId != null ) {
      this.atomicState.set( State.assigned, true );
      Addresses.getInstance().register( this );
      this.atomicState.set( State.assigned, false );
    } else {
      this.atomicState.set( State.allocated, true );
      if ( this.isSystemOwned( ) ) {
        Addresses.getInstance( ).registerDisabled( this );
        this.setOwner( Principals.nobodyFullName( ) );
        this.instanceAddress = UNASSIGNED_INSTANCEADDR;
        this.instanceUuid = UNASSIGNED_INSTANCEUUID;
        this.instanceId = UNASSIGNED_INSTANCEID;
        this.associationId = null;
        this.networkInterfaceId = null;
        this.networkInterfaceOwnerId = null;
        this.privateAddress = null;
        Address.removeAddress( this.getDisplayName( ) );
        this.atomicState.set( State.unallocated, false );
      } else {
        Addresses.getInstance( ).register( this );
        this.atomicState.set( State.allocated, false );
      }
    }
    LOG.debug( "Initialized address: " + this.toString( ) );
  }
  
  private boolean transition( State expectedState, State newState, boolean expectedMark, boolean newMark, SplitTransition transition ) {
    if ( !this.atomicState.compareAndSet( expectedState, newState, expectedMark, newMark ) ) {
      throw new IllegalStateException( String.format( "Cannot mark address as %s[%s.%s->%s.%s] when it is %s.%s: %s", transition.getName( ), expectedState,
                                                      expectedMark, newState, newMark, this.atomicState.getReference( ), this.atomicState.isMarked( ),
                                                      this.toString( ) ) );
    }
    this.transition = transition;
    EventRecord.caller( this.getClass( ), EventType.ADDRESS_STATE, "TOP", this.toString( ) ).info( );
    try {
      this.transition.top( );
    } catch ( RuntimeException ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
      throw ex;
    }
    return true;
  }
  
  public Address allocate( final OwnerFullName ownerFullName, final Domain domain ) {
    this.transition( State.unallocated, State.allocated, false, true, new SplitTransition( Transition.allocating ) {
      public void top( ) {
        Address.this.instanceUuid = UNASSIGNED_INSTANCEUUID;
        Address.this.instanceId = UNASSIGNED_INSTANCEID;
        Address.this.instanceAddress = UNASSIGNED_INSTANCEADDR;
        Address.this.associationId = null;
        Address.this.networkInterfaceId = null;
        Address.this.networkInterfaceOwnerId = null;
        Address.this.privateAddress = null;
        Address.this.allocationId = domain == Domain.vpc ? ResourceIdentifiers.generateString( ID_PREFIX_ALLOC ) : null;
        Address.this.domain = domain;
        Address.this.setOwner( ownerFullName );
        Address.addAddress( Address.this );
        try {
          Addresses.getInstance( ).register( Address.this );
        } catch ( NoSuchElementException e ) {
          LOG.debug( e );
        }
        Address.this.stateUuid = UUID.randomUUID( ).toString( );
        Address.this.atomicState.attemptMark( State.allocated, false );
      }
      
      public void bottom( ) {}
      
    } );
    fireUsageEvent( ownerFullName, Suppliers.ofInstance( AddressEvent.forAllocate() ) );
    return this;
  }

  public Address release( ) {
    fireUsageEvent( Suppliers.ofInstance( AddressEvent.forRelease() ) );

    SplitTransition release = new SplitTransition( Transition.unallocating ) {
      public void top( ) {
        Address.this.instanceUuid = UNASSIGNED_INSTANCEUUID;
        Address.this.instanceId = UNASSIGNED_INSTANCEID;
        Address.this.instanceAddress = UNASSIGNED_INSTANCEADDR;
        Address.this.associationId = null;
        Address.this.networkInterfaceId = null;
        Address.this.networkInterfaceOwnerId = null;
        Address.this.privateAddress = null;
        Address.this.allocationId = null;
        Address.this.domain = null;
        Address.removeAddress( Address.this.getDisplayName( ) );
        Address.this.setOwner( Principals.nobodyFullName( ) );
        Address.this.stateUuid = UUID.randomUUID( ).toString( );
        Address.this.atomicState.attemptMark( State.unallocated, false );
      }
      
      public void bottom( ) {}
    };
    if ( State.impending.equals( this.atomicState.getReference( ) ) ) {
      this.transition( State.impending, State.unallocated, this.isPending( ), true, release );
    } else {
      this.transition( State.allocated, State.unallocated, false, true, release );
    }
    return this;
  }
  
  private static void removeAddress( final String ipAddress ) {
    try {
      Addresses.getInstance( ).disable( ipAddress );
    } catch ( NoSuchElementException e1 ) {
      LOG.debug( e1 );
    }
    final EntityTransaction db = Entities.get( Address.class );
    try {
      Entities.delete( Entities.uniqueResult( forIp( ipAddress ) ) );
      db.commit( );
    } catch ( final NoSuchElementException e ) {
      LOG.debug( "Address not found for removal '" + ipAddress + "'" );
    } catch ( final Exception e ) {
      Logs.extreme( ).error( e, e );
    } finally {
      if (db.isActive()) db.rollback();
    }
  }
  
  public Address unassign( ) {
    fireUsageEvent( new Supplier<EventActionInfo<AddressAction>>() {
      @Override
      public EventActionInfo<AddressAction> get() {
        return AddressEvent.forDisassociate( instanceUuid, instanceId );
      }
    } );

    SplitTransition unassign = new SplitTransition( Transition.unassigning ) {
      public void top( ) {
        try {
          VmInstance vm = VmInstances.lookup( Address.this.getInstanceId( ) );
        } catch ( NoSuchElementException e ) {
          LOG.debug( e );
        }
      }
      
      public void bottom( ) {
        Address.this.stateUuid = UUID.randomUUID( ).toString( );
        Address.this.instanceUuid = UNASSIGNED_INSTANCEUUID;
        Address.this.instanceId = UNASSIGNED_INSTANCEID;
        Address.this.instanceAddress = UNASSIGNED_INSTANCEADDR;
        Address.this.associationId = null;
        Address.this.networkInterfaceId = null;
        Address.this.networkInterfaceOwnerId = null;
        Address.this.privateAddress = null;
      }
    };
    if ( State.impending.equals( this.atomicState.getReference( ) ) ) {
      this.transition( State.impending, State.allocated, this.isPending( ), true, unassign );
    } else {
      this.transition( State.assigned, State.allocated, false, true, unassign );
    }
    return this;
  }

  public Address unassign( @Nonnull final NetworkInterface networkInterface ) {
    SplitTransition unassign = new SplitTransition( Transition.unassigning ) {
      public void top( ) {
        Address.this.stateUuid = UUID.randomUUID( ).toString( );
        Address.this.instanceUuid = UNASSIGNED_INSTANCEUUID;
        Address.this.instanceId = UNASSIGNED_INSTANCEID;
        Address.this.instanceAddress = UNASSIGNED_INSTANCEADDR;
        Address.this.associationId = null;
        Address.this.networkInterfaceId = null;
        Address.this.networkInterfaceOwnerId = null;
        Address.this.privateAddress = null;
      }

      public void bottom( ) {
      }
    };
    if ( State.impending.equals( this.atomicState.getReference( ) ) ) {
      this.transition( State.impending, State.allocated, this.isPending( ), false, unassign );
    } else {
      this.transition( State.assigned, State.allocated, false, false, unassign );
    }
    return this;
  }

  public Address pendingAssignment( ) {
    this.transition( State.unallocated, State.impending, false, true, //
                     new SplitTransition( Transition.system ) {
                       public void top( ) {
                         Address.this.instanceUuid = PENDING_ASSIGNMENTUUID;
                         Address.this.instanceId = PENDING_ASSIGNMENT;
                         Address.this.instanceAddress = UNASSIGNED_INSTANCEADDR;
                         Address.this.setOwner( Principals.systemFullName( ) );
                         Address.this.stateUuid = UUID.randomUUID( ).toString( );
                         try {
                           Addresses.getInstance( ).register( Address.this );
                         } catch ( NoSuchElementException e ) {
                           LOG.debug( e );
                         }
                       }
                       
                       public void bottom( ) {}
                     } );
    return this;
  }
  
  public Address assign( final VmInstance vm ) {
    if ( vm.getVpcId( ) != null ) throw new IllegalArgumentException( "Cannot assign address to VPC instance" );
    SplitTransition assign = new SplitTransition( Transition.assigning ) {
      public void top( ) {
        Address.this.setInstanceInfo(
            vm.getInstanceUuid(),
            vm.getInstanceId( ),
            vm.getPrivateAddress( )
        );
        Address.this.stateUuid = UUID.randomUUID( ).toString( );
      }
      
      public void bottom( ) {}
    };
    if ( State.impending.equals( this.atomicState.getReference( ) ) ) {
      this.transition( State.impending, State.assigned, true, true, assign );
    } else {
      this.transition( State.allocated, State.assigned, false, true, assign );
    }
    fireUsageEvent( new Supplier<EventActionInfo<AddressAction>>() {
      @Override
      public EventActionInfo<AddressAction> get() {
        return AddressEvent.forAssociate( vm.getInstanceUuid(), vm.getInstanceId() );
      }
    } );
    return this;
  }

  public Address assign( final NetworkInterface networkInterface ) {
    final SplitTransition assign = new SplitTransition( Transition.assigning ) {
      public void top( ) {
        Address.this.setNetworkInterfaceInfo(
          networkInterface.getDisplayName( ),
          networkInterface.getOwnerAccountNumber( ),
          networkInterface.getPrivateIpAddress( )
        );
        Address.this.setInstanceInfo(
            ASSIGNED_UNKNOWN_INSTANCEUUID,
            ASSIGNED_UNKNOWN_INSTANCEID,
            ASSIGNED_UNKNOWN_INSTANCEADDR
        );
        Address.this.stateUuid = UUID.randomUUID( ).toString( );
      }

      public void bottom( ) {}
    };
    if ( State.impending.equals( this.atomicState.getReference( ) ) ) {
      this.transition( State.impending, State.assigned, true, false, assign );
    } else {
      this.transition( State.allocated, State.assigned, false, false, assign );
    }
    return this;
  }

  public Address start( final VmInstance vm ) {
    final SplitTransition start = new SplitTransition( Transition.starting ) {
      public void top( ) {
        if ( getNetworkInterfaceId( ) == null ) {
          throw new IllegalStateException( "Network interface not set" );
        }
        Address.this.setInstanceInfo(
            vm.getInstanceUuid( ),
            vm.getInstanceId( ),
            vm.getPrivateAddress( )
        );
        Address.this.stateUuid = UUID.randomUUID( ).toString( );
      }

      public void bottom( ) {}
    };
    this.transition( State.assigned, State.started, false, false, start );
    fireUsageEvent( new Supplier<EventActionInfo<AddressAction>>( ) {
      @Override
      public EventActionInfo<AddressAction> get( ) {
        return AddressEvent.forAssociate( vm.getInstanceUuid( ), vm.getInstanceId( ) );
      }
    } );
    return this;
  }

  public Address stop( ) {
    fireUsageEvent( new Supplier<EventActionInfo<AddressAction>>() {
      @Override
      public EventActionInfo<AddressAction> get() {
        return AddressEvent.forDisassociate( instanceUuid, instanceId );
      }
    } );

    final SplitTransition stop = new SplitTransition( Transition.stopping ) {
      public void top( ) {
      }

      public void bottom( ) {
        Address.this.stateUuid = UUID.randomUUID( ).toString( );
        Address.this.instanceUuid = UNASSIGNED_INSTANCEUUID;
        Address.this.instanceId = UNASSIGNED_INSTANCEID;
        Address.this.instanceAddress = UNASSIGNED_INSTANCEADDR;
      }
    };
    this.transition( State.started, State.assigned, false, false, stop );
    return this;
  }

  public Transition getTransition( ) {
    return this.transition.getName( );
  }
  
  public RemoteCallback<? extends BaseMessage, ? extends BaseMessage> 
     getCallback(final BaseMessage originReq) {
    final RemoteCallback<? extends BaseMessage, ? extends BaseMessage> cb =
        this.getCallback();
    return cb;
  }
  
  public RemoteCallback<? extends BaseMessage, ? extends BaseMessage> getCallback( ) {
    try {
      Class cbClass = this.transition.getName( ).getCallback( );
      Constructor cbCons = cbClass.getConstructor( Address.class );
      return ( RemoteCallback<? extends BaseMessage, ? extends BaseMessage> ) cbCons.newInstance( this );
    } catch ( Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
      try {
        this.clearPending( );
      } catch ( Exception ex1 ) {
      }
      return new NOOP( );
    }
  }
  
  public Address clearPending( ) {
    if ( !this.atomicState.isMarked( ) ) {
      throw new IllegalStateException( "Trying to clear an address which is not currently pending." );
    } else {
      EventRecord.caller( this.getClass( ), EventType.ADDRESS_STATE, "BOTTOM", this.toString( ) ).info( );
      try {
        this.transition.bottom( );
      } catch ( RuntimeException ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      } finally {
        this.transition = this.QUIESCENT;
        this.atomicState.set( this.atomicState.getReference( ), false );
      }
    }
    return this;
  }
  
  public boolean isAllocated( ) {
    return this.atomicState.getReference( ).ordinal( ) > State.unallocated.ordinal( );
  }
  
  public boolean isSystemOwned( ) {
    return Principals.systemFullName( ).equals( ( UserFullName ) this.getOwner( ) );
  }

  /**
   * Is the instance assigned or with an impending assignment.
   *
   * <P>WARNING! in this state the instance ID may not be a valid instance
   * identifier.</P>
   *
   * @return True if assigned or assignment impending.
   * @see #getInstanceId()
   * @see #getInstanceUuid()
   */
  public boolean isAssigned( ) {
    return this.atomicState.getReference( ).ordinal( ) > State.allocated.ordinal( );
  }

  /**
   * Is it really assigned? Really?
   *
   * <P>In this state the instance ID and UUID are expected to be valid.</P>
   *
   * @return True if assigned to an instance.
   * @see #getInstanceId()
   * @see #getInstanceUuid()
   */
  public boolean isReallyAssigned( ) {
    return this.atomicState.getReference( ).ordinal( ) > State.impending.ordinal( );
  }

  public boolean isStarted( ) {
    return this.atomicState.getReference( ).ordinal( ) > State.assigned.ordinal( );
  }

  public boolean isPending( ) {
    return this.atomicState.isMarked( );
  }

  private static void addAddress( final Address address ) {
    final EntityTransaction db = Entities.get( Address.class );
    String naturalId = address.getNaturalId();
    try {
      // delete matching persistent instance if any
      try {
        Entities.delete( Entities.uniqueResult( forIp( address.getName( ) ) ) );
      } catch ( NoSuchElementException e ) {
        // nothing to delete
      }

      // create new persistent instance, we avoid making the cached
      // copy of the object persistent by using merge
      address.setNaturalId( null ); // each allocations natural ID should be unique
      final Address persisted = Entities.mergeDirect( address );
      naturalId = persisted.getNaturalId();

      db.commit( );
    } catch ( Exception e ) {
      LOG.error( e, e );
    } finally {
      if ( db.isActive() ) db.rollback();
      address.setNaturalId( naturalId );  // restore/update/set naturalId
    }
  }

  /**
   * Get the instance ID for the instance using this address.
   *
   * <P>The instance ID is only a valid identifier when the address is
   * assigned. In other states the value describes the state of the address
   * (e.g. "available" or "pending") </P>
   *
   * @return The instance ID
   * @see #isReallyAssigned()
   */
  public String getInstanceId( ) {
    return this.instanceId;
  }

  /**
   * Get the instance UUID for the instance using this address.
   *
   * <P>The instance ID is only a valid identifier when the address is
   * assigned.</P>
   *
   * @return The instance UUID
   * @see #isReallyAssigned()
   */
  public String getInstanceUuid( ) {
    return this.instanceUuid;
  }

  public String getUserId( ) {
    return this.getOwner( ).getUserId( );
  }
  
  public String getInstanceAddress( ) {
    return this.instanceAddress;
  }
  
  public String getStateUuid( ) {
    return this.stateUuid;
  }

  @Nullable
  public Domain getDomain( ) {
    return domain;
  }

  @Nullable
  public String getAllocationId( ) {
    return allocationId;
  }

  @Nullable
  public String getAssociationId( ) {
    return associationId;
  }

  @Nullable
  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  @Nullable
  public String getNetworkInterfaceOwnerId( ) {
    return networkInterfaceOwnerId;
  }

  @Nullable
  public String getPrivateAddress( ) {
    return privateAddress;
  }

  private void setInstanceInfo( final String instanceUuid,
                                final String instanceId,
                                final String instanceAddress ) {
    this.instanceUuid = instanceUuid;
    this.instanceId = instanceId;
    this.instanceAddress = instanceAddress;
  }

  private void setNetworkInterfaceInfo( final String networkInterfaceId,
                                        final String networkInterfaceOwnerId,
                                        final String privateAddress ) {
    this.associationId = ResourceIdentifiers.generateString( ID_PREFIX_ASSOC );
    this.networkInterfaceId = networkInterfaceId;
    this.networkInterfaceOwnerId = networkInterfaceOwnerId;
    this.privateAddress = privateAddress;
  }

  @Override
  public String toString( ) {
    return "Address " + this.getDisplayName( ) + " " + ( this.isAllocated( )
      ? this.getOwner( ) + " "
      : "" ) + ( this.isAssigned( )
      ? this.instanceId + " " + this.instanceAddress + " "
      : "" ) + " " + this.transition;
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof Address ) ) return false;
    Address address = ( Address ) o;
    if ( !this.getDisplayName( ).equals( address.getDisplayName( ) ) ) return false;
    return true;
  }
  
  @Override
  public int hashCode( ) {
    return this.getDisplayName( ).hashCode( );
  }
  
  public AddressInfoType getAdminDescription( ) {
    final AddressInfoType addressInfoType = TypeMappers.transform( this, AddressInfoType.class );
    final String desc = String.format( "%s (%s)", this.getInstanceId(), this.getOwner() );
    addressInfoType.setInstanceId( desc );
    return addressInfoType;
  }

  public abstract class SplitTransition {
    private Transition t;
    private State      previous;
    
    public SplitTransition( Transition t ) {
      this.t = t;
      this.previous = Address.this.atomicState != null
        ? Address.this.atomicState.getReference( )
        : State.unallocated;
    }
    
    private Transition getName( ) {
      return this.t;
    }
    
    public abstract void top( );
    
    public abstract void bottom( );
    
    @Override
    public String toString( ) {
      State curr = Address.this.atomicState.getReference( );
      boolean mark = Address.this.atomicState.isMarked( );
      return String.format( "AddressTransition %s:%s(%s)",
                            this.t,
                            this.previous != curr ? this.previous + "->" + curr : this.previous,
                            mark );
    }
  }
  
  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" ).region( ComponentIds.lookup( ClusterController.class ).name( ) ).namespace( this.getPartition( ) ).relativeId( "public-address",
                                                                                                                                                           this.getName( ) );
  }
  
  @Override
  public int compareTo( AccountMetadata that ) {
    if ( that instanceof Address ) {
      return this.getName( ).compareTo( that.getName( ) );
    } else {
      return super.compareTo( that );
    }
  }

  /**
   * @see HasFullName#getPartition()
   */
  @Override
  public String getPartition( ) {
    return "eucalyptus";
  }

  private static Address forIp( final String ip ) {
    final Address example = new Address();
    example.setDisplayName( ip );
    return example;
  }

  private void fireUsageEvent( final Supplier<EventActionInfo<AddressAction>> actionInfoSupplier ) {
    fireUsageEvent( getOwner(), actionInfoSupplier );
  }

  private void fireUsageEvent( final OwnerFullName ownerFullName,
                               final Supplier<EventActionInfo<AddressAction>> actionInfoSupplier ) {
    if ( !Principals.isFakeIdentityAccountNumber( ownerFullName.getAccountNumber() ) ) {
      try {
        ListenerRegistry.getInstance().fireEvent(
            AddressEvent.with(
                getDisplayName(),
                ownerFullName,
                Accounts.lookupAccountById(ownerFullName.getAccountNumber()).getName(),
                actionInfoSupplier.get() ) );
      } catch ( final Throwable e ) {
        LOG.error( e, e );
      }
    }
  }
}
