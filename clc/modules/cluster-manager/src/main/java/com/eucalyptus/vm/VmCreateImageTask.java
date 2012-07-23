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
 ************************************************************************/

package com.eucalyptus.vm;

import java.util.Date;
import javax.persistence.Column;
import org.hibernate.annotations.Parent;

public class VmCreateImageTask {
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
  
  VmCreateImageTask( ) {
    super( );
  }
  
  VmCreateImageTask( final VmInstance vmInstance, final String state, final Date startTime, final Date updateTime,
                     final String progress, final String errorMessage,
                     final String errorCode ) {
    super( );
    this.vmInstance = vmInstance;
    this.state = state;
    this.startTime = startTime;
    this.updateTime = updateTime;
    this.progress = progress;
    this.errorMessage = errorMessage;
    this.errorCode = errorCode;
  }
  
  private VmInstance getVmInstance( ) {
    return this.vmInstance;
  }
  
  private void setVmInstance( final VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
  }
  
  String getState( ) {
    return this.state;
  }
  
  void setState( final String state ) {
    this.state = state;
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
  
}
