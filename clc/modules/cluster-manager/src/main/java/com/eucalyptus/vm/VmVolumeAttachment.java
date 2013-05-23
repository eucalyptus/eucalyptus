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

import java.util.Date;
import java.util.NoSuchElementException;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;

import org.hibernate.annotations.Parent;
import org.hibernate.annotations.Type;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import edu.ucsb.eucalyptus.msgs.AttachedVolume;

@Embeddable
public class VmVolumeAttachment implements Comparable<VmVolumeAttachment> {
  public enum AttachmentState {
    attaching {
      
      @Override
      public boolean isVolatile( ) {
        return true;
      }

      @Override
      public String stateFlag( ) {
        return "a";
      }
    },
    attached {
      @Override
      public String stateFlag( ) {
        return "A";
      }
    },
    detaching {
      @Override
      public boolean isVolatile( ) {
        return true;
      }

      @Override
      public String stateFlag( ) {
        return "d";
      }
    },
    detached {
      @Override
      public String stateFlag( ) {
        return "D";
      }
    },
    detaching_failed {
      
      @Override
      public boolean isFailed( ) {
        return true;
      }

      @Override
      public String stateFlag( ) {
        return "df";
      }
      
    },
    attaching_failed {
      @Override
      public boolean isFailed( ) {
        return true;
      }

      @Override
      public String stateFlag( ) {
        return "af";
      }
      
    };
    public static AttachmentState parse( String stateName ) {
      if ( stateName != null && stateName.indexOf( " " ) != -1 ) {
        stateName = stateName.replace( " ", "_" );
      } else if ( stateName == null ) {
        return AttachmentState.detached;
      }
      return AttachmentState.valueOf( stateName );
    }
    
    public boolean isVolatile( ) {
      return false;
    }
    
    public boolean isFailed( ) {
      return false;
    }
    
    public abstract String stateFlag( );
  }
  
  @Parent
  private VmInstance vmInstance;
  @Column( name = "metadata_vm_volume_id", unique = true )
  private String	volumeId;
  @Column( name = "metadata_vm_volume_device" )
  private String	device;
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  @Column( name = "metadata_vm_volume_remote_device", columnDefinition = "TEXT default ''"  )
  private String	remoteDevice;
  @Column( name = "metadata_vm_volume_status" )
  private String	status;
  @Column( name = "metadata_vm_volume_attach_time" )
  private Date		attachTime;
  @Column( name = "metadata_vm_vol_delete_on_terminate" )
  private Boolean	deleteOnTerminate;
  @Column( name = "metadata_vm_volume_is_root_device", columnDefinition = "boolean default false" )
  private Boolean	isRootDevice;
  
  //  @OneToOne
//  @JoinTable( name = "metadata_vm_has_volume", joinColumns = { @JoinColumn( name = "metadata_vm_id" ) }, inverseJoinColumns = { @JoinColumn( name = "metadata_volume_id" ) } )
//  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
//  private Volume     volume;
  
  VmVolumeAttachment( ) {
    super( );
  }
  
  VmVolumeAttachment( VmInstance vmInstance, String volumeId, String device, String remoteDevice, String status, Date attachTime,
                              Boolean deleteOnTerminate ) {
    this( vmInstance, volumeId, device, remoteDevice, status, attachTime, deleteOnTerminate, Boolean.FALSE );
  }
  
  public VmVolumeAttachment( VmInstance vmInstance, String volumeId, String device, String remoteDevice, String status, Date attachTime ) {
    this( vmInstance, volumeId, device, remoteDevice, status, attachTime, Boolean.TRUE );
  }
  
  public VmVolumeAttachment(VmInstance vmInstance, String volumeId, String device, String remoteDevice, String status, Date attachTime,
			Boolean deleteOnTerminate, Boolean rootDevice) {
    super();
	this.vmInstance = vmInstance;
	this.volumeId = volumeId;
	this.device = device;
	this.remoteDevice = remoteDevice;
	this.status = status;
	this.attachTime = attachTime;
	this.deleteOnTerminate = deleteOnTerminate;
	this.isRootDevice = rootDevice;
  }
  
  public static Function<AttachedVolume, VmVolumeAttachment> fromAttachedVolume( final VmInstance vm ) {
    return new Function<AttachedVolume, VmVolumeAttachment>( ) {
      @Override
      public VmVolumeAttachment apply( AttachedVolume vol ) {
        return new VmVolumeAttachment( vm, vol.getVolumeId( ), vol.getDevice( ), vol.getRemoteDevice( ), vol.getStatus( ), vol.getAttachTime( ), false );
      }
    };
  }
  
  public static Function<AttachedVolume, VmVolumeAttachment> fromTransientAttachedVolume( final VmInstance vm ) {
    return new Function<AttachedVolume, VmVolumeAttachment>( ) {
      @Override
      public VmVolumeAttachment apply( AttachedVolume vol ) {
        return new VmVolumeAttachment( vm, vol.getVolumeId( ), vol.getDevice( ), vol.getRemoteDevice( ), vol.getStatus( ), vol.getAttachTime( ), false );
      }
    };
  }
  
  public static Function<VmVolumeAttachment, AttachedVolume> asAttachedVolume( final VmInstance vm ) {
    return new Function<VmVolumeAttachment, AttachedVolume>( ) {
      @Override
      public AttachedVolume apply( VmVolumeAttachment vol ) {
        AttachedVolume attachment = null;
        if ( vm == null && vol.getVmInstance( ) == null ) {
          throw new NoSuchElementException( "Failed to transform volume attachment because it no longer exists: " + vol );
        } else if ( vm == null ) {
          attachment = new AttachedVolume( vol.getVolumeId( ), vol.getVmInstance( ).getInstanceId( ), vol.getDevice( ), vol.getRemoteDevice( ) );
        } else {
          attachment = new AttachedVolume( vol.getVolumeId( ), vm.getInstanceId( ), vol.getDevice( ), vol.getRemoteDevice( ) );
        }
        attachment.setAttachTime( vol.getAttachTime( ) );
        attachment.setStatus( vol.getStatus( ) );
        if ( !attachment.getDevice( ).replaceAll( "unknown,requested:", "" ).startsWith( "/dev/" ) ) {
          attachment.setDevice( "/dev/" + attachment.getDevice( ).replaceAll( "unknown,requested:", "" ) );
        }
        return attachment;
      }
    };
  }
  
//  Volume getVolume( ) {
//    return this.volume;
//  }
  
  public VmInstance getVmInstance( ) {
    return this.vmInstance;
  }
  
  public String getVolumeId( ) {
    return this.volumeId;
  }
  
  void setVolumeId( String volumeId ) {
    this.volumeId = volumeId;
  }
  
  public String getDevice( ) {
    return this.device;
  }
  
  void setDevice( String device ) {
    this.device = device;
  }
  
  public String getRemoteDevice( ) {
    return this.remoteDevice;
  }
  
  public void setRemoteDevice( String remoteDevice ) {
    this.remoteDevice = remoteDevice;
  }
  
  public AttachmentState getAttachmentState( ) {
    return AttachmentState.parse( this.status );
  }
  
  public String getStatus( ) {
    return this.status;
  }
  
  public void setStatus( String status ) {
    this.status = status;
  }
  
  public Date getAttachTime( ) {
    return this.attachTime;
  }
  
  void setAttachTime( Date attachTime ) {
    this.attachTime = attachTime;
  }
  
  public Boolean getDeleteOnTerminate( ) {
    return this.deleteOnTerminate;
  }
  
  void setDeleteOnTerminate( Boolean value ) {
    this.deleteOnTerminate = value;
  }
  
  public int compareTo( VmVolumeAttachment that ) {
    return this.volumeId.compareTo( that.getVolumeId( ) );
  }
  
  /**
   * @param instanceId
   */
  public void setInstanceId( String instanceId ) {}
  
  void setVmInstance( VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
  }
  
  public Boolean getIsRootDevice() {
	return isRootDevice;
  }
	
  public void setIsRootDevice(Boolean isRootDevice) {
	this.isRootDevice = isRootDevice;
  }

@Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "VmVolumeAttachment:" );
    if ( this.volumeId != null ) builder.append( "volumeId=" ).append( this.volumeId ).append( ":" );
    if ( this.device != null ) builder.append( "device=" ).append( this.device ).append( ":" );
    if ( this.remoteDevice != null ) builder.append( "remoteDevice=" ).append( this.remoteDevice ).append( ":" );
    if ( this.status != null ) builder.append( "status=" ).append( this.status ).append( ":" );
    if ( this.attachTime != null ) builder.append( "attachTime=" ).append( this.attachTime );
    return builder.toString( );
  }
  
  public static VmVolumeAttachment exampleWithVolumeId( final String volumeId ) {
    VmVolumeAttachment ex = new VmVolumeAttachment( );
    ex.setVolumeId( volumeId );
    return ex;
  }
  
  static Predicate<VmVolumeAttachment> volumeDeviceFilter( final String deviceName ) {
    return new Predicate<VmVolumeAttachment>( ) {
      @Override
      public boolean apply( VmVolumeAttachment input ) {
        return input.getDevice( ).replaceAll( "unknown,requested:", "" ).equals( deviceName );
      }
    };
  }
  
  static Predicate<VmVolumeAttachment> volumeIdFilter( final String volumeId ) {
    return new Predicate<VmVolumeAttachment>( ) {
      @Override
      public boolean apply( VmVolumeAttachment input ) {
        return input.getVolumeId( ).equals( volumeId );
      }
    };
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.volumeId == null ) ? 0 : this.volumeId.hashCode( ) );
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
    VmVolumeAttachment other = ( VmVolumeAttachment ) obj;
    if ( this.volumeId == null ) {
      if ( other.volumeId != null ) {
        return false;
      }
    } else if ( !this.volumeId.equals( other.volumeId ) ) {
      return false;
    }
    return true;
  }
  
  public static class NonTransientVolumeException extends NoSuchElementException {

	private static final long serialVersionUID = 1L;

	public NonTransientVolumeException(String s) {
		super(s);
	}
  }
}
