/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.compute.common.internal.vm;

import static com.eucalyptus.util.Strings.truncate;
import java.lang.Object;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.BaseInstanceProfile;
import com.eucalyptus.compute.common.CloudMetadata.VmInstanceMetadata;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.GroupItemType;
import com.eucalyptus.compute.common.GroupSetType;
import com.eucalyptus.compute.common.IamInstanceProfile;
import com.eucalyptus.compute.common.ImageMetadata.Platform;
import com.eucalyptus.compute.common.internal.images.Images;
import com.eucalyptus.compute.common.internal.network.PrivateAddressReferrer;
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.id.Dns;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.InstanceBlockDeviceMapping;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceAssociationType;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceAttachmentType;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetItemType;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetType;
import com.eucalyptus.compute.common.InstancePrivateIpAddressesSetItemType;
import com.eucalyptus.compute.common.InstancePrivateIpAddressesSetType;
import com.eucalyptus.compute.common.InstanceStateType;
import com.eucalyptus.compute.common.InstanceStatusDetailsSetItemType;
import com.eucalyptus.compute.common.InstanceStatusDetailsSetType;
import com.eucalyptus.compute.common.InstanceStatusEventType;
import com.eucalyptus.compute.common.InstanceStatusEventsSetType;
import com.eucalyptus.compute.common.InstanceStatusItemType;
import com.eucalyptus.compute.common.InstanceStatusType;
import com.eucalyptus.compute.common.ReservationInfoType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.internal.images.BlockStorageImageInfo;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransientEntityException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.compute.common.internal.keys.SshKeyPair;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.InstanceCreationEvent;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.PreUpgrade;
import com.eucalyptus.upgrade.Upgrades.Version;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import groovy.sql.Sql;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_instances", indexes = {
    @Index( name = "metadata_instances_user_id_idx", columnList = "metadata_user_id" ),
    @Index( name = "metadata_instances_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "metadata_instances_display_name_idx", columnList = "metadata_display_name" ),
    @Index( name = "metadata_vm_private_address_idx", columnList = "metadata_vm_private_address" ),
    @Index( name = "metadata_vm_public_address_idx", columnList = "metadata_vm_public_address" ),
} )
public class VmInstance extends UserMetadata<VmState> implements VmInstanceMetadata, PrivateAddressReferrer {
  private static final long    serialVersionUID = 1L;
  private static final Logger  LOG              = Logger.getLogger( VmInstance.class );

  public static final String         DEFAULT_TYPE         = "m1.small";
  public static final String         ROOT_DEVICE_TYPE_EBS = "ebs";
  public static final String         ID_PREFIX            = "i";
  public static final String         VM_NC_HOST_TAG       = "euca:node";

  @Embedded
  private VmNetworkConfig      networkConfig;
  @Embedded
  private final VmId           vmId;
  @Embedded
  private VmBootRecord         bootRecord;
  @Embedded
  private final VmUsageStats   usageStats;
  @Embedded
  private final VmLaunchRecord launchRecord;
  @Embedded
  private VmRuntimeState       runtimeState;
  @Embedded
  private VmVolumeState        transientVolumeState;
  @Embedded
  private final VmPlacement    placement;

  @Column( name = "metadata_vm_expiration" )
  private final Date           expiration;
  @Column( name = "metadata_vm_private_networking" )
  private final Boolean        privateNetwork;
  @Column( name = "metadata_vm_disable_api_termination" )
  private Boolean              disableApiTermination;
  @NotFound( action = NotFoundAction.IGNORE )
  @ManyToMany( cascade = { CascadeType.ALL },
               fetch = FetchType.LAZY )
  private Set<NetworkGroup>    networkGroups    = Sets.newHashSet( );

  @ElementCollection
  @CollectionTable(
      name = "metadata_vm_instance_groups",
      joinColumns = @JoinColumn( name = "vminstance_id", referencedColumnName = "id" ),
      indexes = @Index( name = "metadata_vm_instance_groups_vminstance_idx", columnList = "vminstance_id" )
  )
  private Set<NetworkGroupId>  networkGroupIds = Sets.newHashSet( );

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "instance" )
  private Collection<VmInstanceTag> tags;

  public static Criterion criterion( VmState... state ) {
    return Restrictions.in( "state", state );
  }

  public static Criterion zoneCriterion( String... zone ) {
    return Restrictions.in( "placement.partitionName", zone );
  }

  public static Criterion nonNullNodeCriterion( ) {
    return Restrictions.isNotNull( "runtimeState.serviceTag" );
  }

  public static Criterion nullNodeCriterion( ) {
    return Restrictions.isNull( "runtimeState.serviceTag" );
  }

  public static Criterion serviceTagCriterion( final String tag ) {
    return Restrictions.eq( "runtimeState.serviceTag", tag );
  }

  public static Criterion lastUpdatedCriterion( final long timestamp ) {
    return Restrictions.lt( "lastUpdateTimestamp", new Date( timestamp ) );
  }

  public static Projection instanceIdProjection( ) {
    return Projections.property( "displayName" );
  }

  public static Projection instanceUuidProjection( ) {
    return Projections.property( "naturalId" );
  }

  /**
   *
   */
  public static RunningInstancesItemType transform( final VmInstance vm ) {
    return Transform.INSTANCE.apply( vm );
  }

  private enum StringPropertyFunctions implements Function<VmInstance,String> {
    CLIENT_TOKEN {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getClientToken( );
      }
    },
  }

  public enum Filters implements Predicate<VmInstance> {
    BUNDLING {

      @Override
      public boolean apply( final VmInstance arg0 ) {
        return arg0.getRuntimeState( ).isBundling( );
      }

    }
  }

  public enum VmStateSet implements Predicate<VmInstance> {
    RUN( VmState.PENDING, VmState.RUNNING ),
    CHANGING( VmState.PENDING, VmState.STOPPING, VmState.SHUTTING_DOWN ) {

      @Override
      public boolean apply( final VmInstance arg0 ) {
        return super.apply( arg0 ) || !arg0.eachVolumeAttachment( new Predicate<VmVolumeAttachment>( ) {
          @Override
          public boolean apply( final VmVolumeAttachment input ) {
            return !input.getAttachmentState( ).isVolatile( );
          }
        } );
      }

    },
    EXPECTING_TEARDOWN( VmState.STOPPING, VmState.SHUTTING_DOWN ),
    TORNDOWN( VmState.STOPPED, VmState.TERMINATED, VmState.BURIED ),
    STOP( VmState.STOPPING, VmState.STOPPED ),
    NOT_RUNNING( VmState.STOPPING, VmState.STOPPED, VmState.SHUTTING_DOWN, VmState.TERMINATED, VmState.BURIED ),
    DONE( VmState.TERMINATED, VmState.BURIED );

    private final Set<VmState> states;

    VmStateSet( final VmState... states ) {
      this.states = Collections.unmodifiableSet( EnumSet.copyOf( Sets.newHashSet( states ) ) );
    }

    @Override
    public boolean apply( final VmInstance arg0 ) {
      return this.states.contains( arg0.getState( ) );
    }

    public boolean contains( final VmState state ) {
      return this.states.contains( state );
    }

    public Predicate<VmInstance> not( ) {
      return Predicates.not( this );
    }

    public Set<VmState> set( ) {
      return states;
    }

    public VmState[] array( ) {
      return states.toArray( new VmState[ states.size( ) ] );
    }
  }

  public enum VmState implements Predicate<VmInstance> {
    PENDING( 0 ),
    RUNNING( 16 ),
    SHUTTING_DOWN( 32 ),
    TERMINATED( 48 ),
    STOPPING( 64 ),
    STOPPED( 80 ),
    BURIED( 128, TERMINATED );

    private final String name;
    private final int    code;
    private final VmState displayState;

    VmState( final int code ) {
      this( code, null );
    }

    VmState( final int code, final VmState displayState ) {
      this.name = this.name( ).toLowerCase( ).replace( "_", "-" );
      this.code = code;
      this.displayState = Objects.firstNonNull( displayState, this );
    }

    public String getName( ) {
      return this.name;
    }

    public int getCode( ) {
      return this.code;
    }

    public VmState getDisplayState( ) {
      return this.displayState;
    }

    public static class Mapper {
      private static Map<String, VmState> stateMap = getStateMap( );

      private static Map<String, VmState> getStateMap( ) {
        final Map<String, VmState> map = new HashMap<String, VmState>( );
        map.put( "Extant", VmState.RUNNING );
        map.put( "Pending", VmState.PENDING );
        map.put( "Teardown", VmState.SHUTTING_DOWN );
        return map;
      }

      public static VmState get( final String stateName ) {
        return Mapper.stateMap.get( stateName );
      }
    }

    @Override
    public boolean apply( final VmInstance arg0 ) {
      return this.equals( arg0.getState( ) );
    }

    public Predicate<VmInstance> not( ) {
      return Predicates.not( this );
    }
  }

  public enum Lookup implements Function<String, VmInstance> {
    INSTANCE {

      @Nonnull
      @Override
      public VmInstance apply( final String arg0 ) {
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, arg0 ) );
          if ( ( vm == null ) ) {
            throw new NoSuchElementException( "Failed to lookup vm instance: " + arg0 );
          }
          db.commit( );
          return vm;
        } catch ( final NoSuchElementException ex ) {
          throw ex;
        } catch ( final Exception ex ) {
          throw new NoSuchElementException( "An error occurred while trying to lookup vm instance " + arg0 + ": " + ex.getMessage( ) + "\n"
                                            + Exceptions.causeString( ex ) );
        } finally {
          if ( db.isActive() ) db.rollback();
        }
      }
    };

    @Nonnull
    @Override
    public abstract VmInstance apply( final String arg0 );
  }

  public VmInstance( final OwnerFullName owner,
                     final VmId vmId,
                     final VmBootRecord bootRecord,
                     final VmLaunchRecord launchRecord,
                     final VmPlacement placement,
                     final List<NetworkGroup> networkRulesGroups,
                     final Boolean usePrivateAddressing,
                     final Boolean disableApiTermination,
                     final Date expiration ) throws ResourceAllocationException {
    super( owner, vmId.getInstanceId( ) );
    this.setState( VmState.PENDING );
    this.vmId = vmId;
    this.expiration = expiration;
    this.bootRecord = bootRecord;
    this.launchRecord = launchRecord;
    this.placement = placement;
    this.privateNetwork = Boolean.FALSE;
    this.disableApiTermination = disableApiTermination;
    this.usageStats = new VmUsageStats( this );
    this.runtimeState = new VmRuntimeState( this );
    this.transientVolumeState = new VmVolumeState( this );
    this.networkConfig = new VmNetworkConfig( this, usePrivateAddressing );
    final Function<NetworkGroup, NetworkGroup> func = Entities.merge( );
    this.networkGroups.addAll( Collections2.transform( networkRulesGroups, func ) );
    this.store( );
  }

  private VmInstance( final OwnerFullName owner, final VmId vmId ) {
    super( owner, null );
    this.vmId = vmId;
    this.expiration = null;
    this.runtimeState = null;
    this.bootRecord = null;
    this.launchRecord = null;
    this.placement = null;
    this.privateNetwork = null;
    this.usageStats = null;
    this.networkConfig = null;
    this.transientVolumeState = null;
  }

  protected VmInstance( final OwnerFullName ownerFullName, final String instanceId2 ) {
    super( ownerFullName, instanceId2 );
    this.expiration = null;
    this.runtimeState = null;
    this.vmId = null;
    this.bootRecord = null;
    this.launchRecord = null;
    this.placement = null;
    this.privateNetwork = null;
    this.usageStats = null;
    this.networkConfig = null;
    this.transientVolumeState = null;
  }

  protected VmInstance( ) {
    this.expiration = null;
    this.vmId = null;
    this.bootRecord = null;
    this.launchRecord = null;
    this.placement = null;
    this.privateNetwork = null;
    this.usageStats = null;
    this.runtimeState = null;
    this.networkConfig = null;
    this.transientVolumeState = null;
  }

  @Override
  protected String createUniqueName() {
    return getDisplayName( ) == null ? null : truncate( getDisplayName( ), 10 );
  }

  /**
   * Clear references that are not valid for a terminated instance
   */
  public void clearReferences( ) {
    if (bootRecord.getArchitecture() == null) bootRecord.setArchitecture(bootRecord.getMachine().getArchitecture());
    bootRecord.setMachine( );
    bootRecord.setKernel();
    bootRecord.setRamdisk();
    bootRecord.setVpc( null );
    bootRecord.setSubnet( null );
    clearRunReferences( );
  }

  /**
   * Clear any references that are not valid for a stopped instance
   */
  public void clearRunReferences( ) {
    getRuntimeState( ).setServiceTag( null );
  }

  public void updatePublicAddress( final String publicAddr, final String publicDnsName ) {
    this.getNetworkConfig( ).setPublicAddress( publicAddr );
    this.getNetworkConfig( ).setPublicDnsName( dnsHostnamesEnabled( ) ? publicDnsName : "" );
  }

  public void updatePrivateAddress( final String privateAddr, final String privateDnsName ) {
    this.getNetworkConfig( ).setPrivateAddress( privateAddr );
    this.getNetworkConfig( ).setPrivateDnsName( privateDnsName );
  }

  public void updateMacAddress( final String macAddress ) {
    if ( getMacAddress( ) == null ) {
      getNetworkConfig().setMacAddress( macAddress );
    }
  }

  public VmRuntimeState getRuntimeState( ) {
    if ( this.runtimeState == null ) {
      this.runtimeState = new VmRuntimeState( this );
    }
    return this.runtimeState;
  }

  private boolean dnsHostnamesEnabled( ) {
    final Vpc vpc = getBootRecord( ).getVpc( );
    return vpc == null || Objects.firstNonNull( vpc.getDnsHostnames(), Boolean.FALSE );
  }

  public void store( ) {
    this.updateTimeStamps( );
    this.firePersist( );
    this.fireUsageEvent( );
  }

  private void firePersist( ) {
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      if ( Entities.isPersistent( this ) ) try {
        Entities.merge( this );
        db.commit( );
      } catch ( final Exception ex ) {
        LOG.debug( ex );
      }
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  private void fireUsageEvent( ) {
    if ( VmState.RUNNING.equals( this.getState( ) ) ) {
      try {
        final OwnerFullName owner = this.getOwner();
        final String userId = owner.getUserId();
        final String userName = owner.getUserName();
        final String accountId = owner.getAccountNumber();

        ListenerRegistry.getInstance( ).fireEvent( new InstanceCreationEvent(
            getInstanceUuid(),
            getDisplayName(),
            this.bootRecord.getVmType().getName(),
            userId,
            userName,
            accountId,
            Accounts.lookupAccountAliasById(accountId),
            this.placement.getPartitionName()));
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
      }
    }
  }



  public synchronized long getSplitTime( ) {
    final long time = System.currentTimeMillis( );
    final long split = time - super.getLastUpdateTimestamp( ).getTime( );
    return split;
  }

  public synchronized long getCreationSplitTime( ) {
    final long time = System.currentTimeMillis( );
    final long split = time - super.getCreationTimestamp( ).getTime( );
    return split;
  }

  public String getImageId( ) {
    return this.bootRecord.getDisplayMachineImageId();
  }

  @Nullable
  public String getRamdiskId( ) {
    return this.bootRecord.getDisplayRamdiskImageId();
  }

  @Nullable
  public String getKernelId( ) {
    return this.bootRecord.getDisplayKernelImageId();
  }

  public boolean hasPublicAddress( ) {
    return ( this.networkConfig != null )
           && !( VmNetworkConfig.DEFAULT_IP.equals( this.getNetworkConfig( ).getPublicAddress( ) ) || this.getNetworkConfig( ).getPrivateAddress( ).equals(
             this.getNetworkConfig( ).getPublicAddress( ) ) );
  }

  public String getInstanceId( ) {
    return super.getDisplayName( );
  }

  public VmType getVmType( ) {
    return this.bootRecord.getVmType( );
  }

  public boolean isUsePrivateAddressing() {
    // allow for null value
    return Boolean.TRUE.equals( this.getNetworkConfig( ).getUsePrivateAddressing( ) );
  }

  public String getPrivateAddress( ) {
    return this.getNetworkConfig( ).getPrivateAddress( );
  }

  public String getPublicAddress( ) {
    return this.getNetworkConfig( ).getPublicAddress( );
  }

  public String getPrivateDnsName( ) {
    return this.getNetworkConfig( ).getPrivateDnsName( );
  }

  public String getPublicDnsName( ) {
    return this.getNetworkConfig( ).getPublicDnsName( );
  }

  public String getMacAddress( ) {
    return this.getNetworkConfig( ).getMacAddress( );
  }

  public List<NetworkInterface> getNetworkInterfaces( ) {
    return ImmutableList.copyOf( Iterables.filter(
        this.getNetworkConfig( ).getNetworkInterfaces( ),
        Predicates.<NetworkInterface>notNull( )
    ) );
  }

  public void addNetworkInterface( final NetworkInterface networkInterface ) {
    final List<NetworkInterface> networkInterfaces = this.getNetworkConfig( ).getNetworkInterfaces( );
    final int index = networkInterface.getAttachment( ).getDeviceIndex( );
    if ( networkInterfaces.size( ) > index ) {
      networkInterfaces.set( index, networkInterface );
    } else {
      while ( networkInterfaces.size( ) < index ) networkInterfaces.add( null );
      networkInterfaces.add( networkInterface );
    }
  }

  public String getPasswordData( ) {
    return this.getRuntimeState( ).getPasswordData( );
  }

  public void updatePasswordData( final String passwordData ) {
    this.getRuntimeState( ).setPasswordData( passwordData );
  }

  /**
   * @return the platform
   */
  public String getPlatform( ) {
    return this.bootRecord.getPlatform( ).toString( );
  }

  public String getDisplayPlatform( ) {
    return Platform.windows == this.bootRecord.getPlatform( ) ?
        Platform.windows.name() :
        "";
  }

  @Nullable
  public String getSubnetId( ) {
    return this.bootRecord.getSubnetId( );
  }

  @Nullable
  public String getVpcId( ) {
    return this.bootRecord.getVpcId( );
  }

  @Override
  public String getPartition( ) {
    return this.placement.getPartitionName( );
  }

  public String getInstanceUuid( ) {
    return this.getNaturalId( );
  }

  public static VmInstance named( final String instanceId ) {
    return new VmInstance( null, instanceId );
  }

  public static VmInstance named( final OwnerFullName ownerFullName, final String instanceId ) {
    return new VmInstance( ownerFullName, instanceId );
  }

  public static VmInstance withToken( final OwnerFullName ownerFullName, final String clientToken ) {
    return new VmInstance( ownerFullName, new VmId( null, null, clientToken, null ) );
  }

  public static VmInstance withUuid( final String uuid ) {
    final VmInstance example = new VmInstance( );
    example.setNaturalId( uuid );
    return example;
  }

  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" )
                          .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                          .namespace( this.getOwnerAccountNumber( ) )
                          .relativeId( "instance", this.getDisplayName( ) );
  }

  public enum Reason implements Predicate<VmInstance> {
    NORMAL( "" ),
    EXPIRED( "Instance expired after not being reported." ),
    FAILED( "The instance failed to start on the NC." ),
    USER_TERMINATED( true, "User terminated." ),
    USER_STOPPED( true, "User stopped." ),
    USER_STARTED( true, "User started." ),
    APPEND( "" );

    private final boolean user;
    private final String message;

    Reason( final String message ) {
      this( false, message );
    }

    Reason( final boolean user, final String message ) {
      this.user = user;
      this.message = message;
    }

    public boolean user( ) {
      return user;
    }

    @Override
    public String toString( ) {
      return this.message;
    }

    @Override
    public boolean apply( final VmInstance vmInstance ) {
      return this.equals( vmInstance.getRuntimeState().reason() );
    }
  }

  private Boolean getPrivateNetwork( ) {
    return this.privateNetwork;
  }

  @Nullable
  public Boolean getDisableApiTermination( ) {
    return disableApiTermination;
  }

  public Collection<VmInstanceTag> getTags() {
    return tags;
  }

  public Set<NetworkGroup> getNetworkGroups( ) {
    return this.networkGroups != null ?
        this.networkGroups :
        Sets.newHashSet( );
  }

  public Set<NetworkGroupId> getNetworkGroupIds( ) {
    return this.networkGroupIds;
  }

  static long getSerialversionuid( ) {
    return serialVersionUID;
  }

  static Logger getLOG( ) {
    return LOG;
  }

  static String getDEFAULT_IP( ) {
    return VmNetworkConfig.DEFAULT_IP;
  }

  static String getDEFAULT_TYPE( ) {
    return DEFAULT_TYPE;
  }

  VmId getVmId( ) {
    return this.vmId;
  }

  public VmBootRecord getBootRecord( ) {
    return this.bootRecord;
  }

  VmUsageStats getUsageStats( ) {
    return this.usageStats;
  }

  public VmLaunchRecord getLaunchRecord( ) {
    return this.launchRecord;
  }

  VmPlacement getPlacement( ) {
    return this.placement;
  }

  public Partition lookupPartition( ) {
    return this.placement.lookupPartition( );
  }

  /**
   *
   */
  public VmVolumeAttachment lookupVolumeAttachmentByDevice( final String volumeDevice ) {
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      final VmInstance entity = Entities.merge( this );
      VmVolumeAttachment ret;
      try {
        ret = entity.getTransientVolumeState( ).lookupVolumeAttachmentByDevice( volumeDevice );
      } catch ( final Exception ex ) {
        ret = Iterables.find( entity.getBootRecord( ).getPersistentVolumes( ), VmVolumeAttachment.volumeDeviceFilter( volumeDevice ) );
      }
      db.commit( );
      return ret;
    } catch ( final NoSuchElementException ex ) {
      Logs.extreme( ).error( ex, ex );
      throw ex;
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      throw new NoSuchElementException( "Failed to lookup volume with device: " + volumeDevice );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  /**
   *
   */
  public VmVolumeAttachment lookupVolumeAttachment( final String volumeId ) {
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      final VmInstance entity = Entities.isPersistent( this ) ?
          this :
          Entities.uniqueResult( this );
      VmVolumeAttachment volumeAttachment;
      try {
        volumeAttachment = entity.getTransientVolumeState( ).lookupVolumeAttachment( volumeId );
      } catch ( final NoSuchElementException ex ) {
        volumeAttachment = Iterables.find( entity.getBootRecord( ).getPersistentVolumes( ), VmVolumeAttachment.volumeIdFilter( volumeId ) );
      }
      db.commit( );
      return volumeAttachment;
    } catch ( final Exception ex ) {
      throw new NoSuchElementException( "Failed to lookup volume: " + volumeId );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  /**
   *
   */
  public boolean eachVolumeAttachment( final Predicate<VmVolumeAttachment> predicate ) {
    if ( VmStateSet.DONE.contains( this.getState( ) ) && !VmStateSet.EXPECTING_TEARDOWN.contains( this.getLastState( ) ) ) {
      return false;
    } else {
      final EntityTransaction db = Entities.get( VmInstance.class );
      try {
        final VmInstance entity = Entities.merge( this );
        Set<VmVolumeAttachment> persistentAttachments = Sets.<VmVolumeAttachment>newHashSet( entity.getBootRecord( ).getPersistentVolumes( ) );
    	boolean ret = Iterables.all( persistentAttachments, new Predicate<VmVolumeAttachment>( ) {

            @Override
            public boolean apply( final VmVolumeAttachment arg0 ) {
              return predicate.apply( arg0 );
            }
          } );

    	ret |= entity.getTransientVolumeState( ).eachVolumeAttachment( new Predicate<VmVolumeAttachment>( ) {

          @Override
          public boolean apply( final VmVolumeAttachment arg0 ) {
            return predicate.apply( arg0 );
          }
        } );
    	db.commit( );
        return ret;
      } catch ( final Exception ex ) {
        Logs.extreme( ).error( ex, ex );
        db.rollback( );
        return false;
      }
    }
  }

  /**
   *
   */
  public String getServiceTag( ) {
    return this.getRuntimeState( ).getServiceTag( );
  }

  /**
   *
   */
  public String getReservationId( ) {
    return this.vmId.getReservationId( );
  }

  /**
   *
   */
  public byte[] getUserData( ) {
    return this.bootRecord.getUserData( );
  }

  public void setUserDataAsString( final String userData ){
    if(userData == null || userData.length()<=0){
      this.bootRecord.setUserData(new byte[0]);
    } else {
      this.bootRecord.setUserData(userData.getBytes());
    }
  }

  public void setDisableApiTermination( final Boolean disableApiTermination ) {
    this.disableApiTermination = disableApiTermination;
  }

  /**
   * Get the state suitable for EC2 API.
   *
   * @see #getState
   */
  public VmState getDisplayState( ) {
    return getState( ).getDisplayState( );
  }

  @Nonnull
  public String getDisplayPublicDnsName( ) {
    return VmStateSet.TORNDOWN.apply( this ) ?
        "" :
        dns() ?
            getPublicDnsName( ) :
            getDisplayPublicAddress( );
  }

  @Nonnull
  public String getDisplayPublicAddress( ) {
    return VmStateSet.TORNDOWN.apply( this ) ?
        "" :
        VmNetworkConfig.DEFAULT_IP.equals( Objects.firstNonNull( Strings.emptyToNull( getPublicAddress( ) ), VmNetworkConfig.DEFAULT_IP ) ) ?
            getVpcId( ) == null ? getDisplayPrivateAddress( ) : "" :
            getPublicAddress( );
  }

  @Nonnull
  public String getDisplayPrivateDnsName( ) {
    return VmStateSet.TORNDOWN.apply( this ) ?
        "" :
        dns() ?
            getPrivateDnsName( ) :
            getDisplayPrivateAddress( );
  }

  @Nonnull
  public String getDisplayPrivateAddress( ) {
    return VmStateSet.TORNDOWN.apply( this ) ?
        "" :
        VmNetworkConfig.DEFAULT_IP.equals( Objects.firstNonNull( Strings.emptyToNull( getPrivateAddress( ) ), VmNetworkConfig.DEFAULT_IP ) ) ?
            VmNetworkConfig.DEFAULT_IP :
            getPrivateAddress( );
  }

  private static boolean dns( ) {
    return StackConfiguration.USE_INSTANCE_DNS && !ComponentIds.lookup( Dns.class ).runLimitedServices( );
  }

  @TypeMapper
  public enum Transform implements Function<VmInstance, RunningInstancesItemType> {
    INSTANCE;

    /**
     * @see Supplier#get()
     */
    @Override
    public RunningInstancesItemType apply( final VmInstance input ) {
      if ( !Entities.isPersistent( input ) ) {
        throw new TransientEntityException( input.toString( ) );
      } else {
        try {
          final RunningInstancesItemType runningInstance = new RunningInstancesItemType( );

          runningInstance.setAmiLaunchIndex( Integer.toString( input.getLaunchRecord( ).getLaunchIndex( ) ) );
          final VmState displayState = input.getDisplayState();
          runningInstance.setStateCode( Integer.toString( displayState.getCode() ) );
          runningInstance.setStateName( displayState.getName() );
          runningInstance.setPlatform( input.getPlatform( ) );

          runningInstance.setInstanceId( input.getVmId( ).getInstanceId( ) );
          //ASAP:FIXME:GRZE: restore.
          runningInstance.setProductCodes( new ArrayList<String>( ) );
          runningInstance.setImageId( input.getImageId() );
          runningInstance.setKernel( input.getKernelId() );
          runningInstance.setRamdisk( input.getRamdiskId() );
          runningInstance.setPlatform( Strings.emptyToNull( input.getDisplayPlatform() ) );
          runningInstance.setDnsName( input.getDisplayPublicDnsName() );
          runningInstance.setIpAddress( Strings.emptyToNull( input.getDisplayPublicAddress() ) );
          runningInstance.setPrivateDnsName( input.getDisplayPrivateDnsName() );
          runningInstance.setPrivateIpAddress( Strings.emptyToNull( input.getDisplayPrivateAddress() ) );
          if (input.getBootRecord() == null || input.getBootRecord().getArchitecture() == null) {
            LOG.debug("WARNING: No architecture set for instance " + input.getInstanceId() + ", defaulting to x86_64");
            runningInstance.setArchitecture( "x86_64" );
          } else {
            runningInstance.setArchitecture( input.getBootRecord().getArchitecture().toString() );
          }
          runningInstance.setReason( input.runtimeState.getDisplayReason( ) );
          if ( input.getBootRecord( ).getSshKeyPair( ) != null ) {
            runningInstance.setKeyName( input.getBootRecord( ).getSshKeyPair( ).getName( ) );
            if (  ( runningInstance.getKeyName( ) != null ) && ( runningInstance.getKeyName( ).isEmpty( ) ) )
                runningInstance.setKeyName( null );
          } else runningInstance.setKeyName( "" );

          runningInstance.setInstanceType( input.getVmType( ).getName( ) );
          runningInstance.setPlacement( input.getPlacement( ).getPartitionName( ) );

          runningInstance.setLaunchTime( input.getLaunchRecord( ).getLaunchTime( ) );
          runningInstance.setClientToken( input.getClientToken() );

          runningInstance.setVpcId( input.getVpcId() );
          runningInstance.setSubnetId( input.getSubnetId( ) );

          if ( !Strings.isNullOrEmpty( input.getIamInstanceProfileId( ) ) ) {
            runningInstance.setIamInstanceProfile( new IamInstanceProfile(
                input.getIamInstanceProfileArn( ),
                input.getIamInstanceProfileId( )
            ) );
          } else if ( !Strings.isNullOrEmpty( input.getIamInstanceProfileArn( ) ) &&
              input.getIamInstanceProfileArn().startsWith("arn:") ) {

            final String rawName = input.getIamInstanceProfileArn();
            final int nameIndex = input.getIamInstanceProfileArn().lastIndexOf('/');
            final String name = input.getIamInstanceProfileArn().substring(nameIndex + 1, rawName.length());

            try {
              BaseInstanceProfile instanceProfile = Accounts.lookupInstanceProfileByName( input.getOwnerAccountNumber( ), name);
              final String profileArn = Accounts.getInstanceProfileArn(instanceProfile);
              IamInstanceProfile iamInstanceProfile = new IamInstanceProfile();
              iamInstanceProfile.setArn(profileArn);
              iamInstanceProfile.setId(instanceProfile.getInstanceProfileId());
              runningInstance.setIamInstanceProfile(iamInstanceProfile);

            } catch (NoSuchElementException nsee ) {
              LOG.debug("profile arn : " + name, nsee);
            }

          } else if ( !Strings.isNullOrEmpty( input.getIamInstanceProfileArn( ) ) &&
              !input.getIamInstanceProfileArn().startsWith("arn:") ) {

            try {
              final BaseInstanceProfile instanceProfile = Accounts.lookupInstanceProfileByName(input.getOwnerAccountNumber(), input.getIamInstanceProfileArn());
              final String profileArn = Accounts.getInstanceProfileArn(instanceProfile);
              IamInstanceProfile iamInstanceProfile = new IamInstanceProfile();
              iamInstanceProfile.setArn(profileArn);
              iamInstanceProfile.setId(instanceProfile.getInstanceProfileId());
              runningInstance.setIamInstanceProfile(iamInstanceProfile);
            } catch (NoSuchElementException nsee ) {
              LOG.debug("profile name : " + input.getIamInstanceProfileArn(),nsee);
            }
          }

          if (input.getMonitoring()) {
            runningInstance.setMonitoring("enabled");
          } else {
            runningInstance.setMonitoring("disabled");
          }

          runningInstance.getGroupSet( ).addAll( Collections2.transform(
             input.getNetworkGroupIds( ),
              TypeMappers.lookup( NetworkGroupId.class, GroupItemType.class ) ) );
          Collections.sort( runningInstance.getGroupSet( ) );

          runningInstance.setVirtualizationType( input.getVirtualizationType( ) );

          if ( input.isBlockStorage( ) ) {
            runningInstance.setRootDeviceType( ROOT_DEVICE_TYPE_EBS );
          }

          if ( input.getBootRecord( ).hasPersistentVolumes( ) ) {
            for ( final VmVolumeAttachment attachedVol : input.getBootRecord( ).getPersistentVolumes( ) ) {
              runningInstance.getBlockDevices( ).add( new InstanceBlockDeviceMapping( attachedVol.getDevice( ), attachedVol.getVolumeId( ),
                                                                                      attachedVol.getStatus( ),
                                                                                      attachedVol.getAttachTime( ),
                                                                                      attachedVol.getDeleteOnTerminate( ) ) );
              if( attachedVol.getIsRootDevice() ) {
            	runningInstance.setRootDeviceName(attachedVol.getDevice());
              }
            }
          }
          for ( final VmVolumeAttachment attachedVol : input.getTransientVolumeState( ).getAttachments( ) ) {
            runningInstance.getBlockDevices( ).add( new InstanceBlockDeviceMapping( attachedVol.getDevice( ), attachedVol.getVolumeId( ),
                                                                                    attachedVol.getStatus( ),
                                                                                    attachedVol.getAttachTime( ),
                                                                                    attachedVol.getDeleteOnTerminate( ) ) );
          }

          for ( final NetworkInterface networkInterface : input.getNetworkInterfaces( ) ) {
            if ( runningInstance.getNetworkInterfaceSet( ) == null ) {
              runningInstance.setNetworkInterfaceSet( new InstanceNetworkInterfaceSetType( ) );
            }
            if ( networkInterface.getAttachment( ).getDeviceIndex() == 0 ) {
              // set properties for instance
              runningInstance.setSourceDestCheck( networkInterface.getSourceDestCheck( ) );
            }
            runningInstance.getNetworkInterfaceSet( ).getItem( ).add( new InstanceNetworkInterfaceSetItemType(
              networkInterface.getDisplayName( ),
              networkInterface.getSubnet( ).getDisplayName( ),
              networkInterface.getVpc( ).getDisplayName( ),
              networkInterface.getDescription( ),
              networkInterface.getOwnerAccountNumber( ),
              String.valueOf( networkInterface.getState( ) ),
              networkInterface.getMacAddress( ),
              networkInterface.getPrivateIpAddress( ),
              networkInterface.getPrivateDnsName( ),
              networkInterface.getSourceDestCheck( ),
              new GroupSetType( Collections2.transform(
                  networkInterface.getNetworkGroups( ),
                  TypeMappers.lookup( NetworkGroup.class, GroupItemType.class ) ) ),
              new InstanceNetworkInterfaceAttachmentType(
                networkInterface.getAttachment( ).getAttachmentId( ),
                networkInterface.getAttachment( ).getDeviceIndex(),
                String.valueOf( networkInterface.getAttachment( ).getStatus( ) ),
                networkInterface.getAttachment( ).getAttachTime( ),
                networkInterface.getAttachment( ).getDeleteOnTerminate( )
              ),
              networkInterface.isAssociated( ) ? new InstanceNetworkInterfaceAssociationType(
                  networkInterface.getAssociation( ).getPublicIp( ),
                  networkInterface.getAssociation( ).getPublicDnsName( ),
                  networkInterface.getAssociation( ).getDisplayIpOwnerId( )
              ) : null,
              new InstancePrivateIpAddressesSetType( Lists.newArrayList(
                new InstancePrivateIpAddressesSetItemType(
                    networkInterface.getPrivateIpAddress( ),
                    networkInterface.getPrivateDnsName( ),
                    true,
                    networkInterface.isAssociated( ) ? new InstanceNetworkInterfaceAssociationType(
                        networkInterface.getAssociation( ).getPublicIp( ),
                        networkInterface.getAssociation( ).getPublicDnsName( ),
                        networkInterface.getAssociation( ).getDisplayIpOwnerId( )
                    ) : null
                )
              ) )
            ) );
          }

          return runningInstance;
        } catch ( final NoSuchElementException ex ) {
          throw ex;
        } catch ( final Exception ex ) {
          throw new NoSuchElementException( "Failed to lookup vm instance: " + input );
        }
      }
    }
  }

  @TypeMapper
  public enum ReservationTransform implements Function<VmInstance, ReservationInfoType> {
    INSTANCE;

    @Override
    public ReservationInfoType apply( final VmInstance instance ) {
      return new ReservationInfoType(
          instance.getReservationId( ),
          instance.getOwner( ).getAccountNumber( ),
          Collections2.transform( instance.getNetworkGroupIds(), TypeMappers.lookup( NetworkGroupId.class, GroupItemType.class ) ) );
    }
  }

  @TypeMapper
  public enum NetworkGroupIdTransform implements Function<NetworkGroup,NetworkGroupId> {
    INSTANCE;

    @Override
    public NetworkGroupId apply( final NetworkGroup networkGroup ) {
      return new NetworkGroupId(
        networkGroup.getGroupId(),
        networkGroup.getName()
      );
    }
  }

  @TypeMapper
  public enum NetworkGroupIdToGroupItemTypeTransform implements Function<NetworkGroupId,GroupItemType> {
    INSTANCE;

    @Override
    public GroupItemType apply( final NetworkGroupId networkGroupId ) {
      return new GroupItemType(
          networkGroupId.getGroupId( ),
          networkGroupId.getGroupName()
      );
    }
  }

  @TypeMapper
  public enum NetworkGroupToGroupItemTypeTransform implements Function<NetworkGroup,GroupItemType> {
    INSTANCE;

    @Override
    public GroupItemType apply( final NetworkGroup networkGroupId ) {
      return new GroupItemType(
          networkGroupId.getGroupId( ),
          networkGroupId.getName()
      );
    }
  }

  public boolean isBlockStorage( ) {
    return this.bootRecord.isBlockStorage( );
  }

  public boolean isEbsOptimized( ) {
    return false;
  }

  public String getInstanceType( ) {
    return getVmType( ).getName();
  }

  @Override
  public void setNaturalId( final String naturalId ) {
    super.setNaturalId( naturalId );
  }

  public VmVolumeState getTransientVolumeState( ) {
    if ( this.transientVolumeState == null ) {
      this.transientVolumeState = new VmVolumeState( this );
    }
    return this.transientVolumeState;
  }

  @Override
  public String toString( ) {
    final StringBuilder builder2 = new StringBuilder( );
    builder2.append( "VmInstance:" );
    if ( this.networkConfig != null ) builder2.append( "networkConfig=" ).append( this.getNetworkConfig( ) ).append( ":" );
    if ( this.vmId != null ) builder2.append( "vmId=" ).append( this.vmId ).append( ":" );
    if ( this.bootRecord != null ) builder2.append( "bootRecord=" ).append( this.bootRecord ).append( ":" );
    if ( this.usageStats != null ) builder2.append( "usageStats=" ).append( this.usageStats ).append( ":" );
    if ( this.launchRecord != null ) builder2.append( "launchRecord=" ).append( this.launchRecord ).append( ":" );
    if ( this.runtimeState != null ) builder2.append( "runtimeState=" ).append( this.runtimeState ).append( ":" );
    if ( this.transientVolumeState != null ) builder2.append( "transientVolumeState=" ).append( this.transientVolumeState ).append( ":" );
    if ( this.placement != null ) builder2.append( "placement=" ).append( this.placement ).append( ":" );
    if ( this.privateNetwork != null ) builder2.append( "privateNetwork=" ).append( this.privateNetwork ).append( ":" );
    if ( Entities.isReadable( this.networkGroups ) ) builder2.append( "networkGroups=" ).append( this.networkGroups ).append( ":" );
    return builder2.toString( );
  }

  private VmNetworkConfig getNetworkConfig( ) {
    if ( this.networkConfig == null ) {
      this.networkConfig = new VmNetworkConfig( this );
    }
    return this.networkConfig;
  }

  private void setNetworkGroups( final Set<NetworkGroup> networkGroups ) {
    this.networkGroups = networkGroups;
  }

  /**
   *
   */
  void setNetworkConfig( final VmNetworkConfig networkConfig ) {
    this.networkConfig = networkConfig;
  }

  public Date getExpiration( ) {
    return this.expiration;
  }

  public Integer getLaunchIndex( ) {
    return this.getLaunchRecord( ).getLaunchIndex( );
  }

  @Nullable
  public String getClientToken() {
    return this.getVmId().getClientToken();
  }

  /**
   * For instances created prior to 3.4. this is the ARN or the profile name.
   */
  @Nullable
  public String getIamInstanceProfileArn() {
    return getBootRecord( ).getIamInstanceProfileArn( );
  }

  @Nullable
  public String getIamInstanceProfileId() {
    return getBootRecord( ).getIamInstanceProfileId();
  }

  @Nullable
  public String getIamRoleArn() {
    return getBootRecord( ).getIamRoleArn();
  }

  public SshKeyPair getKeyPair( ) {
    return this.getBootRecord( ).getSshKeyPair( );
  }

  public String getVirtualizationType( ) {
    return this.getBootRecord( ).getDisplayVirtualizationType( ).toString( );
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( this.vmId == null )
                                                     ? 0
                                                     : this.vmId.hashCode( ) );
    return result;
  }

  @Override
  public boolean equals( final Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( !super.equals( obj ) ) {
      return false;
    }
    if ( this.getClass( ) != obj.getClass( ) ) {
      return false;
    }
    final VmInstance other = ( VmInstance ) obj;
    if ( this.vmId == null ) {
      if ( other.vmId != null ) {
        return false;
      }
    } else if ( !this.vmId.equals( other.vmId ) ) {
      return false;
    }
    return true;
  }

  /**
   *
   */
  public static VmInstance create( ) {
    return new VmInstance( );
  }

  public static VmInstance exampleWithPublicIp( String ip ) {
    VmInstance vmExample = VmInstance.create( );
    vmExample.setNetworkConfig( VmNetworkConfig.exampleWithPublicIp( ip ) );
    return vmExample;
  }

  public static VmInstance exampleWithPrivateIp( String ip ) {
    VmInstance vmExample = VmInstance.create( );
    vmExample.setNetworkConfig( VmNetworkConfig.exampleWithPrivateIp( ip ) );
    return vmExample;
  }

  public static VmInstance exampleResource( final String instanceId,
                                            final OwnerFullName owner,
                                            final String availabilityZone,
                                            final String instanceProfileArn,
                                            final String instanceType,
                                            final boolean isBlockStorage ) {
    return new VmInstance( owner, instanceId ) {
      @Override public String getIamInstanceProfileArn( ) { return instanceProfileArn; }
      @Override public String getInstanceType( ) { return instanceType; }
      @Override public String getPartition( ) { return availabilityZone; }
      @Override public boolean isBlockStorage( ) { return isBlockStorage; }
    };
  }

  public static Function<VmInstance,String> clientToken( ) {
    return StringPropertyFunctions.CLIENT_TOKEN;
  }

  private void setBootRecord( VmBootRecord bootRecord ) {
    this.bootRecord = bootRecord;
  }

  @TypeMapper
  public enum StatusTransform implements Function<VmInstance, InstanceStatusItemType> {
    INSTANCE;

    @Override
    public InstanceStatusItemType apply( final VmInstance instance ) {
      final InstanceStatusItemType instanceStatusItemType = new InstanceStatusItemType();
      final VmState displayState = instance.getDisplayState();

      instanceStatusItemType.setInstanceId( instance.getInstanceId() );
      instanceStatusItemType.setAvailabilityZone( instance.getPlacement().getPartitionName() );
      instanceStatusItemType.setEventsSet( buildEventSet( instance ) );

      final InstanceStateType state = new InstanceStateType();
      state.setCode( displayState.getCode() );
      state.setName( displayState.getName() );
      instanceStatusItemType.setInstanceState( state );
      instanceStatusItemType.setInstanceStatus( buildStatus( Optional.of( Pair.pair(
          VmRuntimeState.InstanceStatus.Ok,
          Pair.pair( VmRuntimeState.ReachabilityStatus.Passed, Optional.<Date>absent( ) )
      ) ) ) );
      instanceStatusItemType.setSystemStatus( buildStatus( instance ) );

      return instanceStatusItemType;
    }

    public static Optional<InstanceStatusEventType> getEventInfo( final VmInstance instance ) {
      Optional<InstanceStatusEventType> eventInfo = Optional.absent( );
      if ( VmState.RUNNING.apply( instance ) ) {
        final VmRuntimeState vmRuntimeState = instance.getRuntimeState( );
        final Date unreachableTimestamp = vmRuntimeState.getUnreachableTimestamp( );
        if ( Boolean.TRUE.equals( vmRuntimeState.getZombie( ) ) && unreachableTimestamp != null ) {
          final InstanceStatusEventType event = new InstanceStatusEventType( );
          event.setCode( "instance-retirement" );
          event.setDescription( "Instance recovered with degraded functionality" );
          event.setNotAfter( new Date( unreachableTimestamp.getTime( ) + TimeUnit.DAYS.toMillis( 365 ) ) );
          event.setNotBefore( unreachableTimestamp );
          eventInfo = Optional.of( event );
        }
      }
      return eventInfo;
    }

    private InstanceStatusEventsSetType buildEventSet( final VmInstance instance ) {
      InstanceStatusEventsSetType eventSet = null;
      final Optional<InstanceStatusEventType> eventInfo = getEventInfo( instance );
      if ( eventInfo.isPresent( ) ) {
        eventSet = new InstanceStatusEventsSetType( );
        eventSet.getItem( ).add( eventInfo.get( ) );
      }
      return eventSet;
    }

    private InstanceStatusType buildStatus( final VmInstance instance ) {
      if ( VmState.RUNNING.apply( instance ) ) {
        final VmRuntimeState vmRuntimeState = instance.getRuntimeState( );
        final VmRuntimeState.InstanceStatus instanceStatus = Objects.firstNonNull(
            vmRuntimeState.getInstanceStatus( ),
            VmRuntimeState.InstanceStatus.Ok );
        final VmRuntimeState.ReachabilityStatus reachabilityStatus = Objects.firstNonNull(
            vmRuntimeState.getReachabilityStatus(),
            VmRuntimeState.ReachabilityStatus.Passed );
        final Date unreachabilityTimestamp = reachabilityStatus != VmRuntimeState.ReachabilityStatus.Passed ?
            vmRuntimeState.getUnreachableTimestamp( ) :
            null;
        return buildStatus( Optional.of( Pair.pair( instanceStatus, Pair.ropair( reachabilityStatus, unreachabilityTimestamp ) ) ) );
      } else {
        return buildStatus( Optional.<Pair<VmRuntimeState.InstanceStatus,Pair<VmRuntimeState.ReachabilityStatus,Optional<Date>>>>absent( ) );
      }
    }

    private InstanceStatusType buildStatus( final Optional<Pair<VmRuntimeState.InstanceStatus,Pair<VmRuntimeState.ReachabilityStatus,Optional<Date>>>> status ) {
      final InstanceStatusType instanceStatusType = new InstanceStatusType( );
      if ( status.isPresent( ) ) {
        final VmRuntimeState.InstanceStatus instanceStatus = status.get( ).getLeft( );
        final VmRuntimeState.ReachabilityStatus reachabilityStatus = status.get( ).getRight( ).getLeft( );
        final Optional<Date> unreachableTimestamp = status.get( ).getRight( ).getRight( );
        final InstanceStatusDetailsSetItemType statusDetailsItem = new InstanceStatusDetailsSetItemType( );
        statusDetailsItem.setName( "reachability" );
        statusDetailsItem.setStatus( reachabilityStatus.toString( ) );
        statusDetailsItem.setImpairedSince( unreachableTimestamp.orNull( ) );

        final InstanceStatusDetailsSetType statusDetails = new InstanceStatusDetailsSetType();
        statusDetails.getItem().add( statusDetailsItem );

        instanceStatusType.setStatus( instanceStatus.toString( ) );
        instanceStatusType.setDetails( statusDetails );
      } else {
        instanceStatusType.setStatus( "not-applicable" );
      }
      return instanceStatusType;
    }
  }

  public Boolean getMonitoring() {
    return this.getBootRecord().isMonitoring( );
  }

  @PrePersist
  @PreUpdate
  private void updateGroups( ) {
    networkGroupIds.clear( );
    CollectionUtils.fluent( networkGroups )
        .transform( TypeMappers.lookup( NetworkGroup.class, NetworkGroupId.class ) )
        .copyInto( networkGroupIds );
  }

  @EntityUpgrade( entities = { VmInstance.class }, since = Version.v3_3_0, value = Eucalyptus.class )
  public enum VmInstanceUpgrade_3_3_0 implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( VmInstance.VmInstanceUpgrade_3_3_0.class );
    @Override
    public boolean apply( Class arg0 ) {
      EntityTransaction db = Entities.get( VmInstance.class );
      try {
        List<VmInstance> instances = Entities.query( new VmInstance( ) );
        for ( VmInstance vm : instances ) {
          if( vm.getBootRecord().getMachine() instanceof BlockStorageImageInfo ) {
        	LOG.info( "Upgrading bfebs VmInstance: " + vm.toString() );
        	if( vm.getBootRecord().getEphemeralStorage().isEmpty() ) {
        	  LOG.info("Adding ephemeral disk at " + Images.DEFAULT_EPHEMERAL_DEVICE);
        	  vm.getBootRecord( ).getEphemeralStorage( ).add( new VmEphemeralAttachment( vm, "ephemeral0", Images.DEFAULT_EPHEMERAL_DEVICE ) );
        	}

        	// Pre 3.3 code allowed only one persistent volume i.e. the root volume. Check before upgrading
        	if ( vm.getBootRecord().getPersistentVolumes().size() == 1 ) {
        	  VmVolumeAttachment attachment	= vm.getBootRecord().getPersistentVolumes().iterator().next();
        	  LOG.info("Found the only VmVolumeAttachment: " + attachment.toString());
        	  LOG.info("Setting root device flag to true");
              attachment.setIsRootDevice(Boolean.TRUE);
              LOG.info("Changing the device name to " + Images.DEFAULT_ROOT_DEVICE);
              attachment.setDevice( Images.DEFAULT_ROOT_DEVICE);
        	} else { // This should not be the case updating to 3.3
        	 // If the instance has more or less than one persistent volume, iterate through them and update the one with device "/dev/sda1"
        	  for ( VmVolumeAttachment attachment : vm.getBootRecord().getPersistentVolumes() ) {
        		LOG.info("Found VmVolumeAttachment: " + attachment.toString());
        		if ( attachment.getDevice().equalsIgnoreCase(Images.DEFAULT_PARTITIONED_ROOT_DEVICE) ) {
        		  LOG.info("Setting root device flag to true");
                  attachment.setIsRootDevice(Boolean.TRUE);
                  LOG.info("Changing the device name from " + Images.DEFAULT_PARTITIONED_ROOT_DEVICE + " to " + Images.DEFAULT_ROOT_DEVICE);
                  attachment.setDevice(Images.DEFAULT_ROOT_DEVICE);
                }
              }
        	}
          }
          Entities.persist( vm );
        }
        db.commit( );
        return true;
      } catch ( Exception ex ) {
    	LOG.error("Error upgrading VmInstance: ", ex);
    	db.rollback();
        throw Exceptions.toUndeclared( ex );
      }
    }
  }

  @PreUpgrade( value = Compute.class, since = Version.v5_0_0 )
  public static class VmInstance500PreUpgrade implements Callable<Boolean> {
    private static final Logger logger = Logger.getLogger( VmInstance500PreUpgrade.class );

    @Override
    public Boolean call( ) throws Exception {
      Sql sql = null;
      try {
        logger.info( "Updating instance unique names" );
        sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloud" );
        sql.execute(
            "update metadata_instances set metadata_unique_name=substr(metadata_unique_name,14,10) " +
            "where length(metadata_unique_name) = 23" );
        return true;
      } catch ( Exception ex ) {
        logger.error( "Error updating instance unique names (check for duplicates)", ex );
        return true;
      } finally {
        if ( sql != null ) {
          sql.close( );
        }
      }
    }
  }
}
