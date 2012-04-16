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
 *******************************************************************************
 * @author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cloud.run;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Addresses;
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
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
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
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
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
      EventRecord.here( AdmissionControl.class, EventType.VM_RESERVED, LogUtil.dumpObject( allocInfo ) ).trace( );
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
    for ( ResourceAllocator rollback : Iterables.reverse( finished ) ) {
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
      final ResourceState state = cluster.getNodeState( );
      final List<ResourceToken> tokens = state.requestResourceAllocation( allocInfo, tryAmount, maxAmount );
      final Supplier<ResourceToken> allocator = new Supplier<ResourceToken>( ) {
        Iterator<ResourceToken> iter = tokens.iterator( );
        
        @Override
        public ResourceToken get( ) {
          ResourceToken ret = this.iter.next( );
          allocInfo.getAllocationTokens( ).add( ret );
          return ret;
        }
      };
      RestrictedTypes.allocateUnitlessResources( tokens.size( ), allocator );
      return allocInfo.getAllocationTokens( );
    }
    
    @Override
    public void allocate( Allocation allocInfo ) throws Exception {
      RunInstancesType request = allocInfo.getRequest( );
      String clusterName = allocInfo.getPartition( ).getName( );
      String vmTypeName = allocInfo.getVmType( ).getName( );
      final int minAmount = allocInfo.getMinCount( );
      final int maxAmount = allocInfo.getMaxCount( );
      Context ctx = Contexts.lookup( );
      String zoneName = ( clusterName != null )
        ? clusterName
        : "default";
      List<Cluster> authorizedClusters = this.doPrivilegedLookup( zoneName, vmTypeName );
      int remaining = maxAmount;
      int available = 0;
      LOG.info( "Found authorized clusters: " + Iterables.transform( authorizedClusters, HasName.GET_NAME ) );
      if ( ( available = checkAvailability( vmTypeName, authorizedClusters ) ) < minAmount ) {
        throw new NotEnoughResourcesException( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances." );
      } else {
        for ( Cluster cluster : authorizedClusters ) {
          if ( remaining <= 0 ) {
            break;
          } else {
            ResourceState state = cluster.getNodeState( );
            Partition partition = cluster.getConfiguration( ).lookupPartition( );
            if ( allocInfo.getBootSet( ).getMachine( ) instanceof BlockStorageImageInfo ) {
              try {
                ServiceConfiguration sc = Topology.lookup( Storage.class, partition );
              } catch ( Exception ex ) {
                throw new NotEnoughResourcesException( "Not enough resources: Cannot run EBS instances in partition w/o a storage controller: " + ex.getMessage( ), ex );
              }
            }
            try {
              int tryAmount = ( remaining > state.getAvailability( vmTypeName ).getAvailable( ) )
                ? state.getAvailability( vmTypeName ).getAvailable( )
                : remaining;
              
              List<ResourceToken> tokens = this.requestResourceToken( allocInfo, tryAmount, maxAmount );
              remaining -= tokens.size( );
              allocInfo.setPartition( partition );
            } catch ( Exception t ) {
              LOG.error( t );
              Logs.extreme( ).error( t, t );
              if ( ( ( available = checkAvailability( vmTypeName, authorizedClusters ) ) < remaining ) || remaining > 0 ) {
                allocInfo.abort( );
                throw new NotEnoughResourcesException( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances.", t );
              } else {
                throw new NotEnoughResourcesException( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances.", t );
              }
            }
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
    
    private List<Cluster> doPrivilegedLookup( String partitionName, String vmTypeName ) throws NotEnoughResourcesException {
      if ( "default".equals( partitionName ) ) {
        Iterable<Cluster> authorizedClusters = Iterables.filter( Clusters.getInstance( ).listValues( ), RestrictedTypes.filterPrivileged( ) );
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
            assertThat( exNet, notNullValue( ) );
            PrivateNetworkIndex addrIndex = exNet.allocateNetworkIndex( );
            rscToken.setNetworkIndex( addrIndex );
            rscToken.setExtantNetwork( Entities.merge( exNet ) );
            db.commit( );
          } catch ( Exception ex ) {
            db.rollback( );
            throw new NotEnoughResourcesException( "Not enough addresses left in the network subnet assigned to requested group: " + rscToken, ex );
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
