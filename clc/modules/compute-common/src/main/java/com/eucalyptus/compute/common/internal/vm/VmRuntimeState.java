/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.compute.common.internal.vm;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Parent;
import org.hibernate.annotations.Type;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.compute.common.internal.vm.VmBundleTask.BundleState;
import com.eucalyptus.compute.common.internal.vm.VmInstance.Reason;
import com.google.common.base.CaseFormat;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

@Embeddable
public class VmRuntimeState {

  private static final Logger LOG                 = Logger.getLogger( VmRuntimeState.class );

  public enum InstanceStatus implements Predicate<VmInstance> {
    Ok,
    Impaired,
    InsufficientData,
    NotApplicable,
    ;

    public String toString( ) {
      return CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_HYPHEN, name( ) );
    }

    public static Function<String,InstanceStatus> fromString( ) {
      return new Function<String,InstanceStatus>( ){
        @Nullable
        @Override
        public InstanceStatus apply( final String value ) {
          return Enums.getIfPresent(
              InstanceStatus.class,
              CaseFormat.LOWER_HYPHEN.to( CaseFormat.UPPER_CAMEL, value )
          ).orNull( );
        }
      };
    }

    @Override
    public boolean apply( @Nullable final VmInstance instance ) {
      return instance != null && instance.getRuntimeState( ).getInstanceStatus( ) == this;
    }
  }

  public enum ReachabilityStatus {
    Passed,
    Failed,
    Insufficient_Data,
    ;

    public String toString( ) {
      return CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_HYPHEN, name( ) );
    }

    public static Function<String,ReachabilityStatus> fromString( ) {
      return new Function<String,ReachabilityStatus>( ){
        @Nullable
        @Override
        public ReachabilityStatus apply( final String value ) {
          return Enums.getIfPresent(
              ReachabilityStatus.class,
              CaseFormat.LOWER_HYPHEN.to( CaseFormat.UPPER_CAMEL, value )
          ).orNull( );
        }
      };
    }
  }

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
  private Set<String>         reasonDetails       = Sets.newHashSet( );
  @Type(type="text")
  @Column( name = "metadata_vm_password_data" )
  private String              passwordData;
  @Column( name = "metadata_vm_pending" )
  private Boolean             pending;
  @Column( name = "metadata_vm_zombie" )
  private Boolean             zombie;
  @Column( name = "metadata_vm_guest_state" )
  private String              guestState;
  
  @Embedded
  private VmMigrationTask     migrationTask;

  @Enumerated( EnumType.STRING )
  @Column( name = "metadata_instance_status" )
  private InstanceStatus instanceStatus;

  @Enumerated( EnumType.STRING )
  @Column( name = "metadata_reachability_status" )
  private ReachabilityStatus reachabilityStatus;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "metadata_instance_unreachable_timestamp")
  private Date unreachableTimestamp;

  VmRuntimeState( final VmInstance vmInstance ) {
    super( );
    this.vmInstance = vmInstance;
    this.instanceStatus = InstanceStatus.Ok;
    this.reachabilityStatus = ReachabilityStatus.Passed;
  }
  
  VmRuntimeState( ) {
    super( );
  }

  public String getDisplayReason() {
    if ( this.reason == null ) {
      this.reason = Reason.NORMAL;
    }
    return this.reason.name( ) + ": " + this.reason + ( this.reasonDetails != null ? " -- " + this.reasonDetails : "" );
  }

  public void addReasonDetail( final String... extra ) {
    Collections.addAll( this.reasonDetails, extra );
  }
  
  public void reachable( ) {
    // zombie instances cannot be reachable
    if ( !Objects.firstNonNull( getZombie( ), Boolean.FALSE ) ) {
      setUnreachableTimestamp( null );
      setInstanceStatus( VmRuntimeState.InstanceStatus.Ok );
      setReachabilityStatus( VmRuntimeState.ReachabilityStatus.Passed );
    }
  }

  public void zombie( ) {
    setZombie( true );
    setUnreachableTimestamp( new Date( ) );
    setInstanceStatus( InstanceStatus.Impaired );
    setReachabilityStatus( ReachabilityStatus.Failed );
  }

  public Boolean getZombie( ) {
    return zombie;
  }

  public void setZombie( final Boolean zombie ) {
    this.zombie = zombie;
  }

  @Nullable
  public InstanceStatus getInstanceStatus( ) {
    return instanceStatus;
  }

  public void setInstanceStatus( final InstanceStatus instanceStatus ) {
    this.instanceStatus = instanceStatus;
  }

  @Nullable
  public ReachabilityStatus getReachabilityStatus( ) {
    return reachabilityStatus;
  }

  public void setReachabilityStatus( final ReachabilityStatus reachabilityStatus ) {
    this.reachabilityStatus = reachabilityStatus;
  }

  @Nullable
  public Date getUnreachableTimestamp() {
    return unreachableTimestamp;
  }

  public void setUnreachableTimestamp( final Date unreachableTimestamp ) {
    this.unreachableTimestamp = unreachableTimestamp;
  }

  public String getServiceTag( ) {
    return this.serviceTag;
  }
  
  public void setServiceTag( final String serviceTag ) {
    this.serviceTag = serviceTag;
  }

  public Reason getReason() {
    return reason;
  }

  public void setReason( final Reason reason ) {
    this.reason = reason;
  }
  
  String getPasswordData( ) {
    return this.passwordData;
  }
  
  void setPasswordData( final String passwordData ) {
    this.passwordData = passwordData;
  }
  
  public void setGuestState( final String guestState ) {
	  this.guestState = guestState;
  }
  
  public String getGuestState( ){
	  return this.guestState;
  }
  
  public VmInstance getVmInstance( ) {
    return this.vmInstance;
  }
  
  public VmBundleTask getBundleTask( ) {
    return this.bundleTask;
  }
  
  /**
   * @return
   */
  public Boolean isBundling( ) {
    return this.bundleTask != null && !BundleState.none.equals( this.bundleTask.getState( ) );
  }
  
  public BundleState getBundleTaskState( ) {
    if ( this.bundleTask != null ) {
      return this.getBundleTask( ).getState( );
    } else {
      return BundleState.none;
    }
  }
  
  public void setBundleTask( final VmBundleTask bundleTask ) {
    this.bundleTask = bundleTask;
  }
  
  public VmMigrationTask getMigrationTask( ) {
    return this.migrationTask == null
                                     ? this.migrationTask = VmMigrationTask.create( this.getVmInstance( ) )
                                     : this.migrationTask;
  }
  
  void setMigrationTask( VmMigrationTask migrationTask ) {
    this.migrationTask = migrationTask;
  }
  
  private void setVmInstance( final VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
  }
  
  public Boolean isCreatingImage( ) {
    return this.createImageTask != null &&
        !VmCreateImageTask.idleStates( ).contains( this.createImageTask.getState( ) );
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
    if ( this.passwordData != null ) builder.append( "passwordData=" ).append( this.passwordData ).append( ":" );
    if ( this.pending != null ) builder.append( "pending=" ).append( this.pending );
    if ( this.zombie != null ) builder.append( "zombie=" ).append( this.zombie );
    if ( this.instanceStatus != null ) builder.append( "instanceStatus=" ).append( this.instanceStatus );
    if ( this.reachabilityStatus != null ) builder.append( "reachabilityStatus=" ).append( this.reachabilityStatus );
    return builder.toString( );
  }
  
  @Nullable
  public Reason reason( ) {
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
  
  public Set<String> getReasonDetails( ) {
    return this.reasonDetails;
  }
  
  public void setReasonDetails( Set<String> reasonDetails ) {
    this.reasonDetails = reasonDetails;
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
