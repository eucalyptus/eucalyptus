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

package com.eucalyptus.cloud.run;

import static com.eucalyptus.util.RestrictedTypes.BatchAllocator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.cloud.VmInstanceToken;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.compute.common.CloudMetadataLimitedType;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.google.common.base.Function;
import org.apache.log4j.Logger;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.cloud.VmInstanceLifecycleHelper;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.compute.common.internal.util.IllegalMetadataAccessException;
import com.eucalyptus.compute.common.internal.util.NotEnoughResourcesException;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.ResourceState;
import com.eucalyptus.cluster.common.ResourceState.VmTypeAvailability;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.network.DnsHostNamesFeature;
import com.eucalyptus.compute.common.network.NetworkFeature;
import com.eucalyptus.compute.common.network.NetworkResource;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResultType;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType;
import com.eucalyptus.context.ServiceStateException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.compute.common.internal.images.BlockStorageImageInfo;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

public class AdmissionControl {
  private static Logger LOG = Logger.getLogger( AdmissionControl.class );
  
  public static Predicate<Allocation> run( ) {
    return RunAdmissionControl.INSTANCE;
  }

  public static Predicate<Allocation> restore( ) {
    return Restore.INSTANCE;
  }

  enum RunAdmissionControl implements Predicate<Allocation> {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) {
      if ( EventRecord.isTraceEnabled( AdmissionControl.class ) ) {
        EventRecord.here( AdmissionControl.class, EventType.VM_RESERVED, LogUtil.dumpObject( allocInfo ) ).trace( );
      }
      List<ResourceAllocator> finished = Lists.newArrayList( );
      EntityTransaction db = Entities.get( NetworkGroup.class );
      try {
        for ( ResourceAllocator allocator : allocators ) {
          runAllocatorSafely( allocInfo, allocator );
          finished.add( allocator );
        }
        db.commit( );
        return true;
      } catch ( Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        rollbackAllocations( allocInfo, finished, ex );
        db.rollback( );
        throw Exceptions.toUndeclared( new NotEnoughResourcesException( Exceptions.getCauseMessage( ex ), ex ) );
      }
    }
    
  }

  enum Restore implements Predicate<Allocation> {
    INSTANCE;

    @Override
    public boolean apply( Allocation allocInfo ) {
      List<ResourceAllocator> finished = Lists.newArrayList( );
      EntityTransaction db = Entities.get( NetworkGroup.class );
      try {
        for ( ResourceAllocator allocator : restorers ) {
          runAllocatorSafely( allocInfo, allocator );
          finished.add( allocator );
        }
        db.commit( );
        return true;
      } catch ( Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        rollbackAllocations( allocInfo, finished, ex );
        db.rollback( );
        throw Exceptions.toUndeclared( new NotEnoughResourcesException( ex.getMessage( ), ex ) );
      }
    }
  }

  private static void rollbackAllocations( Allocation allocInfo, List<ResourceAllocator> finished, Exception e ) {
    for ( ResourceAllocator rollback : Lists.reverse( finished ) ) {
      try {
        rollback.fail( allocInfo, e );
      } catch ( Exception e1 ) {
        LOG.debug( e1, e1 );
      }
    }
  }
  
  private static void runAllocatorSafely( Allocation allocInfo, ResourceAllocator allocator ) throws Exception {
    try {
      allocator.allocate( allocInfo );
    } catch ( ScriptExecutionFailedException e ) {
      if ( e.getCause( ) != null ) {
        throw new EucalyptusCloudException( e.getCause( ).getMessage( ), e.getCause( ) );
      } else {
        throw new EucalyptusCloudException( e.getMessage( ), e );
      }
    } catch ( Exception e ) {
      LOG.debug( e, e );
      try {
        allocator.fail( allocInfo, e );
      } catch ( Exception e1 ) {
        LOG.debug( e1, e1 );
      }
      throw e;
    }
  }
  
  private interface ResourceAllocator {
    public void allocate( Allocation allocInfo ) throws Exception;
    
    public void fail( Allocation allocInfo, Throwable t );
    
  }
  
  private static final List<ResourceAllocator> allocators = ImmutableList.<ResourceAllocator>of(
      NodeResourceAllocator.INSTANCE,
      NetworkingAllocator.INSTANCE
  );

  private static final List<ResourceAllocator> restorers = ImmutableList.<ResourceAllocator>of(
      NetworkingAllocator.INSTANCE
  );

  enum NodeResourceAllocator implements ResourceAllocator {
    INSTANCE;
    private List<VmInstanceToken> requestResourceToken( final Allocation allocInfo, final int tryAmount, final int maxAmount ) throws Exception {
      ServiceConfiguration config = Topology.lookup( ClusterController.class, allocInfo.getPartition( ) );
      Cluster cluster = Clusters.lookupAny( config );
      /**
       * TODO:GRZE: this is the call path which needs to trigger gating.  
       * It shouldn't be handled directly here, but instead be handled in {@link ResourceState#requestResourceAllocation().
       * 
       */
      if ( cluster.getGateLock( ).readLock( ).tryLock( 60, TimeUnit.SECONDS ) ) {
        try {
          final ResourceState state = cluster.getNodeState( );
          /**
           * NOTE: If the defined instance type has an ordering conflict w/ some other type then it
           * isn't safe to service TWO requests which use differing types during the same resource refresh
           * duty cycle.
           * This determines whether or not an asynchronous allocation is safe to do for the
           * request instance type or whether a synchronous resource availability refresh is needed.
           * 
           */
          boolean unorderedType = VmTypes.isUnorderedType( allocInfo.getVmType( ) );
          boolean forceResourceRefresh = state.hasUnorderedTokens( ) || unorderedType;
          /**
           * GRZE: if the vm type is not "nicely" ordered then we force a refresh of the actual
           * cluster state. Note: we already hold the cluster gating lock here so this update will
           * be mutual exclusive wrt both resource allocations and cluster state updates.
           */
          if ( forceResourceRefresh ) {
            cluster.refreshResources( );
          }
          final BatchAllocator<VmInstanceToken> allocator = new BatchAllocator<VmInstanceToken>( ) {
            @Override
            public List<VmInstanceToken> allocate( int min, int max ) {
              try {
              // do quotas for "active" instances
                RestrictedTypes.allocateMeasurableResource(Long.valueOf(1L*max),
                  new Function<Long, CloudMetadataLimitedType.VmInstanceActiveMetadata>() {
                    @Nullable
                    @Override
                    public CloudMetadataLimitedType.VmInstanceActiveMetadata apply(@Nullable Long amount) {
                      return new CloudMetadataLimitedType.VmInstanceActiveMetadata() {
                      }; // kind of a marker for active instances
                    }
                  });
               // do quotas for instance specific items (cpu, memory, disk)
                RestrictedTypes.allocateMeasurableResource(max * Long.valueOf(allocInfo.getVmType().getCpu().longValue()),
                  new Function<Long, CloudMetadataLimitedType.CpuMetadata>() {
                    @Nullable
                    @Override
                    public CloudMetadataLimitedType.CpuMetadata apply(@Nullable Long amount) {
                      return new CloudMetadataLimitedType.CpuMetadata() {
                      }; // kind of a marker for cpu
                    }
                  });
                RestrictedTypes.allocateMeasurableResource(max * Long.valueOf(allocInfo.getVmType().getMemory().longValue()),
                  new Function<Long, CloudMetadataLimitedType.MemoryMetadata>() {
                    @Nullable
                    @Override
                    public CloudMetadataLimitedType.MemoryMetadata apply(@Nullable Long amount) {
                      return new CloudMetadataLimitedType.MemoryMetadata() {
                      }; // kind of a marker for memory
                    }
                  });
                RestrictedTypes.allocateMeasurableResource(max * Long.valueOf(allocInfo.getVmType().getDisk().longValue()),
                  new Function<Long, CloudMetadataLimitedType.DiskMetadata>() {
                    @Nullable
                    @Override
                    public CloudMetadataLimitedType.DiskMetadata apply(@Nullable Long amount) {
                      return new CloudMetadataLimitedType.DiskMetadata() {
                      }; // kind of a marker for disk
                    }
                  });
                final List<VmInstanceToken> ret = state.requestResourceAllocation( allocInfo.getVmType( ), min, max, new Supplier<VmInstanceToken>( ) {
                  private int count = 0;
                  @Override
                  public VmInstanceToken get( ) {
                    return new VmInstanceToken( allocInfo, count++ );
                  }
                } );
                allocInfo.getAllocationTokens().addAll( ret );
                return ret;
              } catch ( final NotEnoughResourcesException | AuthException e ) {
                throw Exceptions.toUndeclared( e );
              }
            }
          };

          if ( allocInfo.getAllocationType( ) == Allocations.AllocationType.Start &&
              maxAmount==1 && allocInfo.getInstanceIds( ).size( ) == 1 ) {
            RestrictedTypes.reallocateUnitlessResource( CloudMetadata.VmInstanceMetadata.class, allocator );
          } else {
            RestrictedTypes.allocateUnitlessResources(
                CloudMetadata.VmInstanceMetadata.class,
                tryAmount,
                maxAmount,
                allocator,
                allocInfo.exampleInstanceResource( maxAmount==1 ) );
          }
          return allocInfo.getAllocationTokens( );
        } finally {
          cluster.getGateLock( ).readLock( ).unlock( );
        }
      } else {
        throw new ServiceStateException( "Failed to allocate resources in the zone " + cluster.getPartition( ) + ", it is currently locked for maintenance." );
      }
    }
    
    @Override
    public void allocate( Allocation allocInfo ) throws Exception {
      Partition reqPartition = allocInfo.getPartition();
      String zoneName = reqPartition.getName( );
      VmType vmType = allocInfo.getVmType( );
      
      /* Validate min and max amount */
      final int minAmount = allocInfo.getMinCount( );
      final int maxAmount = allocInfo.getMaxCount( );
      if(minAmount > maxAmount)
    	  throw new RuntimeException("Maximum instance count must not be smaller than minimum instance count");
      
      /* Retrieve our context and list of clusters associated with this zone */
      List<Cluster> authorizedClusters = this.doPrivilegedLookup( zoneName, vmType );
      
      int remaining = maxAmount;
      int allocated = 0;
      int available;
      
      LOG.info( "Found authorized clusters: " + Iterables.transform( authorizedClusters, HasName.GET_NAME ) );
      
      /* Do we have any VM available throughout our clusters? */
      if ( ( available = checkAvailability( vmType, authorizedClusters ) ) < minAmount ) {
        throw new NotEnoughResourcesException( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances." );
      } else {
        for ( Cluster cluster : authorizedClusters ) {
          if ( remaining <= 0 ) {
            break;
          } else {
            ResourceState state = cluster.getNodeState( );
            Partition partition = cluster.getConfiguration( ).lookupPartition( );
            
            /* Has a partition been set if the AZ was not specified? */
            if( allocInfo.getPartition( ).equals( Partition.DEFAULT ) ) {
            	/* 
            	 * Ok, do we have enough slots in this partition to support our request? We should have at least
            	 * the minimum. The list is sorted in order of resource availability from the cluster with the most 
            	 * available to the cluster with the least amount available. This is why we don't check against the
            	 * maxAmount value since its a best effort at this point. If we select the partition here and we
            	 * can't fit maxAmount, based on the sorting order, the next partition will not fit maxAmount anyway. 
            	 */
            	int zoneAvailable = checkZoneAvailability( vmType, partition, authorizedClusters );
            	if( zoneAvailable < minAmount )
            	  continue;
            	
            	/* Lets use this partition */
                allocInfo.setPartition( partition );
            }
            else if( !allocInfo.getPartition( ).equals( partition ) ) {
              /* We should only pick clusters that are part of the selected AZ */
          	  continue;
            }

            if ( !RestrictedTypes.filterPrivileged( ).apply( allocInfo.exampleInstanceResource( maxAmount==1 )) ) {
              throw new IllegalMetadataAccessException( "Instance resource denied." );
            }

            if ( allocInfo.getBootSet( ).getMachine( ) instanceof BlockStorageImageInfo ) {
              try {
                Topology.lookup( Storage.class, partition );
              } catch ( Exception ex ) {
                allocInfo.abort( );
                allocInfo.setPartition( reqPartition );
                throw new NotEnoughResourcesException( "Not enough resources: Cannot run EBS instances in partition w/o a storage controller: " + ex.getMessage( ), ex );
              }
            }
            
            try {
              int tryAmount = ( remaining > state.getAvailability( vmType ).getAvailable( ) )
                ? state.getAvailability( vmType ).getAvailable( )
                : remaining;
              
              List<VmInstanceToken> tokens = this.requestResourceToken( allocInfo, tryAmount, maxAmount );
              remaining -= tokens.size( );
              allocated += tokens.size( );
            } catch ( Exception t ) {
              LOG.error( t );
              Logs.extreme( ).error( t, t );
              
              allocInfo.abort( );
              allocInfo.setPartition( reqPartition );
              
              /* if we still have some allocation remaining AND no more resources are available */
              if ( ( ( available = checkZoneAvailability( vmType, partition, authorizedClusters ) ) < remaining ) && ( remaining > 0 ) ) {
                throw new NotEnoughResourcesException( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances.", t );
              } else {
                throw new NotEnoughResourcesException( t.getMessage(), t );
              }
            }
          }
        }
        
        /* Were we able to meet our minimum requirements? */
        if ( ( allocated < minAmount) && ( remaining > 0 ) ) {
          allocInfo.abort( );
          allocInfo.setPartition( reqPartition );
          
          if( reqPartition.equals( Partition.DEFAULT ) ) {
            throw new NotEnoughResourcesException( "Not enough resources available in all zone for " + minAmount + "): vm instances." );
          }
          else {
        	available = checkZoneAvailability( vmType, reqPartition, authorizedClusters );
            throw new NotEnoughResourcesException( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances." );
          }
        }
      }
    }
    
    private int checkAvailability( VmType vmType, List<Cluster> authorizedClusters ) throws NotEnoughResourcesException {
      int available = 0;
      for ( Cluster authorizedCluster : authorizedClusters ) {
        VmTypeAvailability vmAvailability = authorizedCluster.getNodeState( ).getAvailability( vmType );
        available += vmAvailability.getAvailable( );
        LOG.info( "Availability: " + authorizedCluster.getName( ) + " -> " + vmAvailability.getAvailable( ) );
      }
      return available;
    }
    
    private int checkZoneAvailability( VmType vmType, Partition partition, List<Cluster> authorizedClusters ) throws NotEnoughResourcesException {
      int available = 0;
      for ( Cluster authorizedCluster : authorizedClusters ) {
    	if( !authorizedCluster.getConfiguration( ).lookupPartition( ).equals( partition ) )
    		continue;
    	
        VmTypeAvailability vmAvailability = authorizedCluster.getNodeState( ).getAvailability( vmType );
        available += vmAvailability.getAvailable( );
        LOG.info( "Availability: " + authorizedCluster.getName( ) + " -> " + vmAvailability.getAvailable( ) );
      }
      return available;
    }
      
    private List<Cluster> doPrivilegedLookup( String partitionName, VmType vmType ) throws NotEnoughResourcesException {
      if ( Partition.DEFAULT_NAME.equals( partitionName ) ) {
        Iterable<Cluster> authorizedClusters = Clusters.stream( ).filter( RestrictedTypes.filterPrivilegedWithoutOwner( ) );
        Multimap<VmTypeAvailability, Cluster> sorted = TreeMultimap.create( );
        for ( Cluster c : authorizedClusters ) {
          sorted.put( c.getNodeState( ).getAvailability( vmType ), c );
        }
        if ( sorted.isEmpty( ) ) {
          throw new NotEnoughResourcesException( "Not enough resources: no availability zone is available in which you have permissions to run instances." );
        } else {
          return Lists.newArrayList( sorted.values( ) );
        }
      } else {
        ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, Partitions.lookupByName( partitionName ) );
        Cluster cluster = Clusters.lookupAny( ccConfig );
        if ( cluster == null ) {
          throw new NotEnoughResourcesException( "Can't find cluster " + partitionName );
        }
        if ( ! RestrictedTypes.filterPrivilegedWithoutOwner( ).apply( cluster ) ) {
          throw new NotEnoughResourcesException( "Not authorized to use cluster " + partitionName );
        }
        return Lists.newArrayList( cluster );
      }
    }
    
    @Override
    public void fail( Allocation allocInfo, Throwable t ) {
      allocInfo.abort( );
    }
    
  }
  
  enum NetworkingAllocator implements ResourceAllocator {
    INSTANCE;

    @Override
    public void allocate( Allocation allocInfo ) throws Exception {
      try {
        final VmInstanceLifecycleHelper helper = VmInstanceLifecycleHelper.get( );

        final PrepareNetworkResourcesType request = new PrepareNetworkResourcesType( );
        request.setAvailabilityZone( allocInfo.getPartition( ).getName( ) );
        request.setFeatures( Lists.<NetworkFeature>newArrayList( new DnsHostNamesFeature( ) ) );
        helper.prepareNetworkAllocation( allocInfo, request );
        final PrepareNetworkResourcesResultType result = Networking.getInstance().prepare( request ) ;

        for ( final VmInstanceToken token : allocInfo.getAllocationTokens( ) ) {
          for ( final NetworkResource networkResource : result.getResources( ) ) {
            if ( token.getInstanceId( ).equals( networkResource.getOwnerId( ) ) ) {
              token.getAttribute( VmInstanceLifecycleHelper.NetworkResourcesKey ).add( networkResource );
            }
          }
        }

        helper.verifyNetworkAllocation( allocInfo, result );
      } catch ( Exception e ) {
        throw MoreObjects.firstNonNull( Exceptions.findCause( e, NotEnoughResourcesException.class ), e );
      }
    }
    
    @Override
    public void fail( Allocation allocInfo, Throwable t ) {
      allocInfo.abort( );
    }
  }
}
