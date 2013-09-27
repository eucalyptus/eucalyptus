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

package com.eucalyptus.vm;

import java.net.URI;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Parent;
import org.hibernate.annotations.Type;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenResponseType;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenType;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.callback.StartInstanceCallback;
import com.eucalyptus.cluster.callback.StopInstanceCallback;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.MessageCallback;
import com.eucalyptus.vm.Bundles.BundleCallback;
import com.eucalyptus.vm.VmBundleTask.BundleState;
import com.eucalyptus.vm.VmInstance.Reason;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstance.VmStateSet;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.AttachVolumeType;
import edu.ucsb.eucalyptus.msgs.CreateTagsType;
import edu.ucsb.eucalyptus.msgs.ResourceTag;

@Embeddable
public class VmRuntimeState {
  /**
   * 
   */
  private static final String VM_NC_HOST_TAG      = "euca:node";
  @Transient
  private static String       SEND_USER_TERMINATE = "SIGTERM";
  @Transient
  private static String       SEND_USER_STOP      = "SIGSTOP";
  @Transient
  private static Logger       LOG                 = Logger.getLogger( VmRuntimeState.class );
  @Parent
  private VmInstance          vmInstance;
  @Embedded
  private VmBundleTask        bundleTask;
  @Embedded
  private VmCreateImageTask   createImageTask;
  @Column( name = "metadata_vm_service_tag" )
  private String              serviceTag;
  @Enumerated( EnumType.STRING )
  @Column( name = "metadata_vm_reason" )
  private Reason              reason;
  @ElementCollection
  @CollectionTable( name = "metadata_instances_state_reasons" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<String>         reasonDetails       = Sets.newHashSet( );
  @Transient
  private StringBuffer        consoleOutput       = new StringBuffer( );
  @Lob
  @Type( type = "org.hibernate.type.StringClobType" )
  @Column( name = "metadata_vm_password_data" )
  private String              passwordData;
  @Column( name = "metadata_vm_pending" )
  private Boolean             pending;
  @Column( name = "metadata_vm_guest_state" )
  private String 			  guestState;
  
  @Embedded
  private VmMigrationTask     migrationTask;
  
  VmRuntimeState( final VmInstance vmInstance ) {
    super( );
    this.vmInstance = vmInstance;
  }
  
  VmRuntimeState( ) {
    super( );
  }
  
  public String getReason( ) {
    if ( this.reason == null ) {
      this.reason = Reason.NORMAL;
    }
    return this.reason.name( ) + ": " + this.reason + ( this.reasonDetails != null ? " -- " + this.reasonDetails : "" );
  }
  
  private void addReasonDetail( final String... extra ) {
    for ( final String s : extra ) {
      this.reasonDetails.add( s );
    }
  }
  
  public void setState( final VmState newState, Reason reason, final String... extra ) {
    final VmState olderState = this.getVmInstance( ).getLastState( );
    final VmState oldState = this.getVmInstance( ).getState( );
    Callable<Boolean> action = null;
    if ( VmStateSet.RUN.contains( newState ) && VmStateSet.NOT_RUNNING.contains( oldState ) ) {
      action = this.cleanUpRunnable( SEND_USER_TERMINATE );
    } else if ( !oldState.equals( newState ) ) {
      action = handleStateTransition( newState, oldState, olderState );
    }
    this.getVmInstance( ).updateTimeStamps( );
    if ( action != null ) {
      if ( Reason.APPEND.equals( reason ) ) {
        reason = this.reason;
      }
      this.addReasonDetail( extra );
      this.reason = reason;
      try {
        Threads.enqueue( Eucalyptus.class, VmInstance.class, VmInstances.MAX_STATE_THREADS, action ).get( 10, TimeUnit.MILLISECONDS );//GRZE: yes.  wait for 10ms. because.
      } catch ( final TimeoutException ex ) {} catch ( final InterruptedException ex ) {} catch ( final Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      }
    }
  }
  
  private Callable<Boolean> handleStateTransition( final VmState newState, final VmState oldState, final VmState olderState ) {
    Callable<Boolean> action = null;
    LOG.info( String.format( "%s state change: %s -> %s (previously %s)", this.getVmInstance( ).getInstanceId( ), oldState, newState, olderState ) );
    if ( VmStateSet.RUN.contains( oldState )
         && VmStateSet.NOT_RUNNING.contains( newState ) ) {
      this.getVmInstance( ).setState( newState );
      action = VmState.SHUTTING_DOWN.equals( newState ) ?
                                                       this.tryCleanUpRunnable( ) : // try cleanup now, will try again when moving to final state
                                                       this.cleanUpRunnable( );
    } else if ( VmState.PENDING.equals( oldState )
                && VmState.RUNNING.equals( newState ) ) {
      this.getVmInstance( ).setState( newState );
      if ( VmState.STOPPED.equals( olderState ) ) {
        this.restoreVolumeState( );
      }
    } else if ( VmState.PENDING.equals( oldState )
                && VmState.TERMINATED.equals( newState )
                && VmState.STOPPED.equals( olderState ) ) {
      this.getVmInstance( ).setState( VmState.STOPPED );
      this.getVmInstance( ).updatePublicAddress( this.getVmInstance( ).getPrivateAddress( ) );
      action = this.cleanUpRunnable( );
    } else if ( VmState.STOPPED.equals( oldState )
                && VmState.TERMINATED.equals( newState ) ) {
      this.getVmInstance( ).setState( VmState.TERMINATED );
      this.getVmInstance( ).updatePublicAddress( this.getVmInstance( ).getPrivateAddress( ) );
      action = this.cleanUpRunnable( );
    } else if ( VmStateSet.EXPECTING_TEARDOWN.contains( oldState )
                && VmStateSet.RUN.contains( newState ) ) {
      this.getVmInstance( ).setState( oldState );//mask/ignore running on {stopping,shutting-down} transitions 
    } else if ( VmStateSet.EXPECTING_TEARDOWN.contains( oldState )
                && VmStateSet.TORNDOWN.contains( newState ) ) {
      if ( VmState.SHUTTING_DOWN.equals( oldState ) ) {
        this.getVmInstance( ).setState( VmState.TERMINATED );
        this.getVmInstance( ).updatePublicAddress( this.getVmInstance( ).getPrivateAddress( ) );
      } else {//if ( VmState.STOPPING.equals( oldState ) ) {
        this.getVmInstance( ).setState( VmState.STOPPED );
        this.getVmInstance( ).updateAddresses( "", "" );
      }
      action = this.cleanUpRunnable( );
    } else if ( VmState.STOPPED.equals( oldState )
                && VmState.PENDING.equals( newState ) ) {
      this.getVmInstance( ).setState( VmState.PENDING );
    } else if ( VmStateSet.RUN.contains( oldState )
                && VmStateSet.NOT_RUNNING.contains( newState ) ) {
      this.getVmInstance( ).setState( newState );
      action = this.cleanUpRunnable( );
    } else {//if ( VmState.TERMINATED.equals( newState ) && ( oldState.ordinal( ) > VmState.RUNNING.ordinal( ) ) ) {
      this.getVmInstance( ).setState( newState );
    }
    try {
      this.getVmInstance( ).store( );
    } catch ( final Exception ex1 ) {
      LOG.error( ex1, ex1 );
    }
    return action;
  }
  
  private void restoreVolumeState( ) {
    final VmInstance vm = this.getVmInstance( );
    if ( vm.isBlockStorage( ) ) {
      final String vmId = vm.getInstanceId( );
      final ServiceConfiguration scConfig = Topology.lookup( Storage.class, vm.lookupPartition( ) );
      final ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, vm.lookupPartition( ) );
      final Predicate<VmVolumeAttachment> attachVolumes = new Predicate<VmVolumeAttachment>( ) {
        public boolean apply( VmVolumeAttachment input ) {
          final String volumeId = input.getVolumeId( );
          final String vmDevice = input.getDevice( );
          try {
            LOG.debug( vmId + ": attaching volume: " + input );
            //final AttachStorageVolumeType attachMsg = new AttachStorageVolumeType( Nodes.lookupIqns( ccConfig ), volumeId );
            GetVolumeTokenType tokenRequest = new GetVolumeTokenType(volumeId);
            final CheckedListenableFuture<GetVolumeTokenResponseType> scGetTokenReplyFuture = AsyncRequests.dispatch( scConfig, tokenRequest );
            final Callable<Boolean> ncAttachRequest = new Callable<Boolean>( ) {
              public Boolean call( ) {
                try {
                  LOG.debug( vmId + ": waiting for storage volume: " + volumeId );
                  GetVolumeTokenResponseType scReply = scGetTokenReplyFuture.get();
                  String token = StorageProperties.formatVolumeAttachmentTokenForTransfer(scReply.getToken(), volumeId);
                  LOG.debug( vmId + ": " + volumeId + " => " + scGetTokenReplyFuture.get( ) );
                  AsyncRequests.dispatch( ccConfig, new AttachVolumeType( volumeId, vmId, vmDevice, token));
//                  final EntityTransaction db = Entities.get( VmInstance.class );
//                  try {
//                    final VmInstance entity = Entities.merge( vm );
//                    entity.lookupVolumeAttachment( volumeId ).setRemoteDevice( scReply.getRemoteDeviceString( ) );
//                    db.commit( );
//                  } catch ( final Exception ex ) {
//                    Logs.extreme( ).error( ex, ex );
//                    db.rollback( );
//                  }
                } catch ( Exception ex ) {
                  Exceptions.maybeInterrupted( ex );
                  LOG.error( vmId + ": " + ex );
                  Logs.extreme( ).error( ex, ex );
                }
                return true;
              }
            };
            Threads.enqueue( Eucalyptus.class, VmRuntimeState.class, ncAttachRequest );
          } catch ( Exception ex ) {
            LOG.error( vmId + ": " + ex );
            Logs.extreme( ).error( ex, ex );
          }
          return true;
        }
      };
      try {
        vm.getTransientVolumeState( ).eachVolumeAttachment( attachVolumes );
      } catch ( Exception ex ) {
        LOG.error( vm.getInstanceId( ) + ": " + ex );
        Logs.extreme( ).error( vm.getInstanceId( ) + ": " + ex, ex );
      }
    }
  }
  
  private Callable<Boolean> tryCleanUpRunnable( ) {
    return this.cleanUpRunnable( null, new Predicate<VmInstance>( ) {
      @Override
      public boolean apply( final VmInstance vmInstance ) {
        VmInstances.tryCleanUp( vmInstance );
        return true;
      }
    } );
  }
  
  private Callable<Boolean> cleanUpRunnable( ) {
    return this.cleanUpRunnable( null );
  }
  
  private Callable<Boolean> cleanUpRunnable( @Nullable final String reason ) {
    return this.cleanUpRunnable( reason, new Predicate<VmInstance>( ) {
      @Override
      public boolean apply( final VmInstance vmInstance ) {
        VmInstances.cleanUp( vmInstance );
        return true;
      }
    } );
  }
  
  private Callable<Boolean> cleanUpRunnable( @Nullable final String reason,
                                             final Predicate<VmInstance> cleaner ) {
    Logs.extreme( ).info( "Preparing to clean-up instance: " + this.getVmInstance( ).getInstanceId( ),
      Exceptions.filterStackTrace( new RuntimeException( ) ) );
    return new Callable<Boolean>( ) {
      @Override
      public Boolean call( ) {
        cleaner.apply( VmRuntimeState.this.getVmInstance( ) );
        if ( ( reason != null ) && !VmRuntimeState.this.reasonDetails.contains( reason ) ) {
          VmRuntimeState.this.addReasonDetail( reason );
        }
        return Boolean.TRUE;
      }
    };
  }
  
  VmBundleTask resetBundleTask( ) {
    final VmBundleTask oldTask = this.bundleTask;
    Bundles.putPreviousTask(oldTask);
    this.bundleTask = null;
    return oldTask;
  }
  
  String getServiceTag( ) {
    return this.serviceTag;
  }
  
  public void setServiceTag( final String serviceTag ) {
    if ( !Strings.nullToEmpty( this.serviceTag ).equals( serviceTag ) ) {
      this.serviceTag = serviceTag;
      this.setNodeTag( serviceTag );
    }
  }
  
  /**
   * Asynchronously assign the tag for this instance, do so only if it has changed.
   */
  private void setNodeTag( String serviceTag2 ) {
    final String host = URI.create( serviceTag2 ).getHost( );
    final VmInstance vm = VmRuntimeState.this.getVmInstance( );
    final CreateTagsType createTags = new CreateTagsType( ) {
      {
        this.getTagSet( ).add( new ResourceTag( VM_NC_HOST_TAG, host ) );
        this.getResourcesSet( ).add( vm.getInstanceId( ) );
        try {
          this.setEffectiveUserId( Accounts.lookupAccountByName( "eucalyptus" ).lookupAdmin( ).getUserId( ) );
        } catch ( AuthException ex ) {
          LOG.error( ex );
        }
      }
    };
    try {
      AsyncRequests.dispatch( Topology.lookup( Eucalyptus.class ), createTags );
    } catch ( Exception ex ) {
      LOG.error( ex );
    }
  }
  
  void setReason( final Reason reason ) {
    this.reason = reason;
  }
  
  StringBuffer getConsoleOutput( ) {
    return this.consoleOutput;
  }
  
  void setConsoleOutput( final StringBuffer consoleOutput ) {
    this.consoleOutput = consoleOutput;
    if ( this.passwordData == null ) {
      final String tempCo = consoleOutput.toString( ).replaceAll( "[\r\n]*", "" );
      if ( tempCo.matches( ".*<Password>[\\w=+/]*</Password>.*" ) ) {
        this.passwordData = tempCo.replaceAll( ".*<Password>", "" ).replaceAll( "</Password>.*", "" );
      }
    }
  }
  
  String getPasswordData( ) {
    return this.passwordData;
  }
  
  void setPasswordData( final String passwordData ) {
    this.passwordData = passwordData;
  }
  
  void setGuestState( final String guestState ) {
	  this.guestState = guestState;
  }
  
  String getGuestState( ){
	  return this.guestState;
  }
  
  VmInstance getVmInstance( ) {
    return this.vmInstance;
  }
  
  VmBundleTask getBundleTask( ) {
    return this.bundleTask;
  }
  
  /**
   * @return
   */
  public Boolean isBundling( ) {
    return this.bundleTask != null && !BundleState.none.equals( this.bundleTask.getState( ) );
  }
  
  BundleState getBundleTaskState( ) {
    if ( this.bundleTask != null ) {
      return this.getBundleTask( ).getState( );
    } else {
      return BundleState.none;
    }
  }
  
  public Boolean cancelBundleTask( ) {
    if ( this.getBundleTask( ) != null ) {
      this.getBundleTask( ).setState( BundleState.canceling );
      EventRecord.here( VmRuntimeState.class, EventType.BUNDLE_CANCELING, this.vmInstance.getOwner( ).toString( ), this.getBundleTask( ).getBundleId( ),
        this.getVmInstance( ).getInstanceId( ),
        "" + this.getBundleTask( ).getState( ) ).info( );
      return true;
    } else {
      return false;
    }
  }
  
  public Boolean restartBundleTask( ) {
    if ( this.getBundleTask( ) != null ) {
      this.getBundleTask( ).setState( BundleState.none );
      EventRecord.here( VmRuntimeState.class, EventType.BUNDLE_RESTART, this.vmInstance.getOwner( ).toString( ), this.getBundleTask( ).getBundleId( ),
        this.getVmInstance( ).getInstanceId( ),
        "" + this.getBundleTask( ).getState( ) ).info( );
      return true;
    }
    return false;
  }
  
  public Boolean submittedBundleTask( ) {
    if ( this.getBundleTask( ) != null ) {
      if ( BundleState.cancelled.equals( this.getBundleTaskState( ) ) ) {
        EventRecord.here( VmRuntimeState.class, EventType.BUNDLE_CANCELLED, this.vmInstance.getOwner( ).toString( ), this.getBundleTask( ).getBundleId( ),
          this.getVmInstance( ).getInstanceId( ),
          "" + this.getBundleTask( ).getState( ) ).info( );
        this.resetBundleTask( );
        return true;
      } else if ( this.getBundleTask( ).getState( ).ordinal( ) >= BundleState.storing.ordinal( ) ) {
        this.getBundleTask( ).setState( BundleState.storing );
        EventRecord.here( VmRuntimeState.class, EventType.BUNDLE_STARTING,
          this.vmInstance.getOwner( ).toString( ),
          this.getBundleTask( ).getBundleId( ),
          this.getVmInstance( ).getInstanceId( ),
          "" + this.getBundleTask( ).getState( ) ).info( );
        return true;
      }
    }
    return false;
  }
  
  public Boolean startBundleTask( final VmBundleTask task ) {
    if ( !this.isBundling( ) ) {
      Bundles.putPreviousTask(this.bundleTask);
      this.bundleTask = task;
      return true;
    } else {
      if ( ( this.getBundleTask( ) != null )
           && ( BundleState.failed.equals( task.getState( ) ) || BundleState.canceling.equals( task.getState( ) ) || BundleState.cancelled.equals( task.getState( ) ) ) ) {
        this.resetBundleTask( );
        this.bundleTask = task;
        return true;
      } else {
        return false;
      }
    }
  }
  
  void setBundleTask( final VmBundleTask bundleTask ) {
    Bundles.putPreviousTask(this.bundleTask);
    this.bundleTask = bundleTask;
  }
  
  VmMigrationTask getMigrationTask( ) {
    return this.migrationTask == null
                                     ? this.migrationTask = VmMigrationTask.create( this.getVmInstance( ) )
                                     : this.migrationTask;
  }
  
  void setMigrationTask( VmMigrationTask migrationTask ) {
    this.migrationTask = migrationTask;
  }
  
  public void startMigration( ) {
    this.getMigrationTask( ).updateMigrationTask( MigrationState.pending.name( ), null, null );
    //TODO:GRZE: VolumeMigration.update( vmInstance );
    MigrationTags.update( this.getVmInstance( ) );
  }
  
  public void abortMigration( ) {
    this.getMigrationTask( ).updateMigrationTask( MigrationState.none.name( ), null, null );
    //TODO:GRZE: VolumeMigration.update( vmInstance );
    MigrationTags.update( this.getVmInstance( ) );
  }

  public void setMigrationState( String stateName, String sourceHost, String destHost ) {
    if ( this.getMigrationTask( ).updateMigrationTask( stateName, sourceHost, destHost ) ) {//actually updated the state
      //TODO:GRZE: VolumeMigration.update( vmInstance );
      MigrationTags.update( this.getVmInstance( ) );
    }
  }
  
  private void setVmInstance( final VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
  }
  
  public Boolean isCreatingImage( ) {
    return this.createImageTask != null && 
    		! ( 	VmCreateImageTask.CreateImageState.none.equals(this.createImageTask.getState()) ||
    				VmCreateImageTask.CreateImageState.complete.equals(this.createImageTask.getState()) ||
    				VmCreateImageTask.CreateImageState.failed.equals(this.createImageTask.getState()));
    				
  }
  
  public void setCreateImageTaskState( final VmCreateImageTask.CreateImageState state ) {
    if ( this.createImageTask != null ) {
    	this.createImageTask.setState( state );
    }else{
    	throw Exceptions.toUndeclared(new Exception("No VmCreateImage task is found for the instance"));
    }
  }
  
  public VmCreateImageTask.CreateImageState getCreateImageTaskState(){
	  if ( this.createImageTask != null ) {
		  return this.createImageTask.getState();
	  }else
		  throw Exceptions.toUndeclared(new Exception("No VmCreateImageTask is found for the instance"));
  }
  
  public VmCreateImageTask getVmCreateImageTask(){
	  return this.createImageTask;
  }

  public VmCreateImageTask resetCreateImageTask(final VmCreateImageTask.CreateImageState state, final String imageId, final String snapshotId, final Boolean noReboot ) {
    final VmCreateImageTask oldTask = this.createImageTask;
    this.createImageTask  = new VmCreateImageTask(this.vmInstance, state.toString(), new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()),
			null, null, null, imageId, noReboot);
    return oldTask;
  }
  
  @Override
  public String toString( ) {
    final StringBuilder builder = new StringBuilder( );
    builder.append( "VmRuntimeState:" );
    if ( this.bundleTask != null ) builder.append( "bundleTask=" ).append( this.bundleTask ).append( ":" );
    if ( this.createImageTask != null ) builder.append( "createImageTask=" ).append( this.createImageTask ).append( ":" );
    if ( this.serviceTag != null ) builder.append( "serviceTag=" ).append( this.serviceTag ).append( ":" );
    if ( this.reason != null ) builder.append( "reason=" ).append( this.reason ).append( ":" );
    if ( Entities.isReadable( this.reasonDetails ) ) builder.append( "reasonDetails=" ).append( this.reasonDetails ).append( ":" );
    if ( this.consoleOutput != null ) builder.append( "consoleOutput=" ).append( this.consoleOutput ).append( ":" );
    if ( this.passwordData != null ) builder.append( "passwordData=" ).append( this.passwordData ).append( ":" );
    if ( this.pending != null ) builder.append( "pending=" ).append( this.pending );
    return builder.toString( );
  }
  
  @Nullable
  Reason reason( ) {
    return reason;
  }
  
  private VmCreateImageTask getCreateImageTask( ) {
    return this.createImageTask;
  }
  
  private void setCreateImageTask( VmCreateImageTask createImageTask ) {
    this.createImageTask = createImageTask;
  }
  
  private Boolean getPending( ) {
    return this.pending;
  }
  
  private void setPending( Boolean pending ) {
    this.pending = pending;
  }
  
  private Set<String> getReasonDetails( ) {
    return this.reasonDetails;
  }
  
  private void setReasonDetails( Set<String> reasonDetails ) {
    this.reasonDetails = reasonDetails;
  }
  
  public void updateBundleTaskState( String state ) {
    BundleState next = BundleState.mapper.apply( state );
    updateBundleTaskState( next );
  }
  
  public void bundleRestartInstance( VmBundleTask bundleTask ) {
    BundleState state = bundleTask.getState( );
    if ( BundleState.complete.equals( state ) || BundleState.failed.equals( state ) || BundleState.cancelled.equals( state ) ) {
      final BundleRestartInstanceType request = new BundleRestartInstanceType( );
      final BundleRestartInstanceResponseType reply = request.getReply( );
      
      reply.set_return( true );
      try {
        LOG.info( EventRecord.here( BundleCallback.class, EventType.BUNDLE_RESTART, vmInstance.getOwner( ).getUserName( ),
          bundleTask.getBundleId( ),
          vmInstance.getInstanceId( ) ) );
        
        ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, vmInstance.lookupPartition( ) );
        final Cluster cluster = Clusters.lookup( ccConfig );
        
        request.setInstanceId( vmInstance.getInstanceId( ) );
        reply.setTask( Bundles.transform( bundleTask ) );
        AsyncRequests.newRequest( Bundles.bundleRestartInstanceCallback( request ) ).dispatch( cluster.getConfiguration( ) );
      } catch ( final Exception e ) {
        Logs.extreme( ).trace( "Failed to find bundle task: " + bundleTask.getBundleId( ) );
      }
    }
  }
  
  public void stopVmInstance(final StopInstanceCallback cb) {
	  final StopInstanceType request = new StopInstanceType();
	  try{
		  ServiceConfiguration ccConfig = Topology.lookup(ClusterController.class, vmInstance.lookupPartition());
		  final Cluster cluster = Clusters.lookup(ccConfig);
		  request.setInstanceId(vmInstance.getInstanceId());
		  cb.setRequest(request);
		  AsyncRequests.newRequest( cb ).dispatch(cluster.getConfiguration());
	  } catch (final Exception e) {
		  Exceptions.toUndeclared(e);
	  }
  }
  
  public void startVmInstance(final StartInstanceCallback cb) {
	  final StartInstanceType request = new StartInstanceType();
	  try{
		  ServiceConfiguration ccConfig = Topology.lookup(ClusterController.class, vmInstance.lookupPartition());
		  final Cluster cluster = Clusters.lookup(ccConfig);
		  request.setInstanceId(vmInstance.getInstanceId());
		  cb.setRequest(request);
		  AsyncRequests.newRequest( cb ).dispatch(cluster.getConfiguration());
	  }catch (final Exception e){
		  Exceptions.toUndeclared(e);
	  }
  }
  
  public void updateBundleTaskState( BundleState state ) {
    if ( this.getBundleTask( ) != null ) {
      final BundleState current = this.getBundleTask( ).getState( );
      if ( BundleState.complete.equals( state ) && !BundleState.complete.equals( current ) && !BundleState.none.equals( current ) ) {
        this.getBundleTask( ).setState( state );
        bundleRestartInstance( this.getBundleTask( ) );
      } else if ( BundleState.failed.equals( state ) && !BundleState.failed.equals( current ) && !BundleState.none.equals( current ) ) {
        this.getBundleTask( ).setState( state );
        bundleRestartInstance( this.getBundleTask( ) );
      } else if ( BundleState.cancelled.equals( state ) && !BundleState.cancelled.equals( current ) && !BundleState.none.equals( current ) ) {
        this.getBundleTask( ).setState( state );
        bundleRestartInstance( this.getBundleTask( ) );
      } else if ( BundleState.canceling.equals( state ) || BundleState.canceling.equals( current ) ) {
        //
      } else if ( BundleState.pending.equals( current ) && !BundleState.none.equals( state ) ) {
        this.getBundleTask( ).setState( state );
        this.getBundleTask( ).setUpdateTime( new Date( ) );
        EventRecord.here( VmRuntimeState.class, EventType.BUNDLE_TRANSITION, this.vmInstance.getOwner( ).toString( ), "" + this.getBundleTask( ) ).info( );
      } else if ( BundleState.storing.equals( state ) ) {
        this.getBundleTask( ).setState( state );
        this.getBundleTask( ).setUpdateTime( new Date( ) );
        EventRecord.here( VmRuntimeState.class, EventType.BUNDLE_TRANSITION, this.vmInstance.getOwner( ).toString( ), "" + this.getBundleTask( ) ).info( );
      }
    } else {
      this.setBundleTask( new VmBundleTask( this.vmInstance, state.name( ), new Date( ), new Date( ), 0, "unknown", "unknown", "unknown", "unknown" ) );
      Logs.extreme( ).trace( "Unhandle bundle task state update: " + state );
    }
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.vmInstance == null ) ? 0 : this.vmInstance.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    VmRuntimeState other = ( VmRuntimeState ) obj;
    if ( this.vmInstance == null ) {
      if ( other.vmInstance != null ) {
        return false;
      }
    } else if ( !this.vmInstance.equals( other.vmInstance ) ) {
      return false;
    }
    return true;
  }
  
}
