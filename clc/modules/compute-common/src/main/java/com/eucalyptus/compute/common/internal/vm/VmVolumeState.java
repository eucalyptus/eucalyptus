/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
import java.util.NoSuchElementException;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Parent;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment.AttachmentState;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@Embeddable
public class VmVolumeState {
  @Transient
  private static Logger                 LOG         = Logger.getLogger( VmVolumeState.class );
  @Parent
  private VmInstance                    vmInstance;
  @OneToMany( mappedBy = "vmInstance", orphanRemoval = true, cascade = CascadeType.ALL )
  private final Set<VmStandardVolumeAttachment> attachments = Sets.newHashSet( );
  
  VmVolumeState( ) {
    super( );
    this.vmInstance = null;
  }
  
  VmVolumeState( VmInstance vmInstance ) {
    super( );
    this.vmInstance = vmInstance;
  }
  
  private VmVolumeAttachment resolveVolumeId( final String volumeId ) throws NoSuchElementException {
    final VmVolumeAttachment v = Iterables.find( this.attachments, VmVolumeAttachment.volumeIdFilter( volumeId ) );
    if ( v == null ) {
      throw new NoSuchElementException( "Failed to find volume attachment for instance " + this.getVmInstance( ).getInstanceId( ) + " and volume " + volumeId );
    } else {
      return v;
    }
  }
  
  public VmVolumeAttachment removeVolumeAttachment( final String volumeId ) throws NoSuchElementException {
    final VmVolumeAttachment v = Iterables.find( this.attachments, VmVolumeAttachment.volumeIdFilter( volumeId ) );
    if ( v == null ) {
      throw new NoSuchElementException( "Failed to find volume attachment for instance " + this.getVmInstance( ).getInstanceId( ) + " and volume " + volumeId );
    } else {
      this.attachments.remove( v );
      return v;
    }
  }
  
  public void updateVolumeAttachment( final String volumeId, final AttachmentState state ) throws NoSuchElementException {
    final VmVolumeAttachment v = this.resolveVolumeId( volumeId );
    v.setStatus( state.name( ) );
  }
  
  public VmVolumeAttachment lookupVolumeAttachment( final String volumeId ) throws NoSuchElementException {
    return this.resolveVolumeId( volumeId );
  }
  
  public <T> Iterable<T> transformVolumeAttachments( final Function<? super VmVolumeAttachment, T> function ) throws NoSuchElementException {
    return Iterables.transform( this.attachments, function );
  }
  
  public boolean eachVolumeAttachment( final Predicate<VmVolumeAttachment> pred ) throws NoSuchElementException {
    return Iterables.all( this.attachments, pred );
  }
  
  public void addVolumeAttachment( final VmStandardVolumeAttachment volume ) {
    final String volumeId = volume.getVolumeId( );
    volume.setStatus( AttachmentState.attaching.name( ) );
    volume.setAttachTime( volume.getAttachTime( ) != null ? volume.getAttachTime( ) : new Date( ) );
    volume.setInstanceId( this.getVmInstance( ).getInstanceId( ) );
    if ( !this.attachments.add( volume ) ) {
      Exceptions.trace( "Failed to add volume to attachment set: " + volume );
    }
  }
  
  public enum VmVolumeAttachmentName implements Function<VmVolumeAttachment, String> {
    INSTANCE;
    public String apply( final VmVolumeAttachment input ) {
      return input.getVolumeId( );
    }
  }
  
  public enum VmVolumeAttachmentStateInfo implements Function<VmVolumeAttachment, String> {
    INSTANCE;
    public String apply( final VmVolumeAttachment input ) {
      return input.getVolumeId( ) + ":" + input.getAttachmentState( ).stateFlag( );
    }
  }
  
  private VmInstance getVmInstance( ) {
    return this.vmInstance;
  }
  
  public VmVolumeAttachment lookupVolumeAttachmentByDevice( String volumeDevice ) {
    return Iterables.find( this.attachments, VmVolumeAttachment.volumeDeviceFilter( volumeDevice ) );
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "VmVolumeState:" );
    if ( Entities.isReadable( this.attachments ) ) builder.append( "attachments=" ).append( this.attachments );
    return builder.toString( );
  }
  
  public Set<VmStandardVolumeAttachment> getAttachments( ) {
    return this.attachments;
  }
  
  private void setVmInstance( VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
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
    VmVolumeState other = ( VmVolumeState ) obj;
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
   * @return
   */
  public boolean isEmpty( ) {
    return this.attachments.isEmpty( );
  }
  
}
