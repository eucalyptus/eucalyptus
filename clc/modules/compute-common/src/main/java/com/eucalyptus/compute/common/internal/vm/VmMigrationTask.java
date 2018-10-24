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
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Parent;
import com.google.common.base.Strings;

/**
 * @todo doc
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
@Embeddable
public class VmMigrationTask {
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    if ( this.state != null )
      builder.append( this.state );
    if ( !Strings.isNullOrEmpty(this.sourceHost) && !Strings.isNullOrEmpty(this.destinationHost) )
      builder.append( " " ).append( this.sourceHost ).append( "->" ).append( this.destinationHost );
    return builder.toString( );
  }

  private static Logger  LOG = Logger.getLogger( VmMigrationTask.class );
  
  @Parent
  private VmInstance     vmInstance;
  
  /**
   * Most recently reported migration state.
   * 
   * @see {@link MigrationState}
   */
  @Enumerated( EnumType.STRING )
  @Column( name = "metadata_vm_migration_state" )
  private MigrationState state;
  
  /**
   * Source host for the migration as it is registered.
   */
  @Column( name = "metadata_vm_migration_source_host" )
  private String         sourceHost;
  
  /**
   * Destination host for the migration as it is registered.
   */
  @Column( name = "metadata_vm_migration_dest_host" )
  private String         destinationHost;
  
  /**
   * The purpose of this timestamp is to serve as a periodic trigger for state propagation; viz. to
   * tags (unlike the AbstractPersistent timestamps). In
   * {@code #updateMigrationTask(String, String, String)} this timer is used to determine when a
   * false-positive indicate that state needs to be refreshed.
   * 
   * @see {@link #updateMigrationTask(String, String, String)}
   */
  @Temporal( TemporalType.TIMESTAMP )
  @Column( name = "metadata_vm_migration_state_timer" )
  private Date           refreshTimer;
  
  private VmMigrationTask( ) {}
  
  private VmMigrationTask( VmInstance vmInstance, MigrationState state, String sourceHost, String destinationHost ) {
    this.vmInstance = vmInstance;
    this.state = state;
    this.sourceHost = Strings.nullToEmpty( sourceHost );
    this.destinationHost = Strings.nullToEmpty( destinationHost );
    this.refreshTimer = new Date( );
  }
  
  private VmMigrationTask( VmInstance vmInstance, String state, String sourceHost, String destinationHost ) {
    this( vmInstance, MigrationState.defaultValueOf( state ), sourceHost, destinationHost );
  }
  
  public static VmMigrationTask create( VmInstance vmInstance ) {
    return new VmMigrationTask( vmInstance, MigrationState.none, null, null );
  }
  
  public static VmMigrationTask create( VmInstance vmInstance, String state, String sourceHost, String destinationHost ) {
    return new VmMigrationTask( vmInstance, state, sourceHost, destinationHost );
  }
  
  public Date getRefreshTimer( ) {
    return this.refreshTimer == null ? this.refreshTimer = new Date( ) : this.refreshTimer;
  }

  public void setRefreshTimer( Date refreshTimer ) {
    this.refreshTimer = refreshTimer;
  }

  public void updateRefreshTimer( ) {
    this.setRefreshTimer( new Date( ) );
  }

  protected VmInstance getVmInstance( ) {
    return this.vmInstance;
  }
  
  protected void setVmInstance( VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
  }
  
  public MigrationState getState( ) {
    return this.state;
  }

  public void setState( MigrationState state ) {
    if ( !this.state.equals( state ) ) {
      this.updateRefreshTimer( );
      this.state = state;
    }
  }
  
  public String getSourceHost( ) {
    return this.sourceHost;
  }

  public void setSourceHost( String sourceHost ) {
    this.sourceHost = Strings.nullToEmpty( sourceHost );
  }
  
  public String getDestinationHost( ) {
    return this.destinationHost;
  }

  public void setDestinationHost( String destinationHost ) {
    this.destinationHost = Strings.nullToEmpty( destinationHost );
  }
}
