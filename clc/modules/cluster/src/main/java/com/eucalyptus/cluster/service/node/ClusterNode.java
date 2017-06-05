/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.cluster.service.node;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import com.eucalyptus.cluster.service.vm.ClusterVm;
import com.eucalyptus.component.Component;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import javaslang.collection.Stream;
import javaslang.control.Option;

/**
 * 
 */
public final class ClusterNode {
  @SuppressWarnings( "WeakerAccess" )
  public static final String NODE_STATUS_DETAIL_ENABLED = "the node is operating normally";
  @SuppressWarnings( "WeakerAccess" )
  public static final String NODE_STATUS_DETAIL_STOPPED = "the node is not accepting new instances";
  @SuppressWarnings( "WeakerAccess" )
  public static final String NODE_STATUS_DETAIL_NOTREADY = "the node is not responding to the cluster controller";
  @SuppressWarnings( "WeakerAccess" )
  public static final String NODE_STATUS_DETAIL_BROKEN = "the node is currently experiencing problems and needs attention";

  private static final Map<String,String> statusToDetailMap = ImmutableMap.of(
      Component.State.ENABLED.name(), NODE_STATUS_DETAIL_ENABLED,
      Component.State.STOPPED.name(), NODE_STATUS_DETAIL_STOPPED,
      Component.State.NOTREADY.name(), NODE_STATUS_DETAIL_NOTREADY,
      Component.State.BROKEN.name(), NODE_STATUS_DETAIL_BROKEN
  );

  private final String node;

  private final AtomicReference<ClusterNodeAvailability> availability =
      new AtomicReference<>( ClusterNodeAvailability.none( ) );
  private volatile ClusterNodeDetails details;
  private volatile ClusterNodeStatus status;

  private final List<ClusterVm> vms = Lists.newCopyOnWriteArrayList( );

  public ClusterNode( final String node ) {
    this( node, null, 0, 0, 0 );
  }

  public ClusterNode( final String node,
                      final String iqn,
                      final int cores,
                      final int disk,
                      final int memory
  ) {
    this.node = node;
    this.availability.set( ClusterNodeAvailability.of( cores, disk, memory ) );
    this.details = Strings.isNullOrEmpty( iqn ) ?
        ClusterNodeDetails.none( ) :
        ClusterNodeDetails.of( null, iqn, null, null );
    this.status = ClusterNodeStatus.of( Component.State.NOTREADY.name( ), NODE_STATUS_DETAIL_NOTREADY );
  }

  public String getHypervisor( ) {
    return details.getHypervisor( );
  }

  public String getIqn( ) {
    return details.getIqn( );
  }

  @SuppressWarnings( "unused" )
  public Boolean getMigrationCapable( ) {
    return details.getMigrationCapable( );
  }

  @SuppressWarnings( "unused" )
  public String getPublicSubnets( ) {
    return details.getPublicSubnets( );
  }

  @SuppressWarnings( "WeakerAccess" )
  public boolean setDetails(
      final String hypervisor,
      final String iqn,
      final Boolean migrationCapable,
      final String publicSubnets
  ) {
    final ClusterNodeDetails details = ClusterNodeDetails.of( hypervisor, iqn, migrationCapable, publicSubnets );
    final boolean updated = !this.details.equals( details );
    if ( updated ) {
      this.details = details;
    }
    return updated;
  }

  public int getCoresAvailable( ) {
    return availability.get( ).getCoresAvailable( );
  }

  public int getCoresTotal( ) {
    return availability.get( ).getCoresTotal( );
  }

  public int getDiskAvailable( ) {
    return availability.get( ).getDiskAvailable( );
  }

  public int getDiskTotal( ) {
    return availability.get( ).getDiskTotal( );
  }

  public int getMemoryAvailable( ) {
    return availability.get( ).getMemoryAvailable( );
  }

  public int getMemoryTotal( ) {
    return availability.get( ).getMemoryTotal( );
  }

  @SuppressWarnings( "WeakerAccess" )
  public boolean setAvailability(
      final int coresAvailable,
      final int coresTotal,
      final int diskAvailable,
      final int diskTotal,
      final int memoryAvailable,
      final int memoryTotal
  ) {
    while( true ) {
      final ClusterNodeAvailability current = this.availability.get( );
      final ClusterNodeAvailability availability = ClusterNodeAvailability.of(
          coresAvailable,
          coresTotal,
          diskAvailable,
          diskTotal,
          memoryAvailable,
          memoryTotal
      );
      final boolean updated = !current.equals( availability );
      if ( updated ) {
        if ( !this.availability.compareAndSet( current, availability ) ) continue;
      }
      return updated;
    }
  }

  @SuppressWarnings( { "WeakerAccess", "UnusedReturnValue" } )
  public boolean setAvailability(
      final int coresAvailable,
      final int diskAvailable,
      final int memoryAvailable
  ) {
    while( true ) {
      final ClusterNodeAvailability current = this.availability.get( );
      final ClusterNodeAvailability availability = ClusterNodeAvailability.of(
          coresAvailable,
          current.getCoresTotal( ),
          diskAvailable,
          current.getDiskTotal( ),
          memoryAvailable,
          current.getMemoryTotal( )
      );
      final boolean updated = !current.equals( availability );
      if ( updated ) {
        if ( !this.availability.compareAndSet( current, availability ) ) continue;
      }
      return updated;
    }
  }

  public String getNodeStatus( ) {
    return status.getStatus( );
  }

  public String getNodeStatusDetail( ) {
    return status.getDetail( );
  }

  @SuppressWarnings( "WeakerAccess" )
  public boolean setStatusFromNode( final String nodeStatus ) {
    String status;
    switch ( nodeStatus ) {
      case "disabled":
        status = Component.State.STOPPED.name( );
        break;
      default:
        status = nodeStatus.toUpperCase( );
    }
    return setStatus( status, detailForStatus( status, NODE_STATUS_DETAIL_BROKEN ) );
  }

  @SuppressWarnings( "WeakerAccess" )
  public boolean setStatus( final String status, final String detail ) {
    final ClusterNodeStatus nodeStatus = ClusterNodeStatus.of( status, detail );
    final boolean updated = !this.status.equals( nodeStatus );
    if ( updated ) {
      this.status = nodeStatus;
    }
    return updated;
  }

  public String getNode( ) {
    return node;
  }

  public String getServiceTag( ) {
    return "http://" + node + ":8775/axis2/services/EucalyptusNC";
  }

  @SuppressWarnings( "WeakerAccess" )
  public static String detailForStatus( final String status, final String defaultDetail ) {
    return statusToDetailMap.getOrDefault( status, defaultDetail );
  }

  @SuppressWarnings( "WeakerAccess" )
  public void clearCapacity( ) {
    this.availability.set( ClusterNodeAvailability.none( ) );
  }

  public Stream<ClusterVm> getVms( ) {
    return Stream.ofAll( vms );
  }

  @SuppressWarnings( "WeakerAccess" )
  public ClusterVm rvm( final ClusterVm vm ) {
    vms.remove( vm );
    return vm;
  }

  public ClusterVm vm( final ClusterVm vm ) {
    vms.add( vm );
    return vm;
  }

  public Option<ClusterVm> vm( final String id ) {
    return getVms( ).find( vm -> id.equals( vm.getId( ) ) );
  }

  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .omitNullValues( )
        .add( "node", getNode( ) )
        .add( "details", details )
        .add( "status", status )
        .add( "availability", availability.get( ) )
        .toString( );
  }

  private static final class ClusterNodeDetails {
    private static final ClusterNodeDetails NONE =
        ClusterNodeDetails.of( null, null, null, null );

    private final String hypervisor;
    private final String iqn;
    private final Boolean migrationCapable;
    private final String publicSubnets;

    ClusterNodeDetails(
        final String hypervisor,
        final String iqn,
        final Boolean migrationCapable,
        final String publicSubnets
    ) {
      this.hypervisor = hypervisor;
      this.iqn = iqn;
      this.migrationCapable = migrationCapable;
      this.publicSubnets = publicSubnets;
    }

    static ClusterNodeDetails of(
        final String hypervisor,
        final String iqn,
        final Boolean migrationCapable,
        final String publicSubnets
    ) {
      return new ClusterNodeDetails( hypervisor, iqn, migrationCapable, publicSubnets );
    }

    static ClusterNodeDetails none( ) {
      return NONE;
    }

    String getHypervisor( ) {
      return hypervisor;
    }

    String getIqn( ) {
      return iqn;
    }

    Boolean getMigrationCapable( ) {
      return migrationCapable;
    }

    String getPublicSubnets( ) {
      return publicSubnets;
    }

    public String toString( ) {
      return MoreObjects.toStringHelper( this )
          .omitNullValues( )
          .add( "hypervisor", getHypervisor( ) )
          .add( "iqn", getIqn( ) )
          .add( "migration-capable", getMigrationCapable( ) )
          .add( "public-subnets", getPublicSubnets( ) )
          .toString( );
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final ClusterNodeDetails that = (ClusterNodeDetails) o;
      return Objects.equals( getHypervisor( ), that.getHypervisor( ) ) &&
          Objects.equals( getIqn( ), that.getIqn( ) ) &&
          Objects.equals( getMigrationCapable( ), that.getMigrationCapable( ) ) &&
          Objects.equals( getPublicSubnets( ), that.getPublicSubnets( ) );
    }

    @Override
    public int hashCode() {
      return Objects.hash( getHypervisor( ), getIqn( ), getMigrationCapable( ), getPublicSubnets( ) );
    }
  }

  private static final class ClusterNodeStatus {
    private final String status;
    private final String detail;

    ClusterNodeStatus(
        final String status,
        final String detail
    ) {
      this.status = status;
      this.detail = detail;
    }

    public static ClusterNodeStatus of(
        final String status,
        final String detail
    ) {
      return new ClusterNodeStatus( status, detail );
    }

    String getStatus( ) {
      return status;
    }

    String getDetail( ) {
      return detail;
    }

    public String toString( ) {
      return MoreObjects.toStringHelper( this )
          .add( "status", getStatus( ) )
          .add( "detail", getDetail( ) )
          .toString( );
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final ClusterNodeStatus that = (ClusterNodeStatus) o;
      return Objects.equals( getStatus( ), that.getStatus( ) ) &&
          Objects.equals( getDetail( ), that.getDetail( ) );
    }

    @Override
    public int hashCode() {
      return Objects.hash( getStatus( ), getDetail( ) );
    }
  }

  private static final class ClusterNodeAvailability {
    private static final ClusterNodeAvailability NONE = ClusterNodeAvailability.of( 0, 0, 0 );

    private final int coresAvailable;
    private final int coresTotal;
    private final int diskAvailable;
    private final int diskTotal;
    private final int memoryAvailable;
    private final int memoryTotal;

    ClusterNodeAvailability(
        final int coresAvailable,
        final int coresTotal,
        final int diskAvailable,
        final int diskTotal,
        final int memoryAvailable,
        final int memoryTotal
    ) {
      this.coresAvailable = coresAvailable;
      this.coresTotal = coresTotal;
      this.diskAvailable = diskAvailable;
      this.diskTotal = diskTotal;
      this.memoryAvailable = memoryAvailable;
      this.memoryTotal = memoryTotal;
    }

    static ClusterNodeAvailability of(
        final int coresAvailable,
        final int coresTotal,
        final int diskAvailable,
        final int diskTotal,
        final int memoryAvailable,
        final int memoryTotal
    ) {
      return new ClusterNodeAvailability(
          coresAvailable,
          coresTotal,
          diskAvailable,
          diskTotal,
          memoryAvailable,
          memoryTotal
      );
    }

    static ClusterNodeAvailability of(
        final int coresTotal,
        final int diskTotal,
        final int memoryTotal
    ) {
      return of(
          coresTotal,
          coresTotal,
          diskTotal,
          diskTotal,
          memoryTotal,
          memoryTotal
      );
    }

    static ClusterNodeAvailability none( ) {
      return NONE;
    }

    int getCoresAvailable( ) {
      return coresAvailable;
    }

    int getCoresTotal( ) {
      return coresTotal;
    }

    int getDiskAvailable( ) {
      return diskAvailable;
    }

    int getDiskTotal( ) {
      return diskTotal;
    }

    int getMemoryAvailable( ) {
      return memoryAvailable;
    }

    int getMemoryTotal( ) {
      return memoryTotal;
    }

    public String toString( ) {
      return MoreObjects.toStringHelper( this )
          .add( "cores-available", getCoresAvailable( ) )
          .add( "cores-total", getCoresTotal( ) )
          .add( "disk-available", getDiskAvailable( ) )
          .add( "disk-total", getDiskTotal( ) )
          .add( "memory-available", getMemoryAvailable( ) )
          .add( "memory-total", getMemoryTotal( ) )
          .toString( );
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final ClusterNodeAvailability that = (ClusterNodeAvailability) o;
      return getCoresAvailable( ) == that.getCoresAvailable( ) &&
          getCoresTotal( ) == that.getCoresTotal( ) &&
          getDiskAvailable( ) == that.getDiskAvailable( ) &&
          getDiskTotal( ) == that.getDiskTotal( ) &&
          getMemoryAvailable( ) == that.getMemoryAvailable( ) &&
          getMemoryTotal( ) == that.getMemoryTotal( );
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          getCoresAvailable( ),
          getCoresTotal( ),
          getDiskAvailable( ),
          getDiskTotal( ),
          getMemoryAvailable( ),
          getMemoryTotal( ) );
    }
  }
}
