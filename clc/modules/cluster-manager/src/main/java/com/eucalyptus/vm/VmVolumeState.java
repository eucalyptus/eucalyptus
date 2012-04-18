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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.vm;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Parent;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.vm.VmVolumeAttachment.AttachmentState;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@Embeddable
public class VmVolumeState {
  @Transient
  private static Logger                 LOG         = Logger.getLogger( VmVolumeState.class );
  @Parent
  private VmInstance                    vmInstance;
  @ElementCollection
  @CollectionTable( name = "metadata_instances_volume_attachments" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private final Set<VmVolumeAttachment> attachments = Sets.newHashSet( );
  
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
  
  public void addVolumeAttachment( final VmVolumeAttachment volume ) {
    final String volumeId = volume.getVolumeId( );
    volume.setStatus( AttachmentState.attaching.name( ) );
    volume.setAttachTime( volume.getAttachTime( ) != null ? volume.getAttachTime( ) : new Date( ) );
    volume.setInstanceId( this.getVmInstance( ).getInstanceId( ) );
    if ( !this.attachments.add( volume ) ) {
      Exceptions.trace( "Failed to add volume to attachment set: " + volume );
    }
  }
  
  enum VmVolumeAttachmentName implements Function<VmVolumeAttachment, String> {
    INSTANCE;
    public String apply( final VmVolumeAttachment input ) {
      return input.getVolumeId( );
    }
  }
  
  enum VmVolumeAttachmentStateInfo implements Function<VmVolumeAttachment, String> {
    INSTANCE;
    public String apply( final VmVolumeAttachment input ) {
      return input.getVolumeId( ) + ":" + input.getAttachmentState( ).stateFlag( );
    }
  }
  
  public void updateVolumeAttachments( final List<VmVolumeAttachment> ncAttachedVols ) throws NoSuchElementException {
    Set<String> remoteVolumes = Sets.newHashSet( Collections2.transform( ncAttachedVols, VmVolumeAttachmentName.INSTANCE ) );
    Set<String> localVolumes = Sets.newHashSet( Collections2.transform( this.getAttachments( ), VmVolumeAttachmentName.INSTANCE ) );
    Set<String> intersection = Sets.intersection( remoteVolumes, localVolumes );
    Set<String> remoteOnly = Sets.difference( remoteVolumes, localVolumes );
    Set<String> localOnly = Sets.difference( localVolumes, remoteVolumes );
    if ( !intersection.isEmpty( ) || !remoteOnly.isEmpty( ) || !localOnly.isEmpty( ) ) {
      LOG.debug( "Updating volume attachments for: " + this.getVmInstance( ).getInstanceId( )
                 + " intersection=" + intersection
                 + " local=" + localOnly
                 + " remote=" + remoteOnly );
      LOG.debug( "Reported state for: " + this.getVmInstance( ).getInstanceId( )
                 + Collections2.transform( ncAttachedVols, VmVolumeAttachmentStateInfo.INSTANCE ) );
    }
    final Map<String, VmVolumeAttachment> ncAttachedVolMap = new HashMap<String, VmVolumeAttachment>( ) {
      
      {
        for ( final VmVolumeAttachment v : ncAttachedVols ) {
          this.put( v.getVolumeId( ), v );
        }
      }
    };
    for ( String volId : intersection ) {
      try {
        VmVolumeAttachment ncVolumeAttachment = ncAttachedVolMap.get( volId );
        VmVolumeAttachment localVolumeAttachment = this.lookupVolumeAttachment( volId );
        final AttachmentState localState = localVolumeAttachment.getAttachmentState( );
        final AttachmentState remoteState = AttachmentState.parse( ncVolumeAttachment.getStatus( ) );
        if ( !localState.isVolatile( ) ) {
          if ( AttachmentState.detached.equals( remoteState ) ) {
            this.removeVolumeAttachment( volId );
          } else if ( AttachmentState.attaching_failed.equals( remoteState ) ) {
            this.removeVolumeAttachment( volId );
          } else if ( AttachmentState.detaching_failed.equals( remoteState ) && !AttachmentState.attached.equals( localState ) ) {
            this.updateVolumeAttachment( volId, AttachmentState.attached );
          } else if ( AttachmentState.attached.equals( remoteState ) && !AttachmentState.attached.equals( localState ) ) {
            this.updateVolumeAttachment( volId, AttachmentState.attached );
          }
        } else {
          if ( AttachmentState.detaching.equals( localState ) && AttachmentState.detached.equals( remoteState ) ) {
            this.removeVolumeAttachment( volId );
          } else if ( AttachmentState.attaching.equals( localState ) && AttachmentState.attached.equals( remoteState ) ) {
            this.updateVolumeAttachment( volId, AttachmentState.attached );
          } else if ( AttachmentState.attaching.equals( localState ) && AttachmentState.attaching_failed.equals( remoteState ) ) {
            this.removeVolumeAttachment( volId );
          } else if ( AttachmentState.detaching.equals( localState ) && AttachmentState.detaching_failed.equals( remoteState ) ) {
            this.updateVolumeAttachment( volId, AttachmentState.attached );
          }
        }
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
    }
    for ( String volId : remoteOnly ) {
      try {
        VmVolumeAttachment ncVolumeAttachment = ncAttachedVolMap.get( volId );
        final AttachmentState remoteState = AttachmentState.parse( ncVolumeAttachment.getStatus( ) );
        if ( AttachmentState.attached.equals( remoteState ) || AttachmentState.detaching_failed.equals( remoteState ) ) {
          LOG.warn( "Restoring volume attachment state for " + this.getVmInstance( ).getInstanceId( ) + " with " + ncVolumeAttachment.toString( ) );
          this.addVolumeAttachment( ncVolumeAttachment );
        }
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
    }
    for ( String volId : localOnly ) {
      try {
        final AttachmentState localState = this.lookupVolumeAttachment( volId ).getAttachmentState( );
        if ( !localState.isVolatile( ) ) {

        }
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
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
    if ( this.attachments != null ) builder.append( "attachments=" ).append( this.attachments );
    return builder.toString( );
  }
  
  Set<VmVolumeAttachment> getAttachments( ) {
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
