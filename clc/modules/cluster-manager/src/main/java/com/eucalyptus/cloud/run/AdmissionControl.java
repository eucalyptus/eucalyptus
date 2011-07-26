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
package com.eucalyptus.cloud.run;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterNodeState;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.VmTypeAvailability;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

public class AdmissionControl {
  private static Logger LOG = Logger.getLogger( AdmissionControl.class );
  
  public interface ResourceAllocator {
    public void allocate( Allocation allocInfo ) throws Exception;
    
    public void fail( Allocation allocInfo, Throwable t );
    
  }
  
  private static final List<ResourceAllocator> pending = new ArrayList<ResourceAllocator>( ) {
                                                         {
                                                           this.add( NodeResourceAllocator.INSTANCE );
                                                           this.add( PublicAddressAllocator.INSTANCE );
                                                           this.add( PrivateNetworkAllocator.INSTANCE );
                                                           this.add( SubnetIndexAllocator.INSTANCE );
                                                         }
                                                       };
  
  public Allocation evaluate( Allocation allocInfo ) throws EucalyptusCloudException {
    List<ResourceAllocator> finished = Lists.newArrayList( );
    
    for ( ResourceAllocator allocator : pending ) {
      try {
        allocator.allocate( allocInfo );
        finished.add( allocator );
      } catch ( ScriptExecutionFailedException e ) {
        if ( e.getCause( ) != null ) {
          throw new EucalyptusCloudException( e.getCause( ).getMessage( ), e.getCause( ) );
        } else {
          throw new EucalyptusCloudException( e.getMessage( ), e );
        }
      } catch ( Throwable e ) {
        LOG.debug( e, e );
        try {
          allocator.fail( allocInfo, e );
        } catch ( Throwable e1 ) {
          LOG.debug( e1, e1 );
        }
        for ( ResourceAllocator rollback : Iterables.reverse( finished ) ) {
          try {
            rollback.fail( allocInfo, e );
          } catch ( Throwable e1 ) {
            LOG.debug( e1, e1 );
          }
        }
        throw new EucalyptusCloudException( e.getMessage( ), e );
      }
    }
    EventRecord.here( this.getClass( ), EventType.VM_RESERVED, LogUtil.dumpObject( allocInfo ) ).trace( );
    return allocInfo;
  }
  
  enum NodeResourceAllocator implements ResourceAllocator {
    INSTANCE;
    @Override
    public void allocate( Allocation allocInfo ) throws Exception {
      RunInstancesType request = allocInfo.getRequest( );
      String clusterName = request.getAvailabilityZone( );
      String vmTypeName = request.getInstanceType( );
      final int minAmount = request.getMinCount( );
      final int maxAmount = request.getMaxCount( );
      Context ctx = Contexts.lookup( );
      //if ( ctx.getGroups( ).isEmpty( ) ) {
      if ( false ) {
        throw new NotEnoughResourcesAvailable( "Not authorized: you do not have sufficient permission to use " + clusterName );
      } else {
        String zoneName = ( clusterName != null )
          ? clusterName
          : "default";
        String action = PolicySpec.requestToAction( request );
        User requestUser = ctx.getUser( );
        List<Cluster> authorizedClusters = this.doPrivilegedLookup( zoneName, vmTypeName, action, requestUser );
        int remaining = maxAmount;
        int available = 0;
        LOG.info( "Found authorized clusters: " + Iterables.transform( authorizedClusters, new Function<Cluster, String>( ) {
          @Override
          public String apply( Cluster arg0 ) {
            return arg0.getName( );
          }
        } ) );
        if ( ( available = checkAvailability( vmTypeName, authorizedClusters ) ) < minAmount ) {
          throw new NotEnoughResourcesAvailable( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances." );
        } else {
          for ( Cluster cluster : authorizedClusters ) {
            if ( remaining <= 0 ) {
              break;
            } else {
              ClusterNodeState state = cluster.getNodeState( );
              Partition partition = cluster.getConfiguration( ).lookupPartition( );
              if ( allocInfo.getBootSet( ).getMachine( ) instanceof BlockStorageImageInfo ) {
                try {
                  ServiceConfiguration sc = Partitions.lookupService( Storage.class, partition );
                } catch ( Exception ex ) {
                  throw new NotEnoughResourcesAvailable( "Not enough resources: " + ex.getMessage( ), ex );
                }
              }
              try {
                int tryAmount = ( remaining > state.getAvailability( vmTypeName ).getAvailable( ) )
                  ? state.getAvailability( vmTypeName ).getAvailable( )
                  : remaining;
                
                ResourceToken token = allocInfo.requestResourceToken( state, vmTypeName, tryAmount, maxAmount );
                remaining -= token.getAmount( );
              } catch ( Throwable t ) {
                if ( ( ( available = checkAvailability( vmTypeName, authorizedClusters ) ) < remaining ) || remaining > 0 ) {
                  allocInfo.releaseAllocationTokens( );
                  throw new NotEnoughResourcesAvailable( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances." );
                } else {
                  LOG.error( t, t );
                  throw new NotEnoughResourcesAvailable( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances." );
                }
              }
            }
          }
        }
      }
    }
    
    private int checkAvailability( String vmTypeName, List<Cluster> authorizedClusters ) throws NotEnoughResourcesAvailable {
      int available = 0;
      for ( Cluster authorizedCluster : authorizedClusters ) {
        VmTypeAvailability vmAvailability = authorizedCluster.getNodeState( ).getAvailability( vmTypeName );
        available += vmAvailability.getAvailable( );
        LOG.info( "Availability: " + authorizedCluster.getName( ) + " -> " + vmAvailability.getAvailable( ) );
      }
      return available;
    }
    
    private List<Cluster> doPrivilegedLookup( String partitionName, String vmTypeName, final String action, final User requestUser ) throws NotEnoughResourcesAvailable {
      if ( "default".equals( partitionName ) ) {
        Iterable<Cluster> authorizedClusters = Iterables.filter( Clusters.getInstance( ).listValues( ), new Predicate<Cluster>( ) {
          @Override
          public boolean apply( final Cluster c ) {
            try {
              return Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_AVAILABILITYZONE, c.getName( ), null, action, requestUser );
            } catch ( Exception e ) {}
            return false;
          }
        } );
        Multimap<VmTypeAvailability, Cluster> sorted = TreeMultimap.create( );
        for ( Cluster c : authorizedClusters ) {
          sorted.put( c.getNodeState( ).getAvailability( vmTypeName ), c );
        }
        if ( sorted.isEmpty( ) ) {
          throw new NotEnoughResourcesAvailable( "Not enough resources: no availability zone is available in which you have permissions to run instances." );
        } else {
          return Lists.newArrayList( sorted.values( ) );
        }
      } else {
        Cluster cluster = Clusters.getInstance( ).lookup( Partitions.lookupService( ClusterController.class, partitionName ) );
        if ( cluster == null ) {
          throw new NotEnoughResourcesAvailable( "Can't find cluster " + partitionName );
        }
        if ( !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_AVAILABILITYZONE, partitionName, null, action, requestUser ) ) {
          throw new NotEnoughResourcesAvailable( "Not authorized to use cluster " + partitionName + " for " + requestUser.getName( ) );
        }
        return Lists.newArrayList( cluster );
      }
    }
    
    @Override
    public void fail( Allocation allocInfo, Throwable t ) {
      allocInfo.releaseAllocationTokens( );
    }
    
  }
  
  enum PublicAddressAllocator implements ResourceAllocator {
    INSTANCE;
    
    @Override
    public void allocate( Allocation allocInfo ) throws Exception {
      if ( Clusters.getInstance( ).hasNetworking( ) ) {
        allocInfo.requestAddressTokens( );
      }
    }
    
    @Override
    public void fail( Allocation allocInfo, Throwable t ) {
      allocInfo.releaseAddressTokens( );
    }
  }
  
  enum PrivateNetworkAllocator implements ResourceAllocator {
    INSTANCE;
    
    @Override
    public void allocate( Allocation allocInfo ) throws Exception {
      if ( Clusters.getInstance( ).hasNetworking( ) ) {
        allocInfo.requestNetworkTokens( );
      }
    }
    
    @Override
    public void fail( Allocation allocInfo, Throwable t ) {
      allocInfo.releaseNetworkAllocationTokens( );
    }
  }
  
  enum SubnetIndexAllocator implements ResourceAllocator {
    INSTANCE;
    
    @Override
    public void allocate( Allocation allocInfo ) throws Exception {
      if ( Clusters.getInstance( ).hasNetworking( ) ) {
        allocInfo.requestNetworkIndexes( );
      }
    }
    
    @Override
    public void fail( Allocation allocInfo, Throwable t ) {
      allocInfo.releaseNetworkIndexes( );
    }
  }
}
