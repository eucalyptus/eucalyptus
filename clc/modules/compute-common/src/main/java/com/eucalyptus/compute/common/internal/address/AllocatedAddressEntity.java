/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.compute.common.internal.address;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.entities.AccountMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.util.HasFullName;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;

import groovy.sql.Sql;
import org.apache.log4j.Logger;

/**
 * Entity representing an in-use address
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_addresses", indexes = {
    @Index( name = "metadata_addresses_user_id_idx", columnList = "metadata_user_id" ),
    @Index( name = "metadata_addresses_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "metadata_addresses_display_name_idx", columnList = "metadata_display_name" ),
} )
public class AllocatedAddressEntity extends UserMetadata<AddressState> implements AddressI, CloudMetadata.AddressMetadata {

  private static final long serialVersionUID        = 1L;

  /**
   * EC2 VPC domain. Null unless allocated for use in VPC.
   */
  @Enumerated( EnumType.STRING )
  @Column( name = "metadata_domain" )
  private AddressDomain domain;

  /**
   * The instance ID that the address is assigned to (Classic) of that the addresses
   * associated ENI is attached to.
   */
  @Column( name = "metadata_instance_id" )
  private String instanceId;

  /**
   * The instance uuid that the address is assigned to (Classic) of that the addresses
   * associated ENI is attached to.
   */
  @Column( name = "metadata_instance_uuid" )
  private String instanceUuid;

  /**
   * EC2 VPC allocation identifier. Null unless allocated for use in VPC.
   */
  @Column( name = "metadata_allocation_id" )
  private String allocationId;

  /**
   * EC2 VPC association identifier. Null unless associated and allocated for use in VPC.
   */
  @Column( name = "metadata_association_id" )
  private String associationId;

  /**
   * EC2 VPC network interface identifier. Null unless associated and allocated for use in VPC.
   */
  @Column( name = "metadata_association_eni_id" )
  private String networkInterfaceId;

  /**
   * EC2 VPC network interface owner identifier. Null unless associated and allocated for use in VPC.
   */
  @Column( name = "metadata_association_eni_owner_id" )
  private String networkInterfaceOwnerId;

  /**
   * EC2 VPC private address. Null unless associated and allocated for use in VPC.
   */
  @Column( name = "metadata_association_private_address" )
  private String privateAddress;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "allocatedAddress" )
  private Collection<AllocatedAddressTag> tags;

  protected AllocatedAddressEntity() {}

  protected AllocatedAddressEntity( final OwnerFullName owner, final String ipAddress ) {
    super( owner, ipAddress );
  }

  public static AllocatedAddressEntity create( ) {
    return new AllocatedAddressEntity( );
  }

  public static AllocatedAddressEntity exampleWithAddress(
      @Nullable final String ip
  ) {
    return exampleWithOwnerAndAddress( null, ip );
  }

  public static AllocatedAddressEntity exampleWithOwnerAndAddress(
      @Nullable final OwnerFullName owner,
      @Nullable final String ip
  ) {
    return new AllocatedAddressEntity( owner, ip );
  }

  public static AllocatedAddressEntity example() {
    return new AllocatedAddressEntity(null, null);
  }

  public String getAddress( ) {
    return getDisplayName( );
  }

  public boolean isAllocated( ) {
    return this.getState( ).ordinal( ) > AddressState.unallocated.ordinal( );
  }

  public boolean isSystemOwned( ) {
    return Principals.systemFullName( ).equals( this.getOwner( ) );
  }

  /**
   * Is the instance assigned or with an impending assignment.
   *
   * <P>WARNING! in this state the instance ID may not be a valid instance
   * identifier.</P>
   *
   * @return True if assigned or assignment impending.
   */
  public boolean isAssigned( ) {
    return this.getState( ).ordinal( ) > AddressState.allocated.ordinal( );
  }

  public boolean isStarted( ) {
    return this.getState( ).ordinal( ) > AddressState.assigned.ordinal( );
  }

  public String getUserId( ) {
    return this.getOwner( ).getUserId( );
  }

  /**
   * Some address states
   */
  @Nonnull
  public AddressDomain domainWithDefault( ) {
    return MoreObjects.firstNonNull( getDomain( ), AddressDomain.standard );
  }

  @Nullable
  public AddressDomain getDomain( ) {
    return domain;
  }

  public void setDomain( final AddressDomain domain ) {
    this.domain = domain;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( final String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getInstanceUuid( ) {
    return instanceUuid;
  }

  public void setInstanceUuid( final String instanceUuid ) {
    this.instanceUuid = instanceUuid;
  }

  public String getAllocationId( ) {
    return allocationId;
  }

  public void setAllocationId( final String allocationId ) {
    this.allocationId = allocationId;
  }

  public String getAssociationId( ) {
    return associationId;
  }

  public void setAssociationId( final String associationId ) {
    this.associationId = associationId;
  }

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( final String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public String getNetworkInterfaceOwnerId( ) {
    return networkInterfaceOwnerId;
  }

  public void setNetworkInterfaceOwnerId( final String networkInterfaceOwnerId ) {
    this.networkInterfaceOwnerId = networkInterfaceOwnerId;
  }

  public String getPrivateAddress( ) {
    return privateAddress;
  }

  public void setPrivateAddress( final String privateAddress ) {
    this.privateAddress = privateAddress;
  }

  @Override
  public void setOwnerAccountNumber( final String ownerAccountId ) {
    super.setOwnerAccountNumber( ownerAccountId );
  }

  @Override
  public String toString( ) {
    return "Address " + this.getDisplayName( ) + " " + ( this.isAllocated( )
        ? this.getOwner( ) + " "
        : "" );
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof AllocatedAddressEntity ) ) return false;
    AllocatedAddressEntity address = ( AllocatedAddressEntity ) o;
    if ( !this.getDisplayName( ).equals( address.getDisplayName( ) ) ) return false;
    return true;
  }

  @Override
  public int hashCode( ) {
    return this.getDisplayName( ).hashCode( );
  }

  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" ).region( "cluster" ).namespace( this.getPartition( ) ).relativeId( "public-address",
        this.getName( ) );
  }

  @Override
  public int compareTo( AccountMetadata that ) {
    if ( that instanceof AllocatedAddressEntity ) {
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

  @Override
  protected String createUniqueName( ) {
    return getDisplayName( );
  }

  @PrePersist
  @PreUpdate
  private void checkState( ) {
    if ( getState( ).ordinal( ) < AddressState.allocated.ordinal( ) ) {
      throw new IllegalStateException( "Address " + getDisplayName( ) + " state not valid for persistence: " + getState( ) );
    }
  }

  public static Function<AddressI,String> allocation( ) {
    return FilterFunctions.ALLOCATION_ID;
  }

  public enum FilterFunctions implements Function<AddressI,String> {
    ALLOCATION_ID {
      @Nullable
      @Override
      public String apply( final AddressI address ) {
        return address.getAllocationId( );
      }
    },
    ASSOCIATION_ID {
      @Override
      public String apply( final AddressI address ) {
        return address.getAssociationId( );
      }
    },
    DOMAIN {
      @Override
      public String apply( final AddressI address ) {
        return address.getDomain( ) == null ?
            AddressDomain.standard.toString( ) :
            address.getDomain( ).toString( );
      }
    },
    INSTANCE_ID {
      @Override
      public String apply( final AddressI address ) {
        return address.getInstanceId( );
      }
    },
    NETWORK_INTERFACE_ID {
      @Override
      public String apply( final AddressI address ) {
        return address.getNetworkInterfaceId( );
      }
    },
    NETWORK_INTERFACE_OWNER_ID {
      @Override
      public String apply( final AddressI address ) {
        return address.getNetworkInterfaceOwnerId( );
      }
    },
    PRIVATE_IP_ADDRESS {
      @Override
      public String apply( final AddressI address ) {
        return address.getPrivateAddress( );
      }
    },
    PUBLIC_IP {
      @Override
      public String apply( final AddressI address ) {
        return address.getAddress( );
      }
    },
  }
  @Upgrades.PreUpgrade( since = Upgrades.Version.v4_2_0, value = Compute.class )
  public static class AllocatedAddressEntity420PreUpgrade implements Callable<Boolean> {
    private static final Logger logger = Logger.getLogger( AllocatedAddressEntity420PreUpgrade.class );

    @Override
    public Boolean call( ) throws Exception {
      logger.info( "Cleaning up public addresses for instances" );
      Sql sql = null;
      try {
        sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloud" );
        int updated = sql.executeUpdate(
                "update metadata_instances set metadata_vm_public_address = '0.0.0.0', version = version + 1 where " +
                        "metadata_vm_public_address != '0.0.0.0' and ( metadata_vm_public_address = '' or " +
                        "( metadata_vpc_id is null and metadata_state = 'STOPPED' ) )"
        );
        logger.info( "Cleared public address for " + updated + " instances" );
      } catch ( final Exception e ) {
        logger.error( "Error cleaning up public addresses for instances", e );
      } finally {
        if ( sql != null ) {
          sql.close( );
        }
      }
      return Boolean.TRUE;
    }
  }
  @Upgrades.EntityUpgrade( entities = AllocatedAddressEntity.class, since = Upgrades.Version.v4_2_0, value = Compute.class )
  public enum AllocatedAddressEntity420Upgrade implements Predicate<Class> {
    INSTANCE;

    @Override
    public boolean apply( final Class entityClass ) {
      final List<VmInstance> vmInstances = VmInstances.list( Predicates.alwaysTrue( ) );
      try ( final TransactionResource tx = Entities.transactionFor( AllocatedAddressEntity.class ) ) {
        // Populate Elastic IP info
        final Set<String> addresses = Sets.newHashSetWithExpectedSize( 128 );
        for ( final AllocatedAddressEntity entity : Entities.query( AllocatedAddressEntity.exampleWithAddress( null ) ) ) {
          addresses.add( entity.getAddress( ) );
          try {
            final VmInstance instance = VmInstances.lookupByPublicIp( entity.getAddress( ) );
            if ( instance.getVpcId( ) == null ) {
              entity.setState( AddressState.assigned );
              entity.setDomain( AddressDomain.standard );
              entity.setInstanceId( instance.getDisplayName( ) );
              entity.setInstanceUuid( instance.getInstanceUuid( ) );
              entity.setPrivateAddress( instance.getPrivateAddress( ) );
            } else if ( instance.getNetworkInterfaces( ) != null ) {
              for ( final NetworkInterface networkInterface : instance.getNetworkInterfaces( ) ) {
                if ( networkInterface.isAssociated( ) ) {
                  entity.setState( AddressState.assigned );
                  entity.setDomain( AddressDomain.vpc );
                  entity.setAllocationId( networkInterface.getAssociation( ).getAllocationId( ) );
                  entity.setAssociationId( networkInterface.getAssociation( ).getAssociationId( ) );
                  entity.setNetworkInterfaceId( networkInterface.getDisplayName( ) );
                  entity.setNetworkInterfaceOwnerId( networkInterface.getOwnerUserId( ) );
                  entity.setPrivateAddress( networkInterface.getPrivateIpAddress( ) );
                  if ( networkInterface.isAttached( ) && networkInterface.getAttachment( ).getDeviceIndex( ) == 0 ) {
                    entity.setInstanceId( instance.getDisplayName( ) );
                    entity.setInstanceUuid( instance.getInstanceUuid( ) );
                  }
                }
              }
            }
          } catch ( NoSuchElementException e ) {
            entity.setState( AddressState.allocated );
            if ( entity.getDomain( ) == null ) {
              entity.setDomain( AddressDomain.standard );
            }
          }
        }

        // Add missing public IPs for instances
        for ( final VmInstance instance : vmInstances ) {
          if ( !VmNetworkConfig.DEFAULT_IP.equals( instance.getPublicAddress( ) ) && !addresses.contains( instance.getPublicAddress( ) )  ) {
            if ( instance.getVpcId( ) == null ) {
              final AllocatedAddressEntity entity = AllocatedAddressEntity.create( );
              entity.setDisplayName( instance.getPublicAddress( ) );
              entity.setState( AddressState.assigned );
              entity.setOwner( Principals.systemFullName( ) );
              entity.setInstanceId( instance.getDisplayName( ) );
              entity.setInstanceUuid( instance.getInstanceUuid( ) );
              entity.setPrivateAddress( instance.getPrivateAddress( ) );
              Entities.persist( entity );
            } else if ( instance.getNetworkInterfaces( ) != null ) {
              for ( final NetworkInterface networkInterface : instance.getNetworkInterfaces( ) ) {
                if ( networkInterface.isAssociated( ) ) {
                  final AllocatedAddressEntity entity = AllocatedAddressEntity.create( );
                  entity.setDisplayName( networkInterface.getAssociation( ).getPublicIp( ) );
                  entity.setState( AddressState.assigned );
                  entity.setOwner( Principals.systemFullName( ) );
                  entity.setAllocationId( networkInterface.getAssociation( ).getAllocationId( ) );
                  entity.setAssociationId( networkInterface.getAssociation( ).getAssociationId( ) );
                  entity.setNetworkInterfaceId( networkInterface.getDisplayName( ) );
                  entity.setNetworkInterfaceOwnerId( networkInterface.getOwnerUserId( ) );
                  entity.setPrivateAddress( networkInterface.getPrivateIpAddress( ) );
                  if ( networkInterface.isAttached( ) && networkInterface.getAttachment( ).getDeviceIndex( ) == 0 ) {
                    entity.setInstanceId( instance.getDisplayName( ) );
                    entity.setInstanceUuid( instance.getInstanceUuid( ) );
                  }
                  Entities.persist( entity );
                }
              }
            }
          }
        }

        tx.commit() ;
      }

      return true;
    }
  }

}
