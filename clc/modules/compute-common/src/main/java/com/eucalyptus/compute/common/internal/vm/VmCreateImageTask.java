/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;

import org.hibernate.annotations.Parent;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class VmCreateImageTask {
	public enum CreateImageState {
	   none ( "none" ), pending( "pending" ), guest_stopping( "stopping"), creating_snapshot("snapshotting"), guest_starting("starting"), complete ("complete"), failed( "failed" );
	   
	   private String strState = null;
	   private CreateImageState(final String state){
		   strState = state;
	   }
	   @Override
	   public String toString(){
		   return strState;
	   }
	   
	   public static Function<String, CreateImageState> mapper = new Function<String, CreateImageState> () {
		   @Override
		   public CreateImageState apply(final String input){
			   for ( final CreateImageState s : CreateImageState.values( ) ) {
                   if ( ( s.toString( ) != null ) && s.toString( ).equals( input ) ) {
                     return s;
                   }
                 }
                 return none;
		   }
	   };
	}
	
  @Parent
  private VmInstance vmInstance;
  @Column( name = "metadata_vm_createimage_state" )
  private String     state;
  @Column( name = "metadata_vm_createimage_start_time" )
  private Date       startTime;
  @Column( name = "metadata_vm_createimage_update_time" )
  private Date       updateTime;
  @Column( name = "metadata_vm_createimage_progress" )
  private String     progress;
  @Column( name = "metadata_vm_createimage_error_msg" )
  private String     errorMessage;
  @Column( name = "metadata_vm_createimage_error_code" )
  private String     errorCode;
  @Column( name = "metadata_vm_createimage_image_id")
  private String 	 imageId;
  
  @ElementCollection
  @CollectionTable( name = "metadata_instances_createimage_snapshots" )
  private Set<VmCreateImageSnapshot> snapshots = Sets.newHashSet( );
  
  @Column( name = "metadata_vm_createimage_no_reboot")
  private Boolean 	 noReboot;
  
  
  VmCreateImageTask( ) {
    super( );
  }
  
  VmCreateImageTask( final VmInstance vmInstance, final String state, final Date startTime, final Date updateTime,
                     final String progress, final String errorMessage, final String errorCode, 
                     final String imageId, final Boolean noReboot ) {
    super( );
    this.vmInstance = vmInstance;
    this.state = state;
    this.startTime = startTime;
    this.updateTime = updateTime;
    this.progress = progress;
    this.errorMessage = errorMessage;
    this.errorCode = errorCode;
    this.imageId  = imageId;
    this.noReboot = noReboot;
  }
  
  private VmInstance getVmInstance( ) {
    return this.vmInstance;
  }
  
  private void setVmInstance( final VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
  }
  
  CreateImageState getState( ) {
    return CreateImageState.mapper.apply(this.state);
  }
  
  void setState( final CreateImageState state ) {
    this.state = state.toString();
  }
  
  private Date getStartTime( ) {
    return this.startTime;
  }
  
  private void setStartTime( final Date startTime ) {
    this.startTime = startTime;
  }
  
  private Date getUpdateTime( ) {
    return this.updateTime;
  }
  
  void setUpdateTime( final Date updateTime ) {
    this.updateTime = updateTime;
  }
  
  private String getProgress( ) {
    return this.progress;
  }
  
  private void setProgress( final String progress ) {
    this.progress = progress;
  }
  
  private String getErrorMessage( ) {
    return this.errorMessage;
  }
  
  private void setErrorMessage( final String errorMessage ) {
    this.errorMessage = errorMessage;
  }
  
  private String getErrorCode( ) {
    return this.errorCode;
  }
  
  private void setErrorCode( final String errorCode ) {
    this.errorCode = errorCode;
  }
  
  public String getImageId( ) {
	  return this.imageId;
  }
  
  public void setImageId(final String imageId){
	  this.imageId = imageId;
  }
  
  public void addSnapshot(final String deviceName, final String snapshotId, final Boolean isRootDevice, final Boolean deleteOnTerminate){
	  final VmCreateImageSnapshot newSnapshot = new VmCreateImageSnapshot(deviceName, snapshotId, isRootDevice, deleteOnTerminate);
	  this.snapshots.add(newSnapshot);
  }
  
  public Set<VmCreateImageSnapshot> getSnapshots(){
	  return this.snapshots;
  }
  
  public Boolean getNoReboot() {
	  return this.noReboot;
  }
  
  public void setNoReboot(final Boolean noReboot) {
	  this.noReboot = noReboot;
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.vmInstance == null )
      ? 0
      : this.vmInstance.hashCode( ) );
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
    VmCreateImageTask other = ( VmCreateImageTask ) obj;
    if ( this.vmInstance == null ) {
      if ( other.vmInstance != null ) {
        return false;
      }
    } else if ( !this.vmInstance.equals( other.vmInstance ) ) {
      return false;
    }
    return true;
  }

  /**
   * Instance matching criterion.
   */
  public static Criterion inState( final Set<CreateImageState> states ) {
    return Restrictions.in(
        "runtimeState.createImageTask.state",
        Lists.newArrayList( Iterables.transform( states, Functions.toStringFunction( ) ) ) );
  }

  public static Set<CreateImageState> idleStates( ) {
    return EnumSet.of( CreateImageState.complete, CreateImageState.failed, CreateImageState.none );
  }
}
