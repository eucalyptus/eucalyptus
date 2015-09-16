/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmState.PENDING;
import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmState.RUNNING;
import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmState.SHUTTING_DOWN;
import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmState.STOPPING;
import static com.eucalyptus.compute.common.internal.vm.VmInstances.TerminatedInstanceException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Either;
import com.eucalyptus.util.NonNullFunction;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import com.eucalyptus.bootstrap.Databases;
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
import com.eucalyptus.compute.common.internal.vm.VmBundleTask.BundleState;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmStateSet;
import com.eucalyptus.vm.Bundles;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vm.VmRuntimeState;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmDescribeType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class VmStateCallback extends StateUpdateMessageCallback<Cluster, VmDescribeType, VmDescribeResponseType> {
  private static Logger               LOG                       = Logger.getLogger( VmStateCallback.class );

  private static ConcurrentMap<String, Long> pendingUpdates = Maps.newConcurrentMap( );

  private final Supplier<Set<String>> initialInstances;
  
  public VmStateCallback( ) {
    super( new VmDescribeType( ).<VmDescribeType>regarding( ) );
    this.initialInstances = createInstanceSupplier( this, PENDING, RUNNING, SHUTTING_DOWN, STOPPING );
  }
  
  private static Supplier<Set<String>> createInstanceSupplier(
      final StateUpdateMessageCallback<Cluster, ?, ?> cb,
      final VmState... states
  ) {
    return Suppliers.memoize( new Supplier<Set<String>>( ) {
      @Override
      public Set<String> get( ) {
        return Sets.newHashSet( VmInstances.listWithProjection(
            VmInstances.instanceIdProjection( ),
            VmInstance.criterion( states ),
            VmInstance.nonNullNodeCriterion( ),
            VmInstance.zoneCriterion( cb.getSubject( ).getConfiguration( ).getPartition( ) )
        ) );
      }
    } );
  }

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
    }

    reply.setOriginCluster( this.getSubject( ).getConfiguration( ).getName( ) );
    final Set<String> reportedInstances = Sets.newHashSet( );
    for ( VmInfo vmInfo : reply.getVms( ) ) {
      reportedInstances.add( vmInfo.getInstanceId( ) );
      vmInfo.setPlacement( this.getSubject( ).getConfiguration( ).getName( ) );
      VmTypeInfo typeInfo = vmInfo.getInstanceType( );
      if ( typeInfo.getName( ) == null || "".equals( typeInfo.getName( ) ) ) {
        for ( VmType t : VmTypes.list( ) ) {
          if ( t.getCpu( ).equals( typeInfo.getCores( ) ) && t.getDisk( ).equals( typeInfo.getDisk( ) ) &&
              t.getMemory( ).equals( typeInfo.getMemory( ) ) ) {
            typeInfo.setName( t.getName( ) );
          }
        }
      }
    }

    final Set<String> unreportedInstances =
        Sets.newHashSet( Sets.difference( this.initialInstances.get( ), reportedInstances ) );
    if ( Databases.isVolatile( ) ) {
      return;
    }

    final Set<String> unknownInstances =
        Sets.newHashSet( Sets.difference( reportedInstances, this.initialInstances.get( ) ) );

    final List<Optional<Runnable>> taskList = Lists.newArrayList( );

    for ( final VmInfo runVm : reply.getVms( ) ) {
      if ( this.initialInstances.get( ).contains( runVm.getInstanceId( ) ) ) {
        taskList.add( UpdateTaskFunction.REPORTED.apply( Either.<String, VmInfo>right( runVm ) ) );
      } else if ( unknownInstances.contains( runVm.getInstanceId( ) ) ) {
        taskList.add( UpdateTaskFunction.UNKNOWN.apply( Either.<String, VmInfo>right( runVm ) ) );
      }
    }
    for ( final String vmId : unreportedInstances ) {
      taskList.add( UpdateTaskFunction.UNREPORTED.apply( Either.<String,VmInfo>left( vmId ) ) );
    }
    for ( final Runnable task : Optional.presentInstances( taskList ) ) {
      Threads.enqueue(
          ClusterController.class,
          VmStateCallback.class,
          ( Runtime.getRuntime( ).availableProcessors( ) * 2 ) + 1,
          Executors.callable( task )
      );
    }
  }
  
  private static void handleUnreported( final String vmId ) {
    try {
      final VmInstance vm = VmInstances.lookupAny( vmId );
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
      } else if ( VmStateSet.RUN.apply( vm ) && VmRuntimeState.InstanceStatus.Ok.apply( vm ) ) {
        VmInstances.unreachable( vm );
      }
    } catch ( final Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }
  }
  
  private static void handleReportedState( final VmInfo runVm ) {
    final VmState runVmState = VmState.Mapper.get( runVm.getStateName( ) );
    try {
      try ( final TransactionResource db = Entities.transactionFor( VmInstance.class ) ) {
        VmInstance vm = VmInstances.lookupAny( runVm.getInstanceId() );
        if ( VmStateSet.DONE.apply( vm ) ) {
          db.rollback( );
          if ( VmInstance.Reason.EXPIRED.apply( vm ) ) {
            VmStateCallback.handleUnknown( runVm );
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
          VmInstances.doUpdate( vm ).apply( runVm );
        } else if ( !VmStateSet.RUN.apply( vm ) && VmStateSet.RUN.contains( runVmState )
                    && vm.lastUpdateMillis( ) > ( VmInstances.VOLATILE_STATE_TIMEOUT_SEC * 1000l ) ) {
          VmInstances.doUpdate( vm ).apply( runVm );
        } else {
          return;
        }
        Entities.commit( db );
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
        throw ex;
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

  enum UpdateTaskFunction implements NonNullFunction<Either<String,VmInfo>, Optional<Runnable>> {
    REPORTED {
      void task( final Either<String,VmInfo> idOrVmInfo ) {
        VmStateCallback.handleReportedState( idOrVmInfo.getRight( ) );
      }
    },
    UNKNOWN {
      @Override
      void task( final Either<String,VmInfo> idOrVmInfo ) {
        VmStateCallback.handleUnknown( idOrVmInfo.getRight( ) );
      }
    },
    UNREPORTED {
      @Override
      void task( final Either<String,VmInfo> idOrVmInfo ) {
        VmStateCallback.handleUnreported( idOrVmInfo.getLeft( ) );
      }
    };

    abstract void task( Either<String,VmInfo> idOrVmInfo );

    @Nonnull
    @Override
    public Optional<Runnable> apply( final Either<String,VmInfo> input ) {
      final String instanceId = input == null ?
          null :
          input.isLeft( ) ? input.getLeft( ) : input.getRight( ).getInstanceId( );
      try {
        final Runnable run = new Runnable( ) {
          @Override
          public void run() {
            try {
              UpdateTaskFunction.this.task( input );
            } catch ( Exception e ) {
              LOG.error(
                  "Failed to handle "
                  + UpdateTaskFunction.this.name().toLowerCase()
                  + " instance: "
                  + instanceId
                  + " because of "
                  + e.getMessage()
              );
            } finally {
              pendingUpdates.remove( instanceId );
            }
          }
        };
        if ( input != null
             && instanceId != null
             && pendingUpdates.putIfAbsent( instanceId, System.currentTimeMillis( ) ) == null ) {
          return Optional.of( run );
        } else {
          return Optional.absent( );
        }
      } catch ( Exception e ) {
        return Optional.absent( );
      }
    }
  }
  
  private static void handleUnknown( final VmInfo runVm ) {
    for ( final Optional<VmInstances.RestoreHandler> restoreHandler :
        VmInstances.RestoreHandler.parseList( VmInstances.UNKNOWN_INSTANCE_HANDLERS ) ) {
      if ( restoreHandler.isPresent( ) && handleRestore( runVm, restoreHandler.get( ) ) ) {
        break;
      }
    }
  }

  private static boolean handleRestore( final VmInfo runVm,
                                        final Predicate<VmInfo> restorer ) {
    final VmState runVmState = VmState.Mapper.get( runVm.getStateName( ) );
    if ( VmStateSet.RUN.contains( runVmState ) ) {
      try {
        final VmInstance vm = VmInstances.lookupAny( runVm.getInstanceId() );
        if ( !( VmStateSet.DONE.apply( vm ) && VmInstance.Reason.EXPIRED.apply( vm ) ) ) {
          if ( VmStateSet.TORNDOWN.apply( vm ) ) {
            VmInstances.RestoreHandler.Terminate.apply( runVm );
          }
          return true;
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
        return restorer.apply( runVm );
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
      Bundles.updateBundleTaskState( vm, bundleState, 0.0d );
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
  
  public static class VmPendingCallback extends
      StateUpdateMessageCallback<Cluster, VmDescribeType, VmDescribeResponseType> {

    private final Supplier<Set<String>> initialInstances;
    
    public VmPendingCallback( Cluster cluster ) {
      super( cluster );
      this.initialInstances = createInstanceSupplier( this, PENDING, STOPPING, SHUTTING_DOWN  );
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

  private static final class StateTaskExpiryEventListener implements EventListener<ClockTick> {
    public static void register( ){
      Listeners.register( ClockTick.class, new StateTaskExpiryEventListener( ) );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      final long expiry = System.currentTimeMillis( ) - TimeUnit.MINUTES.toMillis( 5 );
      for ( final Map.Entry<String,Long> entry : pendingUpdates.entrySet( ) ) {
        if ( entry.getValue( ) < expiry ) {
          if ( pendingUpdates.remove( entry.getKey( ), entry.getValue( ) ) ) {
            LOG.warn( "Expired state update task for instance " + entry.getKey( ) );
          }
        }
      }
    }
  }
}
