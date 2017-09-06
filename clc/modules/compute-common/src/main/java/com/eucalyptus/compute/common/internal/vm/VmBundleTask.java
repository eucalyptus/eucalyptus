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
import java.util.concurrent.Callable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Parent;
import org.hibernate.annotations.Type;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.util.Strings;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import groovy.sql.Sql;
import groovy.transform.CompileStatic;
import groovy.transform.TypeCheckingMode;

@Embeddable
public class VmBundleTask {
  public enum BundleState {
    none( "none" ), pending( null ), storing( "bundling" ), canceling( null ), cancelled( "cancelled" ), complete( "succeeded" ), failed( "failed" );
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
  @Type(type="text")
  private String      policy;
  @Column( name = "metadata_vm_bundle_error_msg" )
  private String      errorMessage;
  @Column( name = "metadata_vm_bundle_error_code" )
  private String      errorCode;
  
  VmBundleTask( ) {
    super( );
  }
  
  public VmBundleTask( final VmInstance vmInstance, final String state, final Date startTime, final Date updateTime,
                       final Integer progress, final String bucket, final String prefix, final String errorMessage,
                       final String errorCode ) {
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
  
  private VmBundleTask(VmInstance vmInstance, BundleState state,
      Date startTime, Date updateTime, Integer progress, String bucket,
      String prefix, String errorMessage, String errorCode) {
    super( );
    this.vmInstance = vmInstance;
    this.state = state;
    this.startTime = startTime;
    this.updateTime = updateTime;
    this.progress = progress;
    this.bucket = bucket;
    this.prefix = prefix;
    this.errorMessage = errorMessage;
    this.errorCode = errorCode;
  }

  public static VmBundleTask create( VmInstance vm, String bucket, String prefix, String policy ) {
    return new VmBundleTask( vm, bucket, prefix, policy );
  }
  
  public static VmBundleTask copyOf(VmBundleTask other) {
    if (other == null) return null;
    return new VmBundleTask(other.vmInstance, other.state, other.startTime, other.updateTime, other.progress, other.bucket, other.prefix, other.errorMessage, other.errorCode);
  }
  
  public VmInstance getVmInstance( ) {
    return this.vmInstance;
  }
  
  private void setVmInstance( final VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
  }

  public String getInstanceId( ) {
    return vmInstance.getInstanceId( );
  }

  public String getBundleId( ) {
    return Strings.truncate( getInstanceId( ), 10 ).replaceFirst( "i-", "bun-" );
  }
  
  public BundleState getState( ) {
    return this.state;
  }
  
  public void setState( final BundleState state ) {
    this.state = state;
  }

  public Date getStartTime( ) {
    return this.startTime;
  }
  
  void setStartTime( final Date startTime ) {
    this.startTime = startTime;
  }

  public Date getUpdateTime( ) {
    return this.updateTime;
  }

  public void setUpdateTime( final Date updateTime ) {
    this.updateTime = updateTime;
  }

  public Integer getProgress( ) {
    return this.progress;
  }

  public void setProgress( final Integer progress ) {
    this.progress = progress;
  }

  public String getBucket( ) {
    return this.bucket;
  }

  public void setBucket( final String bucket ) {
    this.bucket = bucket;
  }

  public String getPrefix( ) {
    return this.prefix;
  }

  public void setPrefix( final String prefix ) {
    this.prefix = prefix;
  }

  public String getErrorMessage( ) {
    return this.errorMessage;
  }

  public void setErrorMessage( final String errorMessage ) {
    this.errorMessage = errorMessage;
  }

  public String getErrorCode( ) {
    return this.errorCode;
  }

  public void setErrorCode( final String errorCode ) {
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
  
  public enum Filters implements Predicate<VmBundleTask> {
    BUNDLING {
      
      @Override
      public boolean apply( final VmBundleTask arg0 ) {
        return (arg0 != null) && (!BundleState.none.equals(arg0.getState()));
      }
    }
  }

  @Upgrades.PostUpgrade( value = Eucalyptus.class, since = Upgrades.Version.v4_1_0 )
  @CompileStatic( TypeCheckingMode.SKIP )
  public static class VmBundleTaskPostUpgrade410 implements Callable<Boolean> {
    private static Logger LOG = Logger.getLogger( VmBundleTaskPostUpgrade410.class );

    @Override
    public Boolean call( ) throws Exception {
     Sql sql = null;
     try {
       sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloud" );
       sql.execute( "ALTER TABLE metadata_instances ALTER COLUMN metadata_vm_bundle_policy TYPE text" );
       return true;
     } catch ( Exception ex ) {
       LOG.error( ex, ex );
       return false;
     } finally {
       if ( sql != null ) {
         sql.close( );
       }
     }
    }
  }
}
