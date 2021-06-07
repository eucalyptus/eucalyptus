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
package com.eucalyptus.cluster;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.msgs.ClusterMigrateInstancesType;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.compute.common.internal.vm.MigrationState;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceStateException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.images.Emis;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 *
 */
public class Migrations {
  private static Logger LOG = Logger.getLogger( Migrations.class );

  private final Cluster cluster;
  private final ReadWriteLock gateLock;
  private final Predicate<VmInstance> filterPartition = new Predicate<VmInstance>( ) {
    @Override
    public boolean apply( VmInstance input ) {
      return input.getPartition( ).equals( getPartition( ) ) && MigrationState.isMigrating( input );
    }
  };



  private Migrations( final Cluster cluster ) {
    this.cluster = cluster;
    this.gateLock = cluster.getGateLock( );
  }

  public static Migrations using( @Nonnull final Cluster cluster ) {
    return new Migrations( cluster );
  }

  private ServiceConfiguration getConfiguration( ) {
    return cluster.getConfiguration( );
  }

  private String getPartition( ) {
    return cluster.getPartition( );
  }

  /**
   * <ol>
   * <li> Mark this cluster as gated.
   * <li> Update node and resource information; describe resources.
   * <li> Find all VMs and update their migration state and volumes
   * <li> Send the MigrateInstances operation.
   * <li> Update node and resource information; describe resources.
   * <li> Unmark this cluster as gated.
   * </ol>
   * @param sourceHost
   * @param destHostsWhiteList -- the destination host list is a white list when true and a black list when false
   * @param destHosts -- list of hosts which are either a white list or black list based on {@code destHostsWhiteList}
   * @throws EucalyptusCloudException
   * @throws Exception
   */
  public void migrateInstances( final String sourceHost, final Boolean destHostsWhiteList, final List<String> destHosts ) throws Exception {
    //#1 Mark this cluster as gated.
    if ( this.gateLock.writeLock( ).tryLock( 60, TimeUnit.SECONDS ) ) {
      try {
        //#2 Only one migration per cluster for now
        List<VmInstance> currentMigrations = this.lookupCurrentMigrations( );
        if ( !currentMigrations.isEmpty( ) ) {
          throw Exceptions.toUndeclared( "Cannot start a new migration because the following are already ongoing: "
              + Joiner.on( ", " ).join( Iterables.transform( currentMigrations, CloudMetadatas.toDisplayName( ) ) ) );
        }
        //#3 Update node and resource information
        this.retryCheck( );
        //#4 Find all VMs and update their migration state and volumes
        List<String> instanceIds = this.prepareInstanceEvacuations( sourceHost );
        //#5 Send the MigrateInstances operation.
        try {
          //Get updated download manifests for PV instances
          final Map<Boolean, Set<String>> updatedResources = getFreshBootrecords(instanceIds, true);

          final ClusterMigrateInstancesType migrateInstances = new ClusterMigrateInstancesType( );
          migrateInstances.setCorrelationId( Contexts.lookup( ).getCorrelationId( ) );
          migrateInstances.setSourceHost( sourceHost );
          migrateInstances.setResourceLocations( Lists.newArrayList(updatedResources.get(true)));
          migrateInstances.setAllowHosts( destHostsWhiteList );
          migrateInstances.getDestinationHosts( ).addAll( destHosts );
          AsyncRequests.sendSync( this.getConfiguration( ), migrateInstances );
        } catch ( Exception ex ) {
          //#5 On error go back and abort the migration status for every instance
          this.rollbackInstanceEvacuations( sourceHost );
          throw ex;
        }
        //#6 Update node and resource information; describe resources.
        this.retryCheck( );
      } catch ( Exception ex ) {
        LOG.error( ex );
        throw ex;
      } finally {
        //#6 Unmark this cluster as gated.
        this.gateLock.writeLock( ).unlock( );
      }
    } else {
      throw new ServiceStateException( "Failed to request migration in the zone " + this.getPartition( ) + ", it is currently locked for maintenance." );
    }
  }

  /**
   * Given a list of instance IDs, return a list of VmTypeInfos with updated download manifest URLs
   * that are valid for the default timeout (hours) for the instances in the id list that are PV
   * instances. Thus, length of input and output lists may vary due to filtering.
   * @param instanceIdsToRefresh
   * @return map of true->Set of eki/eri=signedUrls and false->Set of instanceIds with some failure (e.g eri/eki not found)
   */
  protected static Map<Boolean, Set<String>> getFreshBootrecords(List<String> instanceIdsToRefresh, boolean pvOnly)
      throws MetadataException {
    VmInstance vm;
    Map<Boolean, Set<String>> outputMap = Maps.newHashMap();
    outputMap.put(true, new HashSet<String>());
    outputMap.put(false, new HashSet<String>());

    for(String id : instanceIdsToRefresh) {
      try ( final TransactionResource db = Entities.transactionFor(VmInstance.class) ) {//scope for transaction
        vm = VmInstances.lookup(id);
        //Only update PV images, because NC needs URLs for ramdisk and kernels
        if(pvOnly && ImageMetadata.VirtualizationType.paravirtualized.equals(
            ImageMetadata.VirtualizationType.fromString().apply(vm.getVirtualizationType()))) {

          Emis.BootableSet bs = Emis.recreateBootableSet(vm);

          if(bs.hasKernel() && !outputMap.get(true).contains(bs.getKernel().getDisplayName())) {
            try {
              outputMap.get(true).add(bs.getKernel().getDisplayName() + "=" + bs
                  .getKernelDownloadManifest(
                      Partitions.lookupByName(vm.getPartition()).getNodeCertificate()
                          .getPublicKey(), vm.getReservationId()));
            } catch(MetadataException ex) {
              LOG.warn("Could not get kernel download manifest for migration of instance: " + id + ". Migration may fail for this instance", ex);
              throw ex;
            }
          }

          if(bs.hasRamdisk() && !outputMap.get(true).contains(bs.getRamdisk().getDisplayName())) {
            try {
              outputMap.get(true).add(bs.getRamdisk().getDisplayName() + "=" + bs
                  .getRamdiskDownloadManifest(
                      Partitions.lookupByName(vm.getPartition()).getNodeCertificate()
                          .getPublicKey(), vm.getReservationId()));
            } catch(MetadataException ex) {
              LOG.warn("Could not get ramdisk download manifest for migration of instance: " + id + ". Migration may fail for this instance", ex);
              throw ex;
            }
          }
        }
      } catch (Exception e) {
        LOG.warn("Failure during update of download manifest while building new bootset. May not be able migrate this instance: " + id, e);
        outputMap.get(false).add(id);
      }
    }

    return outputMap;
  }

  /**
   * <ol>
   * <li> Mark this cluster as gated.
   * <li> Update node and resource information; describe resources.
   * <li> Find the VM and its volume attachments and authorize every node's IQN.
   * <li> Send the MigrateInstances operation.
   * <li> Update node and resource information; describe resources.
   * <li> Unmark this cluster as gated.
   * </ol>
   * @param destHostsWhiteList -- the destination host list is a white list when true and a black list when false
   * @param destHosts -- list of hosts which are either a white list or black list based on {@code destHostsWhiteList}
   * @throws EucalyptusCloudException
   * @throws Exception
   */
  public void migrateInstance( final String instanceId, final Boolean destHostsWhiteList, final List<String> destHosts ) throws Exception {
    //#1 Mark this cluster as gated.
    if ( this.gateLock.writeLock( ).tryLock( 60, TimeUnit.SECONDS ) ) {
      try {
        //#2 Only one migration per cluster for now
        List<VmInstance> currentMigrations = this.lookupCurrentMigrations( );
        if ( !currentMigrations.isEmpty( ) ) {
          throw Exceptions.toUndeclared( "Cannot start a new migration because the following are already ongoing: "
              + Joiner.on( ", " ).join( Iterables.transform( currentMigrations, CloudMetadatas.toDisplayName( ) ) ) );
        }
        //#3 Update node and resource information
        this.retryCheck( );
        //#4 Find all VMs and update their migration state and volumes
        this.prepareInstanceMigrations( instanceId );

        try {
          //Get updated download manifests for PV instances
          final Map<Boolean, Set<String>> updatedResources = getFreshBootrecords( ImmutableList.of(instanceId), true);

          //#5 Send the MigrateInstances operation.
          final ClusterMigrateInstancesType migrateInstances = new ClusterMigrateInstancesType( );
          migrateInstances.setCorrelationId( Contexts.lookup( ).getCorrelationId());
          migrateInstances.setInstanceId(instanceId);
          migrateInstances.setResourceLocations(Lists.newArrayList(updatedResources.get(true)));
          migrateInstances.setAllowHosts(destHostsWhiteList);
          migrateInstances.getDestinationHosts( ).addAll( destHosts );
          AsyncRequests.sendSync( this.getConfiguration( ), migrateInstances );
        } catch ( Exception ex ) {
          //#5 On error go back and abort the migration status for every instance
          this.rollbackInstanceMigrations( instanceId );
          throw ex;
        }
        //#6 Update node and resource information; describe resources.
        this.retryCheck( );
      } catch ( Exception ex ) {
        LOG.error( ex );
        throw ex;
      } finally {
        //#6 Unmark this cluster as gated.
        this.gateLock.writeLock( ).unlock( );
      }
    } else {
      throw new ServiceStateException( "Failed to request migration in the zone " + this.getPartition( ) + ", it is currently locked for maintenance." );
    }
  }

  private void rollbackInstanceEvacuations( final String sourceHost ) {
    Predicate<VmInstance> filterHost = new Predicate<VmInstance>( ) {

      @Override
      public boolean apply( @Nullable VmInstance input ) {
        String vmHost = URI.create( input.getServiceTag( ) ).getHost( );
        return Strings.nullToEmpty( vmHost ).equals( sourceHost );
      }
    };
    Predicate<VmInstance> rollbackMigration = new Predicate<VmInstance>( ) {

      @Override
      public boolean apply( @Nullable VmInstance input ) {
        VmInstances.abortMigration( input );
        return true;
      }
    };
    Predicate<VmInstance> filterAndAbort = Predicates.and( this.filterPartition, rollbackMigration );
    Predicate<VmInstance> rollbackMigrationTx = Entities.asTransaction( VmInstance.class, filterAndAbort );
    VmInstances.list( rollbackMigrationTx );
  }

  @SuppressWarnings( "unchecked" )
  private List<String> prepareInstanceEvacuations( final String sourceHost ) {
    Predicate<VmInstance> filterHost = new Predicate<VmInstance>( ) {

      @Override
      public boolean apply( @Nullable VmInstance input ) {
        String vmHost = URI.create( input.getServiceTag( ) ).getHost( );
        return Strings.nullToEmpty( vmHost ).equals( sourceHost );
      }
    };
    Predicate<VmInstance> startMigration = new Predicate<VmInstance>( ) {

      @Override
      public boolean apply( @Nullable VmInstance input ) {
        VmInstances.startMigration( input );
        return true;
      }
    };
    Predicate<VmInstance> filterAndAbort = Predicates.and( this.filterPartition, startMigration );
    Predicate<VmInstance> startMigrationTx = Entities.asTransaction( VmInstance.class, filterAndAbort );
    return Lists.transform(VmInstances.list(startMigrationTx), new Function<VmInstance, String>() {
      @Nullable
      @Override
      public String apply(@Nullable VmInstance vmInstance) {
        return vmInstance.getInstanceId();
      }
    });

  }

  private void rollbackInstanceMigrations( final String instanceId ) {
    Predicate<VmInstance> rollbackMigration = new Predicate<VmInstance>( ) {

      @Override
      public boolean apply( @Nullable VmInstance input ) {
        VmInstances.abortMigration( input );
        return true;
      }
    };
    Predicate<VmInstance> rollbackMigrationTx = Entities.asTransaction( VmInstance.class, rollbackMigration );
    rollbackMigrationTx.apply( VmInstances.lookup( instanceId ) );
  }

  @SuppressWarnings( "unchecked" )
  private void prepareInstanceMigrations( final String instanceId ) {
    Predicate<VmInstance> startMigration = new Predicate<VmInstance>( ) {

      @Override
      public boolean apply( @Nullable VmInstance input ) {
        VmInstances.startMigration( input );
        return true;
      }
    };
    Predicate<VmInstance> startMigrationTx = Entities.asTransaction( VmInstance.class, startMigration );
    startMigrationTx.apply( VmInstances.lookup( instanceId ) );
  }

  private List<VmInstance> lookupCurrentMigrations( ) throws Exception {
    return VmInstances.list( this.filterPartition );
  }

  private void retryCheck( ) throws Exception {
    Exception lastEx = null;
    for ( int i = 0; i < 5; i++ ) {
      try {
        this.cluster.check( );
        return;
      } catch ( Exception ex ) {
        LOG.debug( "Retrying after failed attempt to refresh cluster state in check(): " + ex.getMessage( ) );
        lastEx = ex;
        TimeUnit.SECONDS.sleep( 2 );
      }
    }
    throw new ServiceStateException( "Failed to request migration in the zone "
        + this.getPartition( )
        + " because updating resources returned an error: "
        + ( lastEx != null ? lastEx.getMessage( ) : "unknown error" ) );
  }


}
