/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cluster.callback;

import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.compute.common.network.InstanceResourceReportType;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.UpdateInstanceResourcesType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.util.async.SubjectMessageCallback;
import com.eucalyptus.vm.VmBundleTask.BundleState;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstance.VmStateSet;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmInstances.TerminatedInstanceException;
import com.eucalyptus.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmDescribeType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class VmStateCallback extends StateUpdateMessageCallback<Cluster, VmDescribeType, VmDescribeResponseType> {
  private static Logger               LOG                       = Logger.getLogger( VmStateCallback.class );
  private final Supplier<Set<String>> initialInstances;
  
  public VmStateCallback( ) {
    super( new VmDescribeType( ) {
      {
        regarding( );
      }
    } );
    this.initialInstances = createInstanceSupplier( this, partitionFilter( this ) );
  }
  
  private static Supplier<Set<String>> createInstanceSupplier( final StateUpdateMessageCallback<Cluster, ?, ?> cb, final Predicate<VmInstance> filter ) {
    return Suppliers.memoize( new Supplier<Set<String>>( ) {
      
      @Override
      public Set<String> get( ) {
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          Collection<VmInstance> clusterInstances =  VmInstances.list( null, Restrictions.conjunction( ), Collections.<String,String>emptyMap( ), filter );
          Collection<String> instanceNames = Collections2.transform( clusterInstances, CloudMetadatas.toDisplayName( ) );
          return Sets.newHashSet( instanceNames );
        } catch ( Exception ex ) {
          Logs.extreme( ).error( ex, ex );
          return Sets.newHashSet( );
        } finally {
          db.rollback();
        }
      }
    } );
  }
  
  /**
   * @see com.eucalyptus.cluster.callback.StateUpdateMessageCallback#fireException(com.eucalyptus.util.async.FailedRequestException)
   * @param t
   */
  @Override
  public void fireException( FailedRequestException t ) {
    LOG.debug( "Request to " + this.getSubject( ).getName( ) + " failed: " + t.getMessage( ) );
  }
  
  @Override
  public void fire( VmDescribeResponseType reply ) {
    UpdateInstanceResourcesType update = new UpdateInstanceResourcesType( );
    update.setPartition( this.getSubject().getPartition() );
    update.setResources( TypeMappers.transform( reply, InstanceResourceReportType.class ) );
    Networking.getInstance( ).update( update );

    if ( Databases.isVolatile( ) ) {
      return;
    } else {
      reply.setOriginCluster( this.getSubject( ).getConfiguration( ).getName( ) );
      final Set<String> reportedInstances = Sets.newHashSet( );
      for ( VmInfo vmInfo : reply.getVms( ) ) {
        reportedInstances.add( vmInfo.getInstanceId( ) );
        vmInfo.setPlacement( this.getSubject( ).getConfiguration( ).getName( ) );
        VmTypeInfo typeInfo = vmInfo.getInstanceType( );
        if ( typeInfo.getName( ) == null || "".equals( typeInfo.getName( ) ) ) {
          for ( VmType t : VmTypes.list( ) ) {
            if ( t.getCpu( ).equals( typeInfo.getCores( ) ) && t.getDisk( ).equals( typeInfo.getDisk( ) ) && t.getMemory( ).equals( typeInfo.getMemory( ) ) ) {
              typeInfo.setName( t.getName( ) );
            }
          }
        }
      }
      
      final Set<String> unreportedInstances = Sets.newHashSet( Sets.difference( this.initialInstances.get( ), reportedInstances ) );
      final Set<String> restoreInstances = Sets.newHashSet( Sets.difference( reportedInstances, this.initialInstances.get( ) ) );
      for ( final VmInfo runVm : reply.getVms( ) ) {
        if ( Databases.isVolatile( ) ) {
          return;
        } else if ( this.initialInstances.get( ).contains( runVm.getInstanceId( ) ) ) {
          VmStateCallback.handleReportedState( runVm );
        } else if ( restoreInstances.contains( runVm.getInstanceId( ) ) ) {
          VmStateCallback.handleRestore( runVm );
        }
      }
      for ( final String vmId : unreportedInstances ) {
        if ( Databases.isVolatile( ) ) {
          return;
        } else {
          VmStateCallback.handleUnreported( vmId );
        }
      }
    }
  }
  
  private static void handleUnreported( final String vmId ) {
    try {
      VmInstance vm = VmInstances.lookupAny( vmId );
      if ( VmState.PENDING.apply( vm ) && vm.lastUpdateMillis( ) < ( VmInstances.VM_INITIAL_REPORT_TIMEOUT * 1000 ) ) {
        //do nothing during first VM_INITIAL_REPORT_TIMEOUT millis of instance life
        return;
      } else if ( vm.isBlockStorage( ) && VmInstances.Timeout.UNREPORTED.apply( vm ) ) {
        VmInstances.stopped( vm );
      } else if ( VmState.STOPPING.apply( vm ) ) {
        VmInstances.stopped( vm );
      } else if ( VmState.SHUTTING_DOWN.apply( vm ) ) {
        VmInstances.terminated( vm );
      } else if ( VmInstances.Timeout.TERMINATED.apply( vm ) ) {
        VmInstances.buried( vm );
      } else if ( VmInstances.Timeout.BURIED.apply( vm ) ) {
        VmInstances.delete( vm );
      } else if ( VmInstances.Timeout.SHUTTING_DOWN.apply( vm ) ) {
        VmInstances.terminated( vm );
      } else if ( VmInstances.Timeout.STOPPING.apply( vm ) ) {
        VmInstances.stopped( vm );
      } else if ( VmInstances.Timeout.UNREPORTED.apply( vm ) ) {
        VmInstances.terminated( vm );
      } else {
        return;
      }
    } catch ( final Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }
  }
  
  private static void handleReportedState( final VmInfo runVm ) {
    final VmState runVmState = VmState.Mapper.get( runVm.getStateName( ) );
    try {
      final EntityTransaction db = Entities.get( VmInstance.class );
      try {
        VmInstance vm = VmInstances.lookupAny( runVm.getInstanceId() );
        if ( VmStateSet.DONE.apply( vm ) ) {
          db.rollback( );
          if ( VmInstance.Reason.EXPIRED.apply( vm ) ) {
            VmStateCallback.handleRestore( runVm );
          } else {
            LOG.trace( "Ignore state update to terminated instance: " + runVm.getInstanceId( ) );
          }
          return;
        }

        if ( VmInstances.Timeout.EXPIRED.apply( vm ) ) {
          if ( vm.isBlockStorage( ) ) {
            VmInstances.stopped( vm );
          } else {
            VmInstances.shutDown( vm );
          }
        } else if ( VmState.SHUTTING_DOWN.equals( runVmState ) ) {
          db.rollback();
          VmStateCallback.handleReportedTeardown( vm, runVm );
          return;
        } else if ( VmStateSet.RUN.apply( vm ) ) {
          vm.doUpdate( ).apply( runVm );
        } else if ( !VmStateSet.RUN.apply( vm ) && VmStateSet.RUN.contains( runVmState )
                    && vm.lastUpdateMillis( ) > ( VmInstances.VOLATILE_STATE_TIMEOUT_SEC * 1000l ) ) {
          vm.doUpdate( ).apply( runVm );
        } else {
          return;
        }
        Entities.commit( db );
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
        throw ex;
      } finally {
        if ( db.isActive() ) db.rollback();
      }
    } catch ( TerminatedInstanceException ex1 ) {
      LOG.trace( "Ignore state update to terminated instance: " + runVm.getInstanceId( ) );
    } catch ( NoSuchElementException ex1 ) {
//      VmStateCallback.handleRestore( runVm );
    } catch ( Exception ex1 ) {
      LOG.error( ex1 );
      Logs.extreme( ).error( ex1, ex1 );
    }
  }
  
  private static boolean handleRestore( final VmInfo runVm ) {
    final VmState runVmState = VmState.Mapper.get( runVm.getStateName( ) );
    if ( VmStateSet.RUN.contains( runVmState ) ) {
      try {
        final VmInstance vm = VmInstances.lookupAny( runVm.getInstanceId() );
        if ( vm != null &&
            !( VmStateSet.DONE.apply( vm ) && VmInstance.Reason.EXPIRED.apply( vm ) ) ) {
          return false;
        }
      } catch ( NoSuchElementException ex ) {
        LOG.debug( "Instance record not found for restore: " + runVm.getInstanceId( ) );
        Logs.extreme( ).error( ex, ex );
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      }
      try {
        LOG.debug( "Instance " + runVm.getInstanceId( ) + " " + runVm );
        return VmInstance.RestoreAllocation.INSTANCE.apply( runVm );
      } catch ( Throwable ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      }
    }
    return false;
  }
  
  private static void handleReportedTeardown( VmInstance vm, final VmInfo runVm ) throws TransactionException {
    /**
     * TODO:GRZE: based on current local instance state we need to handle reported
     * SHUTTING_DOWN state differently
     **/
    BundleState bundleState = BundleState.mapper.apply( runVm.getBundleTaskStateName( ) );
    if ( !BundleState.none.equals( bundleState ) ) {
      vm.getRuntimeState( ).updateBundleTaskState( bundleState );
      VmInstances.terminated( vm );
    } else if ( VmState.SHUTTING_DOWN.apply( vm ) ) {
      VmInstances.terminated( vm );
    } else if ( VmState.STOPPING.apply( vm ) ) {
      VmInstances.stopped( vm );
    } else if ( VmStateSet.RUN.apply( vm ) && vm.getSplitTime( ) > ( VmInstances.VM_STATE_SETTLE_TIME * 1000 ) ) {
      if ( vm.isBlockStorage( ) ) {
        VmInstances.stopped( vm );
      } else {
        VmInstances.shutDown( vm );
      }
    }
  }
  
  private static Predicate<VmInstance> stateSettleFilter( ) {
    return new Predicate<VmInstance>( ) {
      
      @Override
      public boolean apply( VmInstance input ) {
        return input.getCreationSplitTime( ) > ( VmInstances.VM_STATE_SETTLE_TIME * 1000 );
      }
    };
  }
  
  private static Predicate<VmInstance> partitionFilter( final SubjectMessageCallback<Cluster, ?, ?> cb ) {
    return new Predicate<VmInstance>( ) {
      
      @Override
      public boolean apply( VmInstance arg0 ) {
        return arg0.getPartition( ).equals( cb.getSubject( ).getConfiguration( ).getPartition( ) );
      }
    };
  }
  
  public static class VmPendingCallback extends StateUpdateMessageCallback<Cluster, VmDescribeType, VmDescribeResponseType> {
    @SuppressWarnings( "unchecked" )
    private final Predicate<VmInstance> filter = Predicates.and( VmStateSet.TORNDOWN.not( ), stateSettleFilter( ), partitionFilter( this ) );
    private final Supplier<Set<String>> initialInstances;
    
    public VmPendingCallback( Cluster cluster ) {
      super( cluster );
      this.initialInstances = createInstanceSupplier( this, this.filter );
      this.setRequest( new VmDescribeType( ) {
        {
          regarding( );
          this.getInstancesSet( ).addAll( VmPendingCallback.this.initialInstances.get( ) );
        }
      } );
      if ( this.getRequest( ).getInstancesSet( ).isEmpty( ) ) {
        throw new CancellationException( );
      }
    }
    
    @Override
    public void fire( VmDescribeResponseType reply ) {
      for ( final VmInfo runVm : reply.getVms( ) ) {
        if ( Databases.isVolatile( ) ) {
          return;
        } else if ( this.initialInstances.get( ).contains( runVm.getInstanceId( ) ) ) {
          VmStateCallback.handleReportedState( runVm );
        }
      }
    }
    
    /**
     * @see com.eucalyptus.cluster.callback.StateUpdateMessageCallback#fireException(com.eucalyptus.util.async.FailedRequestException)
     * @param t
     */
    @Override
    public void fireException( FailedRequestException t ) {
      LOG.debug( "Request to " + this.getSubject( ).getName( ) + " failed: " + t.getMessage( ) );
    }
    
  }
  
  @Override
  public void setSubject( Cluster subject ) {
    super.setSubject( subject );
    this.initialInstances.get( );
  }

  @TypeMapper
  public enum VmDescribeResponseTypeToInstanceResourceReport implements Function<VmDescribeResponseType,InstanceResourceReportType> {
    INSTANCE;

    @Nullable
    @Override
    public InstanceResourceReportType apply( final VmDescribeResponseType response ) {
      final InstanceResourceReportType report = new InstanceResourceReportType( );
      for ( final VmInfo vmInfo : response.getVms( ) ) {
        if ( !"Teardown".equals( vmInfo.getStateName() ) && vmInfo.getNetParams() != null ) {
          report.getPublicIps( ).add( vmInfo.getNetParams( ).getIgnoredPublicIp( ) );
          report.getPrivateIps().add( vmInfo.getNetParams( ).getIpAddress() );
          report.getMacs().add( vmInfo.getNetParams( ).getMacAddress() );
        }
      }

      return report;
    }
  }
}
