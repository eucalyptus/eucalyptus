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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Parent;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@Embeddable
public class VmVolumeState {
  @Transient
  private static Logger    LOG = Logger.getLogger( VmVolumeState.class );
  @Parent
  private VmInstance vmInstance;
  @ElementCollection
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
    final VmVolumeAttachment v = Iterables.find( this.attachments, volumeIdFilter( volumeId ) );
    if ( v == null ) {
      throw new NoSuchElementException( "Failed to find volume attachment for instance " + this.getVmInstance( ).getInstanceId( ) + " and volume " + volumeId );
    } else {
      return v;
    }
  }
  
  private static Predicate<VmVolumeAttachment> volumeDeviceFilter( final String deviceName ) {
    return new Predicate<VmVolumeAttachment>( ) {
      @Override
      public boolean apply( VmVolumeAttachment input ) {
        return input.getDevice( ).replaceAll( "unknown,requested:", "" ).equals( deviceName );
      }
    };
  }
  
  private static Predicate<VmVolumeAttachment> volumeIdFilter( final String volumeId ) {
    return new Predicate<VmVolumeAttachment>( ) {
      @Override
      public boolean apply( VmVolumeAttachment input ) {
        return input.getVolumeId( ).equals( volumeId );
      }
    };
  }
  
  public VmVolumeAttachment removeVolumeAttachment( final String volumeId ) throws NoSuchElementException {
    final VmVolumeAttachment v = Iterables.find( this.attachments, volumeIdFilter( volumeId ) );
    if ( v == null ) {
      throw new NoSuchElementException( "Failed to find volume attachment for instance " + this.getVmInstance( ).getInstanceId( ) + " and volume " + volumeId );
    } else {
      this.attachments.remove( v );
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
  
  public <T> Iterable<T> transformVolumeAttachments( final Function<? super VmVolumeAttachment, T> function ) throws NoSuchElementException {
    return Iterables.transform( this.attachments, function );
  }
  
  public boolean eachVolumeAttachment( final Predicate<VmVolumeAttachment> pred ) throws NoSuchElementException {
    return Iterables.all( this.attachments, pred );
  }
  
  public void addVolumeAttachment( final VmVolumeAttachment volume ) {
    final String volumeId = volume.getVolumeId( );
    volume.setStatus( "attaching" );
    if ( !this.attachments.add( volume ) ) {
      Exceptions.trace( "Failed to add volume to attachment set: " + volume );
    }
  }
  
  public void updateVolumeAttachments( final List<VmVolumeAttachment> ncAttachedVols ) throws NoSuchElementException {
    final Map<String, VmVolumeAttachment> ncAttachedVolMap = new HashMap<String, VmVolumeAttachment>( ) {
      
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
          if ( "detached".equals( ncVol.getStatus( ) ) || "attaching failed".equals( ncVol.getStatus( ) ) ) {
            VmVolumeState.this.removeVolumeAttachment( volId );
          } else if ( "attaching".equals( arg0.getStatus( ) ) || "attached".equals( ncVol.getStatus( ) ) || "detach failed".equals( ncVol.getStatus( ) ) ) {
            VmVolumeState.this.updateVolumeAttachment( volId, arg0.getStatus( ) );
          }
        }
        ncAttachedVolMap.remove( volId );
        return true;
      }
    } );
    for ( final VmVolumeAttachment v : ncAttachedVolMap.values( ) ) {
      if( "attached".equals( v.getStatus( ) ) || "detach failed".equals( v.getStatus( ) ) ) {
        LOG.warn( "Restoring volume attachment state for " + this.getVmInstance( ).getInstanceId( ) + " with " + v.toString( ) );
        this.addVolumeAttachment( v );
      }
    }
  }
  
  private VmInstance getVmInstance( ) {
    return this.vmInstance;
  }
  
  public VmVolumeAttachment lookupVolumeAttachmentByDevice( String volumeDevice ) {
    return Iterables.find( this.attachments, this.volumeDeviceFilter( volumeDevice ) );
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
  
}
