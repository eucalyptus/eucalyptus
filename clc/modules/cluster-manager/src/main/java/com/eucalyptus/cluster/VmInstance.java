/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.cluster;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import com.eucalyptus.cloud.CloudMetadata.VmInstanceMetadata;
import com.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.cloud.UserMetadata;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cloud.util.Resource.SetReference;
import com.eucalyptus.cloud.util.ResourceAllocationException;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Dns;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionExecutionException;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.images.Emis.BootableSet;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.Networks;
import com.eucalyptus.network.PrivateNetworkIndex;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.InstanceEvent;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.vm.BundleTask;
import com.eucalyptus.vm.VmState;
import com.eucalyptus.vm.VmType;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;
import edu.ucsb.eucalyptus.msgs.InstanceBlockDeviceMapping;
import edu.ucsb.eucalyptus.msgs.NetworkConfigType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_instances" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class VmInstance extends UserMetadata<VmState> implements VmInstanceMetadata<VmInstance> {
  private static final long       serialVersionUID = 1L;
  @Transient
  private static Logger           LOG              = Logger.getLogger( VmInstance.class );
  @Transient
  public static String            DEFAULT_TYPE     = "m1.small";
  @Embedded
  private final VmNetworkConfig   networkConfig;
  @Embedded
  private final VmId              vmId;
  @Embedded
  private final VmBootRecord      bootRecord;
  @Embedded
  private final VmUsageStats      usageStats;
  @Embedded
  private final VmLaunchRecord    launchRecord;
  @Embedded
  private final VmRuntimeState    runtimeState;
  @Embedded
  private final VmPlacement       placement;
  
  @Column( name = "metadata_vm_private_networking" )
  private final Boolean           privateNetwork;
  @ManyToMany
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private final Set<NetworkGroup> networkGroups    = Sets.newHashSet( );
  
  @NotFound( action = NotFoundAction.IGNORE )
  @OneToOne( fetch = FetchType.EAGER, cascade = CascadeType.REMOVE )
  @JoinColumn( name = "metadata_vm_network_index", nullable = true, insertable = true, updatable = true )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private PrivateNetworkIndex     networkIndex;
  
  public enum CreateAllocation implements Function<ResourceToken, VmInstance> {
    INSTANCE;
    
    /**
     * @see com.google.common.base.Predicate#apply(java.lang.Object)
     */
    @Override
    public VmInstance apply( final ResourceToken token ) {
      final EntityTransaction db = Entities.get( VmInstance.class );
      try {
        final Allocation allocInfo = token.getAllocationInfo( );
        VmInstance vmInst = new VmInstance.Builder( ).owner( allocInfo.getOwnerFullName( ) )
                                                     .withIds( token.getInstanceId( ), allocInfo.getReservationId( ) )
                                                     .bootRecord( allocInfo.getBootSet( ),
                                                                  allocInfo.getUserData( ),
                                                                  allocInfo.getSshKeyPair( ),
                                                                  allocInfo.getVmType( ) )
                                                     .placement( allocInfo.getPartition( ), allocInfo.getRequest( ).getAvailabilityZone( ) )
                                                     .networking( allocInfo.getNetworkGroups( ), token.getNetworkIndex( ) )
                                                     .build( token.getLaunchIndex( ) );
        vmInst = Entities.persist( vmInst );
        token.getNetworkIndex( ).set( vmInst );
        db.commit( );
        token.setVmInstance( vmInst );
        return vmInst;
      } catch ( final ResourceAllocationException ex ) {
        db.rollback( );
        Logs.extreme( ).error( ex, ex );
        throw Exceptions.toUndeclared( ex );
      } catch ( final Exception ex ) {
        db.rollback( );
        Logs.extreme( ).error( ex, ex );
        throw Exceptions.toUndeclared( new TransactionExecutionException( ex ) );
      }
    }
    
  }
  
  public static class Builder {
    VmId                                          vmId;
    VmBootRecord                                  vmBootRecord;
    VmUsageStats                                  vmUsageStats;
    VmPlacement                                   vmPlacement;
    VmLaunchRecord                                vmLaunchRecord;
    List<NetworkGroup>                            networkRulesGroups;
    SetReference<PrivateNetworkIndex, VmInstance> networkIndex;
    OwnerFullName                                 owner;
    
    public Builder owner( final OwnerFullName owner ) {
      this.owner = owner;
      return this;
    }
    
    public Builder networking( final List<NetworkGroup> groups, final SetReference<PrivateNetworkIndex, VmInstance> networkIndex ) {
      this.networkRulesGroups = groups;
      this.networkIndex = networkIndex;
      return this;
    }
    
    public Builder withIds( final String instanceId, final String reservationId ) {
      this.vmId = new VmId( reservationId, instanceId );
      return this;
    }
    
    public Builder placement( final Partition partition, final String clusterName ) {
      final ServiceConfiguration config = Partitions.lookupService( ClusterController.class, partition );
      this.vmPlacement = new VmPlacement( config.getName( ), config.getPartition( ) );
      return this;
    }
    
    public Builder bootRecord( final BootableSet bootSet, final byte[] userData, final SshKeyPair sshKeyPair, final VmType vmType ) {
      this.vmBootRecord = new VmBootRecord( bootSet, userData, sshKeyPair, vmType );
      return this;
    }
    
    private ServiceConfiguration lookupServiceConfiguration( final String name ) {
      ServiceConfiguration config = null;
      try {
        config = ServiceConfigurations.lookupByName( ClusterController.class, name );
      } catch ( final PersistenceException ex ) {
        LOG.debug( "Failed to find cluster configuration named: " + name + " using that as the partition name." );
      }
      return config;
    }
    
    public VmInstance build( final Integer launchndex ) {
      return new VmInstance( this.owner, this.vmId, this.vmBootRecord, new VmLaunchRecord( launchndex, new Date( ) ), this.vmPlacement,
                             this.networkRulesGroups, this.networkIndex );
    }
  }
  
  private VmInstance( final OwnerFullName owner,
                      final VmId vmId,
                      final VmBootRecord bootRecord,
                      final VmLaunchRecord launchRecord,
                      final VmPlacement placement,
                      final List<NetworkGroup> networkRulesGroups,
                      final SetReference<PrivateNetworkIndex, VmInstance> networkIndex ) {
    super( owner, vmId.getInstanceId( ) );
    this.vmId = vmId;
    this.bootRecord = bootRecord;
    this.launchRecord = launchRecord;
    this.placement = placement;
    this.privateNetwork = Boolean.FALSE;
    this.usageStats = new VmUsageStats( this );
    this.networkIndex = networkIndex.get( );
    this.networkGroups.addAll( networkRulesGroups );
    this.runtimeState = new VmRuntimeState( this );
    this.networkConfig = new VmNetworkConfig( this );
    this.store( );
  }
  
  protected VmInstance( final OwnerFullName ownerFullName, final String instanceId2 ) {
    super( ownerFullName, instanceId2 );
    this.runtimeState = null;
    this.vmId = null;
    this.bootRecord = null;
    this.launchRecord = null;
    this.placement = null;
    this.privateNetwork = null;
    this.usageStats = null;
    this.networkConfig = null;
  }
  
  protected VmInstance( ) {
    this.vmId = null;
    this.bootRecord = null;
    this.launchRecord = null;
    this.placement = null;
    this.privateNetwork = null;
    this.networkIndex = null;
    this.usageStats = null;
    this.runtimeState = null;
    this.networkConfig = null;
  }
  
  @PrePersist
  @PreUpdate
  private void preLoad( ) {
    this.setState( this.runtimeState.getRuntimeState( ) );
    for ( final VmVolumeAttachment vol : this.runtimeState.getTransientVolumeAttachments( ) ) {
      this.runtimeState.getTransientVolumes( ).put( vol.getVolumeId( ), vol );
    }
  }
  
//  @PostLoad
//  private void postLoad( ) {
//    this.runtimeState.setState( this.getState( ), Reason.NORMAL );
//  }
  
  public void updateBlockBytes( final long blkbytes ) {
    this.usageStats.setBlockBytes( this.usageStats.getBlockBytes( ) + blkbytes );
  }
  
  public void updateNetworkBytes( final long netbytes ) {
    this.usageStats.setNetworkBytes( this.usageStats.getNetworkBytes( ) + netbytes );
  }
  
  public void updateAddresses( final String privateAddr, final String publicAddr ) {
    this.updatePrivateAddress( privateAddr );
    this.updatePublicAddress( publicAddr );
  }
  
  public void updatePublicAddress( final String publicAddr ) {
    if ( !VmNetworkConfig.DEFAULT_IP.equals( publicAddr ) && !"".equals( publicAddr )
         && ( publicAddr != null ) ) {
      this.networkConfig.setPublicAddress( publicAddr );
    }
  }
  
  public void updatePrivateAddress( final String privateAddr ) {
    if ( !VmNetworkConfig.DEFAULT_IP.equals( privateAddr ) && !"".equals( privateAddr ) && ( privateAddr != null ) ) {
      this.networkConfig.setPrivateAddress( privateAddr );
    }
    this.networkConfig.updateDns( );
  }
  
  public VmState getRuntimeState( ) {
    return this.runtimeState.getRuntimeState( );
  }
  
  public void setRuntimeState( final VmState state ) {
    this.runtimeState.setState( state, Reason.NORMAL );
  }
  
  void store( ) {
    this.setState( this.getRuntimeState( ) );
    this.fireUsageEvent( );
    this.firePersist( );
  }
  
  private void firePersist( ) {
    final EntityTransaction db = Entities.get( VmInstance.class );
    if ( !Entities.isPersistent( this ) ) {
      db.rollback( );
    } else {
      try {
        Entities.merge( this );
        db.commit( );
      } catch ( final Exception ex ) {
        db.rollback( );
        LOG.debug( ex );
      }
    }
  }
  
  private void fireUsageEvent( ) {
    try {
      ListenerRegistry.getInstance( ).fireEvent( new InstanceEvent( this.getInstanceUuid( ), this.getDisplayName( ),
                                                                    this.bootRecord.getVmType( ).getName( ),
                                                                    this.getOwner( ).getUserId( ), this.getOwnerUserName( ),
                                                                    this.getOwner( ).getAccountNumber( ), this.getOwnerAccountName( ),
                                                                    this.placement.getClusterName( ), this.placement.getPartitionName( ),
                                                                    this.usageStats.getNetworkBytes( ), this.usageStats.getBlockBytes( ) ) );
    } catch ( final EventFailedException ex ) {
      LOG.error( ex, ex );
    }
  }
  
  public String getByKey( final String pathArg ) {
    final Map<String, String> m = this.getMetadataMap( );
    String path = ( pathArg != null )
      ? pathArg
      : "";
    LOG.debug( "Servicing metadata request:" + path + " -> " + m.get( path ) );
    if ( m.containsKey( path + "/" ) ) path += "/";
    return m.get( path ).replaceAll( "\n*\\z", "" );
  }
  
  private Map<String, String> getMetadataMap( ) {
    final boolean dns = !ComponentIds.lookup( Dns.class ).runLimitedServices( );
    final Map<String, String> m = new HashMap<String, String>( );
    //ASAP: FIXME: GRZE:
//    m.put( "ami-id", this.getImageInfo( ).getImageId( ) );
//    m.put( "product-codes", this.getImageInfo( ).getProductCodes( ).toString( ).replaceAll( "[\\Q[]\\E]", "" ).replaceAll( ", ", "\n" ) );
    m.put( "ami-launch-index", "" + this.launchRecord.getLaunchIndex( ) );
//    m.put( "ancestor-ami-ids", this.getImageInfo( ).getAncestorIds( ).toString( ).replaceAll( "[\\Q[]\\E]", "" ).replaceAll( ", ", "\n" ) );
    
//    m.put( "ami-manifest-path", this.getImageInfo( ).getImageLocation( ) );
    m.put( "hostname", this.getPublicAddress( ) );
    m.put( "instance-id", this.getInstanceId( ) );
    m.put( "instance-type", this.getVmType( ).getName( ) );
    if ( dns ) {
      m.put( "local-hostname", this.networkConfig.getPrivateDnsName( ) );
    } else {
      m.put( "local-hostname", this.networkConfig.getPrivateAddress( ) );
    }
    m.put( "local-ipv4", this.networkConfig.getPrivateAddress( ) );
    if ( dns ) {
      m.put( "public-hostname", this.networkConfig.getPublicDnsName( ) );
    } else {
      m.put( "public-hostname", this.getPublicAddress( ) );
    }
    m.put( "public-ipv4", this.getPublicAddress( ) );
    m.put( "reservation-id", this.vmId.getReservationId( ) );
    m.put( "kernel-id", this.bootRecord.getKernel( ).getDisplayName( ) );
    if ( this.bootRecord.getRamdisk( ) != null ) {
      m.put( "ramdisk-id", this.bootRecord.getRamdisk( ).getDisplayName( ) );
    }
    m.put( "security-groups", this.getNetworkNames( ).toString( ).replaceAll( "[\\Q[]\\E]", "" ).replaceAll( ", ", "\n" ) );
    
    m.put( "block-device-mapping/", "emi\nephemeral\nephemeral0\nroot\nswap" );
    m.put( "block-device-mapping/emi", "sda1" );
    m.put( "block-device-mapping/ami", "sda1" );
    m.put( "block-device-mapping/ephemeral", "sda2" );
    m.put( "block-device-mapping/ephemeral0", "sda2" );
    m.put( "block-device-mapping/swap", "sda3" );
    m.put( "block-device-mapping/root", "/dev/sda1" );
    
    m.put( "public-keys/", "0=" + this.bootRecord.getSshKeyPair( ).getName( ) );
    m.put( "public-keys/0", "openssh-key" );
    m.put( "public-keys/0/", "openssh-key" );
    m.put( "public-keys/0/openssh-key", this.bootRecord.getSshKeyPair( ).getPublicKey( ) );
    
    m.put( "placement/", "availability-zone" );
    m.put( "placement/availability-zone", this.getPartition( ) );
    String dir = "";
    for ( final String entry : m.keySet( ) ) {
      if ( ( entry.contains( "/" ) && !entry.endsWith( "/" ) ) ) {
//          || ( "ramdisk-id".equals(entry) && this.getImageInfo( ).getRamdiskId( ) == null ) ) {
        continue;
      }
      dir += entry + "\n";
    }
    m.put( "", dir );
    return m;
  }
  
  @Override
  public int compareTo( final VmInstance that ) {
    return this.getName( ).compareTo( that.getName( ) );
  }
  
  public synchronized long getSplitTime( ) {
    final long time = System.currentTimeMillis( );
    final long split = time - super.getLastUpdateTimestamp( ).getTime( );
    return split;
  }
  
  public enum BundleState {
    none( "none" ), pending( null ), storing( "bundling" ), canceling( null ), complete( "succeeded" ), failed( "failed" );
    private String mappedState;
    
    BundleState( final String mappedState ) {
      this.mappedState = mappedState;
    }
    
    public String getMappedState( ) {
      return this.mappedState;
    }
  }
  
  public Boolean isBundling( ) {
    return this.runtimeState.isBundling( );
  }
  
  public VmBundleTask resetBundleTask( ) {
    return this.runtimeState.resetBundleTask( );
  }
  
  private BundleState getBundleTaskState( ) {
    return this.runtimeState.getBundleTaskState( );
  }
  
  public void setBundleTaskState( final String state ) {
    this.runtimeState.setBundleTaskState( state );
  }
  
  public String getImageId( ) {
    return this.bootRecord.getMachine( ).getDisplayName( );
  }
  
  public boolean hasPublicAddress( ) {
    return ( this.networkConfig != null )
           && !( VmNetworkConfig.DEFAULT_IP.equals( this.networkConfig.getPublicAddress( ) ) || this.networkConfig.getPrivateAddress( ).equals( this.networkConfig.getPublicAddress( ) ) );
  }
  
  public String getInstanceId( ) {
    return super.getDisplayName( );
  }
  
  public String getConsoleOutputString( ) {
    return new String( Base64.encode( this.runtimeState.getConsoleOutput( ).toString( ).getBytes( ) ) );
  }
  
  public void setConsoleOutput( final StringBuffer consoleOutput ) {
    this.runtimeState.setConsoleOutput( consoleOutput );
  }
  
  public VmType getVmType( ) {
    return this.bootRecord.getVmType( );
  }
  
  public Set<NetworkGroup> getNetworkRulesGroups( ) {
    return this.networkGroups;
  }
  
  public List<NetworkGroup> getNetworks( ) {
    return Lists.newArrayList( this.networkGroups );
  }
  
  public NavigableSet<String> getNetworkNames( ) {
    return new TreeSet<String>( Collections2.transform( this.networkGroups, new Function<NetworkGroup, String>( ) {
      
      @Override
      public String apply( final NetworkGroup arg0 ) {
        return arg0.getDisplayName( );
      }
    } ) );
  }
  
  public String getPrivateAddress( ) {
    return this.networkConfig.getPrivateAddress( );
  }
  
  public String getPublicAddress( ) {
    return this.networkConfig.getPublicAddress( );
  }
  
  public String getPrivateDnsName( ) {
    return this.networkConfig.getPrivateDnsName( );
  }
  
  public String getPublicDnsName( ) {
    return this.networkConfig.getPublicDnsName( );
  }
  
  public String getPasswordData( ) {
    return this.runtimeState.getPasswordData( );
  }
  
  public void setPasswordData( final String passwordData ) {
    this.runtimeState.setPasswordData( passwordData );
  }
  
  /**
   * @return the platform
   */
  public String getPlatform( ) {
    return this.bootRecord.getPlatform( );
  }
  
  /**
   * @return the bundleTask
   */
  public BundleTask getBundleTask( ) {
    return VmBundleTask.asBundleTask( this ).apply( this.runtimeState.getBundleTask( ) );
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
  
  public static VmInstance named( final OwnerFullName ownerFullName, final String instanceId ) {
    return new VmInstance( ownerFullName, instanceId );
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
  
  public enum Reason {
    NORMAL( "" ),
    EXPIRED( "Instance expired after not being reported for %s ms.", VmInstances.SHUT_DOWN_TIME ),
    FAILED( "The instance failed to start on the NC." ),
    USER_TERMINATED( "User initiated terminate." ),
    USER_STOPPED( "User initiated stop." ),
    BURIED( "Instance buried after timeout of %s ms.", VmInstances.BURY_TIME ),
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
    
  }
  
  private PrivateNetworkIndex getNetworkIndex( ) {
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
  
  private Set<NetworkGroup> getNetworkGroups( ) {
    return this.networkGroups;
  }
  
  @Override
  public String toString( ) {
    final StringBuilder builder2 = new StringBuilder( );
    builder2.append( "VmInstance:" );
    if ( this.vmId != null ) builder2.append( "vmId=" ).append( this.vmId ).append( ":" );
    if ( this.networkConfig != null ) builder2.append( "networkConfig=" ).append( this.networkConfig ).append( ":" );
    if ( this.privateNetwork != null ) builder2.append( "privateNetwork=" ).append( this.privateNetwork ).append( ":" );
    if ( this.placement != null ) builder2.append( "placement=" ).append( this.placement ).append( ":" );
    if ( this.launchRecord != null ) builder2.append( "launchRecord=" ).append( this.launchRecord ).append( ":" );
    if ( this.networkIndex != null ) builder2.append( "networkIndex=" ).append( this.networkIndex ).append( ":" );
    if ( this.runtimeState != null ) builder2.append( "runtimeState=" ).append( this.runtimeState ).append( ":" );
    if ( this.networkGroups != null ) builder2.append( "networkGroups=" ).append( this.networkGroups ).append( ":" );
    if ( this.bootRecord != null ) builder2.append( "bootRecord=" ).append( this.bootRecord ).append( ":" );
    if ( this.usageStats != null ) builder2.append( "usageStats=" ).append( this.usageStats );
    return builder2.toString( );
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
  
  VmBootRecord getBootRecord( ) {
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
   * @param stopping
   * @param reason
   */
  public void setState( final VmState stopping, final Reason reason, final String... extra ) {
    this.runtimeState.setState( stopping, reason, extra );
  }
  
  /**
   * @param predicate
   */
  public void lookupVolumeAttachment( final Predicate<AttachedVolume> predicate ) {
    this.runtimeState.lookupVolumeAttachment( new Predicate<VmVolumeAttachment>( ) {
      
      @Override
      public boolean apply( final VmVolumeAttachment vol ) {
        return predicate.apply( VmVolumeAttachment.asAttachedVolume( VmInstance.this ).apply( vol ) );
      }
    } );
  }
  
  /**
   * @param volumeId
   * @return
   */
  public AttachedVolume lookupVolumeAttachment( final String volumeId ) {
    return VmVolumeAttachment.asAttachedVolume( this ).apply( this.runtimeState.lookupVolumeAttachment( volumeId ) );
  }
  
  /**
   * @param attachVol
   */
  public void addVolumeAttachment( final AttachedVolume vol ) {
    this.runtimeState.addVolumeAttachment( VmVolumeAttachment.fromAttachedVolume( this ).apply( vol ) );
  }
  
  /**
   * @return
   */
  public ServiceConfiguration lookupClusterConfiguration( ) {
    return Partitions.lookupService( ClusterController.class, this.lookupPartition( ) );
  }
  
  /**
   * @param predicate
   * @return
   */
  public boolean eachVolumeAttachment( final Predicate<AttachedVolume> predicate ) {
    return this.runtimeState.eachVolumeAttachment( new Predicate<VmVolumeAttachment>( ) {
      
      @Override
      public boolean apply( final VmVolumeAttachment arg0 ) {
        return predicate.apply( VmVolumeAttachment.asAttachedVolume( VmInstance.this ).apply( arg0 ) );
      }
    } );
  }
  
  /**
   * @param volumeId
   * @return
   */
  public AttachedVolume removeVolumeAttachment( final String volumeId ) {
    return VmVolumeAttachment.asAttachedVolume( this ).apply( this.runtimeState.removeVolumeAttachment( volumeId ) );
  }
  
  /**
   * @return
   */
  public String getServiceTag( ) {
    return this.runtimeState.getServiceTag( );
  }
  
  /**
   * @return
   */
  public String getReservationId( ) {
    return this.vmId.getReservationId( );
  }
  
  /**
   * @return
   */
  public byte[] getUserData( ) {
    return this.bootRecord.getUserData( );
  }
  
  public void clearPending( ) {
    this.runtimeState.clearPending( );
  }
  
  public void clearPendingBundleTask( ) {
    this.runtimeState.clearPendingBundleTask( );
  }
  
  /**
   * @param volumeId
   * @param newState
   */
  public void updateVolumeAttachment( final String volumeId, final String newState ) {
    this.runtimeState.updateVolumeAttachment( volumeId, newState );
  }
  
  /**
   * @return
   */
  public Predicate<VmInfo> doUpdate( ) {
    return new Predicate<VmInfo>( ) {
      
      @Override
      public boolean apply( final VmInfo runVm ) {
        
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          final VmInstance vm = Entities.merge( VmInstance.this );
          final VmState state = VmState.Mapper.get( runVm.getStateName( ) );
          final long splitTime = VmInstance.this.getSplitTime( );
          final VmState oldState = VmInstance.this.getRuntimeState( );
          VmInstance.this.runtimeState.setServiceTag( runVm.getServiceTag( ) );
          VmInstance.this.setBundleTaskState( runVm.getBundleTaskStateName( ) );
          
          if ( VmState.SHUTTING_DOWN.equals( VmInstance.this.getRuntimeState( ) ) && ( splitTime > VmInstances.SHUT_DOWN_TIME ) ) {
            VmInstance.this.setState( VmState.TERMINATED, Reason.EXPIRED );
          } else if ( VmState.STOPPING.equals( VmInstance.this.getRuntimeState( ) ) && ( splitTime > VmInstances.SHUT_DOWN_TIME ) ) {
            VmInstance.this.setState( VmState.STOPPED, Reason.EXPIRED );
          } else if ( VmState.STOPPING.equals( VmInstance.this.getRuntimeState( ) )
                      && VmState.SHUTTING_DOWN.equals( VmState.Mapper.get( runVm.getStateName( ) ) ) ) {
            VmInstance.this.setState( VmState.STOPPED, Reason.APPEND, "STOPPED" );
          } else if ( VmState.SHUTTING_DOWN.equals( VmInstance.this.getRuntimeState( ) )
                      && VmState.SHUTTING_DOWN.equals( VmState.Mapper.get( runVm.getStateName( ) ) ) ) {
            VmInstance.this.setState( VmState.TERMINATED, Reason.APPEND, "DONE" );
          } else if ( ( VmState.PENDING.equals( state ) || VmState.RUNNING.equals( state ) )
                      && ( VmState.PENDING.equals( VmInstance.this.getRuntimeState( ) ) || VmState.RUNNING.equals( VmInstance.this.getRuntimeState( ) ) ) ) {
            if ( !VmNetworkConfig.DEFAULT_IP.equals( runVm.getNetParams( ).getIpAddress( ) ) ) {
              VmInstance.this.updateAddresses( runVm.getNetParams( ).getIpAddress( ), runVm.getNetParams( ).getIgnoredPublicIp( ) );
            }
            VmInstance.this.setState( VmState.Mapper.get( runVm.getStateName( ) ), Reason.APPEND, "UPDATE" );
            VmInstance.this.updateVolumeAttachments( runVm.getVolumes( ) );
            try {
              final NetworkGroup network = Networks.getInstance( ).lookup( runVm.getOwnerId( ) + "-" + runVm.getGroupNames( ).get( 0 ) );
              //GRZE:NET//        network.extantNetworkIndex( VmInstance.this.getClusterName( ), VmInstance.this.getNetworkIndex( ) );
            } catch ( final Exception e ) {}
          }
          db.commit( );
        } catch ( final Exception ex ) {
          Logs.exhaust( ).error( ex, ex );
          db.rollback( );
        }
        return true;
      }
    };
  }
  
  /**
   * @param volumes
   */
  public void updateVolumeAttachments( final List<AttachedVolume> volumes ) {
    this.runtimeState.updateVolumeAttachments( Lists.transform( volumes, VmVolumeAttachment.fromAttachedVolume( VmInstance.this ) ) );
  }
  
  /**
   * @param serviceTag
   */
  public void setServiceTag( final String serviceTag ) {
    this.runtimeState.setServiceTag( serviceTag );
  }
  
  /**
   * @param bundleTask
   * @return
   */
  public boolean startBundleTask( final BundleTask bundleTask ) {
    return this.runtimeState.startBundleTask( VmBundleTask.fromBundleTask( this ).apply( bundleTask ) );
  }
  
  private void setNetworkIndex( final PrivateNetworkIndex networkIndex ) {
    this.networkIndex = networkIndex;
  }
  
  @TypeMapper
  public enum Transform implements Function<VmInstance, RunningInstancesItemType> {
    INSTANCE;
    
    /**
     * @see com.google.common.base.Supplier#get()
     */
    @Override
    public RunningInstancesItemType apply( final VmInstance input ) {
      RunningInstancesItemType runningInstance;
      try {
        final boolean dns = !ComponentIds.lookup( Dns.class ).runLimitedServices( );
        runningInstance = new RunningInstancesItemType( );
        
        runningInstance.setAmiLaunchIndex( Integer.toString( input.launchRecord.getLaunchIndex( ) ) );
        if ( ( input.getBundleTaskState( ) != null ) && !BundleState.none.equals( input.getBundleTaskState( ) ) ) {
          runningInstance.setStateCode( Integer.toString( VmState.TERMINATED.getCode( ) ) );
          runningInstance.setStateName( VmState.TERMINATED.getName( ) );
        } else {
          runningInstance.setStateCode( Integer.toString( input.runtimeState.getRuntimeState( ).getCode( ) ) );
          runningInstance.setStateName( input.runtimeState.getRuntimeState( ).getName( ) );
        }
        runningInstance.setPlatform( input.getPlatform( ) );
        
        runningInstance.setStateCode( Integer.toString( input.runtimeState.getRuntimeState( ).getCode( ) ) );
        runningInstance.setStateName( input.runtimeState.getRuntimeState( ).getName( ) );
        runningInstance.setInstanceId( input.vmId.getInstanceId( ) );
        //ASAP:FIXME:GRZE: restore.
        runningInstance.setProductCodes( new ArrayList<String>( ) );
        runningInstance.setImageId( input.bootRecord.getMachine( ).getDisplayName( ) );
        runningInstance.setKernel( input.bootRecord.getKernel( ).getDisplayName( ) );
        runningInstance.setRamdisk( input.bootRecord.getRamdisk( ).getDisplayName( ) );
        if ( dns ) {
          String publicDnsName = input.getPublicDnsName( );
          String privateDnsName = input.getPrivateDnsName( );
          publicDnsName = ( publicDnsName == null
            ? VmNetworkConfig.DEFAULT_IP
            : publicDnsName );
          privateDnsName = ( privateDnsName == null
            ? VmNetworkConfig.DEFAULT_IP
            : privateDnsName );
          runningInstance.setDnsName( publicDnsName );
          runningInstance.setIpAddress( publicDnsName );
          runningInstance.setPrivateDnsName( privateDnsName );
          runningInstance.setPrivateIpAddress( privateDnsName );
        } else {
          String publicDnsName = input.getPublicAddress( );
          String privateDnsName = input.getPrivateAddress( );
          publicDnsName = ( publicDnsName == null
            ? VmNetworkConfig.DEFAULT_IP
            : publicDnsName );
          privateDnsName = ( privateDnsName == null
            ? VmNetworkConfig.DEFAULT_IP
            : privateDnsName );
          runningInstance.setPrivateDnsName( privateDnsName );
          runningInstance.setPrivateIpAddress( privateDnsName );
          if ( !VmNetworkConfig.DEFAULT_IP.equals( publicDnsName ) ) {
            runningInstance.setDnsName( publicDnsName );
            runningInstance.setIpAddress( publicDnsName );
          } else {
            runningInstance.setDnsName( privateDnsName );
            runningInstance.setIpAddress( privateDnsName );
          }
        }
        
        runningInstance.setReason( input.runtimeState.getReason( ) );
        
        if ( input.bootRecord.getSshKeyPair( ) != null )
          runningInstance.setKeyName( input.bootRecord.getSshKeyPair( ).getName( ) );
        else runningInstance.setKeyName( "" );
        
        runningInstance.setInstanceType( input.getVmType( ).getName( ) );
        runningInstance.setPlacement( input.placement.getPartitionName( ) );
        
        runningInstance.setLaunchTime( input.launchRecord.getLaunchTime( ) );
        
        runningInstance.getBlockDevices( ).add( new InstanceBlockDeviceMapping( "/dev/sda1" ) );
        for ( final VmVolumeAttachment attachedVol : input.runtimeState.getTransientVolumes( ).values( ) ) {
          runningInstance.getBlockDevices( ).add( new InstanceBlockDeviceMapping( attachedVol.getDevice( ), attachedVol.getVolumeId( ),
                                                                                  attachedVol.getStatus( ),
                                                                                  attachedVol.getAttachTime( ) ) );
        }
        return runningInstance;
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
        throw Exceptions.toUndeclared( ex );
      }
      
    }
    
  }
}
