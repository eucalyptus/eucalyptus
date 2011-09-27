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
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import org.hibernate.annotations.Parent;
import com.google.common.base.Function;

@Embeddable
public class VmBundleTask {
  public enum BundleState {
    none( "none" ), pending( null ), storing( "bundling" ), canceling( null ), complete( "succeeded" ), failed( "failed" );
    private String mappedState;
    
    BundleState( final String mappedState ) {
      this.mappedState = mappedState;
    }
    
    public String getMappedState( ) {
      return this.mappedState;
    }
    
    public static Function<String, BundleState> mapper = new Function<String, BundleState>( ) {
                                                         
                                                         @Override
                                                         public BundleState apply( final String input ) {
                                                           for ( final BundleState s : BundleState.values( ) ) {
                                                             if ( ( s.getMappedState( ) != null ) && s.getMappedState( ).equals( input ) ) {
                                                               return s;
                                                             }
                                                           }
                                                           return none;
                                                         }
                                                       };
  }
  
  @Parent
  private VmInstance  vmInstance;
  @Enumerated( EnumType.STRING )
  @Column( name = "metadata_vm_bundle_state" )
  private BundleState state;
  @Column( name = "metadata_vm_bundle_start_time" )
  private Date        startTime;
  @Column( name = "metadata_vm_bundle_update_time" )
  private Date        updateTime;
  @Column( name = "metadata_vm_bundle_progress" )
  private Integer     progress;
  @Column( name = "metadata_vm_bundle_bucket" )
  private String      bucket;
  @Column( name = "metadata_vm_bundle_prefix" )
  private String      prefix;
  @Column( name = "metadata_vm_bundle_policy" )
  private String      policy;
  @Column( name = "metadata_vm_bundle_error_msg" )
  private String      errorMessage;
  @Column( name = "metadata_vm_bundle_error_code" )
  private String      errorCode;
  
  VmBundleTask( ) {
    super( );
  }
  
  VmBundleTask( final VmInstance vmInstance, final String state, final Date startTime, final Date updateTime, final Integer progress, final String bucket,
                final String prefix,
                final String errorMessage, final String errorCode ) {
    super( );
    this.vmInstance = vmInstance;
    this.state = BundleState.mapper.apply( state );
    this.startTime = startTime;
    this.updateTime = updateTime;
    this.progress = progress;
    this.bucket = bucket;
    this.prefix = prefix;
    this.errorMessage = errorMessage;
    this.errorCode = errorCode;
  }
  
  private VmBundleTask( VmInstance vm, String bucket, String prefix, String policy ) {
    super( );
    this.vmInstance = vm;
    this.state = BundleState.pending;
    this.startTime = new Date( );
    this.updateTime = new Date( );
    this.progress = 0;
    this.bucket = bucket;
    this.prefix = prefix;
    this.policy = policy;
    this.errorCode = null;
    this.errorMessage = null;
  }
  
  static VmBundleTask create( VmInstance vm, String bucket, String prefix, String policy ) {
    return new VmBundleTask( vm, bucket, prefix, policy );
  }
  
  public static Function<BundleTask, VmBundleTask> fromBundleTask( final VmInstance vm ) {
    return new Function<BundleTask, VmBundleTask>( ) {
      
      @Override
      public VmBundleTask apply( final BundleTask input ) {
        return new VmBundleTask( vm,
                                 input.getState( ),
                                 input.getStartTime( ),
                                 input.getUpdateTime( ),
                                 input.getProgress( ) != null
                                   ? Integer.parseInt( input.getProgress( ).replace( "%", "" ) )
                                   : 0,
                                 input.getBucket( ),
                                 input.getPrefix( ),
                                 input.getErrorMessage( ),
                                 input.getErrorCode( ) );
      }
    };
  }
  
  public static Function<VmBundleTask, BundleTask> asBundleTask( ) {
    return new Function<VmBundleTask, BundleTask>( ) {
      
      @Override
      public BundleTask apply( final VmBundleTask input ) {
        return new BundleTask( input.getVmInstance( ).getInstanceId( ),//GRZE: this constructor reference is crap.
                               input.getBundleId( ),
                               input.getState( ).name( ),
                               input.getStartTime( ),
                               input.getUpdateTime( ),
                               "" + input.getProgress( ),
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
  
  private void setVmInstance( final VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
  }
  
  String getBundleId( ) {
    return this.vmInstance.getInstanceId( ).replaceFirst( "i-", "bun-" );
  }
  
  BundleState getState( ) {
    return this.state;
  }
  
  void setState( final BundleState state ) {
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
  
  private Integer getProgress( ) {
    return this.progress;
  }
  
  private void setProgress( final Integer progress ) {
    this.progress = progress;
  }
  
  private String getBucket( ) {
    return this.bucket;
  }
  
  private void setBucket( final String bucket ) {
    this.bucket = bucket;
  }
  
  private String getPrefix( ) {
    return this.prefix;
  }
  
  private void setPrefix( final String prefix ) {
    this.prefix = prefix;
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
    return this.vmInstance.hashCode( );
  }
  
  @Override
  public boolean equals( final Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( this.getClass( ) != obj.getClass( ) ) {
      return false;
    }
    final VmBundleTask other = ( VmBundleTask ) obj;
    return this.getVmInstance( ).equals( other.getVmInstance( ) );
  }
  
  @Override
  public String toString( ) {
    final StringBuilder builder = new StringBuilder( );
    builder.append( "VmBundleTask " );
    if ( this.vmInstance != null ) builder.append( this.vmInstance.getInstanceId( ) )
                                          .append( " " )
                                          .append( "bundleId=" )
                                          .append( this.getBundleId( ) )
                                          .append( ":" );
    if ( this.state != null ) builder.append( "state=" ).append( this.state ).append( ":" );
    if ( this.progress != null ) builder.append( "progress=" ).append( this.progress );
    return builder.toString( );
  }
  
}
