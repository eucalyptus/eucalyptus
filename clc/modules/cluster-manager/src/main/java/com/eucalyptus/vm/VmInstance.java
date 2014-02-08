/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.vm;

import static com.eucalyptus.util.Strings.isPrefixOf;
import static com.eucalyptus.util.Strings.upper;
import java.lang.Object;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PreRemove;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.blockstorage.State;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.blockstorage.Volumes;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeType;
import com.eucalyptus.cloud.CloudMetadata.VmInstanceMetadata;
import com.eucalyptus.cloud.CloudMetadatas;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.cloud.ImageMetadata.Platform;
import com.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.cloud.util.ResourceAllocationException;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Dns;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Tokens;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionExecutionException;
import com.eucalyptus.entities.TransientEntityException;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.images.BootableImageInfo;
import com.eucalyptus.images.Emis;
import com.eucalyptus.images.Emis.BootableSet;
import com.eucalyptus.images.MachineImageInfo;
import com.eucalyptus.keys.KeyPairs;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.network.ExtantNetwork;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.network.PrivateNetworkIndex;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.InstanceCreationEvent;
import com.eucalyptus.tokens.AssumeRoleResponseType;
import com.eucalyptus.tokens.AssumeRoleType;
import com.eucalyptus.tokens.CredentialsType;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.Version;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vm.VmBundleTask.BundleState;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstances.Timeout;
import com.eucalyptus.vm.VmVolumeAttachment.AttachmentState;
import com.eucalyptus.vm.VmVolumeAttachment.NonTransientVolumeException;
import com.eucalyptus.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import edu.ucsb.eucalyptus.cloud.VirtualBootRecord;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;
import edu.ucsb.eucalyptus.msgs.IamInstanceProfile;
import edu.ucsb.eucalyptus.msgs.InstanceBlockDeviceMapping;
import edu.ucsb.eucalyptus.msgs.InstanceStateType;
import edu.ucsb.eucalyptus.msgs.InstanceStatusDetailsSetItemType;
import edu.ucsb.eucalyptus.msgs.InstanceStatusDetailsSetType;
import edu.ucsb.eucalyptus.msgs.InstanceStatusItemType;
import edu.ucsb.eucalyptus.msgs.InstanceStatusType;
import edu.ucsb.eucalyptus.msgs.ReservationInfoType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
import net.sf.json.JSONObject;


@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_instances" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class VmInstance extends UserMetadata<VmState> implements VmInstanceMetadata {
  private static final long    serialVersionUID = 1L;
  private static final Logger  LOG              = Logger.getLogger( VmInstance.class );
  private static final com.google.common.cache.Cache<MetadataKey,ImmutableMap<String,String>> metadataCache =
      CacheBuilder.newBuilder( ).expireAfterWrite( 5, TimeUnit.MINUTES ).maximumSize( 1000 ).build( );

  public static final String         DEFAULT_TYPE         = "m1.small";
  public static final String         ROOT_DEVICE_TYPE_EBS = "ebs";
  public static final String         ROOT_DEVICE_TYPE_INSTANCE_STORE = "instance-store";

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
  @NotFound( action = NotFoundAction.IGNORE )
  @ManyToMany( cascade = { CascadeType.ALL },
               fetch = FetchType.LAZY )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<NetworkGroup>    networkGroups    = Sets.newHashSet( );
  
  @NotFound( action = NotFoundAction.IGNORE )
  @OneToOne( fetch = FetchType.LAZY,
             cascade = { CascadeType.ALL },
             orphanRemoval = true,
             optional = true )
  @JoinColumn( name = "metadata_vm_network_index",
               nullable = true,
               insertable = true,
               updatable = true )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private PrivateNetworkIndex  networkIndex;
 
  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "instance" )
  private Collection<VmInstanceTag> tags;
  
  @PreRemove
  void cleanUp( ) {
    if ( this.networkGroups != null ) {
      this.networkGroups.clear( );
      this.networkGroups = null;
    }
    try {
      if ( this.networkIndex != null ) {
        this.networkIndex.release( );
        this.networkIndex.teardown( );
        this.networkIndex = null;
      }
    } catch ( final ResourceAllocationException ex ) {
      LOG.error( ex, ex );
    }
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
    TORNDOWN( VmState.STOPPED, VmState.TERMINATED ),
    STOP( VmState.STOPPING, VmState.STOPPED ),
    TERM( VmState.SHUTTING_DOWN, VmState.TERMINATED ),
    NOT_RUNNING( VmState.STOPPING, VmState.STOPPED, VmState.SHUTTING_DOWN, VmState.TERMINATED ),
    DONE( VmState.TERMINATED, VmState.BURIED );

    private Set<VmState> states;
    
    VmStateSet( final VmState... states ) {
      this.states = Sets.newHashSet( states );
    }
    
    @Override
    public boolean apply( final VmInstance arg0 ) {
      return this.states.contains( arg0.getState( ) );
    }
    
    public boolean contains( final Object o ) {
      return this.states.contains( o );
    }
    
    public Predicate<VmInstance> not( ) {
      return Predicates.not( this );
    }
    
  }
  
  public enum VmState implements Predicate<VmInstance> {
    PENDING( 0 ),
    RUNNING( 16 ),
    SHUTTING_DOWN( 32 ),
    TERMINATED( 48 ),
    STOPPING( 64 ),
    STOPPED( 80 ),
    BURIED( 128 );
    private String name;
    private int    code;
    
    VmState( final int code ) {
      this.name = this.name( ).toLowerCase( ).replace( "_", "-" );
      this.code = code;
    }
    
    public String getName( ) {
      return this.name;
    }
    
    public int getCode( ) {
      return this.code;
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
  
  private enum ValidateVmInfo implements Predicate<VmInfo> {
    INSTANCE;

    @Override
    public boolean apply( VmInfo arg0 ) {
      if ( arg0.getGroupNames( ).isEmpty( ) ) {
        LOG.warn( "Instance " + arg0.getInstanceId( ) + " reported no groups: " + arg0.getGroupNames( ) );
      }
      if ( arg0.getInstanceType( ).getName( ) == null ) {
        LOG.warn( "Instance " + arg0.getInstanceId( ) + " reported no instance type: " + arg0.getInstanceType( ) );
      }
      if ( arg0.getInstanceType( ).getVirtualBootRecord( ).isEmpty( ) ) {
        LOG.warn( "Instance " + arg0.getInstanceId( ) + " reported no vbr entries: " + arg0.getInstanceType( ).getVirtualBootRecord( ) );
        return false;
      }
      try {
        VirtualBootRecord vbr = arg0.getInstanceType( ).lookupRoot( );
      } catch ( NoSuchElementException ex ) {
        LOG.warn( "Instance " + arg0.getInstanceId( ) + " reported no root vbr entry: " + arg0.getInstanceType( ).getVirtualBootRecord( ) );
        return false;
      }
      try {
        Topology.lookup( ClusterController.class, Clusters.getInstance( ).lookup( arg0.getPlacement( ) ).lookupPartition( ) );
      } catch ( NoSuchElementException ex ) {
        return false;//GRZE:ARG: skip restoring while cluster is enabling since Builder.placement() depends on a running cluster...
      }
      return true;
    }
    
  }
  
  public enum RestoreAllocation implements Predicate<VmInfo> {
    INSTANCE;
    
    private static Function<String, NetworkGroup> transformNetworkNames( final UserFullName userFullName ) {
      return new Function<String, NetworkGroup>( ) {
        
        @Override
        public NetworkGroup apply( final String arg0 ) {
          
          final EntityTransaction db = Entities.get( NetworkGroup.class );
          try {
            SimpleExpression naturalId = Restrictions.like( "naturalId", arg0.replace( userFullName.getAccountNumber( ) + "-", "" ) + "%" );
            NetworkGroup result = ( NetworkGroup ) Entities.createCriteria( NetworkGroup.class )
                                                                 .add( naturalId )
                                                                 .uniqueResult( );
            if ( result == null ) {
              SimpleExpression displayName = Restrictions.like( "displayName", arg0.replace( userFullName.getAccountNumber( ) + "-", "" ) + "%" );
              result = ( NetworkGroup ) Entities.createCriteria( NetworkGroup.class )
                                                .add( displayName )
                                                .uniqueResult( );
            }
            db.commit( );
            return result;
          } catch ( Exception ex ) {
            Logs.extreme( ).error( ex, ex );
            throw Exceptions.toUndeclared( ex );
          } finally {
            if ( db.isActive() ) db.rollback();
          }
        }
      };
    }
    
    @Override
    public boolean apply( final VmInfo input ) {
      final VmState inputState = VmState.Mapper.get( input.getStateName( ) );
      if ( !VmStateSet.RUN.contains( inputState ) ) {
        return false;
      } else if ( !ValidateVmInfo.INSTANCE.apply( input ) ) {
        return false;
      } else {
        final UserFullName userFullName = UserFullName.getInstance( input.getOwnerId( ) );
        final EntityTransaction db = Entities.get( VmInstance.class );
        boolean building = false;
        try {
          final List<NetworkGroup> networks = RestoreAllocation.restoreNetworks( input, userFullName );
          final PrivateNetworkIndex index = RestoreAllocation.restoreNetworkIndex( input, networks );
          final VmType vmType = RestoreAllocation.restoreVmType( input );
          final Partition partition = RestoreAllocation.restorePartition( input );
          final String imageId = RestoreAllocation.restoreImage( input );
          final String kernelId = RestoreAllocation.restoreKernel( input );
          final String ramdiskId = RestoreAllocation.restoreRamdisk( input );
          final BootableSet bootSet = RestoreAllocation.restoreBootSet( input, imageId, kernelId, ramdiskId );
          final int launchIndex = RestoreAllocation.restoreLaunchIndex( input );
          final SshKeyPair keyPair = RestoreAllocation.restoreSshKeyPair( input, userFullName );
          final byte[] userData = RestoreAllocation.restoreUserData( input );
          building = true;
          final VmInstance vmInst = new VmInstance.Builder( ).owner( userFullName )
                                              .withIds( input.getInstanceId( ),
                                                        input.getReservationId( ),
                                                        null, null )
                                              .bootRecord( bootSet,
                                                           userData,
                                                           keyPair,
                                                           vmType,
                                                           Boolean.FALSE,
                                                           null, null, null )
                                              .placement( partition, partition.getName( ) )
                                              .networking( networks, index )
                                              .build( launchIndex );
          vmInst.setNaturalId( input.getUuid( ) );
          RestoreAllocation.restoreAddress( input, vmInst );
          Entities.persist( vmInst );
          db.commit( );
          return true;
        } catch ( final Exception ex ) {
          LOG.error( "Failed to restore instance " + input.getInstanceId( ) + " because of: " + ex.getMessage( ), building ? null : ex );
          Logs.extreme( ).error( ex, ex );
          return false;
        } finally {
          if ( db.isActive() ) db.rollback();
        }
      }
//TODO:GRZE: this is the case in restore where we either need to report the failed instance restore, terminate the instance, or handle partial reporting of the instance info.
//      } catch ( NoSuchElementException e ) {
//        ClusterConfiguration config = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getConfiguration( );
//        AsyncRequests.newRequest( new TerminateCallback( runVm.getInstanceId( ) ) ).dispatch( runVm.getPlacement( ) );
    }
    
    private static PrivateNetworkIndex restoreNetworkIndex( final VmInfo input, final List<NetworkGroup> networks ) {
      PrivateNetworkIndex index = null;
      String displayName = null;
      if ( networks.isEmpty( ) ) {
        LOG.warn( "Failed to recover network index for " + input.getInstanceId( )
                  + " because no network group information is available: "
                  + input.getGroupNames( ) );
      } else {
        final EntityTransaction db = Entities.get( NetworkGroup.class );
        try {
          final NetworkGroup network = networks.get( 0 );
          final NetworkGroup entity = Entities.merge( network );
          displayName = entity.getDisplayName();
          ExtantNetwork exNet = null;
          if ( entity.hasExtantNetwork( ) && entity.extantNetwork( ).getTag( ).equals( input.getNetParams( ).getVlan( ) ) ) {
            LOG.info( "Found matching extant network for " + input.getInstanceId( ) + ": " + entity.extantNetwork( ) );
            index = entity.extantNetwork( ).reclaimNetworkIndex( input.getNetParams( ).getNetworkIndex( ) );
          } else if ( entity.hasExtantNetwork( ) && !entity.extantNetwork( ).getTag( ).equals( input.getNetParams( ).getVlan( ) ) ) {
            LOG.warn( "Found conflicting extant network for " + input.getInstanceId( ) + ": " + entity.extantNetwork( ) );
          } else {
            LOG.debug( "Restoring extant network for " + input.getInstanceId( ) + ": " + input.getNetParams( ).getVlan( ) );
            exNet = entity.reclaim( input.getNetParams( ).getVlan( ) );
            LOG.info( "Restore extant network for " + input.getInstanceId( ) + ": " + entity.extantNetwork( ) );
            LOG.debug( "Restoring private network index for " + input.getInstanceId( ) + ": " + input.getNetParams( ).getNetworkIndex( ) );
            index = exNet.reclaimNetworkIndex( input.getNetParams( ).getNetworkIndex( ) );
          }
          db.commit( );
        } catch ( final Exception ex ) {
          LOG.debug(" Failed to restore network index for instanceid " + input.getInstanceId( ) + ": vlan " + input.getNetParams( ).getVlan( )
                  + ": Entity " + displayName + " because of: " + ex.getMessage());
          Logs.extreme( ).error( ex, ex );
        } finally {
          if ( db.isActive() ) db.rollback();
        }
      }
      return index;
    }
    
    private static List<NetworkGroup> restoreNetworks( final VmInfo input, final UserFullName userFullName ) {
	final List<NetworkGroup> networks = Lists.newArrayList( );
	networks.addAll( Lists.transform( input.getGroupNames( ), transformNetworkNames( userFullName ) ) );
	Iterables.removeIf(networks, Predicates.isNull());

	if ( networks.isEmpty() ) {
	    final EntityTransaction restore = Entities.get( NetworkGroup.class );
	    int index = input.getGroupNames().get(0).lastIndexOf("-");
	    String truncatedSecGroup = (String) input.getGroupNames().get(0).subSequence(0, index);
	    String orphanedSecGrp =  truncatedSecGroup.concat("-orphaned");
	    try {
		NetworkGroup found = NetworkGroups.lookup(userFullName,
			orphanedSecGrp);
		networks.add(found);
		restore.commit();
	    } catch (NoSuchMetadataException ex) {

		try {
		    NetworkGroup restoredGroup = NetworkGroups.create(userFullName,
			    orphanedSecGrp,
			    orphanedSecGrp);
		    networks.add(restoredGroup);    
		} catch (Exception e) {
		    LOG.debug("Failed to restored security group : " + orphanedSecGrp);
		    restore.rollback();
		} 

	    } catch (Exception e) {
		    LOG.debug("Failed to restore security group : " + orphanedSecGrp + " for InstanceID : " + input.getInstanceId()
                   +  " User Name  : " + userFullName + " because of: " + e.getMessage() );
		    restore.rollback();
	    } finally {
	      if ( restore.isActive( ) ) {
	        restore.rollback( );
	      }
	    }
	}
	return networks;
    }
    
    private static void restoreAddress( final VmInfo input, final VmInstance vmInst ) {
      try {
        final UserFullName userFullName = UserFullName.getInstance( input.getOwnerId( ) );
        final Address addr = Addresses.getInstance( ).lookup( input.getNetParams( ).getIgnoredPublicIp( ) );
        if ( addr.isAssigned( ) &&
             addr.getInstanceAddress( ).equals( input.getNetParams( ).getIpAddress( ) ) &&
             addr.getInstanceId( ).equals( input.getInstanceId( ) ) ) {
          vmInst.updateAddresses( input.getNetParams( ).getIpAddress( ), input.getNetParams( ).getIgnoredPublicIp( ) );
        } else if ( !addr.isAssigned( ) && addr.isAllocated( ) && ( addr.isSystemOwned( ) || addr.getOwner( ).equals( userFullName ) ) ) {
          vmInst.updateAddresses( input.getNetParams( ).getIpAddress( ), input.getNetParams( ).getIgnoredPublicIp( ) );
          if ( addr.isPending() ) try {
            addr.clearPending();
          } catch ( Exception e ) {
          }
          addr.assign( vmInst ).clearPending();
        } else { // the public address used by the instance is not available
          vmInst.updateAddresses( input.getNetParams( ).getIpAddress( ), input.getNetParams( ).getIpAddress() );
        }
      } catch ( NoSuchElementException e ) { // Address disabled
        try {
            final Address addr = Addresses.getInstance( ).lookupDisabled( input.getNetParams( ).getIgnoredPublicIp( ) );
            vmInst.updateAddresses( input.getNetParams( ).getIpAddress( ), input.getNetParams( ).getIgnoredPublicIp( ) );
            addr.pendingAssignment().assign( vmInst ).clearPending();

        } catch ( final Exception ex2 ) {
            LOG.error( "Failed to restore address state (from disabled) " + input.getNetParams( )
              + " for instance "
              + input.getInstanceId( )
              + " because of: "
              + ex2.getMessage( ) );
          Logs.extreme( ).error( ex2, ex2 );
        }
      } catch ( final Exception ex2 ) {
        LOG.error( "Failed to restore address state " + input.getNetParams( )
                   + " for instance "
                   + input.getInstanceId( )
                   + " because of: "
                   + ex2.getMessage( ) );
        Logs.extreme( ).error( ex2, ex2 );
      }
    }

    private static byte[] restoreUserData( final VmInfo input ) {
      byte[] userData;
      try {
        userData = Base64.decode( input.getUserData( ) );
      } catch ( final Exception ex ) {
        LOG.debug("Failed to restore user data for : " + input.getInstanceId( ) + " because of: " + ex.getMessage( ) );
        userData = new byte[0];
      }
      return userData;
    }
    
    private static SshKeyPair restoreSshKeyPair( final VmInfo input, final UserFullName userFullName ) {
      String keyValue = input.getKeyValue( );
      if ( keyValue == null || keyValue.indexOf( "@" ) == -1 ) {
        return KeyPairs.noKey( );
      } else {
        String keyName = keyValue.replaceAll( ".*@eucalyptus\\.", "" );
        return SshKeyPair.withPublicKey( null, keyName, keyValue );
      }
    }
    
    private static int restoreLaunchIndex( final VmInfo input ) {
      int launchIndex = 1;
      try {
        launchIndex = Integer.parseInt( input.getLaunchIndex( ) );
      } catch ( final Exception ex1 ) {
        LOG.debug("Failed to get LaunchIndex setting it to '1' for: " + input.getInstanceId( ) + " because of: " + ex1.getMessage( ) );
        launchIndex = 1;
      }
      return launchIndex;
    }

    @Nonnull
    private static BootableSet restoreBootSet( @Nonnull  final VmInfo input,
                                               @Nullable final String imageId,
                                               @Nullable final String kernelId,
                                               @Nullable final String ramdiskId ) throws MetadataException {
      if ( imageId == null ) {
        throw new MetadataException( "Missing image id for boot set restoration" );
      }

      BootableSet bootSet;
      try {
        bootSet = Emis.recreateBootableSet( imageId, kernelId, ramdiskId );
      } catch ( final NoSuchMetadataException e ) {
        LOG.error( "Using transient bootset in place of imageId " + imageId
            + ", kernelId " + kernelId
            + ", ramdiskId " + ramdiskId
            + " for " + input.getInstanceId( )
            + " because of: " + e.getMessage( ) );
        Platform platform;
        try {
          platform = Platform.valueOf( Strings.nullToEmpty(input.getPlatform()) );
        } catch ( final IllegalArgumentException e2 ) {
          platform = Platform.linux;
        }
        bootSet = Emis.unavailableBootableSet( platform );
      }  catch ( final Exception ex ) {
        LOG.error( "Failed to recreate bootset with imageId " + imageId
                   + ", kernelId " + kernelId
                   + ", ramdiskId " + ramdiskId
                   + " for " + input.getInstanceId( )
                   + " because of: " + ex.getMessage( ) );
        Logs.extreme( ).error( ex, ex );
        if ( ex instanceof MetadataException ) {
          throw (MetadataException) ex;
        } else {
          throw Exceptions.toUndeclared( ex );
        }
      }

      return bootSet;
    }
    
    private static String restoreRamdisk( final VmInfo input ) {
      String ramdiskId = null;
      try {
        ramdiskId = input.getInstanceType( ).lookupRamdisk( ).getId( );
      } catch ( final NoSuchElementException ex ) {
        LOG.debug( "No ramdiskId " + input.getRamdiskId( )
                   + " for: "
                   + input.getInstanceId( )
                   + " because vbr does not contain a ramdisk: "
                   + input.getInstanceType( ).getVirtualBootRecord( ) );
        Logs.extreme( ).error( ex, ex );
      } catch ( final Exception ex ) {
        LOG.error( "Failed to lookup ramdiskId " + input.getRamdiskId( ) + " for: " + input.getInstanceId( ) + " because of: " + ex.getMessage( ) );
        Logs.extreme( ).error( ex, ex );
      }
      return ramdiskId;
    }
    
    private static String restoreKernel( final VmInfo input ) {
      String kernelId = null;
      try {
        kernelId = input.getInstanceType( ).lookupKernel( ).getId( );
      } catch ( final NoSuchElementException ex ) {
        LOG.debug( "No kernelId " + input.getKernelId( )
                   + " for: "
                   + input.getInstanceId( )
                   + " because vbr does not contain a kernel: "
                   + input.getInstanceType( ).getVirtualBootRecord( ) );
        Logs.extreme( ).error( ex, ex );
      } catch ( final Exception ex ) {
        LOG.error( "Failed to lookup kernelId " + input.getKernelId( ) + " for: " + input.getInstanceId( ) + " because of: " + ex.getMessage( ) );
        Logs.extreme( ).error( ex, ex );
      }
      return kernelId;
    }
    
    private static String restoreImage( final VmInfo input ) {
      String imageId = null;
      try {
        imageId = input.getInstanceType( ).lookupRoot( ).getId( );
      } catch ( final Exception ex2 ) {
        LOG.error( "Failed to lookup imageId " + input.getImageId( ) + " for: " + input.getInstanceId( ) + " because of: " + ex2.getMessage( ) );
        Logs.extreme( ).error( ex2, ex2 );
      }
      return imageId;
    }
    
    private static Partition restorePartition( final VmInfo input ) {
      Partition partition = null;
      try {
        partition = Partitions.lookupByName( input.getPlacement( ) );
      } catch ( final Exception ex2 ) {
        try {
          partition = Partitions.lookupByName( Clusters.getInstance( ).lookup( input.getPlacement( ) ).getPartition( ) );
        } catch ( final Exception ex ) {
          LOG.error( "Failed to lookup partition " + input.getPlacement( ) + " for: " + input.getInstanceId( ) + " because of: " + ex.getMessage( ) );
          Logs.extreme( ).error( ex, ex );
        }
      }
      return partition;
    }
    
    private static VmType restoreVmType( final VmInfo input ) {
      VmType vmType = null;
      try {
        vmType = VmTypes.lookup( input.getInstanceType( ).getName( ) );
      } catch ( final Exception ex ) {
        LOG.error( "Failed to lookup vm type " + input.getInstanceType( ).getName( ) + " for: " + input.getInstanceId( ) + " because of: " + ex.getMessage( ) );
        Logs.extreme( ).error( ex, ex );
      }
      return vmType;
    }
    
  }
  
  enum Transitions implements Function<VmInstance, VmInstance> {
    REGISTER {
      @Override
      public VmInstance apply( final VmInstance arg0 ) {
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          final VmInstance entityObj = Entities.merge( arg0 );
          db.commit( );
          return entityObj;
        } catch ( final RuntimeException ex ) {
          Logs.extreme( ).error( ex, ex );
          throw ex;
        } finally {
          if ( db.isActive() ) db.rollback();
        }
      }
    },
    START {
      @Override
      public VmInstance apply( final VmInstance v ) {
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          final VmInstance vm = Entities.merge( v );
          vm.setState( VmState.PENDING, Reason.USER_STARTED );
          db.commit( );
          return vm;
        } catch ( final RuntimeException ex ) {
          Logs.extreme( ).error( ex, ex );
          throw ex;
        } finally {
          if ( db.isActive() ) db.rollback();
        }
      }
    },
    TERMINATED {
      @Override
      public VmInstance apply( final VmInstance v ) {
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, v.getInstanceId( ) ) );
          Reason reason = Timeout.UNREPORTED.apply( vm ) ? Reason.EXPIRED : Reason.USER_TERMINATED;
          if ( VmStateSet.RUN.apply( vm ) ) {
            vm.setState( VmState.SHUTTING_DOWN, reason );
          } else if ( VmState.SHUTTING_DOWN.equals( vm.getState( ) ) ) {
            vm.setState( VmState.TERMINATED, reason );
          } else if ( VmState.STOPPED.equals( vm.getState( ) ) ) {
            vm.setState( VmState.TERMINATED, reason );
          }
          db.commit( );
          return vm;
        } catch ( final Exception ex ) {
          Logs.extreme( ).trace( ex, ex );
          throw new NoSuchElementException( "Failed to lookup instance: " + v );
        } finally {
          if ( db.isActive() ) db.rollback();
        }
      }
    },
    STOPPED {
      @Override
      public VmInstance apply( final VmInstance v ) {
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, v.getInstanceId( ) ) );
          if ( VmStateSet.RUN.apply( vm ) ) {
            vm.setState( VmState.STOPPING, Reason.USER_STOPPED );
          } else if ( VmState.STOPPING.equals( vm.getState( ) ) ) {
            vm.setState( VmState.STOPPED, Reason.USER_STOPPED );
            PrivateNetworkIndex vmIdx = vm.getNetworkIndex( );
            if ( vmIdx != null ) {
              vmIdx.release( );
              vmIdx.teardown( );
              vmIdx = null;
            }
            vm.setNetworkIndex( null );
          }
          db.commit( );
          return vm;
        } catch ( final Exception ex ) {
          Logs.extreme( ).debug( ex, ex );
          throw new NoSuchElementException( "Failed to lookup instance: " + v );
        } finally {
          if ( db.isActive() ) db.rollback();
        }
      }
    },
    DELETE {
      @Override
      public VmInstance apply( final VmInstance v ) {
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, v.getInstanceId( ) ) );
          vm.cleanUp( );
          Entities.delete( vm );
          db.commit( );
        } catch ( final Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
        } finally {
          if ( db.isActive() ) db.rollback();
        }
        try {
          v.cleanUp( );
          v.setState( VmState.TERMINATED );
        } catch ( Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
        }
        return v;
      }
    },
    SHUTDOWN {
      @Override
      public VmInstance apply( final VmInstance v ) {
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, v.getInstanceId( ) ) );
          if ( VmStateSet.RUN.apply( vm ) ) {
            Reason reason = Timeout.SHUTTING_DOWN.apply( vm ) ? Reason.EXPIRED : Reason.USER_TERMINATED;
            vm.setState( VmState.SHUTTING_DOWN, reason );
          }
          db.commit( );
          return vm;
        } catch ( final Exception ex ) {
          Logs.extreme( ).debug( ex, ex );
          throw new NoSuchElementException( "Failed to lookup instance: " + v );
        } finally {
          if ( db.isActive() ) db.rollback();
        }
      }
    };
    @Override
    public abstract VmInstance apply( final VmInstance v );
  }
  
  enum Lookup implements Function<String, VmInstance> {
    INSTANCE {
      
      @Override
      public VmInstance apply( final String arg0 ) {
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, arg0 ) );
          if ( ( vm == null ) ) {
            throw new NoSuchElementException( "Failed to lookup vm instance: " + arg0 );
          } else if ( VmStateSet.DONE.apply( vm ) ) {
            Entities.delete( vm );
            db.commit( );
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
    @Override
    public abstract VmInstance apply( final String arg0 );
  }
  
  public enum Create implements Function<ResourceToken, VmInstance> {
    INSTANCE;
    
    /**
     * @see Predicate#apply(Object)
     */
    @Override
    public VmInstance apply( final ResourceToken token ) {
      final EntityTransaction db = Entities.get( VmInstance.class );
      try {
        final Allocation allocInfo = token.getAllocationInfo( );
        VmInstance vmInst = new VmInstance.Builder( ).owner( allocInfo.getOwnerFullName( ) )
                                                     .withIds( token.getInstanceId(),
                                                               allocInfo.getReservationId(),
                                                               allocInfo.getClientToken(),
                                                               allocInfo.getUniqueClientToken( token.getLaunchIndex( ) ) )
                                                     .bootRecord( allocInfo.getBootSet( ),
                                                                  allocInfo.getUserData( ),
                                                                  allocInfo.getSshKeyPair( ),
                                                                  allocInfo.getVmType( ), 
                                                                  allocInfo.isMonitoring(),
                                                                  allocInfo.getIamInstanceProfileArn(),
                                                                  allocInfo.getIamInstanceProfileId(),
                                                                  allocInfo.getIamRoleArn() )
                                                     .placement( allocInfo.getPartition( ), allocInfo.getRequest( ).getAvailabilityZone( ) )
                                                     .networking( allocInfo.getNetworkGroups( ), token.getNetworkIndex( ) )
                                                     .addressing( allocInfo.isUsePrivateAddressing() )
                                                     .expiresOn( allocInfo.getExpiration() )
                                                     .build( token.getLaunchIndex( ) );
        vmInst.setNaturalId(token.getInstanceUuid());
        vmInst = Entities.persist( vmInst );
        Entities.flush( vmInst );
        db.commit( );
        token.setVmInstance( vmInst );
        return vmInst;
      } catch ( final ResourceAllocationException ex ) {
        Logs.extreme( ).error( ex, ex );
        throw Exceptions.toUndeclared( ex );
      } catch ( final Exception ex ) {
        Logs.extreme( ).error( ex, ex );
        throw Exceptions.toUndeclared( new TransactionExecutionException( ex ) );
      } finally {
        if ( db.isActive() ) db.rollback();
      }
    }
    
  }
  
  private static enum VolumeAttachmentComparator implements Comparator<VmVolumeAttachment> {
    INSTANCE;
		
	@Override
	public int compare(VmVolumeAttachment arg0, VmVolumeAttachment arg1) {
	  return arg0.getDevice().compareToIgnoreCase(arg1.getDevice());
	}	  
  }
  
  public static class Builder {
    private VmId                vmId;
    private VmBootRecord        vmBootRecord;
    private VmPlacement         vmPlacement;
    private List<NetworkGroup>  networkRulesGroups;
    private PrivateNetworkIndex networkIndex;
    private Boolean             usePrivateAddressing;
    private OwnerFullName       owner;
    private Date                expiration = new Date( 32503708800000l ); // 3000
    
    public Builder owner( final OwnerFullName owner ) {
      this.owner = owner;
      return this;
    }

    public Builder expiresOn( final Date expirationTime ) {
      if ( expirationTime != null ) {
        this.expiration = expirationTime;
      }
      return this;
    }
    
    public Builder networking( final List<NetworkGroup> groups, final PrivateNetworkIndex networkIndex ) {
      this.networkRulesGroups = groups;
      this.networkIndex = networkIndex;
      return this;
    }

    public Builder addressing( final Boolean usePrivate ) {
      this.usePrivateAddressing = usePrivate;
      return this;
    }
    
    public Builder withIds( @Nonnull  final String instanceId,
                            @Nonnull  final String reservationId,
                            @Nullable final String clientToken,
                            @Nullable final String uniqueClientToken ) {
      this.vmId = new VmId( reservationId, instanceId, clientToken, uniqueClientToken );
      return this;
    }
    
    public Builder placement( final Partition partition, final String clusterName ) {
      final ServiceConfiguration config = Topology.lookup( ClusterController.class, partition );
      this.vmPlacement = new VmPlacement( config.getName( ), config.getPartition( ) );
      return this;
    }
    
    public Builder bootRecord( final BootableSet bootSet,
                               final byte[] userData,
                               final SshKeyPair sshKeyPair,
                               final VmType vmType,
                               final boolean monitoring,
                               @Nullable final String iamInstanceProfileArn,
                               @Nullable final String iamInstanceProfileId,
                               @Nullable final String iamInstanceRoleArn ) {
      this.vmBootRecord = new VmBootRecord( bootSet, userData, sshKeyPair, vmType, monitoring, iamInstanceProfileArn, iamInstanceProfileId, iamInstanceRoleArn );
      return this;
    }

    public VmInstance build( final Integer launchIndex ) throws ResourceAllocationException {
      return new VmInstance( this.owner, this.vmId, this.vmBootRecord, new VmLaunchRecord( launchIndex, new Date( ) ), this.vmPlacement,
                             this.networkRulesGroups, this.networkIndex, this.usePrivateAddressing, this.expiration );
    }
  }
  
  private VmInstance( final OwnerFullName owner,
                      final VmId vmId,
                      final VmBootRecord bootRecord,
                      final VmLaunchRecord launchRecord,
                      final VmPlacement placement,
                      final List<NetworkGroup> networkRulesGroups,
                      final PrivateNetworkIndex networkIndex,
                      final Boolean usePrivateAddressing,
                      final Date expiration ) throws ResourceAllocationException {
    super( owner, vmId.getInstanceId( ) );
    this.setState( VmState.PENDING );
    this.vmId = vmId;
    this.expiration = expiration;
    this.bootRecord = bootRecord;
    this.launchRecord = launchRecord;
    this.placement = placement;
    this.privateNetwork = Boolean.FALSE;
    this.usageStats = new VmUsageStats( this );
    this.runtimeState = new VmRuntimeState( this );
    this.transientVolumeState = new VmVolumeState( this );
    this.networkConfig = new VmNetworkConfig( this, usePrivateAddressing );
    final Function<NetworkGroup, NetworkGroup> func = Entities.merge( );
    this.networkGroups.addAll( Collections2.transform( networkRulesGroups, func ) );
    this.networkIndex = networkIndex != PrivateNetworkIndex.bogus( )
                                                                    ? Entities.merge( networkIndex.set( this ) )
                                                                    : null;
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
    this.networkIndex = null;
    this.usageStats = null;
    this.runtimeState = null;
    this.networkConfig = null;
    this.transientVolumeState = null;
  }
  
  public void updateBlockBytes( final long blkbytes ) {
    this.usageStats.setBlockBytes( blkbytes );
  }
  
  public void updateNetworkBytes( final long netbytes ) {
    this.usageStats.setNetworkBytes( netbytes );
  }
  
  public void updateAddresses( final String privateAddr, final String publicAddr ) {
    this.updatePrivateAddress( privateAddr );
    this.updatePublicAddress( publicAddr );
  }
  
  public void updatePublicAddress( final String publicAddr ) {
    if ( !VmNetworkConfig.DEFAULT_IP.equals( publicAddr ) && !"".equals( publicAddr ) && ( publicAddr != null ) ) {
      this.getNetworkConfig( ).setPublicAddress( publicAddr );
    } else if ( VmState.STOPPED.equals(this.getState( ) ) ) {
        this.getNetworkConfig( ).setPublicAddress( publicAddr );
    } else {
        this.getNetworkConfig( ).setPublicAddress( VmNetworkConfig.DEFAULT_IP );
    }
    this.getNetworkConfig( ).updateDns( );
  }
  
  public void updatePrivateAddress( final String privateAddr ) {
    if ( !VmNetworkConfig.DEFAULT_IP.equals( privateAddr ) && !"".equals( privateAddr ) && ( privateAddr != null ) ) {
      this.getNetworkConfig( ).setPrivateAddress( privateAddr );
    } else if ( VmState.STOPPED.equals(this.getState( ) ) ) {
        this.getNetworkConfig( ).setPrivateAddress( privateAddr );
    }
    this.getNetworkConfig( ).updateDns( );
  }
  
  public VmRuntimeState getRuntimeState( ) {
    if ( this.runtimeState == null ) {
      this.runtimeState = new VmRuntimeState( this );
    }
    return this.runtimeState;
  }
  
  private void setRuntimeState( final VmState state ) {
    this.setState( state, Reason.NORMAL );
  }
  
  void store( ) {
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
        final String accountId = owner.getAccountNumber();

        ListenerRegistry.getInstance( ).fireEvent( new InstanceCreationEvent(
            getInstanceUuid(),
            getDisplayName(),
            this.bootRecord.getVmType().getName(),
            userId,
            Accounts.lookupUserById(userId).getName(),
            accountId,
            Accounts.lookupAccountById(accountId).getName(),
            this.placement.getPartitionName())); //TODO Add CPU and network utilization
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
      }
    }
  }

  public String getByKey( final String pathArg ) {
    final String path = Objects.firstNonNull( pathArg, "" );
    final String pathNoSlash;
    LOG.debug( "Servicing metadata request:" + path );
    if ( path.endsWith( "/" ) ) {
      pathNoSlash = path.substring( 0, path.length() -1 );
    } else {
      pathNoSlash = path;
    }

    Optional<MetadataGroup> groupOption = Optional.absent();
    for ( final MetadataGroup metadataGroup : MetadataGroup.values() ) {
      if ( metadataGroup.providesPath( pathNoSlash ) ||
          metadataGroup.providesPath( path ) ) {
        groupOption = Optional.of( metadataGroup );
      }
    }
    final MetadataGroup group = groupOption.or( MetadataGroup.Core );
    final Map<String,String> metadataMap =
        Optional.fromNullable( group.apply( this ) ).or( Collections.<String, String>emptyMap() );
    final String value = metadataMap.get( path );
    return value == null ? metadataMap.get( pathNoSlash ) : value;
  }

  private Map<String, String> getCoreMetadataMap( ) {
    final boolean dns = StackConfiguration.USE_INSTANCE_DNS && !ComponentIds.lookup( Dns.class ).runLimitedServices( );
    final Map<String, String> m = Maps.newHashMap( );
    m.put( "ami-id", this.getImageId( ) );
    if ( this.bootRecord.getMachine( ) != null && !this.bootRecord.getMachine( ).getProductCodes( ).isEmpty( ) ) {
      m.put( "product-codes", Joiner.on( '\n' ).join( this.bootRecord.getMachine( ).getProductCodes( ) ) );
    }
    m.put( "ami-launch-index", "" + this.launchRecord.getLaunchIndex( ) );
//ASAP: FIXME: GRZE:
//    m.put( "ancestor-ami-ids", this.getImageInfo( ).getAncestorIds( ).toString( ).replaceAll( "[\\Q[]\\E]", "" ).replaceAll( ", ", "\n" ) );
    if ( this.bootRecord.getMachine( ) instanceof MachineImageInfo ) {
      m.put( "ami-manifest-path", ( ( MachineImageInfo ) this.bootRecord.getMachine( ) ).getManifestLocation( ) );
    }
    if ( dns ) {
      m.put( "hostname", this.getNetworkConfig( ).getPrivateDnsName( ) );
    } else {
      m.put( "hostname", this.getNetworkConfig( ).getPrivateAddress( ) );
    }
    m.put( "instance-id", this.getInstanceId( ) );
    m.put( "instance-type", this.getVmType( ).getName( ) );
    if ( dns ) {
      m.put( "local-hostname", this.getNetworkConfig( ).getPrivateDnsName( ) );
    } else {
      m.put( "local-hostname", this.getNetworkConfig( ).getPrivateAddress( ) );
    }
    m.put( "local-ipv4", this.getNetworkConfig( ).getPrivateAddress( ) );
    m.put( "mac", upper( ).apply( this.getNetworkConfig().getMacAddress() ) );
    if ( dns ) {
      m.put( "public-hostname", this.getNetworkConfig( ).getPublicDnsName( ) );
    } else {
      m.put( "public-hostname", this.getPublicAddress( ) );
    }
    m.put( "public-ipv4", this.getPublicAddress( ) );
    m.put( "reservation-id", this.vmId.getReservationId( ) );
    if ( this.getKernelId( ) != null ) {
      m.put( "kernel-id", this.getKernelId( ) );
    }
    if ( this.getRamdiskId( ) != null ) {
      m.put( "ramdisk-id", this.getRamdiskId( ) );
    }
    m.put( "security-groups", Joiner.on('\n').join( this.getNetworkNames( ) ) );
    m.put( "placement/availability-zone", this.getPartition( ) );
    return m;
  }

  private Map<String, String> getBlockDeviceMappingMetadataMap( ) {
    final Map<String, String> m = Maps.newHashMap( );
    // Metadata should accurately reflect all the ebs mappings and ephemeral mappings if any.
    // Fixes EUCA-4081, EUCA-3954 and implements EUCA-4786
    if( this.bootRecord.getMachine() instanceof BlockStorageImageInfo ) {
      // Get all the volume attachments and order them in some way (by device name for now)
      Set<VmVolumeAttachment> volAttachments = new TreeSet<VmVolumeAttachment>(VolumeAttachmentComparator.INSTANCE);
      volAttachments.addAll(this.bootRecord.getPersistentVolumes());

      // Keep track of all ebs keys for populating block-device-mapping list
      int ebsCount = 0;

      // Iterate through the list of volume attachments and populate ebs mappings
      for (VmVolumeAttachment attachment : volAttachments ) {
        if (attachment.getIsRootDevice()) {
          m.put( "block-device-mapping/ami", attachment.getDevice() );
          m.put( "block-device-mapping/emi", attachment.getDevice() );
          m.put( "block-device-mapping/root", attachment.getDevice() );
        }
        m.put( "block-device-mapping/ebs" + String.valueOf(++ebsCount), attachment.getDevice() );
      }

      // Using ephemeral attachments for bfebs instances only, can be extended to be used by all other instances
      // Get all the ephemeral attachments and order them in some way (by device name for now)
      Set<VmEphemeralAttachment> ephemeralAttachments = new TreeSet<VmEphemeralAttachment>(this.bootRecord.getEphmeralStorage());

      // Iterate through the list of ephemeral attachments and populate ephemeral mappings
      if (!ephemeralAttachments.isEmpty()) {
        for(VmEphemeralAttachment attachment : ephemeralAttachments){
          m.put( "block-device-mapping/" + attachment.getEphemeralId(), attachment.getDevice() );
        }
      }
    } else {
      m.put( "block-device-mapping/emi", "sda1" );
      m.put( "block-device-mapping/ami", "sda1" );
      m.put( "block-device-mapping/ephemeral", "sda2" );
      m.put( "block-device-mapping/ephemeral0", "sda2" );
      m.put( "block-device-mapping/swap", "sda3" );
      m.put( "block-device-mapping/root", "/dev/sda1" );
    }
    return m;
  }

  private Map<String, String> getIamMetadataMap( ) {
    final Map<String, String> m = new HashMap<>( );
    final String instanceProfileNameOrArn = this.getIamInstanceProfileArn();
    if ( instanceProfileNameOrArn != null && !instanceProfileNameOrArn.isEmpty() ) {
      InstanceProfile profile = null;
      String roleName = null;
      String roleArn = this.getIamRoleArn();
      String profileArn = this.getIamInstanceProfileArn( );
      try {
        final Account userAccount = Accounts.lookupAccountById(this.getOwnerAccountNumber());
        String profileName;
        if ( instanceProfileNameOrArn.startsWith("arn:") ) {
          profileName = instanceProfileNameOrArn.substring( instanceProfileNameOrArn.lastIndexOf('/') + 1 );
        } else {
          profileName = instanceProfileNameOrArn;
        }
        profile = userAccount.lookupInstanceProfileByName(profileName);
        profileArn = Accounts.getInstanceProfileArn(profile);
        if ( roleArn == null ) {
          final Role role = profile.getRole();
          if ( role != null ) {
            roleArn = Accounts.getRoleArn( role );
            roleName = role.getName();
          }
        } else {
          // Authorized role from instance creation time must be used if present
          final EuareResourceName ern = (EuareResourceName) Ern.parse( roleArn );
          roleName = ern.getName();
        }
      } catch (AuthException e) {
        LOG.debug(e);
      }

      CredentialsType credentials = null;
      if ( roleArn != null ) {
        final AssumeRoleType assumeRoleType = new AssumeRoleType( );
        assumeRoleType.setRoleArn(roleArn);
        assumeRoleType.setRoleSessionName(Crypto.generateId(roleArn, this.getOwner().getUserId()));

        ServiceConfiguration serviceConfiguration = ServiceConfigurations
            .createEphemeral(ComponentIds.lookup(Tokens.class));
        try {
          credentials = ((AssumeRoleResponseType) AsyncRequests.sendSync(serviceConfiguration, assumeRoleType))
              .getAssumeRoleResult().getCredentials();
        } catch (Exception e) {
          LOG.debug("Unable to send assume role request to token service",e);
        }
      }

      if ( profile != null ) {
        m.put("iam/info/last-updated-date", Timestamps.formatIso8601Timestamp(profile.getCreationTimestamp()));  //TODO : Need to collection and display the real last updated date.
        m.put("iam/info/instance-profile-arn", profileArn );
        m.put("iam/info/instance-profile-id", profile.getInstanceProfileId() );
      }

      if ( roleName != null && credentials != null ) {
        final String jsonCredentials = new JSONObject( )
            .element( "Code", "Success" )
            .element( "LastUpdated", Timestamps.formatIso8601Timestamp( new Date( ) ) )
            .element( "Type", "AWS-HMAC" )
            .element( "AccessKeyId", credentials.getAccessKeyId( ) )
            .element( "SecretAccessKey", credentials.getSecretAccessKey( ) )
            .element( "Token", credentials.getSessionToken( ) )
            .element( "Expiration", Timestamps.formatIso8601Timestamp( credentials.getExpiration( ) ) )
            .toString( 2 );

        m.put("iam/security-credentials/" + roleName + "/AccessKeyId", credentials.getAccessKeyId());
        m.put("iam/security-credentials/" + roleName + "/Expiration",Timestamps.formatIso8601Timestamp(credentials.getExpiration()));
        m.put("iam/security-credentials/" + roleName + "/SecretAccessKey", credentials.getSecretAccessKey());
        m.put("iam/security-credentials/" + roleName + "/Token", credentials.getSessionToken());
        m.put("iam/security-credentials/" + roleName, jsonCredentials );
        m.put("iam/security-credentials", roleName );
        m.put("iam/security-credentials/", roleName );
      }

    }
    return m;
  }

  private Map<String, String> getPublicKeysMetadataMap( ) {
    final Map<String, String> m = Maps.newHashMap( );
    if ( this.bootRecord.getSshKeyPair( ) != null ) {
      m.put( "public-keys", "0=" + this.bootRecord.getSshKeyPair( ).getName( ) );
      m.put( "public-keys/", "0=" + this.bootRecord.getSshKeyPair( ).getName( ) );
      m.put( "public-keys/0/openssh-key", this.bootRecord.getSshKeyPair( ).getPublicKey( ) );
    }
    return m;
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
  
  public VmBundleTask resetBundleTask( ) {
    return this.getRuntimeState( ).resetBundleTask( );
  }
  
  public String getImageId( ) {
    return this.bootRecord.getMachine( ) == null ? "emi-00000000" : this.bootRecord.getMachine( ).getDisplayName( );
  }

  @Nullable
  public String getRamdiskId( ) {
    return CloudMetadatas.toDisplayName().apply( this.bootRecord.getRamdisk( ) );
  }

  @Nullable
  public String getKernelId( ) {
    return CloudMetadatas.toDisplayName().apply(  this.bootRecord.getKernel( ) );
  }
  
  public boolean hasPublicAddress( ) {
    return ( this.networkConfig != null )
           && !( VmNetworkConfig.DEFAULT_IP.equals( this.getNetworkConfig( ).getPublicAddress( ) ) || this.getNetworkConfig( ).getPrivateAddress( ).equals(
             this.getNetworkConfig( ).getPublicAddress( ) ) );
  }
  
  public String getInstanceId( ) {
    return super.getDisplayName( );
  }
  
  public String getConsoleOutputString( ) {
    return new String( Base64.encode( this.getRuntimeState( ).getConsoleOutput( ).toString( ).getBytes( ) ) );
  }
  
  public void setConsoleOutput( final StringBuffer consoleOutput ) {
    this.getRuntimeState( ).setConsoleOutput( consoleOutput );
  }
  
  public VmType getVmType( ) {
    return this.bootRecord.getVmType( );
  }
  
  public NavigableSet<String> getNetworkNames( ) {
    return new TreeSet<String>( Collections2.transform( this.getNetworkGroups( ), new Function<NetworkGroup, String>( ) {
      
      @Override
      public String apply( final NetworkGroup arg0 ) {
        return arg0.getDisplayName( );
      }
    } ) );
  }

  public TreeMap<String,String> getNetworkMap( ) { //A map that contains the security group name and id

    TreeMap<String,String> networkGroupMap = Maps.newTreeMap();

    for (NetworkGroup networkGroup : this.getNetworkGroups( ) ) {
      networkGroupMap.put(networkGroup.getGroupId(), networkGroup.getDisplayName());
    }

    return networkGroupMap;
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
  
  public String getPasswordData( ) {
    return this.getRuntimeState( ).getPasswordData( );
  }
  
  public void setPasswordData( final String passwordData ) {
    this.getRuntimeState( ).setPasswordData( passwordData );
  }
  
  /**
   * @return the platform
   */
  public String getPlatform( ) {
    return this.bootRecord.getPlatform( );
  }

  public String getDisplayPlatform( ) {
    return Platform.windows.name().equals( getPlatform() ) ?
        Platform.windows.name() :
        "";
  }
  
  /**
   * @return the networkBytes
   */
  public Long getNetworkBytes( ) {
    return this.usageStats.getNetworkBytes( );
  }
  
  /**
   * @return the blockBytes
   */
  public Long getBlockBytes( ) {
    return this.usageStats.getBlockBytes( );
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

  public static VmInstance namedTerminated( final OwnerFullName ownerFullName, final String instanceId ) {
    return new VmInstance( ownerFullName, instanceId ) {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;
      
      {
        this.setState( VmState.TERMINATED );
      }
    };
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
    EXPIRED( "Instance expired after not being reported for %s mins.", VmInstances.Timeout.UNREPORTED.getMinutes( ) ),
    FAILED( "The instance failed to start on the NC." ),
    USER_TERMINATED( "User terminated." ),
    USER_STOPPED( "User stopped." ),
    USER_STARTED( "User started." ),
    APPEND( "" );
    private String   message;
    private Object[] args;
    
    Reason( final String message, final Object... args ) {
      this.message = message;
      this.args = args;
    }

    @Override
    public String toString( ) {
      return String.format( this.message.toString( ), this.args );
    }

    @Override
    public boolean apply( final VmInstance vmInstance ) {
      return this.equals( vmInstance.getRuntimeState().reason() );
    }
  }
  
  PrivateNetworkIndex getNetworkIndex( ) {
    return this.networkIndex;
  }
  
  public void releaseNetworkIndex( ) {
    try {
      this.networkIndex.release( );
      
    } catch ( final ResourceAllocationException ex ) {
      LOG.trace( ex, ex );
      LOG.error( ex );
    }
  }
  
  private Boolean getPrivateNetwork( ) {
    return this.privateNetwork;
  }

  public Collection<VmInstanceTag> getTags() {
    return tags;
  }

  public Set<NetworkGroup> getNetworkGroups( ) {
    return ( Set<NetworkGroup> ) ( this.networkGroups != null
                                                             ? this.networkGroups
                                                             : Sets.newHashSet( ) );
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
  
  VmLaunchRecord getLaunchRecord( ) {
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
  public void setState( final VmState stopping, final Reason reason, final String... extra ) {
    
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      final VmInstance entity = Entities.merge( this );
      VmInstances.initialize( ).apply( entity );
      entity.runtimeState.setState( stopping, reason, extra );
      if ( VmStateSet.DONE.apply( entity ) ) {
        entity.cleanUp( );
      }
      db.commit( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      throw Exceptions.toUndeclared( ex );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
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
      final VmInstance entity = Entities.merge( this );
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
  
  public VmVolumeAttachment lookupTransientVolumeAttachment( final String volumeId ) {
	final EntityTransaction db = Entities.get( VmInstance.class );
	try {
	  final VmInstance entity = Entities.merge( this );
	  VmVolumeAttachment volumeAttachment;
      try {
        volumeAttachment = entity.getTransientVolumeState( ).lookupVolumeAttachment( volumeId );
        db.commit( );
      } catch ( final NoSuchElementException ex ) {
        volumeAttachment = Iterables.find( entity.getBootRecord( ).getPersistentVolumes( ), VmVolumeAttachment.volumeIdFilter( volumeId ) );
        db.commit( );
        if(volumeAttachment != null) {
  		  throw new NonTransientVolumeException( volumeId + " is associated with boot from EBS instance " + entity.getInstanceId() + " at launch time.");
  		}
      }
	  return volumeAttachment;
	} catch (NonTransientVolumeException nex) {
	  throw nex;	
	} catch ( final Exception ex ) {
	  throw new NoSuchElementException( "Failed to lookup volume: " + volumeId );
    } finally {
	  if ( db.isActive() ) db.rollback();
	}
  }
  
  /**
   *
   */
  public void addTransientVolume( final String deviceName, final String remoteDevice, final Volume vol ) {
    final Function<Volume, Volume> attachmentFunction = new Function<Volume, Volume>( ) {
      public Volume apply( final Volume input ) {
        final VmInstance entity = Entities.merge( VmInstance.this );
        final Volume volEntity = Entities.merge( vol );
        VmVolumeAttachment attachVol = new VmVolumeAttachment( entity, volEntity.getDisplayName( ), deviceName, remoteDevice, AttachmentState.attaching.name( ), new Date( ), false );
        volEntity.setState( State.BUSY );
        entity.getTransientVolumeState( ).addVolumeAttachment( attachVol );
        return volEntity;
      }
    };
    Entities.asTransaction( VmInstance.class, attachmentFunction, VmInstances.TX_RETRIES ).apply( vol );
  }
  
  public void addPersistentVolume( final String deviceName, final Volume vol, final Boolean isRootDevice ) {
    final Function<Volume, Volume> attachmentFunction = new Function<Volume, Volume>( ) {
      public Volume apply( final Volume input ) {
        final VmInstance entity = Entities.merge( VmInstance.this );
        final Volume volEntity = Entities.merge( vol );
        // At this point the remote device string is not available. Setting this member to null leads to DB lookup issues later. So setting it to empty string instead
        final VmVolumeAttachment volumeAttachment = new VmVolumeAttachment( entity, vol.getDisplayName( ), deviceName, new String(), AttachmentState.attached.name( ), new Date( ), true, isRootDevice );
        entity.bootRecord.getPersistentVolumes( ).add( volumeAttachment );
        return volEntity;
      }
    };
    Entities.asTransaction( VmInstance.class, attachmentFunction, VmInstances.TX_RETRIES ).apply( vol );
  }
  
  public void addPermanentVolume( final String deviceName, final Volume vol, final Boolean isRootDevice ) {
    final Function<Volume, Volume> attachmentFunction = new Function<Volume, Volume>( ) {
      public Volume apply( final Volume input ) {
        final VmInstance entity = Entities.merge( VmInstance.this );
        final Volume volEntity = Entities.merge( vol );
        // At this point the remote device string is not available. Setting this member to null leads to DB lookup issues later. So setting it to empty string instead  
        final VmVolumeAttachment volumeAttachment = new VmVolumeAttachment( entity, vol.getDisplayName( ), deviceName, new String(), AttachmentState.attached.name( ), new Date( ), false, isRootDevice );
        entity.bootRecord.getPersistentVolumes( ).add( volumeAttachment );
        return volEntity;
      }
    };
    Entities.asTransaction( VmInstance.class, attachmentFunction, VmInstances.TX_RETRIES ).apply( vol );
  }
  
  // Added for EUCA-6935. Persistent volume attachments have to be removed one by one within their own transaction.
  // DO NOT invoke this method within a transaction as it might result in  OptimisticLockException or StaleObjectException
  public void removePersistentVolumeAttachment (final VmVolumeAttachment attachment) {
    final Function<VmVolumeAttachment, Boolean> attachmentFunction = new Function<VmVolumeAttachment, Boolean>() {
	  public Boolean apply(VmVolumeAttachment arg0) {
		final VmInstance vm = Entities.merge(VmInstance.this);
	    Volume volume = Volumes.lookup( null, arg0.getVolumeId());
	    if (arg0.getDeleteOnTerminate()) {
  		  try {
  		    final ServiceConfiguration sc = Topology.lookup(Storage.class, vm.lookupPartition());
  		    try {
  		      DeleteStorageVolumeResponseType reply = AsyncRequests.sendSync( sc, new DeleteStorageVolumeType(arg0.getVolumeId()));
  		      if(null != reply && reply.get_return()) {
                Volumes.annihilateStorageVolume(volume);
  		      } else {
  		        LOG.error(arg0.getVolumeId() + ": Failed to delete volume.");
    		  }
  		    } catch (Exception ex) {
  		      LOG.error(arg0.getVolumeId() + ": Failed to delete volume.", ex);
  		    }
  		  } catch ( Exception ex ) {
  		    LOG.error(arg0.getVolumeId() + "Failed to lookup volume", ex);
  		  }
	    }
	    return vm.getBootRecord().getPersistentVolumes().remove(arg0);
	  } 
    };
	Entities.asTransaction(VmInstance.class, attachmentFunction, VmInstances.TX_RETRIES).apply(attachment);
  }
  
  public void removeTransientVolumeAttachment (final VmVolumeAttachment attachment) {
	final Function<VmVolumeAttachment, Boolean> attachmentFunction = new Function<VmVolumeAttachment, Boolean>() {
	  public Boolean apply(VmVolumeAttachment arg0) {
	    final VmInstance vm = Entities.merge( VmInstance.this );
	    final Volume volume = Volumes.lookup( null, arg0.getVolumeId() );
	    try {
	      vm.transientVolumeState.removeVolumeAttachment(arg0.getVolumeId());
		} catch (NoSuchElementException ex) {
		  LOG.debug(arg0.getVolumeId() + ": Unable to find volume attachment. Nothing to remove", ex);
		}
	    if ( State.BUSY.equals( volume.getState( ) ) ) {
	      volume.setState( State.EXTANT );
	    }
	    return true;
	  } 
	};
    Entities.asTransaction( VmInstance.class, attachmentFunction, VmInstances.TX_RETRIES).apply(attachment);
  }
  
  // Creates a DB entity associated with ephemeral devices for boot from ebs instances and stores it in the boot record
  public void addEphemeralAttachment( final String deviceName, final String ephemeralId ) {
    final Function<String, String> attachmentFunction = new Function<String, String>( ) {
	  public String apply( final String input ) {
	    final VmInstance entity = Entities.merge( VmInstance.this );
	    final VmEphemeralAttachment ephemeralAttachment = new VmEphemeralAttachment( entity, ephemeralId, deviceName );
	    entity.bootRecord.getEphmeralStorage().add(ephemeralAttachment);
	    return input;
      }
    };
    Entities.asTransaction( VmInstance.class, attachmentFunction, VmInstances.TX_RETRIES ).apply( ephemeralId );
  }
  
  // Update the volume attachment and volume records to reflect the remote device string
  public void updatePersistantVolume( final String remoteDevice, final Volume vol ) {
	final EntityTransaction db = Entities.get( VmInstance.class );
	VmInstance instanceEntity = Entities.merge( this );
	Volume volEntity = Entities.merge( vol );
	try {
	  final Set<VmVolumeAttachment> attachments = instanceEntity.bootRecord.getPersistentVolumes();
	  for (VmVolumeAttachment attachment : attachments ) {
	    if (attachment.getVolumeId().equals(volEntity.getDisplayName())) {
	      attachment.setRemoteDevice(remoteDevice);
	      break;
	    }
	  }
	  volEntity.setRemoteDevice(remoteDevice);
	  db.commit( );
	} catch ( final Exception ex ) {
	  Logs.extreme( ).error( ex, ex );
	  db.rollback( );
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
        Set<VmVolumeAttachment> persistentAttachments = Sets.newHashSet( entity.getBootRecord( ).getPersistentVolumes( ) );
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
  public VmVolumeAttachment removeVolumeAttachment( final String volumeId ) {
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      final VmInstance entity = Entities.merge( this );
      final Volume volEntity = Volumes.lookup( null, volumeId );
      final VmVolumeAttachment ret = entity.transientVolumeState.removeVolumeAttachment( volumeId );
      if ( State.BUSY.equals( volEntity.getState( ) ) ) {
        volEntity.setState( State.EXTANT );
      }
      db.commit( );
      return ret;
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      throw new NoSuchElementException( "Failed to lookup volume: " + volumeId );
    } finally {
      if ( db.isActive() ) db.rollback();
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
  
  /**
   *
   */
  public void updateVolumeAttachment( final String volumeId, final AttachmentState newState ) {
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      final VmInstance entity = Entities.merge( this );
      entity.getTransientVolumeState( ).updateVolumeAttachment( volumeId, newState );
      db.commit( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }
  
  /**
   *
   */
  public Predicate<VmInfo> doUpdate( ) {
    return new Predicate<VmInfo>( ) {
      
      @Override
      public boolean apply( final VmInfo runVm ) {
        if ( !Entities.isPersistent( VmInstance.this ) ) {
          throw new TransientEntityException( this.toString( ) );
        } else {
          final EntityTransaction db = Entities.get( VmInstance.class );
          try {
            final VmState runVmState = VmState.Mapper.get( runVm.getStateName( ) );
            if ( VmInstance.this.getRuntimeState( ).isBundling( ) ) {
              final BundleState bundleState = BundleState.mapper.apply( runVm.getBundleTaskStateName( ) );
              VmInstance.this.getRuntimeState( ).updateBundleTaskState( bundleState );
            } else if ( VmStateSet.RUN.apply( VmInstance.this ) && VmStateSet.RUN.contains( runVmState ) ) {
              VmInstance.this.setState( runVmState, Reason.APPEND, "UPDATE" );
              this.updateState( runVm );
            } else if ( VmState.SHUTTING_DOWN.apply( VmInstance.this ) && VmState.SHUTTING_DOWN.equals( runVmState ) ) {
              VmInstance.this.setState( VmState.TERMINATED, Reason.APPEND, "DONE" );
            } else if ( VmState.SHUTTING_DOWN.apply( VmInstance.this ) && VmInstances.Timeout.SHUTTING_DOWN.apply( VmInstance.this ) ) {
              VmInstance.this.setState( VmState.TERMINATED, Reason.EXPIRED );
            } else if ( VmState.STOPPING.apply( VmInstance.this ) && VmInstances.Timeout.SHUTTING_DOWN.apply( VmInstance.this ) ) {
              VmInstance.this.setState( VmState.STOPPED, Reason.EXPIRED );
            } else if ( VmStateSet.NOT_RUNNING.apply( VmInstance.this ) && VmStateSet.RUN.contains( runVmState ) ) {
              VmInstance.this.setState( VmState.RUNNING, Reason.APPEND, "MISMATCHED" );
            } else {
              this.updateState( runVm );
            }
            //VmInstance.this.fireUsageEvent( );
            db.commit( );
          } catch ( final Exception ex ) {
            Logs.extreme( ).error( ex, ex );
          } finally {
            if ( db.isActive() ) db.rollback();
          }
        }
        return true;
      }
      
      private void updateState( final VmInfo runVm ) {
        VmInstance.this.getRuntimeState( ).updateBundleTaskState( runVm.getBundleTaskStateName( ) );
        VmInstance.this.getRuntimeState( ).setServiceTag( runVm.getServiceTag( ) );
        VmInstance.this.updateAddresses( runVm.getNetParams( ).getIpAddress( ), runVm.getNetParams( ).getIgnoredPublicIp( ) );
        VmInstance.this.getRuntimeState( ).setGuestState(runVm.getGuestStateName());
        if ( VmState.RUNNING.apply( VmInstance.this ) ) {
          VmInstance.this.updateVolumeAttachments( runVm.getVolumes( ) );
          VmInstance.this.updateBlockBytes( runVm.getBlockBytes( ) );
          VmInstance.this.updateNetworkBytes( runVm.getNetworkBytes( ) );
          VmInstance.this.updateMigrationTaskState( runVm.getMigrationStateName( ), runVm.getMigrationSource( ), runVm.getMigrationDestination( )  );
        }
      }
    };
  }
  
  /**
   * @param migrationStateName
   * @param migrationSource
   * @param migrationDestination
   */
  protected void updateMigrationTaskState( String migrationStateName, String migrationSource, String migrationDestination ) {
    this.getRuntimeState( ).setMigrationState( migrationStateName, migrationSource, migrationDestination );
  }

  
  /**
   *
   */
  private void updateVolumeAttachments( final List<AttachedVolume> volumes ) {
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      final VmInstance entity = Entities.merge( this );
      entity.getTransientVolumeState( ).updateVolumeAttachments( Lists.transform( volumes, VmVolumeAttachment.fromAttachedVolume( entity ) ) );
      db.commit( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }
  
  /**
   *
   */
  public void setServiceTag( final String serviceTag ) {
    this.getRuntimeState( ).setServiceTag( serviceTag );
  }
  
  public void setNetworkIndex( final PrivateNetworkIndex networkIndex ) {
    this.networkIndex = networkIndex;
  }

  VmState getDisplayState() {
 	  return getState( );
  }

  String getDisplayPublicDnsName( ) {
    return dns() ?
        Objects.firstNonNull( getPublicDnsName( ), VmNetworkConfig.DEFAULT_IP ) :
        getDisplayPublicAddress( );
  }

  String getDisplayPublicAddress( ) {
    return dns() ?
        getPublicAddress( ) :
        VmNetworkConfig.DEFAULT_IP.equals( Objects.firstNonNull( getPublicAddress( ), VmNetworkConfig.DEFAULT_IP ) ) ?
            getDisplayPrivateAddress( ) :
            Objects.firstNonNull( getPublicAddress( ), VmNetworkConfig.DEFAULT_IP );
  }

  String getDisplayPrivateDnsName( ) {
    return dns() ?
      Objects.firstNonNull( getPrivateDnsName( ), VmNetworkConfig.DEFAULT_IP ) :
      getDisplayPrivateAddress( );
  }

  String getDisplayPrivateAddress( ) {
    return dns() ?
        getPrivateAddress( ) :
        Objects.firstNonNull( getPrivateAddress( ), VmNetworkConfig.DEFAULT_IP );
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
    public RunningInstancesItemType apply( final VmInstance v ) {
      if ( !Entities.isPersistent( v ) ) {
        throw new TransientEntityException( v.toString( ) );
      } else {
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          final VmInstance input = Entities.merge( v );
          RunningInstancesItemType runningInstance;
          runningInstance = new RunningInstancesItemType( );
          
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
          runningInstance.setIpAddress( input.getDisplayPublicAddress() );
          runningInstance.setPrivateDnsName( input.getDisplayPrivateDnsName() );
          runningInstance.setPrivateIpAddress( input.getDisplayPrivateAddress() );

          runningInstance.setReason( input.runtimeState.getReason( ) );
          if ( input.getBootRecord( ).getSshKeyPair( ) != null ) {
            runningInstance.setKeyName( input.getBootRecord( ).getSshKeyPair( ).getName( ) );
            if (  ( runningInstance.getKeyName( ) != null ) && ( runningInstance.getKeyName( ).isEmpty( ) ) )
                runningInstance.setKeyName( null );
          } else runningInstance.setKeyName( "" );
          
          runningInstance.setInstanceType( input.getVmType( ).getName( ) );
          runningInstance.setPlacement( input.getPlacement( ).getPartitionName( ) );
          
          runningInstance.setLaunchTime( input.getLaunchRecord( ).getLaunchTime( ) );
          runningInstance.setClientToken( input.getClientToken() );

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

            InstanceProfile instanceProfile;

            try {
              instanceProfile = Accounts.lookupAccountById(input.getOwnerAccountNumber())
                .lookupInstanceProfileByName(name);
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
              final InstanceProfile instanceProfile = Accounts.lookupAccountById(input.getOwnerAccountNumber())
                .lookupInstanceProfileByName(input.getIamInstanceProfileArn());
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
          
          runningInstance.setVirtualizationType(input.getVirtulizationType());
          
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
          return runningInstance;
        } catch ( final NoSuchElementException ex ) {
          throw ex;
        } catch ( final Exception ex ) {
          throw new NoSuchElementException( "Failed to lookup vm instance: " + v );
        } finally {
          if ( db.isActive() ) db.rollback();
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
          CollectionUtils.putAll(
              instance.getNetworkGroups(),
              Maps.<String, String>newTreeMap(),
              NetworkGroups.groupId(),
              RestrictedTypes.toDisplayName() ) );
    }
  }

  public boolean isLinux( ) {
    return this.bootRecord.isLinux( );
  }
  
  public boolean isBlockStorage( ) {
    return this.bootRecord.isBlockStorage( );
  }

  public boolean isEbsOptimized( ) {
    return false;
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
    if ( Entities.isReadable( this.networkIndex ) ) builder2.append( "networkIndex=" ).append( this.networkIndex );
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
  
  public void release( ) {
    try {
      Entities.asTransaction( VmInstance.class, Transitions.DELETE, VmInstances.TX_RETRIES ).apply( this );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
    }
  }
  
  public String getVirtulizationType( ) {
	  try{
		  final BootableImageInfo emi = this.getBootRecord().getMachine();
		  final ImageMetadata.VirtualizationType virtType = emi.getVirtualizationType();
		  if(virtType!=null)
			  return virtType.toString();
		  else{
			  if(emi instanceof BlockStorageImageInfo || ImageMetadata.Platform.windows.equals(emi.getPlatform()))
				  return ImageMetadata.VirtualizationType.hvm.toString();
			  else
				  return ImageMetadata.VirtualizationType.paravirtualized.toString();
			  
		  }
	  }catch( final Exception ex){
		  return ImageMetadata.VirtualizationType.paravirtualized.toString();
	  }
	  
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

      final InstanceStateType state = new InstanceStateType();
      state.setCode( displayState.getCode() );
      state.setName( displayState.getName() );
      instanceStatusItemType.setInstanceState( state );
      instanceStatusItemType.setInstanceStatus( buildStatus( displayState) );
      instanceStatusItemType.setSystemStatus( buildStatus( displayState ) );

      return instanceStatusItemType;
    }

    private InstanceStatusType buildStatus( final VmState vmState ) {
      final InstanceStatusType instanceStatus = new InstanceStatusType();
      if ( VmState.RUNNING == vmState ) {
        final InstanceStatusDetailsSetItemType statusDetailsItem = new InstanceStatusDetailsSetItemType();
        statusDetailsItem.setName( "reachability" );
        statusDetailsItem.setStatus( "passed" );

        final InstanceStatusDetailsSetType statusDetails = new InstanceStatusDetailsSetType();
        statusDetails.getItem().add( statusDetailsItem );

        instanceStatus.setStatus( "ok" );
        instanceStatus.setDetails( statusDetails );
      } else {
        instanceStatus.setStatus( "not-applicable" );
      }
      return instanceStatus;
    }
  }

  public Boolean getMonitoring() {
    return this.getBootRecord().isMonitoring( );
  }

  public void startMigration( ) {
    this.runtimeState.startMigration( );
  }

  public void abortMigration( ) {
    this.runtimeState.abortMigration( );
  }

  public VmMigrationTask getMigrationTask( ) {
    return this.runtimeState.getMigrationTask( );
  }


  private enum MetadataGroup implements Function<VmInstance,Map<String,String>> {
    Core {
      @Override
      public Map<String, String> apply( final VmInstance instance ) {
        return addListingEntries( instance, instance.getCoreMetadataMap( ), true );
      }

    },
    BlockDeviceMapping( "block-device-mapping" ) {
      @Override
      public Map<String, String> apply( final VmInstance instance ) {
        return addListingEntries( instance.getBlockDeviceMappingMetadataMap( ) );
      }
    },
    Iam( "iam" ) {
      @Override
      public Map<String, String> apply( final VmInstance instance ) {
        try {
          return metadataCache.get( new MetadataKey( instance.getId(), this ), new Callable<ImmutableMap<String,String>>() {
            @Override
            public ImmutableMap<String,String> call( ) throws Exception {
              return ImmutableMap.copyOf( addListingEntries( instance.getIamMetadataMap( ) ) );
            }
          } );
        } catch ( ExecutionException e ) {
          throw Exceptions.toUndeclared( e ); // Cache load exception not expected
        }
      }

      @Override
      protected boolean isPresent( final VmInstance instance ) {
        return !Strings.isNullOrEmpty( instance.getIamInstanceProfileArn() );
      }
    },
    PublicKeys( "public-keys" ) {
      @Override
      public Map<String, String> apply( final VmInstance instance ) {
        return addListingEntries( instance.getPublicKeysMetadataMap( ) );
      }

      @Override
      protected boolean isPresent( final VmInstance instance ) {
        return instance.bootRecord.getSshKeyPair( ) != null;
      }
    };

    private final Optional<String> prefix;

    private MetadataGroup( ) {
      prefix = Optional.absent( );
    }

    private MetadataGroup( final String path ) {
      prefix = Optional.of( path );
    }

    public boolean providesPath( final String path ) {
      return
          prefix.transform( Functions.forPredicate( isPrefixOf( path ) ) )
              .or( Boolean.FALSE );
    }

    protected boolean isPresent(  final VmInstance instance   ) {
      return true;
    }

    private static Map<String,String> addListingEntries( final Map<String,String> metadataMap ) {
      return addListingEntries( null, metadataMap, false );
    }

    private static Map<String,String> addListingEntries( @Nullable final VmInstance instance,
                                                         final Map<String,String> metadataMap,
                                                         final boolean addRoots ) {
      final TreeMultimap<String,String> listingMap = TreeMultimap.create( );
      final Splitter pathSplitter = Splitter.on( '/' );
      final Joiner pathJoiner = Joiner.on( '/' );
      for ( final String path : metadataMap.keySet() ) {
        final List<String> pathSegments = Lists.newArrayList( pathSplitter.split( path ) );
        for ( int i=0; i<pathSegments.size(); i++ ) {
          listingMap.put(
              pathJoiner.join( pathSegments.subList( 0, i ) ),
              pathSegments.get( i ) + ( i < pathSegments.size() -1 ? "/" : "" ) );
        }
      }

      if ( addRoots && instance != null ) {
        for ( MetadataGroup group : MetadataGroup.values() ) {
          if ( group.isPresent( instance ) && group.prefix.isPresent(  ) ) {
            listingMap.put( "", group.prefix.get() + "/" );
          }
        }
      }

      final Joiner listingJoiner = Joiner.on( "\n" );
      for ( final String key : listingMap.keySet() ) {
        final Set<String> values = listingMap.get( key );
        final Iterator<String> valueIterator = values.iterator( );
        while ( valueIterator.hasNext( ) ) {
          final String value = valueIterator.next( );
          if ( values.contains( value+"/" ) ) valueIterator.remove( );
        }
        if ( !metadataMap.containsKey( key ) ) {
          metadataMap.put( key, listingJoiner.join( values ) );
        } else if ( !metadataMap.containsKey( key + "/" ) ) {
          metadataMap.put( key + "/", listingJoiner.join( values ) );
        }
      }

      return metadataMap;
    }
  }

  private static final class MetadataKey {
    private final String id; // internal id
    private final MetadataGroup metadataGroup;

    private MetadataKey( final String id, final MetadataGroup metadataGroup ) {
      this.id = id;
      this.metadataGroup = metadataGroup;
    }

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final MetadataKey that = (MetadataKey) o;

      if ( !id.equals( that.id ) ) return false;
      if ( metadataGroup != that.metadataGroup ) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = id.hashCode();
      result = 31 * result + metadataGroup.hashCode();
      return result;
    }
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
        	if( vm.getBootRecord().getEphmeralStorage().isEmpty() ) {
        	  LOG.info("Adding ephemeral disk at /dev/sdb");
        	  vm.addEphemeralAttachment("/dev/sdb", "ephemeral0");	
        	}
        	
        	// Pre 3.3 code allowed only one persistent volume i.e. the root volume. Check before upgrading
        	if ( vm.getBootRecord().getPersistentVolumes().size() == 1 ) {
        	  VmVolumeAttachment attachment	= vm.getBootRecord().getPersistentVolumes().iterator().next();
        	  LOG.info("Found the only VmVolumeAttachment: " + attachment.toString());
        	  LOG.info("Setting root device flag to true");
              attachment.setIsRootDevice(Boolean.TRUE);
              LOG.info("Changing the device name to /dev/sda");
              attachment.setDevice("/dev/sda");  
        	} else { // This should not be the case updating to 3.3
        	 // If the instance has more or less than one persistent volume, iterate through them and update the one with device "/dev/sda1"
        	  for ( VmVolumeAttachment attachment : vm.getBootRecord().getPersistentVolumes() ) {
        		LOG.info("Found VmVolumeAttachment: " + attachment.toString());
        		if ( attachment.getDevice().equalsIgnoreCase("/dev/sda1") ) {
        		  LOG.info("Setting root device flag to true");
                  attachment.setIsRootDevice(Boolean.TRUE);
                  LOG.info("Changing the device name from /dev/sda1 to /dev/sda");
                  attachment.setDevice("/dev/sda");  
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
}
