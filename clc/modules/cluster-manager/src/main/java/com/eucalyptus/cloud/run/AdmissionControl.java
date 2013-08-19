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

package com.eucalyptus.cloud.run;

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.ResourceState;
import com.eucalyptus.cluster.ResourceState.VmTypeAvailability;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceStateException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransientEntityException;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.network.ExtantNetwork;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.network.PrivateNetworkIndex;
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
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

public class AdmissionControl {
  private static Logger LOG = Logger.getLogger( AdmissionControl.class );
  
  public static Predicate<Allocation> run( ) {
    return RunAdmissionControl.INSTANCE;
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
  
  private static final List<ResourceAllocator> allocators = new ArrayList<ResourceAllocator>( ) {
                                                         {
                                                           this.add( NodeResourceAllocator.INSTANCE );
                                                           this.add( VmTypePrivAllocator.INSTANCE );
                                                           this.add( PublicAddressAllocator.INSTANCE );
                                                           this.add( PrivateNetworkAllocator.INSTANCE );
                                                           this.add( SubnetIndexAllocator.INSTANCE );
                                                         }
                                                       };
  
  enum VmTypePrivAllocator implements ResourceAllocator {
    INSTANCE;
    
    @SuppressWarnings( "unchecked" )
    @Override
    public void allocate( Allocation allocInfo ) throws Exception {
      RestrictedTypes.allocateNamedUnitlessResources( allocInfo.getAllocationTokens( ).size( ),
                                                      allocInfo.getVmType( ).allocator( ),
                                                      ( Predicate ) Predicates.alwaysTrue( ) );
    }
    
    @Override
    public void fail( Allocation allocInfo, Throwable t ) {}
    
  }
  
  enum NodeResourceAllocator implements ResourceAllocator {
    INSTANCE;
    private List<ResourceToken> requestResourceToken( final Allocation allocInfo, final int tryAmount, final int maxAmount ) throws Exception {
      ServiceConfiguration config = Topology.lookup( ClusterController.class, allocInfo.getPartition( ) );
      Cluster cluster = Clusters.lookup( config );
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
          final List<ResourceToken> tokens = state.requestResourceAllocation( allocInfo, tryAmount, maxAmount );
          final Iterator<ResourceToken> tokenIterator = tokens.iterator( );
          try {
            final Supplier<ResourceToken> allocator = new Supplier<ResourceToken>( ) {
              @Override
              public ResourceToken get( ) {
                final ResourceToken ret = tokenIterator.next( );
                allocInfo.getAllocationTokens( ).add( ret );
                return ret;
              }
            };

            RestrictedTypes.allocateUnitlessResources( tokens.size( ), allocator );
          } finally {
            // release any tokens that were not allocated
            Iterators.all( tokenIterator, new Predicate<ResourceToken>() {
              @Override
              public boolean apply(final ResourceToken resourceToken) {
                state.releaseToken( resourceToken );
                return true;
              }
            } );
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
      RunInstancesType request = allocInfo.getRequest( );
      Partition reqPartition = allocInfo.getPartition();
      String zoneName = reqPartition.getName( );
      String vmTypeName = allocInfo.getVmType( ).getName( );
      
      /* Validate min and max amount */
      final int minAmount = allocInfo.getMinCount( );
      final int maxAmount = allocInfo.getMaxCount( );
      if(minAmount > maxAmount)
    	  throw new RuntimeException("Maximum instance count must not be smaller than minimum instance count");
      
      /* Retrieve our context and list of clusters associated with this zone */
      Context ctx = Contexts.lookup( );
      List<Cluster> authorizedClusters = this.doPrivilegedLookup( zoneName, vmTypeName );
      
      int remaining = maxAmount;
      int allocated = 0;
      int available = 0;
      
      LOG.info( "Found authorized clusters: " + Iterables.transform( authorizedClusters, HasName.GET_NAME ) );
      
      /* Do we have any VM available throughout our clusters? */
      if ( ( available = checkAvailability( vmTypeName, authorizedClusters ) ) < minAmount ) {
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
            	int zoneAvailable = checkZoneAvailability( vmTypeName, partition, authorizedClusters );
            	if( zoneAvailable < minAmount )
            	  continue;
            	
            	/* Lets use this partition */
                allocInfo.setPartition( partition );
            }
            else if( !allocInfo.getPartition( ).equals( partition ) ) {
              /* We should only pick clusters that are part of the selected AZ */
          	  continue;
            }
            
            if ( allocInfo.getBootSet( ).getMachine( ) instanceof BlockStorageImageInfo ) {
              try {
                ServiceConfiguration sc = Topology.lookup( Storage.class, partition );
              } catch ( Exception ex ) {
                allocInfo.abort( );
                allocInfo.setPartition( reqPartition );
                throw new NotEnoughResourcesException( "Not enough resources: Cannot run EBS instances in partition w/o a storage controller: " + ex.getMessage( ), ex );
              }
            }
            
            try {
              int tryAmount = ( remaining > state.getAvailability( vmTypeName ).getAvailable( ) )
                ? state.getAvailability( vmTypeName ).getAvailable( )
                : remaining;
              
              List<ResourceToken> tokens = this.requestResourceToken( allocInfo, tryAmount, maxAmount );
              remaining -= tokens.size( );
              allocated += tokens.size( );
            } catch ( Exception t ) {
              LOG.error( t );
              Logs.extreme( ).error( t, t );
              
              allocInfo.abort( );
              allocInfo.setPartition( reqPartition );
              
              /* if we still have some allocation remaining AND no more resources are available */
              if ( ( ( available = checkZoneAvailability( vmTypeName, partition, authorizedClusters ) ) < remaining ) && ( remaining > 0 ) ) {
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
        	available = checkZoneAvailability( vmTypeName, reqPartition, authorizedClusters );
            throw new NotEnoughResourcesException( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances." );
          }
        }
      }
    }
    
    private int checkAvailability( String vmTypeName, List<Cluster> authorizedClusters ) throws NotEnoughResourcesException {
      int available = 0;
      for ( Cluster authorizedCluster : authorizedClusters ) {
        VmTypeAvailability vmAvailability = authorizedCluster.getNodeState( ).getAvailability( vmTypeName );
        available += vmAvailability.getAvailable( );
        LOG.info( "Availability: " + authorizedCluster.getName( ) + " -> " + vmAvailability.getAvailable( ) );
      }
      return available;
    }
    
    private int checkZoneAvailability( String vmTypeName, Partition partition, List<Cluster> authorizedClusters ) throws NotEnoughResourcesException {
      int available = 0;
      for ( Cluster authorizedCluster : authorizedClusters ) {
    	if( !authorizedCluster.getConfiguration( ).lookupPartition( ).equals( partition ) )
    		continue;
    	
        VmTypeAvailability vmAvailability = authorizedCluster.getNodeState( ).getAvailability( vmTypeName );
        available += vmAvailability.getAvailable( );
        LOG.info( "Availability: " + authorizedCluster.getName( ) + " -> " + vmAvailability.getAvailable( ) );
      }
      return available;
    }
      
    private List<Cluster> doPrivilegedLookup( String partitionName, String vmTypeName ) throws NotEnoughResourcesException {
      if ( Partition.DEFAULT_NAME.equals( partitionName ) ) {
        Iterable<Cluster> authorizedClusters = Iterables.filter( Clusters.getInstance( ).listValues( ), RestrictedTypes.filterPrivilegedWithoutOwner( ) );
        Multimap<VmTypeAvailability, Cluster> sorted = TreeMultimap.create( );
        for ( Cluster c : authorizedClusters ) {
          sorted.put( c.getNodeState( ).getAvailability( vmTypeName ), c );
        }
        if ( sorted.isEmpty( ) ) {
          throw new NotEnoughResourcesException( "Not enough resources: no availability zone is available in which you have permissions to run instances." );
        } else {
          return Lists.newArrayList( sorted.values( ) );
        }
      } else {
        ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, Partitions.lookupByName( partitionName ) );
        Cluster cluster = Clusters.lookup( ccConfig );
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
  
  enum PublicAddressAllocator implements ResourceAllocator {
    INSTANCE;

    @Override
    public void allocate( Allocation allocInfo ) throws Exception {
      if ( NetworkGroups.networkingConfiguration( ).hasNetworking( ) && !allocInfo.isUsePrivateAddressing() ) {
        for ( ResourceToken token : allocInfo.getAllocationTokens( ) ) {
          token.setAddress( Addresses.allocateSystemAddress( token.getAllocationInfo( ).getPartition( ) ) );
        }
      }
    }
    
    @Override
    public void fail( Allocation allocInfo, Throwable t ) {
      allocInfo.abort( );
    }
  }
  
  enum PrivateNetworkAllocator implements ResourceAllocator {
    INSTANCE;
    
    @Override
    public void allocate( Allocation allocInfo ) throws Exception {
      if ( NetworkGroups.networkingConfiguration( ).hasNetworking( ) ) {
        EntityTransaction db = Entities.get( NetworkGroup.class );
        try {
          NetworkGroup net = Entities.merge( allocInfo.getPrimaryNetwork( ) );
          ExtantNetwork exNet = net.extantNetwork( );
          for ( ResourceToken rscToken : allocInfo.getAllocationTokens( ) ) {
            rscToken.setExtantNetwork( exNet );
          }
          Entities.merge( net );//GRZE:TODO: update allocInfo w/ persisted version.
          db.commit( );
        } catch ( TransientEntityException ex ) {
          LOG.error( ex, ex );
          db.rollback( );
          throw ex;
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          db.rollback( );
          throw ex;
        }
      }
    }
    
    @Override
    public void fail( Allocation allocInfo, Throwable t ) {
      allocInfo.abort( );
    }
  }
  
  enum SubnetIndexAllocator implements ResourceAllocator {
    INSTANCE;
    
    @Override
    public void allocate( Allocation allocInfo ) throws Exception {
      if ( NetworkGroups.networkingConfiguration( ).hasNetworking( ) ) {
        for ( ResourceToken rscToken : allocInfo.getAllocationTokens( ) ) {
          EntityTransaction db = Entities.get( ExtantNetwork.class );
          try {
            ExtantNetwork exNet = Entities.merge( rscToken.getExtantNetwork( ) );
            checkParam( exNet, notNullValue() );
            PrivateNetworkIndex addrIndex = exNet.allocateNetworkIndex( );
            rscToken.setNetworkIndex( addrIndex );
            rscToken.setExtantNetwork( Entities.merge( exNet ) );
            db.commit( );
          } catch ( Exception ex ) {
            db.rollback( );
            throw new NotEnoughResourcesException( "Not enough addresses left in the private network subnet assigned to requested group: " + rscToken, ex );
          }
        }
      }
    }
    
    @Override
    public void fail( Allocation allocInfo, Throwable t ) {
      allocInfo.abort( );
    }
  }
}
