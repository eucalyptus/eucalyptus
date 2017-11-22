/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2017 Ent. Services Development Corporation LP
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

package com.eucalyptus.cluster;

import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmStateSet.TORNDOWN;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.vm.VmStateUpdate;
import com.eucalyptus.cluster.common.msgs.AttachedVolume;
import com.eucalyptus.cluster.common.msgs.NetworkConfigType;
import com.eucalyptus.cluster.common.msgs.VmInfo;
import com.eucalyptus.cluster.common.msgs.VmTypeInfo;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.internal.vm.MigrationState;
import com.eucalyptus.compute.common.internal.vm.VmBundleTask;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmRuntimeState;
import com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment;
import com.eucalyptus.compute.common.network.InstanceResourceReportType;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.UpdateInstanceResourcesType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityCache;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.network.NetworkInfoBroadcaster;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Either;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.vm.Bundles;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 *
 */
@ComponentNamed
public class VmStateHandler {
  private static Logger LOG = Logger.getLogger( VmStateHandler.class );

  static {
    Cluster.registerVmStateUpdateConsumer( VmStateHandler::updateVmInfo );
  }

  private static final ConcurrentMap<String, Long> pendingUpdates = Maps.newConcurrentMap( );

  private static final Supplier<Iterable<VmStateView>> instanceViewSupplier =
      Suppliers.memoizeWithExpiration(
          new EntityCache<>(
              VmInstance.named(null),
              Restrictions.not( VmInstance.criterion( TORNDOWN.array( ) ) ),
              Sets.newHashSet( "transientVolumeState.attachments"),
              Sets.newHashSet( "bootRecord.machineImage", "bootRecord.vmType", "networkGroups" ),
              TypeMappers.lookup( VmInstance.class, VmStateView.class )  ),
          10,
          TimeUnit.SECONDS );

  public static void updateVmInfo( final VmStateUpdate stateUpdate ) {
    UpdateInstanceResourcesType update = new UpdateInstanceResourcesType( );
    update.setPartition( stateUpdate.getCluster( ).getPartition() );
    update.setResources( TypeMappers.transform( stateUpdate, InstanceResourceReportType.class ) );
    final boolean requestBroadcast = Networking.getInstance( ).update( update );

    if ( Databases.isVolatile( ) ) {
      return;
    }

    final Cluster cluster = stateUpdate.getCluster( );
    final Set<String> initialInstances = stateUpdate.getRequestedVms( );
    final List<VmInfo> vms = stateUpdate.getVmInfos( );
    final Map<String,VmStateView> localState = ImmutableMap.copyOf( CollectionUtils.putAll(
        instanceViewSupplier.get( ),
        Maps.<String,VmStateView>newHashMapWithExpectedSize( vms.size( ) ),
        HasName.GET_NAME,
        Functions.<VmStateView>identity( ) ) );

    final Set<String> reportedInstances = Sets.newHashSetWithExpectedSize( vms.size( ) );
    for ( VmInfo vmInfo : vms ) {
      reportedInstances.add( vmInfo.getInstanceId( ) );
      vmInfo.setPlacement( cluster.getConfiguration( ).getName( ) );
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
        Sets.newHashSet( Sets.difference( initialInstances, reportedInstances ) );
    if ( Databases.isVolatile( ) ) {
      return;
    }

    final Set<String> unknownInstances =
        Sets.newHashSet( Sets.difference( reportedInstances, initialInstances ) );

    final List<Optional<Runnable>> taskList = Lists.newArrayList( );

    for ( final VmInfo runVm : vms ) {
      if ( initialInstances.contains( runVm.getInstanceId( ) ) ) {
        taskList.add( UpdateTaskFunction.REPORTED.apply( context( localState, runVm ) ) );
      } else if ( unknownInstances.contains( runVm.getInstanceId( ) ) ) {
        taskList.add( UpdateTaskFunction.UNKNOWN.apply( context( localState, runVm ) ) );
      }
    }
    for ( final String vmId : unreportedInstances ) {
      taskList.add( UpdateTaskFunction.UNREPORTED.apply( context( localState, vmId ) ) );
    }
    final Optional<Runnable> broadcastRequestRunnable = requestBroadcast ?
        Optional.<Runnable>of( new Runnable( ) {
          @Override
          public void run( ) {
            NetworkInfoBroadcaster.requestNetworkInfoBroadcast( );
          }
        } ) :
        Optional.<Runnable>absent( );

    for ( final Runnable task :
        Iterables.concat( Optional.presentInstances( taskList ), broadcastRequestRunnable.asSet( ) ) ) {
      Threads.enqueue(
          ClusterController.class,
          VmStateHandler.class,
          ( Runtime.getRuntime( ).availableProcessors( ) * 2 ) + 1,
          Executors.callable( task )
      );
    }
  }


  private static void handleUnreported( final VmStateContext vmStateContext ) {
    try {
      final String vmId = vmStateContext.input.getLeft( );
      final long intitialReportTimeoutMillis = VmInstances.VM_INITIAL_REPORT_TIMEOUT * 1000;
      final VmStateView vmView = vmStateContext.getLocalState( ).get( vmId );
      if ( vmView != null && vmView.getState( ) == VmInstance.VmState.PENDING && (System.currentTimeMillis( ) - vmView.getLastUpdated( )) < intitialReportTimeoutMillis ) {
        return;
      }

      final VmInstance vm = VmInstances.lookupAny( vmId );
      if ( VmInstance.VmState.PENDING.apply( vm ) && vm.lastUpdateMillis( ) < intitialReportTimeoutMillis ) {
        //do nothing during first VM_INITIAL_REPORT_TIMEOUT millis of instance life
        return;
      } else if ( vm.isBlockStorage( ) && ( VmInstances.Timeout.UNREPORTED.apply( vm ) || VmInstances.Timeout.PENDING.apply( vm ) ) ) {
        VmInstances.stopped( vm );
      } else if ( VmInstance.VmState.STOPPING.apply( vm ) ) {
        VmInstances.stopped( vm );
      } else if ( VmInstance.VmState.SHUTTING_DOWN.apply( vm ) ) {
        VmInstances.terminated( vm );
      } else if ( VmInstances.Timeout.TERMINATED.apply( vm ) ) {
        VmInstances.buried( vm );
      } else if ( VmInstances.Timeout.BURIED.apply( vm ) ) {
        VmInstances.delete( vm );
      } else if ( !vm.isBlockStorage( ) && ( VmInstances.Timeout.UNREPORTED.apply( vm ) || VmInstances.Timeout.PENDING.apply( vm ) ) ) {
        VmInstances.terminated( vm );
      } else if ( VmInstance.VmStateSet.RUN.apply( vm ) && VmRuntimeState.InstanceStatus.Ok.apply( vm ) ) {
        VmInstances.unreachable( vm );
      }
    } catch ( final Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }
  }

  private static void handleReportedState( final VmStateContext vmStateContext ) {
    final VmInfo runVm = vmStateContext.getInput( ).getRight( );
    final VmInstance.VmState runVmState = VmInstance.VmState.Mapper.get( runVm.getStateName( ) );
    try {
      final VmStateView vmView = vmStateContext.getLocalState( ).get( runVm.getInstanceId( ) );
      MigrationState migrationState = MigrationState.defaultValueOf( runVm.getMigrationStateName() );
      boolean updateRequired = false;
      if ( vmView != null ) {
        if ( vmView.inState( VmInstance.VmStateSet.DONE ) ) {
          if ( vmView.getReason( ) == VmInstance.Reason.EXPIRED ) {
            handleUnknown( vmStateContext );
          } else {
            LOG.trace( "Ignore state update to terminated instance: " + runVm.getInstanceId( ) );
          }
          return;
        } else if ( vmView.getState( ) == VmInstance.VmState.RUNNING && System.currentTimeMillis( ) > vmView.getExpires( )  ) {
          updateRequired = true;
        } else if ( VmInstance.VmState.SHUTTING_DOWN.equals( runVmState ) ) {
          updateRequired = true;
        } else if ( !vmView.inState( VmInstance.VmStateSet.RUN ) && VmInstance.VmStateSet.RUN.contains( runVmState )
            && ( System.currentTimeMillis( ) - vmView.getLastUpdated( ) ) > ( VmInstances.VOLATILE_STATE_TIMEOUT_SEC * 1000l ) ) {
          updateRequired = true;
        } else if ( vmView.inState( VmInstance.VmStateSet.RUN ) ) {
          updateRequired =
              vmView.isBundling( ) ||
                  vmView.isMigrating( ) ||
                  migrationState.isMigrating( ) ||
                  runVmState != vmView.getState( ) ||
                  !Objects.equals( vmView.getGuestState( ), runVm.getGuestStateName( ) ) ||
                  !Objects.equals( vmView.getServiceTag( ), runVm.getServiceTag( ) ) ||
                  ( System.currentTimeMillis( ) - vmView.getLastUpdated( ) ) > VmInstances.Timeout.UNTOUCHED.getMilliseconds( ) || // for running and pending states
                  vmView.getReachabilityStatus( ) != VmRuntimeState.ReachabilityStatus.Passed ||
                  ( vmView.getState( ) == VmInstance.VmState.RUNNING && !vmView.getVolumeAttachments( ).equals(
                      runVm.getVolumes( ).stream( )
                          .map( TypeMappers.lookupF( AttachedVolume.class, VmStateVolumeAttachmentView.class ) )
                          .collect( Collectors.toMap( HasName.GET_NAME, volumeView -> volumeView ) ) ) ) ||
                  ( vmView.getState( ) == VmInstance.VmState.RUNNING && !vmView.getNetworkInterfaceAttachments( ).equals(
                      runVm.getSecondaryNetConfigList( ).stream( )
                          .map( TypeMappers.lookupF( NetworkConfigType.class, VmStateNetworkInterfaceAttachmentView.class ) )
                          .collect( Collectors.toMap( HasName.GET_NAME, niView -> niView ) ) ) )
          ;
        }
      }
      if ( updateRequired ) try ( final TransactionResource db = Entities.transactionFor( VmInstance.class ) ) {
        VmInstance vm = VmInstances.lookupAny( runVm.getInstanceId() );
        if ( VmInstances.Timeout.EXPIRED.apply( vm ) ) {
          if ( vm.isBlockStorage( ) ) {
            VmInstances.stopped( vm );
          } else {
            VmInstances.shutDown( vm );
          }
        } else if ( VmInstance.VmState.SHUTTING_DOWN.equals( runVmState ) ) {
          db.rollback();
          handleReportedTeardown( vm, runVm );
          return;
        } else {
          VmInstances.doUpdate( vm ).apply( runVm );
        }
        Entities.commit( db );
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
        throw ex;
      }
    } catch ( com.eucalyptus.compute.common.internal.vm.VmInstances.TerminatedInstanceException ex1 ) {
      LOG.trace( "Ignore state update to terminated instance: " + runVm.getInstanceId( ) );
    } catch ( NoSuchElementException ex1 ) {
//      VmStateCallback.handleRestore( runVm );
    } catch ( Exception ex1 ) {
      LOG.error( ex1 );
      Logs.extreme( ).error( ex1, ex1 );
    }
  }

  enum UpdateTaskFunction implements NonNullFunction<VmStateContext, Optional<Runnable>> {
    REPORTED {
      void task( final VmStateContext context ) {
        handleReportedState( context );
      }
    },
    UNKNOWN {
      @Override
      void task( final VmStateContext context ) {
        handleUnknown( context );
      }
    },
    UNREPORTED {
      @Override
      void task( final VmStateContext context ) {
        handleUnreported( context );
      }
    };

    abstract void task( final VmStateContext context );

    @Nonnull
    @Override
    public Optional<Runnable> apply( final VmStateContext context ) {
      final String instanceId = context == null ?
          null :
          context.input.isLeft( ) ? context.input.getLeft( ) : context.input.getRight( ).getInstanceId( );
      try {
        final Runnable run = new Runnable( ) {
          @Override
          public void run() {
            try {
              UpdateTaskFunction.this.task( context );
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
        if ( context != null
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

  private static void handleUnknown( final VmStateContext vmStateContext ) {
    for ( final Optional<VmInstances.RestoreHandler> restoreHandler :
        VmInstances.RestoreHandler.parseList( VmInstances.UNKNOWN_INSTANCE_HANDLERS ) ) {
      if ( restoreHandler.isPresent( ) && handleRestore( vmStateContext.getInput().getRight(), restoreHandler.get( ) ) ) {
        break;
      }
    }
  }

  private static boolean handleRestore( final VmInfo runVm,
                                        final Predicate<VmInfo> restorer ) {
    final VmInstance.VmState runVmState = VmInstance.VmState.Mapper.get( runVm.getStateName( ) );
    if ( VmInstance.VmStateSet.RUN.contains( runVmState ) ) {
      try {
        final VmInstance vm = VmInstances.lookupAny( runVm.getInstanceId() );
        if ( !( VmInstance.VmStateSet.DONE.apply( vm ) && VmInstance.Reason.EXPIRED.apply( vm ) ) ) {
          if ( VmInstance.VmStateSet.TORNDOWN.apply( vm ) ) {
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
    VmBundleTask.BundleState bundleState = VmBundleTask.BundleState.mapper.apply( runVm.getBundleTaskStateName( ) );
    if ( !VmBundleTask.BundleState.none.equals( bundleState ) ) {
      Bundles.updateBundleTaskState( vm, bundleState, 0.0d );
      VmInstances.terminated( vm );
    } else if ( VmInstance.VmState.SHUTTING_DOWN.apply( vm ) ) {
      VmInstances.terminated( vm );
    } else if ( VmInstance.VmState.STOPPING.apply( vm ) ) {
      VmInstances.stopped( vm );
    } else if ( VmInstance.VmStateSet.RUN.apply( vm ) && vm.getSplitTime( ) > ( VmInstances.VM_STATE_SETTLE_TIME * 1000 ) ) {
      if ( vm.isBlockStorage( ) ) {
        VmInstances.stopped( vm );
      } else {
        VmInstances.shutDown( vm );
      }
    }
  }

  private static VmStateContext context( final Map<String,VmStateView> localState, final String vmId ) {
    return new VmStateContext( localState, vmId );
  }

  private static VmStateContext context( final Map<String,VmStateView> localState, final VmInfo vmInfo ) {
    return new VmStateContext( localState, vmInfo );
  }

  @TypeMapper
  public enum VmStateUpdateToInstanceResourceReport implements Function<VmStateUpdate,InstanceResourceReportType> {
    INSTANCE;

    @Nullable
    @Override
    public InstanceResourceReportType apply( final VmStateUpdate vmStateUpdate ) {
      final InstanceResourceReportType report = new InstanceResourceReportType( );
      for ( final VmInfo vmInfo : vmStateUpdate.getVmInfos( ) ) {
        if ( !"Teardown".equals( vmInfo.getStateName() ) && vmInfo.getNetParams() != null ) {
          report.getPublicIps( ).add( vmInfo.getNetParams( ).getIgnoredPublicIp( ) );
          report.getPrivateIps().add( vmInfo.getNetParams( ).getIpAddress() );
          report.getMacs().add( vmInfo.getNetParams( ).getMacAddress() );
          if (vmInfo.getSecondaryNetConfigList() != null && vmInfo.getSecondaryNetConfigList().size() > 0) {
            for(NetworkConfigType netConfig : vmInfo.getSecondaryNetConfigList()) {
              report.getPublicIps().add(netConfig.getIgnoredPublicIp());
              report.getPrivateIps().add(netConfig.getIpAddress());
              report.getMacs().add(netConfig.getMacAddress());
            }
          }
        }
      }

      return report;
    }
  }

  public static final class VmStateVolumeAttachmentView implements HasName<VmStateVolumeAttachmentView> {
    private final String id;
    private final String device;
    private final String removeDevice;
    private final String status;
    private final Long attachTime;

    public VmStateVolumeAttachmentView(
        final String id,
        final String device,
        final String removeDevice,
        final String status,
        final Long attachTime ) {
      this.id = id;
      this.device = device;
      this.removeDevice = removeDevice;
      this.status = status;
      this.attachTime = attachTime;
    }

    public String getId( ) {
      return id;
    }

    @Override
    public String getName( ) {
      return id;
    }

    @Override
    public int compareTo( final VmStateVolumeAttachmentView o ) {
      return id.compareTo( o.id );
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final VmStateVolumeAttachmentView that = (VmStateVolumeAttachmentView) o;
      return Objects.equals( id, that.id ) &&
          Objects.equals( device, that.device ) &&
          Objects.equals( removeDevice, that.removeDevice ) &&
          Objects.equals( status, that.status );
    }

    @Override
    public int hashCode() {
      return Objects.hash( id, device, removeDevice, status );
    }
  }

  public static final class VmStateNetworkInterfaceAttachmentView implements HasName<VmStateNetworkInterfaceAttachmentView> {
    private final String networkInterfaceId;
    private final String attachmentId;

    public VmStateNetworkInterfaceAttachmentView(
        final String networkInterfaceId,
        final String attachmentId
    ) {
      this.networkInterfaceId = networkInterfaceId;
      this.attachmentId = attachmentId;
    }

    public String getId( ) {
      return networkInterfaceId;
    }

    @Override
    public String getName( ) {
      return networkInterfaceId;
    }

    @Override
    public int compareTo( final VmStateNetworkInterfaceAttachmentView o ) {
      return attachmentId.compareTo( o.attachmentId );
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final VmStateNetworkInterfaceAttachmentView that = (VmStateNetworkInterfaceAttachmentView) o;
      return Objects.equals( attachmentId, that.attachmentId );
    }

    @Override
    public int hashCode() {
      return attachmentId.hashCode( );
    }
  }

  public static final class VmStateView implements HasName<VmStateView> {
    private final String id;
    private final int version;
    private final String partition;
    private final String serviceTag;
    private final VmInstance.VmState state;
    private final String guestState;
    private final VmRuntimeState.ReachabilityStatus reachabilityStatus;
    private final VmInstance.Reason reason;
    private final Map<String,VmStateVolumeAttachmentView> volumeAttachments;
    private final Map<String,VmStateNetworkInterfaceAttachmentView> networkInterfaceAttachments;
    private final long lastUpdated;
    private final long expires;
    private final boolean bundling;
    private final boolean migrating;

    public VmStateView(
        final String id,
        final int version,
        final String partition,
        final String serviceTag,
        final VmInstance.VmState state,
        final String guestState,
        final VmRuntimeState.ReachabilityStatus reachabilityStatus,
        final VmInstance.Reason reason,
        final Map<String, VmStateVolumeAttachmentView> volumeAttachments,
        final Map<String, VmStateNetworkInterfaceAttachmentView> networkInterfaceAttachments,
        final long lastUpdated,
        final long expires,
        final boolean bundling,
        final boolean migrating
    ) {
      this.id = id;
      this.version = version;
      this.partition = partition;
      this.serviceTag = serviceTag;
      this.state = state;
      this.guestState = guestState;
      this.reachabilityStatus = reachabilityStatus;
      this.reason = reason;
      this.volumeAttachments = volumeAttachments;
      this.networkInterfaceAttachments = networkInterfaceAttachments;
      this.lastUpdated = lastUpdated;
      this.expires = expires;
      this.bundling = bundling;
      this.migrating = migrating;
    }

    public String getId( ) {
      return id;
    }

    public int getVersion( ) {
      return version;
    }

    public String getPartition( ) {
      return partition;
    }

    public String getServiceTag( ) {
      return serviceTag;
    }

    public VmInstance.VmState getState( ) {
      return state;
    }

    public String getGuestState( ) {
      return guestState;
    }

    public VmRuntimeState.ReachabilityStatus getReachabilityStatus( ) {
      return reachabilityStatus;
    }

    public VmInstance.Reason getReason( ) {
      return reason;
    }

    public Map<String, VmStateVolumeAttachmentView> getVolumeAttachments( ) {
      return volumeAttachments;
    }

    public Map<String, VmStateNetworkInterfaceAttachmentView> getNetworkInterfaceAttachments( ) {
      return networkInterfaceAttachments;
    }

    public long getLastUpdated( ) {
      return lastUpdated;
    }

    public long getExpires( ) {
      return expires;
    }

    public boolean isBundling( ) {
      return bundling;
    }

    public boolean isMigrating( ) {
      return migrating;
    }

    @Override
    public String getName( ) {
      return id;
    }

    @Override
    public int compareTo( final VmStateView o ) {
      return id.compareTo( o.id );
    }

    public boolean inState( final VmInstance.VmStateSet stateSet ) {
      return stateSet.set( ).contains( state );
    }
  }

  @TypeMapper
  public enum VmInstanceToVmStateView implements Function<VmInstance,VmStateView> {
    INSTANCE;

    final Set<NetworkInterfaceAttachment.Status> attachedStates = EnumSet.of(
        NetworkInterfaceAttachment.Status.attached,
        NetworkInterfaceAttachment.Status.detaching
    );

    @Override
    public VmStateView apply( final VmInstance vmInstance ) {
      final Map<String,VmStateVolumeAttachmentView> volumes =
          Stream.concat(
              vmInstance.getBootRecord( ).getPersistentVolumes( ).stream( ),
              vmInstance.getTransientVolumeState( ).getAttachments( ).stream( )
          )
              .map( TypeMappers.lookupF( VmVolumeAttachment.class, VmStateVolumeAttachmentView.class ) )
              .collect( Collectors.toMap( HasName.GET_NAME, volumeView -> volumeView ) );
      final Map<String,VmStateNetworkInterfaceAttachmentView> networkInterfaces =
          vmInstance.getNetworkInterfaces( ).stream( )
              .filter( ni ->
                  ni.getAttachment( ).getDeviceIndex( ) != 0 &&
                      attachedStates.contains( ni.getAttachment( ).getStatus( ) ) )
              .map( TypeMappers.lookupF( NetworkInterface.class, VmStateNetworkInterfaceAttachmentView.class ) )
              .collect( Collectors.toMap( HasName.GET_NAME, niView -> niView ) );
      return new VmStateView(
          vmInstance.getInstanceId( ),
          vmInstance.getVersion( ),
          vmInstance.getPartition( ),
          vmInstance.getServiceTag( ),
          vmInstance.getState( ),
          vmInstance.getRuntimeState( ).getGuestState( ),
          vmInstance.getRuntimeState( ).getReachabilityStatus( ),
          vmInstance.getRuntimeState( ).getReason( ),
          ImmutableMap.copyOf( volumes ),
          ImmutableMap.copyOf( networkInterfaces ),
          vmInstance.getLastUpdateTimestamp( ).getTime( ),
          vmInstance.getExpiration( ) == null ? Long.MAX_VALUE : vmInstance.getExpiration( ).getTime( ),
          vmInstance.getRuntimeState( ).isBundling( ),
          vmInstance.getRuntimeState().getMigrationTask( ).getState( ).isMigrating( )
      );
    }
  }

  private static final class VmStateContext {
    private final Map<String,VmStateView> localState;
    private final Either<String,VmInfo> input;

    VmStateContext(
        final Map<String,VmStateView> localState,
        final String vmId
    ) {
      this.localState = localState;
      this.input = Either.left( vmId );
    }

    VmStateContext(
        final Map<String,VmStateView> localState,
        final VmInfo vmInfo
    ) {
      this.localState = localState;
      this.input = Either.right( vmInfo );
    }

    public Map<String, VmStateView> getLocalState( ) {
      return localState;
    }

    public Either<String, VmInfo> getInput( ) {
      return input;
    }
  }

  @TypeMapper
  public enum AttachedVolumeToVmStateVolumeAttachmentView implements Function<AttachedVolume,VmStateVolumeAttachmentView> {
    INSTANCE;

    @Override
    public VmStateVolumeAttachmentView apply( final AttachedVolume attachedVolume ) {
      return new VmStateVolumeAttachmentView(
          attachedVolume.getVolumeId( ),
          attachedVolume.getDevice( ),
          attachedVolume.getRemoteDevice( ),
          attachedVolume.getStatus( ),
          attachedVolume.getAttachTime( ) == null ? null : attachedVolume.getAttachTime( ).getTime( )
      );
    }
  }

  @TypeMapper
  public enum VmVolumeAttachmentToVmStateVolumeAttachmentView implements Function<VmVolumeAttachment,VmStateVolumeAttachmentView> {
    INSTANCE;

    @Override
    public VmStateVolumeAttachmentView apply( final VmVolumeAttachment volumeAttachment ) {
      return new VmStateVolumeAttachmentView(
          volumeAttachment.getVolumeId( ),
          volumeAttachment.getDevice( ),
          volumeAttachment.getRemoteDevice( ),
          volumeAttachment.getStatus( ),
          volumeAttachment.getAttachTime( ) == null ? null : volumeAttachment.getAttachTime( ).getTime( )
      );
    }
  }

  @TypeMapper
  public enum NetworkConfigTypeToVmStateNetworkInterfaceAttachmentView implements Function<NetworkConfigType,VmStateNetworkInterfaceAttachmentView> {
    INSTANCE;

    @Override
    public VmStateNetworkInterfaceAttachmentView apply( final NetworkConfigType networkConfigType ) {
      return new VmStateNetworkInterfaceAttachmentView(
          networkConfigType.getInterfaceId( ),
          networkConfigType.getAttachmentId( )
      );
    }
  }

  @TypeMapper
  public enum NetworkInterfaceToVmStateNetworkInterfaceAttachmentView implements Function<NetworkInterface,VmStateNetworkInterfaceAttachmentView> {
    INSTANCE;

    @Override
    public VmStateNetworkInterfaceAttachmentView apply( final NetworkInterface networkInterface ) {
      return new VmStateNetworkInterfaceAttachmentView(
          networkInterface.getDisplayName( ),
          networkInterface.getAttachment( ).getAttachmentId( )
      );
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
