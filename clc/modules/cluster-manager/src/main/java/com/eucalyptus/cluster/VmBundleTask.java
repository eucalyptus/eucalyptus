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
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.hibernate.annotations.Parent;
import com.eucalyptus.vm.BundleTask;
import com.google.common.base.Function;

@Embeddable
public class VmBundleTask {
  @Parent
  private VmInstance vmInstance;
  @Column( name = "metadata_vm_bundle_id" )
  private String     bundleId;
  @Column( name = "metadata_vm_bundle_state" )
  private String     state;
  @Column( name = "metadata_vm_bundle_start_time" )
  private Date       startTime;
  @Column( name = "metadata_vm_bundle_update_time" )
  private Date       updateTime;
  @Column( name = "metadata_vm_bundle_progress" )
  private String     progress;
  @Column( name = "metadata_vm_bundle_bucket" )
  private String     bucket;
  @Column( name = "metadata_vm_bundle_prefix" )
  private String     prefix;
  @Column( name = "metadata_vm_bundle_error_msg" )
  private String     errorMessage;
  @Column( name = "metadata_vm_bundle_error_code" )
  private String     errorCode;
  
  VmBundleTask( ) {
    super( );
  }
  
  VmBundleTask( VmInstance vmInstance, String bundleId, String state, Date startTime, Date updateTime, String progress, String bucket, String prefix,
                String errorMessage, String errorCode ) {
    super( );
    this.vmInstance = vmInstance;
    this.bundleId = bundleId;
    this.state = state;
    this.startTime = startTime;
    this.updateTime = updateTime;
    this.progress = progress;
    this.bucket = bucket;
    this.prefix = prefix;
    this.errorMessage = errorMessage;
    this.errorCode = errorCode;
  }
  
  public static Function<BundleTask, VmBundleTask> fromBundleTask( final VmInstance vm ) {
    return new Function<BundleTask, VmBundleTask>( ) {
      
      @Override
      public VmBundleTask apply( BundleTask input ) {
        return new VmBundleTask( vm,
                                 input.getBundleId( ),
                                 input.getState( ),
                                 input.getStartTime( ),
                                 input.getUpdateTime( ),
                                 input.getProgress( ),
                                 input.getBucket( ),
                                 input.getPrefix( ),
                                 input.getErrorMessage( ),
                                 input.getErrorCode( ) );
      }
    };
  }
  
  public static Function<VmBundleTask, BundleTask> asBundleTask( final VmInstance vm ) {
    return new Function<VmBundleTask, BundleTask>( ) {
      
      @Override
      public BundleTask apply( VmBundleTask input ) {
        return new BundleTask( vm.getInstanceId( ),
                               input.getBundleId( ),
                               input.getState( ),
                               input.getStartTime( ),
                               input.getUpdateTime( ),
                               input.getProgress( ),
                               input.getBucket( ),
                               input.getPrefix( ),
                               input.getErrorMessage( ),
                               input.getErrorCode( ) );
      }
    };
  }
  
  private VmInstance getVmInstance( ) {
    return this.vmInstance;
  }
  
  private void setVmInstance( VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
  }
  
  String getBundleId( ) {
    return this.bundleId;
  }
  
  private void setBundleId( String bundleId ) {
    this.bundleId = bundleId;
  }
  
  String getState( ) {
    return this.state;
  }
  
  void setState( String state ) {
    this.state = state;
  }
  
  private Date getStartTime( ) {
    return this.startTime;
  }
  
  private void setStartTime( Date startTime ) {
    this.startTime = startTime;
  }
  
  private Date getUpdateTime( ) {
    return this.updateTime;
  }
  
  void setUpdateTime( Date updateTime ) {
    this.updateTime = updateTime;
  }
  
  private String getProgress( ) {
    return this.progress;
  }
  
  private void setProgress( String progress ) {
    this.progress = progress;
  }
  
  private String getBucket( ) {
    return this.bucket;
  }
  
  private void setBucket( String bucket ) {
    this.bucket = bucket;
  }
  
  private String getPrefix( ) {
    return this.prefix;
  }
  
  private void setPrefix( String prefix ) {
    this.prefix = prefix;
  }
  
  private String getErrorMessage( ) {
    return this.errorMessage;
  }
  
  private void setErrorMessage( String errorMessage ) {
    this.errorMessage = errorMessage;
  }
  
  private String getErrorCode( ) {
    return this.errorCode;
  }
  
  private void setErrorCode( String errorCode ) {
    this.errorCode = errorCode;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.bundleId == null )
      ? 0
      : this.bundleId.hashCode( ) );
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
    VmBundleTask other = ( VmBundleTask ) obj;
    if ( this.bundleId == null ) {
      if ( other.bundleId != null ) {
        return false;
      }
    } else if ( !this.bundleId.equals( other.bundleId ) ) {
      return false;
    }
    return true;
  }
  
}
