/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.cluster;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicMarkableReference;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EntityTransaction;
import javax.persistence.Lob;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Parent;
import com.eucalyptus.cluster.VmInstance.BundleState;
import com.eucalyptus.cluster.VmInstance.Reason;
import com.eucalyptus.cluster.callback.BundleCallback;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.vm.BundleTask;
import com.eucalyptus.vm.VmState;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Embeddable
public class VmRuntimeState {
  @Transient
  private static String                             SEND_USER_TERMINATE        = "SIGTERM";
  @Transient
  private static String                             SEND_USER_STOP             = "SIGSTOP";
  @Transient
  private static Logger                             LOG                        = Logger.getLogger( VmRuntimeState.class );
  @Parent
  private VmInstance                                vmInstance;
  private VmBundleTask                              bundleTask;
  private String                                    serviceTag;
  @Transient
  private Reason                                    reason;
  @Transient
  private List<String>                              reasonDetails              = Lists.newArrayList( );
  @Transient
  private StringBuffer                              consoleOutput              = new StringBuffer( );
//  @Embedded
//private Set<VmVolumeAttachment>             transientVolumes = Sets.newHashSet( );
  @ElementCollection
  @CollectionTable( name = "metadata_vm_transient_volume_attachments" )
  private Set<VmVolumeAttachment>                   transientVolumeAttachments = Sets.newHashSet( );
  @Transient
  private ConcurrentMap<String, VmVolumeAttachment> transientVolumes           = new ConcurrentSkipListMap<String, VmVolumeAttachment>( );
  @Transient
  protected AtomicMarkableReference<VmState>        runtimeState               = new AtomicMarkableReference<VmState>( VmState.PENDING, false );
  @Lob
  @Column( name = "metadata_vm_password_data" )
  private String                                    passwordData;
  
  VmRuntimeState( VmInstance vmInstance ) {
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
    return this.reason.name( ) + ": " + this.reason + ( this.reasonDetails != null
      ? " -- " + this.reasonDetails
      : "" );
  }
  
  private void addReasonDetail( final String... extra ) {
    for ( final String s : extra ) {
      this.reasonDetails.add( s );
    }
  }
  
  public boolean clearPending( ) {
    if ( this.runtimeState.isMarked( ) && ( this.getRuntimeState( ).ordinal( ) > VmState.RUNNING.ordinal( ) ) ) {
      this.runtimeState.set( this.getRuntimeState( ), false );
      VmInstances.cleanUp( this.getVmInstance( ) );
      return true;
    } else {
      this.runtimeState.set( this.getRuntimeState( ), false );
      return false;
    }
  }
  
  public void setState( final VmState newState, Reason reason, final String... extra ) {
    
    EntityTransaction db = Entities.get( VmRuntimeState.class );
    try {
      VmRuntimeState entity = Entities.merge( this );
      if ( newState == null || this.runtimeState == null || this.runtimeState.getReference( ) == null ) {
        this.runtimeState = new AtomicMarkableReference<VmState>( newState != null
          ? newState
          : VmState.PENDING, false );
        return;
      }
      final VmState oldState = this.runtimeState.getReference( );
      if ( VmState.SHUTTING_DOWN.equals( newState ) && VmState.SHUTTING_DOWN.equals( oldState ) && Reason.USER_TERMINATED.equals( reason ) ) {
        VmInstances.cleanUp( this.getVmInstance( ) );
        if ( !this.reasonDetails.contains( SEND_USER_TERMINATE ) ) {
          this.addReasonDetail( SEND_USER_TERMINATE );
        }
      } else if ( VmState.STOPPING.equals( newState ) && VmState.STOPPING.equals( oldState ) && Reason.USER_STOPPED.equals( reason ) ) {
        VmInstances.cleanUp( this.getVmInstance( ) );
        if ( !this.reasonDetails.contains( SEND_USER_STOP ) ) {
          this.addReasonDetail( SEND_USER_STOP );
        }
      } else if ( ( VmState.TERMINATED.equals( newState ) && VmState.TERMINATED.equals( oldState ) ) || VmState.BURIED.equals( newState ) ) {
        VmInstances.deregister( this.getVmInstance( ).getInstanceId( ) );
      } else if ( !this.getRuntimeState( ).equals( newState ) ) {
        if ( Reason.APPEND.equals( reason ) ) {
          reason = this.reason;
        }
        this.addReasonDetail( extra );
        LOG.info( String.format( "%s state change: %s -> %s", this.getVmInstance( ).getInstanceId( ), this.getState( ), newState ) );
        this.reason = reason;
        if ( this.runtimeState.isMarked( ) && VmState.PENDING.equals( this.getRuntimeState( ) ) ) {
          if ( VmState.SHUTTING_DOWN.equals( newState ) || VmState.PENDING.equals( newState ) ) {
            this.runtimeState.set( newState, true );
          } else {
            this.runtimeState.set( newState, false );
          }
        } else if ( this.runtimeState.isMarked( ) && VmState.SHUTTING_DOWN.equals( this.getState( ) ) ) {
          LOG.debug( "Ignoring events for state transition because the instance is marked as pending: " + oldState + " to " + this.getState( ) );
        } else if ( !this.runtimeState.isMarked( ) ) {
          if ( ( oldState.ordinal( ) <= VmState.RUNNING.ordinal( ) ) && ( newState.ordinal( ) > VmState.RUNNING.ordinal( ) ) ) {
            this.runtimeState.set( newState, false );
            VmInstances.cleanUp( this.getVmInstance( ) );
          } else if ( VmState.PENDING.equals( oldState ) && VmState.RUNNING.equals( newState ) ) {
            this.runtimeState.set( newState, false );
          } else if ( VmState.TERMINATED.equals( newState ) && ( oldState.ordinal( ) <= VmState.RUNNING.ordinal( ) ) ) {
            this.runtimeState.set( newState, false );
            VmInstances.disable( this.getVmInstance( ) );
            VmInstances.cleanUp( this.getVmInstance( ) );
          } else if ( VmState.TERMINATED.equals( newState ) && ( oldState.ordinal( ) > VmState.RUNNING.ordinal( ) ) ) {
            this.runtimeState.set( newState, false );
            VmInstances.disable( this.getVmInstance( ) );
          } else if ( ( oldState.ordinal( ) > VmState.RUNNING.ordinal( ) ) && ( newState.ordinal( ) <= VmState.RUNNING.ordinal( ) ) ) {
            this.runtimeState.set( oldState, false );
            VmInstances.cleanUp( this.getVmInstance( ) );
          } else if ( newState.ordinal( ) > oldState.ordinal( ) ) {
            this.runtimeState.set( newState, false );
          }
          this.getVmInstance( ).setState( this.getRuntimeState( ) );
          this.getVmInstance( ).store( );
        } else {
          LOG.debug( "Ignoring events for state transition because the instance is marked as pending: " + oldState + " to " + this.getState( ) );
        }
        this.getVmInstance( ).setState( this.runtimeState.getReference( ) );
        if ( !this.getState( ).equals( oldState ) ) {
          EventRecord.caller( VmInstance.class, EventType.VM_STATE, this.getVmInstance( ).getInstanceId( ), this.vmInstance.getOwner( ),
                              this.runtimeState.getReference( ).name( ) );
        }
      }
      db.commit( );
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
    }
    
  }
  
  /**
   * @return
   */
  private VmState getState( ) {
    return this.runtimeState.getReference( );
  }
  
  VmBundleTask resetBundleTask( ) {
    VmBundleTask oldTask = this.bundleTask;
    this.bundleTask = null;
    return oldTask;
  }
  
  String getServiceTag( ) {
    return this.serviceTag;
  }
  
  void setServiceTag( String serviceTag ) {
    this.serviceTag = serviceTag;
  }
  
  void setReason( Reason reason ) {
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
  
  void setPasswordData( String passwordData ) {
    this.passwordData = passwordData;
  }
  
  VmInstance getVmInstance( ) {
    return this.vmInstance;
  }
  
  VmBundleTask getBundleTask( ) {
    return this.bundleTask;
  }
  
  List<String> getReasonDetails( ) {
    return this.reasonDetails;
  }
  
  VmState getRuntimeState( ) {
    return this.runtimeState.getReference( );
  }
  
  /**
   * @return
   */
  public boolean isMarked( ) {
    return this.runtimeState.isMarked( );
  }
  
  /**
   * @return
   */
  public Boolean isBundling( ) {
    return this.bundleTask != null;
  }
  
  BundleState getBundleTaskState( ) {
    if ( this.bundleTask != null ) {
      return BundleState.valueOf( this.getBundleTask( ).getState( ) );
    } else {
      return null;
    }
  }
  
  void setBundleTaskState( String state ) {
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
    if ( this.bundleTask != null ) {
      final BundleState current = BundleState.valueOf( this.getBundleTask( ).getState( ) );
      if ( BundleState.complete.equals( current ) || BundleState.failed.equals( current ) ) {
        return; //already finished, wait and timeout the state along with the instance.
      } else if ( BundleState.storing.equals( next ) || BundleState.storing.equals( current ) ) {
        this.getBundleTask( ).setState( next.name( ) );
        EventRecord.here( BundleCallback.class, EventType.BUNDLE_TRANSITION, this.vmInstance.getOwner( ).toString( ), this.getBundleTask( ).getBundleId( ),
                          this.getVmInstance( ).getInstanceId( ),
                          this.getBundleTask( ).getState( ) ).info( );
        this.getBundleTask( ).setUpdateTime( new Date( ) );
      } else if ( BundleState.none.equals( next ) && BundleState.failed.equals( current ) ) {
        this.resetBundleTask( );
      }
    }
  }
  
  public Boolean cancelBundleTask( ) {
    if ( this.getBundleTask( ) != null ) {
      this.getBundleTask( ).setState( BundleState.canceling.name( ) );
      EventRecord.here( BundleCallback.class, EventType.BUNDLE_CANCELING, this.vmInstance.getOwner( ).toString( ), this.getBundleTask( ).getBundleId( ),
                        this.getVmInstance( ).getInstanceId( ),
                        this.getBundleTask( ).getState( ) ).info( );
      return true;
    } else {
      return false;
    }
  }
  
  public Boolean clearPendingBundleTask( ) {
    if ( BundleState.pending.name( ).equals( this.getBundleTask( ).getState( ) ) ) {
      this.getBundleTask( ).setState( BundleState.storing.name( ) );
      EventRecord.here( BundleCallback.class, EventType.BUNDLE_STARTING, this.vmInstance.getOwner( ).toString( ), this.getBundleTask( ).getBundleId( ),
                        this.getVmInstance( ).getInstanceId( ),
                        this.getBundleTask( ).getState( ) ).info( );
      return true;
    } else if ( BundleState.canceling.name( ).equals( this.getBundleTask( ).getState( ) ) ) {
      EventRecord.here( BundleCallback.class, EventType.BUNDLE_CANCELLED, this.vmInstance.getOwner( ).toString( ), this.getBundleTask( ).getBundleId( ),
                        this.getVmInstance( ).getInstanceId( ),
                        this.getBundleTask( ).getState( ) ).info( );
      this.resetBundleTask( );
      return true;
    } else {
      return false;
    }
  }
  
  public Boolean startBundleTask( final VmBundleTask task ) {
    if ( this.bundleTask == null ) {
      this.bundleTask = task;
      return true;
    } else {
      if ( ( this.getBundleTask( ) != null ) && BundleState.failed.equals( BundleState.valueOf( this.getBundleTask( ).getState( ) ) ) ) {
        this.resetBundleTask( );
        this.bundleTask = task;
        return true;
      } else {
        return false;
      }
    }
  }
  
  private VmVolumeAttachment resolveVolumeId( final String volumeId ) throws NoSuchElementException {
    final VmVolumeAttachment v = this.getTransientVolumes( ).get( volumeId );
    if ( v == null ) {
      throw new NoSuchElementException( "Failed to find volume attachment for instance " + this.getVmInstance( ).getInstanceId( ) + " and volume " + volumeId );
    } else {
      return v;
    }
  }
  
  public VmVolumeAttachment removeVolumeAttachment( final String volumeId ) throws NoSuchElementException {
    final VmVolumeAttachment v = this.transientVolumes.remove( volumeId );
    this.transientVolumeAttachments.remove( v );
    if ( v == null ) {
      throw new NoSuchElementException( "Failed to find volume attachment for instance " + this.getVmInstance( ).getInstanceId( ) + " and volume " + volumeId );
    } else {
      return v;
    }
  }
  
  public void updateVolumeAttachment( final String volumeId, final String state ) throws NoSuchElementException {
    final VmVolumeAttachment v = this.resolveVolumeId( volumeId );
    v.setStatus( state );
  }
  
  public VmVolumeAttachment lookupVolumeAttachment( final String volumeId ) throws NoSuchElementException {
    return this.resolveVolumeId( volumeId );
  }
  
  public VmVolumeAttachment lookupVolumeAttachment( final Predicate<VmVolumeAttachment> pred ) throws NoSuchElementException {
    final VmVolumeAttachment v = Iterables.find( this.getTransientVolumes( ).values( ), pred );
    if ( v == null ) {
      throw new NoSuchElementException( "Failed to find volume attachment for instance " + this.getVmInstance( ).getInstanceId( ) + " using predicate "
                                        + pred.getClass( ).getCanonicalName( ) );
    } else {
      return v;
    }
  }
  
  public <T> Iterable<T> transformVolumeAttachments( final Function<? super VmVolumeAttachment, T> function ) throws NoSuchElementException {
    return Iterables.transform( this.getTransientVolumes( ).values( ), function );
  }
  
  public boolean eachVolumeAttachment( final Predicate<VmVolumeAttachment> pred ) throws NoSuchElementException {
    return Iterables.all( this.getTransientVolumes( ).values( ), pred );
  }
  
  public void addVolumeAttachment( final VmVolumeAttachment volume ) {
    final String volumeId = volume.getVolumeId( );
    volume.setStatus( "attaching" );
    final VmVolumeAttachment v = this.getTransientVolumes( ).put( volumeId, volume );
    if ( v != null ) {
      this.getTransientVolumes( ).replace( volumeId, v );
      this.transientVolumeAttachments.add( volume );
    }
  }
  
  public void updateVolumeAttachments( final List<VmVolumeAttachment> ncAttachedVols ) throws NoSuchElementException {
    final Map<String, VmVolumeAttachment> ncAttachedVolMap = new HashMap<String, VmVolumeAttachment>( ) {
      /**
       * 
       */
      @Transient
      private static final long serialVersionUID = 1L;
      
      {
        for ( final VmVolumeAttachment v : ncAttachedVols ) {
          this.put( v.getVolumeId( ), v );
        }
      }
    };
    this.eachVolumeAttachment( new Predicate<VmVolumeAttachment>( ) {
      @Override
      public boolean apply( final VmVolumeAttachment arg0 ) {
        final String volId = arg0.getVolumeId( );
        if ( ncAttachedVolMap.containsKey( volId ) ) {
          final VmVolumeAttachment ncVol = ncAttachedVolMap.get( volId );
          if ( "detached".equals( ncVol.getStatus( ) ) ) {
            VmRuntimeState.this.removeVolumeAttachment( volId );
          } else if ( "attaching".equals( arg0.getStatus( ) ) || "attached".equals( arg0.getStatus( ) ) ) {
            VmRuntimeState.this.updateVolumeAttachment( volId, arg0.getStatus( ) );
          }
        } else if ( "detaching".equals( arg0.getStatus( ) ) ) {//TODO:GRZE:remove this case when NC is updated to report "detached" state
          VmRuntimeState.this.removeVolumeAttachment( volId );
        }
        ncAttachedVolMap.remove( volId );
        return true;
      }
    } );
    for ( final VmVolumeAttachment v : ncAttachedVolMap.values( ) ) {
      LOG.warn( "Restoring volume attachment state for " + this.getVmInstance( ).getInstanceId( ) + " with " + v.toString( ) );
      this.addVolumeAttachment( v );
    }
  }
  
  ConcurrentMap<String, VmVolumeAttachment> getTransientVolumes( ) {
    return this.transientVolumes;
  }
  
  private void setBundleTask( VmBundleTask bundleTask ) {
    this.bundleTask = bundleTask;
  }
  
  private void setReasonDetails( List<String> reasonDetails ) {
    this.reasonDetails = reasonDetails;
  }
  
  private void setTransientVolumes( ConcurrentMap<String, VmVolumeAttachment> transientVolumes ) {
    this.transientVolumes = transientVolumes;
  }
  
  private void setVmInstance( VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
  }
  
  Set<VmVolumeAttachment> getTransientVolumeAttachments( ) {
    return this.transientVolumeAttachments;
  }
  
}
