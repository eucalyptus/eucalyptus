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
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicMarkableReference;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.CloudMetadata.VirtualMachineInstance;
import com.eucalyptus.cloud.UserMetadata;
import com.eucalyptus.cloud.util.PersistentResource;
import com.eucalyptus.cluster.callback.BundleCallback;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Dns;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.AbstractStatefulPersistent;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.PrivateNetworkIndex;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.reporting.event.InstanceEvent;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.vm.BundleTask;
import com.eucalyptus.vm.SystemState;
import com.eucalyptus.vm.SystemState.Reason;
import com.eucalyptus.vm.VmState;
import com.eucalyptus.vm.VmType;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;
import edu.ucsb.eucalyptus.msgs.InstanceBlockDeviceMapping;
import edu.ucsb.eucalyptus.msgs.NetworkConfigType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_instances" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class VmInstance extends UserMetadata<VmState> implements VirtualMachineInstance<VmInstance> {
  /**
   * 
   */
  private static final long                           serialVersionUID    = 1L;
  @Transient
  private static Logger                               LOG                 = Logger.getLogger( VmInstance.class );
  @Transient
  public static String                                DEFAULT_IP          = "0.0.0.0";
  @Transient
  public static String                                DEFAULT_TYPE        = "m1.small";
  @Transient
  private static String                               SEND_USER_TERMINATE = "SIGTERM";
  @Transient
  private static String                               SEND_USER_STOP      = "SIGSTOP";
  @Transient
  private final String                                clusterName;
  
  @Transient
  private final AtomicMarkableReference<BundleTask>   bundleTask          = new AtomicMarkableReference<BundleTask>( null, false );
  @Transient
  private final StopWatch                             stopWatch           = new StopWatch( );
  @Transient
  private final StopWatch                             updateWatch         = new StopWatch( );
  @Transient
  private String                                      serviceTag;
  @Transient
  private SystemState.Reason                          reason;
  @Transient
  private final List<String>                          reasonDetails       = Lists.newArrayList( );
  @Transient
  private StringBuffer                                consoleOutput       = new StringBuffer( );
  
  @Column( name = "metadata_vm_reservation_id" )
  private final String                                reservationId;
  @Column( name = "metadata_vm_launch_index" )
  private final Integer                               launchIndex;
  @Column( name = "metadata_vm_instance_id" )
  private final String                                instanceId;
  @Column( name = "metadata_vm_partition_name" )
  private final String                                partitionName;
  @Lob
  @Column( name = "metadata_vm_user_data" )
  private final byte[]                                userData;
  @Column( name = "metadata_vm_launch_time" )
  private final Date                                  launchTime;
  @Column( name = "metadata_vm_password_data" )
  private String                                      passwordData;
  @Column( name = "metadata_vm_private_networking" )
  private final Boolean                               privateNetwork;
  @Column( name = "metadata_vm_block_bytes" )
  private Long                                        blockBytes;
  @Column( name = "metadata_vm_network_bytes" )
  private Long                                        networkBytes;
  @Column( name = "metadata_vm_ssh_key_pair" )
  private final SshKeyPair                            sshKeyPair;
  @Column( name = "metadata_vm_type" )
  private final VmType                                vmType;
  
  @Transient
  private final ConcurrentMap<String, AttachedVolume> transientVolumes    = new ConcurrentSkipListMap<String, AttachedVolume>( );
  @Transient
  private final ConcurrentMap<String, AttachedVolume> persistentVolumes   = new ConcurrentSkipListMap<String, AttachedVolume>( );
  @Transient
  private String                                      platform;
  @Transient
  private final VmTypeInfo                            vbr;
  @ManyToMany( cascade = { CascadeType.PERSIST, CascadeType.MERGE } )
  @JoinTable( name = "metadata_metadata_vm_has_network_groups", joinColumns = { @JoinColumn( name = "metadata_vm_id" ) }, inverseJoinColumns = { @JoinColumn( name = "metadata_network_group_id" ) } )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private final Set<NetworkGroup>                     networkGroups       = Sets.newHashSet( );
  @ManyToOne( cascade = { CascadeType.PERSIST, CascadeType.MERGE } )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private PrivateNetworkIndex                         networkIndex;
  @Transient
  private final NetworkConfigType                     networkConfig       = new NetworkConfigType( );
  @Transient
  private final AtomicMarkableReference<VmState>      state               = new AtomicMarkableReference<VmState>( VmState.PENDING, false );
  
  public VmInstance( final UserFullName owner,
                     final String instanceId, final String instanceUuid,
                     final String reservationId, final int launchIndex,
                     final String placement,
                     final byte[] userData,
                     final VmTypeInfo vbr, final SshKeyPair sshKeyPair, final VmType vmType,
                     final String platform,
                     final List<NetworkGroup> networkRulesGroups, final String networkIndex ) {
    super( owner, instanceId );
    this.privateNetwork = Boolean.FALSE;
    this.launchTime = new Date( );
    this.blockBytes = 0l;
    this.networkBytes = 0l;
    this.reservationId = reservationId;
    this.launchIndex = launchIndex;
    this.instanceId = instanceId;
    this.clusterName = placement;
    this.vbr = vbr;
    String p = null;
    try {
      p = ServiceConfigurations.lookupByName( ClusterController.class, this.clusterName ).getPartition( );
    } catch ( final PersistenceException ex ) {
      p = placement;
      /** ASAP:GRZE: review **/
      LOG.debug( "Failed to find cluster configuration named: " + this.clusterName + " using that as the partition name." );
    }
    this.partitionName = p;
    this.userData = userData;
    this.platform = platform;
    this.sshKeyPair = sshKeyPair;
    this.vmType = vmType;
    this.networkConfig.setMacAddress( "d0:0d:" + VmInstances.asMacAddress( this.instanceId ) );
    this.networkConfig.setIpAddress( DEFAULT_IP );
    this.networkConfig.setIgnoredPublicIp( DEFAULT_IP );
    this.networkConfig.setNetworkIndex( Integer.parseInt( networkIndex ) );
    this.stopWatch.start( );
    this.updateWatch.start( );
    this.updateDns( );
    this.store( );
  }
  
  protected VmInstance( final UserFullName userFullName, final String instanceId2 ) {
    super( userFullName, instanceId2 );
    this.instanceId = instanceId2;
    this.launchTime = null;
    this.launchIndex = null;
    this.blockBytes = null;
    this.networkBytes = null;
    this.reservationId = null;
    this.clusterName = null;
    this.vbr = null;
    this.partitionName = null;
    this.userData = null;
    this.platform = null;
    this.sshKeyPair = null;
    this.vmType = null;
    this.privateNetwork = null;
  }
  
  protected VmInstance( ) {
    this.instanceId = null;
    this.launchTime = null;
    this.launchIndex = null;
    this.blockBytes = null;
    this.networkBytes = null;
    this.reservationId = null;
    this.clusterName = null;
    this.vbr = null;
    this.partitionName = null;
    this.userData = null;
    this.platform = null;
    this.sshKeyPair = null;
    this.vmType = null;
    this.privateNetwork = null;
  }
  
  public void updateBlockBytes( final long blkbytes ) {
    this.blockBytes += blkbytes;
  }
  
  public void updateNetworkBytes( final long netbytes ) {
    this.networkBytes += netbytes;
  }
  
  public void updateNetworkIndex( final Integer newIndex ) {
    if ( ( this.getNetworkConfig( ).getNetworkIndex( ) > 0 ) && ( newIndex > 0 )
         && ( VmState.RUNNING.equals( this.getState( ) ) || VmState.PENDING.equals( this.getState( ) ) ) ) {
      this.getNetworkConfig( ).setNetworkIndex( newIndex );
    }
  }
  
  public void updateAddresses( final String privateAddr, final String publicAddr ) {
    this.updatePrivateAddress( privateAddr );
    this.updatePublicAddress( publicAddr );
  }
  
  public void updatePublicAddress( final String publicAddr ) {
    if ( !VmInstance.DEFAULT_IP.equals( publicAddr ) && !"".equals( publicAddr )
         && ( publicAddr != null ) ) {
      this.getNetworkConfig( ).setIgnoredPublicIp( publicAddr );
    }
  }
  
  private void updateDns( ) {
    String dnsDomain = "dns-disabled";
    try {
      dnsDomain = edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration.getSystemConfiguration( ).getDnsDomain( );
    } catch ( final Exception e ) {}
    this.getNetworkConfig( ).updateDns( dnsDomain );
  }
  
  public void updatePrivateAddress( final String privateAddr ) {
    if ( !VmInstance.DEFAULT_IP.equals( privateAddr ) && !"".equals( privateAddr ) && ( privateAddr != null ) ) {
      this.getNetworkConfig( ).setIpAddress( privateAddr );
    }
    this.updateDns( );
  }
  
  public boolean clearPending( ) {
    if ( this.state.isMarked( ) && ( this.getState( ).ordinal( ) > VmState.RUNNING.ordinal( ) ) ) {
      this.state.set( this.getState( ), false );
      VmInstances.cleanUp( this );
      return true;
    } else {
      this.state.set( this.getState( ), false );
      return false;
    }
  }
  
  @Override
  public VmState getState( ) {
    return this.state.getReference( );
  }
  
  @Override
  public void setState( final VmState state ) {
    this.setState( state, SystemState.Reason.NORMAL );
  }
  
  public String getReason( ) {
    if ( this.reason == null ) {
      this.reason = Reason.NORMAL;
    }
    return this.reason.name( ) + ": " + this.reason + ( this.reasonDetails != null
      ? " -- " + this.reasonDetails
      : "" );
  }
  
  private void addReasonDetail( final String... extra ) {
    for ( final String s : extra ) {
      this.reasonDetails.add( s );
    }
  }
  
  public void setState( final VmState newState, SystemState.Reason reason, final String... extra ) {
    this.updateWatch.split( );
    if ( this.updateWatch.getSplitTime( ) > ( 1000 * 60 * 60 ) ) {
      this.store( );
      this.updateWatch.unsplit( );
    } else {
      this.updateWatch.unsplit( );
    }
    this.resetStopWatch( );
    final VmState oldState = this.state.getReference( );
    if ( VmState.SHUTTING_DOWN.equals( newState ) && VmState.SHUTTING_DOWN.equals( oldState ) && Reason.USER_TERMINATED.equals( reason ) ) {
      VmInstances.cleanUp( this );
      if ( !this.reasonDetails.contains( SEND_USER_TERMINATE ) ) {
        this.addReasonDetail( SEND_USER_TERMINATE );
      }
    } else if ( VmState.STOPPING.equals( newState ) && VmState.STOPPING.equals( oldState ) && Reason.USER_STOPPED.equals( reason ) ) {
      VmInstances.cleanUp( this );
      if ( !this.reasonDetails.contains( SEND_USER_STOP ) ) {
        this.addReasonDetail( SEND_USER_STOP );
      }
    } else if ( VmState.TERMINATED.equals( newState ) && VmState.TERMINATED.equals( oldState ) ) {
      VmInstances.getInstance( ).deregister( this.getName( ) );
      try {
        Transactions.delete( this );
      } catch ( final ExecutionException ex ) {
        LOG.error( ex, ex );
      }
    } else if ( !this.getState( ).equals( newState ) ) {
      if ( Reason.APPEND.equals( reason ) ) {
        reason = this.reason;
      }
      this.addReasonDetail( extra );
      LOG.info( String.format( "%s state change: %s -> %s", this.getInstanceId( ), this.getState( ), newState ) );
      this.reason = reason;
      if ( this.state.isMarked( ) && VmState.PENDING.equals( this.getState( ) ) ) {
        if ( VmState.SHUTTING_DOWN.equals( newState ) || VmState.PENDING.equals( newState ) ) {
          this.state.set( newState, true );
        } else {
          this.state.set( newState, false );
        }
      } else if ( this.state.isMarked( ) && VmState.SHUTTING_DOWN.equals( this.getState( ) ) ) {
        LOG.debug( "Ignoring events for state transition because the instance is marked as pending: " + oldState + " to " + this.getState( ) );
      } else if ( !this.state.isMarked( ) ) {
        if ( ( oldState.ordinal( ) <= VmState.RUNNING.ordinal( ) ) && ( newState.ordinal( ) > VmState.RUNNING.ordinal( ) ) ) {
          this.state.set( newState, false );
          VmInstances.cleanUp( this );
        } else if ( VmState.PENDING.equals( oldState ) && VmState.RUNNING.equals( newState ) ) {
          this.state.set( newState, false );
        } else if ( VmState.TERMINATED.equals( newState ) && ( oldState.ordinal( ) <= VmState.RUNNING.ordinal( ) ) ) {
          this.state.set( newState, false );
          VmInstances.getInstance( ).disable( this.getName( ) );
          VmInstances.cleanUp( this );
        } else if ( VmState.TERMINATED.equals( newState ) && ( oldState.ordinal( ) > VmState.RUNNING.ordinal( ) ) ) {
          this.state.set( newState, false );
          VmInstances.getInstance( ).disable( this.getName( ) );
        } else if ( ( oldState.ordinal( ) > VmState.RUNNING.ordinal( ) ) && ( newState.ordinal( ) <= VmState.RUNNING.ordinal( ) ) ) {
          this.state.set( oldState, false );
          VmInstances.cleanUp( this );
        } else if ( newState.ordinal( ) > oldState.ordinal( ) ) {
          this.state.set( newState, false );
        }
        this.store( );
      } else {
        LOG.debug( "Ignoring events for state transition because the instance is marked as pending: " + oldState + " to " + this.getState( ) );
      }
      if ( !this.getState( ).equals( oldState ) ) {
        EventRecord.caller( VmInstance.class, EventType.VM_STATE, this.instanceId, this.getOwner( ), this.state.getReference( ).name( ), this.launchTime );
      }
    }
  }
  
  private void store( ) {
    try {
      ListenerRegistry.getInstance( ).fireEvent( new InstanceEvent( this.getInstanceUuid( ), this.getDisplayName( ), this.vmType.getName( ),
                                                                    this.getOwner( ).getUserId( ), this.getOwner( ).getAccountNumber( ),
                                                                    this.clusterName, this.partitionName, this.networkBytes, this.blockBytes ) );
    } catch ( final EventFailedException ex ) {
      LOG.error( ex, ex );
    }
    try {
      Transactions.one( VmInstance.named( ( UserFullName ) this.getOwner( ), this.getDisplayName( ) ), new Callback<VmInstance>( ) {
        
        @Override
        public void fire( final VmInstance t ) {
          t.setBlockBytes( VmInstance.this.getBlockBytes( ) );
          t.setNetworkBytes( VmInstance.this.getNetworkBytes( ) );
        }
      } );
    } catch ( final ExecutionException ex ) {
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
    m.put( "ami-launch-index", "" + this.getLaunchIndex( ) );
//    m.put( "ancestor-ami-ids", this.getImageInfo( ).getAncestorIds( ).toString( ).replaceAll( "[\\Q[]\\E]", "" ).replaceAll( ", ", "\n" ) );
    
//    m.put( "ami-manifest-path", this.getImageInfo( ).getImageLocation( ) );
    m.put( "hostname", this.getPublicAddress( ) );
    m.put( "instance-id", this.getInstanceId( ) );
    m.put( "instance-type", this.getVmType( ).getName( ) );
    if ( dns ) {
      m.put( "local-hostname", this.getNetworkConfig( ).getPrivateDnsName( ) );
    } else {
      m.put( "local-hostname", this.getNetworkConfig( ).getIpAddress( ) );
    }
    m.put( "local-ipv4", this.getNetworkConfig( ).getIpAddress( ) );
    if ( dns ) {
      m.put( "public-hostname", this.getNetworkConfig( ).getPublicDnsName( ) );
    } else {
      m.put( "public-hostname", this.getPublicAddress( ) );
    }
    m.put( "public-ipv4", this.getPublicAddress( ) );
    m.put( "reservation-id", this.getReservationId( ) );
//    m.put( "kernel-id", this.getImageInfo( ).getKernelId( ) );
//    if ( this.getImageInfo( ).getRamdiskId( ) != null ) {
//      m.put( "ramdisk-id", this.getImageInfo( ).getRamdiskId( ) );
//    }
    m.put( "security-groups", this.getNetworkNames( ).toString( ).replaceAll( "[\\Q[]\\E]", "" ).replaceAll( ", ", "\n" ) );
    
    m.put( "block-device-mapping/", "emi\nephemeral\nephemeral0\nroot\nswap" );
    m.put( "block-device-mapping/emi", "sda1" );
    m.put( "block-device-mapping/ami", "sda1" );
    m.put( "block-device-mapping/ephemeral", "sda2" );
    m.put( "block-device-mapping/ephemeral0", "sda2" );
    m.put( "block-device-mapping/swap", "sda3" );
    m.put( "block-device-mapping/root", "/dev/sda1" );
    
    m.put( "public-keys/", "0=" + this.getSshKeyPair( ).getName( ) );
    m.put( "public-keys/0", "openssh-key" );
    m.put( "public-keys/0/", "openssh-key" );
    m.put( "public-keys/0/openssh-key", this.getSshKeyPair( ).getPublicKey( ) );
    
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
  
  public synchronized long resetStopWatch( ) {
    this.stopWatch.stop( );
    final long ret = this.stopWatch.getTime( );
    this.stopWatch.reset( );
    this.stopWatch.start( );
    return ret;
  }
  
  public synchronized long getSplitTime( ) {
    this.stopWatch.split( );
    final long ret = this.stopWatch.getSplitTime( );
    this.stopWatch.unsplit( );
    return ret;
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
    return this.bundleTask.getReference( ) != null;
  }
  
  public BundleTask resetBundleTask( ) {
    final BundleTask oldTask = this.bundleTask.getReference( );
    this.bundleTask.set( null, false );
    EventRecord.here( BundleCallback.class, EventType.BUNDLE_RESET, this.getOwner( ).toString( ), this.getBundleTask( ).getBundleId( ), this.getInstanceId( ) ).info( );
    return oldTask;
  }
  
  private BundleState getBundleTaskState( ) {
    if ( this.bundleTask.getReference( ) != null ) {
      return BundleState.valueOf( this.getBundleTask( ).getState( ) );
    } else {
      return null;
    }
  }
  
  public void setBundleTaskState( final String state ) {
    BundleState next = null;
    if ( BundleState.storing.getMappedState( ).equals( state ) ) {
      next = BundleState.storing;
    } else if ( BundleState.complete.getMappedState( ).equals( state ) ) {
      next = BundleState.complete;
    } else if ( BundleState.failed.getMappedState( ).equals( state ) ) {
      next = BundleState.failed;
    } else {
      next = BundleState.none;
    }
    if ( this.bundleTask.getReference( ) != null ) {
      if ( !this.bundleTask.isMarked( ) ) {
        final BundleState current = BundleState.valueOf( this.getBundleTask( ).getState( ) );
        if ( BundleState.complete.equals( current ) || BundleState.failed.equals( current ) ) {
          return; //already finished, wait and timeout the state along with the instance.
        } else if ( BundleState.storing.equals( next ) || BundleState.storing.equals( current ) ) {
          this.getBundleTask( ).setState( next.name( ) );
          EventRecord.here( BundleCallback.class, EventType.BUNDLE_TRANSITION, this.getOwner( ).toString( ), this.getBundleTask( ).getBundleId( ),
                            this.getInstanceId( ),
                            this.getBundleTask( ).getState( ) ).info( );
          this.getBundleTask( ).setUpdateTime( new Date( ) );
        } else if ( BundleState.none.equals( next ) && BundleState.failed.equals( current ) ) {
          this.resetBundleTask( );
        }
      } else {
        this.getBundleTask( ).setUpdateTime( new Date( ) );
      }
    }
  }
  
  public Boolean cancelBundleTask( ) {
    if ( this.getBundleTask( ) != null ) {
      this.bundleTask.set( this.getBundleTask( ), true );
      this.getBundleTask( ).setState( BundleState.canceling.name( ) );
      EventRecord.here( BundleCallback.class, EventType.BUNDLE_CANCELING, this.getOwner( ).toString( ), this.getBundleTask( ).getBundleId( ),
                        this.getInstanceId( ),
                        this.getBundleTask( ).getState( ) ).info( );
      return true;
    } else {
      return false;
    }
  }
  
  public Boolean clearPendingBundleTask( ) {
    if ( BundleState.pending.name( ).equals( this.getBundleTask( ).getState( ) )
         && this.bundleTask.compareAndSet( this.getBundleTask( ), this.getBundleTask( ), true, false ) ) {
      this.getBundleTask( ).setState( BundleState.storing.name( ) );
      EventRecord.here( BundleCallback.class, EventType.BUNDLE_STARTING, this.getOwner( ).toString( ), this.getBundleTask( ).getBundleId( ),
                        this.getInstanceId( ),
                        this.getBundleTask( ).getState( ) ).info( );
      return true;
    } else if ( BundleState.canceling.name( ).equals( this.getBundleTask( ).getState( ) )
                && this.bundleTask.compareAndSet( this.getBundleTask( ), this.getBundleTask( ), true, false ) ) {
      EventRecord.here( BundleCallback.class, EventType.BUNDLE_CANCELLED, this.getOwner( ).toString( ), this.getBundleTask( ).getBundleId( ),
                        this.getInstanceId( ),
                        this.getBundleTask( ).getState( ) ).info( );
      this.resetBundleTask( );
      return true;
    } else {
      return false;
    }
  }
  
  public Boolean startBundleTask( final BundleTask task ) {
    if ( this.bundleTask.compareAndSet( null, task, false, true ) ) {
      return true;
    } else {
      if ( ( this.getBundleTask( ) != null ) && BundleState.failed.equals( BundleState.valueOf( this.getBundleTask( ).getState( ) ) ) ) {
        this.resetBundleTask( );
        this.bundleTask.set( task, true );
        return true;
      } else {
        return false;
      }
    }
  }
  
  public String getImageId( ) {
    return this.vbr.lookupRoot( ).getId( );
  }
  
  public RunningInstancesItemType getAsRunningInstanceItemType( ) {
    final boolean dns = !ComponentIds.lookup( Dns.class ).runLimitedServices( );
    final RunningInstancesItemType runningInstance = new RunningInstancesItemType( );
    
    runningInstance.setAmiLaunchIndex( Integer.toString( this.launchIndex ) );
    if ( ( this.getBundleTaskState( ) != null ) && !BundleState.none.equals( this.getBundleTaskState( ) ) ) {
      runningInstance.setStateCode( Integer.toString( VmState.TERMINATED.getCode( ) ) );
      runningInstance.setStateName( VmState.TERMINATED.getName( ) );
    } else {
      runningInstance.setStateCode( Integer.toString( this.state.getReference( ).getCode( ) ) );
      runningInstance.setStateName( this.state.getReference( ).getName( ) );
    }
    runningInstance.setPlatform( this.getPlatform( ) );
    
    runningInstance.setStateCode( Integer.toString( this.state.getReference( ).getCode( ) ) );
    runningInstance.setStateName( this.state.getReference( ).getName( ) );
    runningInstance.setInstanceId( this.instanceId );
//ASAP:FIXME:GRZE: restore.
    runningInstance.setProductCodes( new ArrayList<String>( ) );
    try {
      runningInstance.setImageId( this.vbr.lookupRoot( ).getId( ) );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
      runningInstance.setImageId( "unknown" );
    }
    try {
      runningInstance.setKernel( this.vbr.lookupKernel( ).getId( ) );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
      runningInstance.setKernel( "unknown" );
    }
    try {
      runningInstance.setRamdisk( this.vbr.lookupRamdisk( ).getId( ) );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
      runningInstance.setRamdisk( "unknown" );
    }
    
    if ( dns ) {
      runningInstance.setDnsName( this.getNetworkConfig( ).getPublicDnsName( ) );
      runningInstance.setPrivateDnsName( this.getNetworkConfig( ).getPrivateDnsName( ) );
    } else {
      runningInstance.setPrivateDnsName( this.getNetworkConfig( ).getIpAddress( ) );
      if ( !VmInstance.DEFAULT_IP.equals( this.getPublicAddress( ) ) ) {
        runningInstance.setDnsName( this.getPublicAddress( ) );
      } else {
        runningInstance.setDnsName( this.getNetworkConfig( ).getIpAddress( ) );
      }
    }
    
    runningInstance.setPrivateIpAddress( this.getNetworkConfig( ).getIpAddress( ) );
    if ( !VmInstance.DEFAULT_IP.equals( this.getPublicAddress( ) ) ) {
      runningInstance.setIpAddress( this.getPublicAddress( ) );
    } else {
      runningInstance.setIpAddress( this.getNetworkConfig( ).getIpAddress( ) );
    }
    
    runningInstance.setReason( this.getReason( ) );
    
    if ( this.getSshKeyPair( ) != null )
      runningInstance.setKeyName( this.getSshKeyPair( ).getName( ) );
    else runningInstance.setKeyName( "" );
    
    runningInstance.setInstanceType( this.getVmType( ).getName( ) );
    runningInstance.setPlacement( this.partitionName );
    
    runningInstance.setLaunchTime( this.launchTime );
    
    runningInstance.getBlockDevices( ).add( new InstanceBlockDeviceMapping( "/dev/sda1" ) );
    for ( final AttachedVolume attachedVol : this.transientVolumes.values( ) ) {
      runningInstance.getBlockDevices( ).add( new InstanceBlockDeviceMapping( attachedVol.getDevice( ), attachedVol.getVolumeId( ), attachedVol.getStatus( ),
                                                                              attachedVol.getAttachTime( ) ) );
    }
    
    return runningInstance;
  }
  
  public void setServiceTag( final String serviceTag ) {
    this.serviceTag = serviceTag;
  }
  
  public String getServiceTag( ) {
    return this.serviceTag;
  }
  
  public boolean hasPublicAddress( ) {
    final NetworkConfigType conf = this.getNetworkConfig( );
    return ( conf != null ) && !( DEFAULT_IP.equals( conf.getIgnoredPublicIp( ) ) || conf.getIpAddress( ).equals( conf.getIgnoredPublicIp( ) ) );
  }
  
  public String getReservationId( ) {
    return this.reservationId;
  }
  
  public String getInstanceId( ) {
    return super.getDisplayName( );
  }
  
  public int getLaunchIndex( ) {
    return this.launchIndex;
  }
  
  public String getClusterName( ) {
    return this.clusterName;
  }
  
  public String getPlacement( ) {
    return this.clusterName;//TODO:GRZE:RELEASE this should be reporting the partition name
  }
  
  public Date getLaunchTime( ) {
    return this.launchTime;
  }
  
  public byte[] getUserData( ) {
    return this.userData;
  }
  
  public String getConsoleOutputString( ) {
    return new String( Base64.encode( this.consoleOutput.toString( ).getBytes( ) ) );
  }
  
  public StringBuffer getConsoleOutput( ) {
    return this.consoleOutput;
  }
  
  public void setConsoleOutput( final StringBuffer consoleOutput ) {
    this.consoleOutput = consoleOutput;
    if ( this.passwordData == null ) {
      final String tempCo = consoleOutput.toString( ).replaceAll( "[\r\n]*", "" );
      if ( tempCo.matches( ".*<Password>[\\w=+/]*</Password>.*" ) ) {
        this.passwordData = tempCo.replaceAll( ".*<Password>", "" ).replaceAll( "</Password>.*", "" );
      }
    }
  }
  
  public VmType getVmType( ) {
    return this.vmType;
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
    return this.networkConfig.getIpAddress( );
  }
  
  public String getPublicAddress( ) {
    return this.networkConfig.getIgnoredPublicIp( );
  }
  
  public String getPrivateDnsName( ) {
    return this.networkConfig.getPrivateDnsName( );
  }
  
  public String getPublicDnsName( ) {
    return this.networkConfig.getPublicDnsName( );
  }
  
  private NetworkConfigType getNetworkConfig( ) {
    return this.networkConfig;
  }
  
  private AttachedVolume resolveVolumeId( final String volumeId ) throws NoSuchElementException {
    final AttachedVolume v = this.transientVolumes.get( volumeId );
    if ( v == null ) {
      throw new NoSuchElementException( "Failed to find volume attachment for instance " + this.getInstanceId( ) + " and volume " + volumeId );
    } else {
      return v;
    }
  }
  
  public AttachedVolume removeVolumeAttachment( final String volumeId ) throws NoSuchElementException {
    final AttachedVolume v = this.transientVolumes.remove( volumeId );
    if ( v == null ) {
      throw new NoSuchElementException( "Failed to find volume attachment for instance " + this.getInstanceId( ) + " and volume " + volumeId );
    } else {
      return v;
    }
  }
  
  public void updateVolumeAttachment( final String volumeId, final String state ) throws NoSuchElementException {
    final AttachedVolume v = this.resolveVolumeId( volumeId );
    v.setStatus( state );
    v.setInstanceId( this.getInstanceId( ) );
  }
  
  public AttachedVolume lookupVolumeAttachment( final String volumeId ) throws NoSuchElementException {
    return this.resolveVolumeId( volumeId );
  }
  
  public AttachedVolume lookupVolumeAttachment( final Predicate<AttachedVolume> pred ) throws NoSuchElementException {
    final AttachedVolume v = Iterables.find( this.transientVolumes.values( ), pred );
    if ( v == null ) {
      throw new NoSuchElementException( "Failed to find volume attachment for instance " + this.getInstanceId( ) + " using predicate "
                                        + pred.getClass( ).getCanonicalName( ) );
    } else {
      return v;
    }
  }
  
  public <T> Iterable<T> transformVolumeAttachments( final Function<? super AttachedVolume, T> function ) throws NoSuchElementException {
    return Iterables.transform( this.transientVolumes.values( ), function );
  }
  
  public boolean eachVolumeAttachment( final Predicate<AttachedVolume> pred ) throws NoSuchElementException {
    return Iterables.all( this.transientVolumes.values( ), pred );
  }
  
  public void addVolumeAttachment( final AttachedVolume volume ) {
    final String volumeId = volume.getVolumeId( );
    volume.setStatus( "attaching" );
    volume.setInstanceId( this.getInstanceId( ) );
    final AttachedVolume v = this.transientVolumes.put( volumeId, volume );
    if ( v != null ) {
      this.transientVolumes.replace( volumeId, v );
    }
  }
  
  public void updateVolumeAttachments( final List<AttachedVolume> ncAttachedVols ) throws NoSuchElementException {
    final Map<String, AttachedVolume> ncAttachedVolMap = new HashMap<String, AttachedVolume>( ) {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;
      
      {
        for ( final AttachedVolume v : ncAttachedVols ) {
          this.put( v.getVolumeId( ), v );
        }
      }
    };
    this.eachVolumeAttachment( new Predicate<AttachedVolume>( ) {
      @Override
      public boolean apply( final AttachedVolume arg0 ) {
        final String volId = arg0.getVolumeId( );
        if ( ncAttachedVolMap.containsKey( volId ) ) {
          final AttachedVolume ncVol = ncAttachedVolMap.get( volId );
          if ( "detached".equals( ncVol.getStatus( ) ) ) {
            VmInstance.this.removeVolumeAttachment( volId );
          } else if ( "attaching".equals( arg0.getStatus( ) ) || "attached".equals( arg0.getStatus( ) ) ) {
            VmInstance.this.updateVolumeAttachment( volId, arg0.getStatus( ) );
          }
        } else if ( "detaching".equals( arg0.getStatus( ) ) ) {//TODO:GRZE:remove this case when NC is updated to report "detached" state
          VmInstance.this.removeVolumeAttachment( volId );
        }
        ncAttachedVolMap.remove( volId );
        return true;
      }
    } );
    for ( final AttachedVolume v : ncAttachedVolMap.values( ) ) {
      LOG.warn( "Restoring volume attachment state for " + this.getInstanceId( ) + " with " + v.toString( ) );
      this.addVolumeAttachment( v );
    }
  }
  
  public String getPasswordData( ) {
    return this.passwordData;
  }
  
  public void setPasswordData( final String passwordData ) {
    this.passwordData = passwordData;
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( ( o == null ) || ( this.getClass( ) != o.getClass( ) ) ) return false;
    
    final VmInstance vmInstance = ( VmInstance ) o;
    
    if ( !this.instanceId.equals( vmInstance.instanceId ) ) return false;
    
    return true;
  }
  
  @Override
  public int hashCode( ) {
    return this.instanceId.hashCode( );
  }
  
  @Override
  public String toString( ) {
    return String
                 .format(
                          "VmInstance [instanceId=%s, keyInfo=%s, launchIndex=%s, launchTime=%s, networkConfig=%s, networks=%s, ownerId=%s, placement=%s, privateNetwork=%s, reason=%s, reservationId=%s, state=%s, stopWatch=%s, userData=%s, vmTypeInfo=%s, volumes=%s]",
                          this.instanceId, this.sshKeyPair, this.launchIndex, this.launchTime, this.networkConfig, this.networkGroups, this.getOwner( ),
                          this.clusterName, this.privateNetwork, this.reason, this.reservationId, this.state, this.stopWatch, this.userData, this.vmType,
                          this.transientVolumes );
  }
  
  public int getNetworkIndex( ) {
    return this.getNetworkConfig( ).getNetworkIndex( );
  }
  
  /**
   * @return the platform
   */
  public String getPlatform( ) {
    return this.platform;
  }
  
  /**
   * @param platform the platform to set
   */
  public void setPlatform( final String platform ) {
    this.platform = platform;
  }
  
  /**
   * @return the bundleTask
   */
  public BundleTask getBundleTask( ) {
    return this.bundleTask.getReference( );
  }
  
  /**
   * @return the networkBytes
   */
  public Long getNetworkBytes( ) {
    return this.networkBytes;
  }
  
  /**
   * @param networkBytes the networkBytes to set
   */
  public void setNetworkBytes( final Long networkBytes ) {
    this.networkBytes = networkBytes;
  }
  
  /**
   * @return the blockBytes
   */
  public Long getBlockBytes( ) {
    return this.blockBytes;
  }
  
  /**
   * @param blockBytes the blockBytes to set
   */
  public void setBlockBytes( final Long blockBytes ) {
    this.blockBytes = blockBytes;
  }
  
  @Override
  public String getPartition( ) {
    return this.partitionName;
  }
  
  public String getInstanceUuid( ) {
    return this.getNaturalId( );
  }
  
  public SshKeyPair getSshKeyPair( ) {
    return this.sshKeyPair;
  }
  
  public static VmInstance named( final UserFullName userFullName, final String instanceId2 ) {
    return new VmInstance( userFullName, instanceId2 );
  }
  
  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" )
                          .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                          .namespace( this.getOwnerAccountNumber( ) )
                          .relativeId( "instance", this.getDisplayName( ) );
  }
}
