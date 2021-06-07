/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;
import org.hibernate.id.UUIDHexGenerator;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import groovy.lang.Closure;
import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;

@MappedSuperclass
public abstract class VmVolumeAttachment extends AbstractPersistent implements Comparable<VmVolumeAttachment> {
  private static final long serialVersionUID = 1L;

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
  
  @ManyToOne( optional = false )
  @JoinColumn( name = "vminstance_id", nullable = false, updatable = false )
  private VmInstance vmInstance;
  @Column( name = "metadata_vm_volume_id", unique = true )
  private String	volumeId;
  @Column( name = "metadata_vm_volume_device" )
  private String	device;
  @Type(type="text")
  @Column( name = "metadata_vm_volume_remote_device", columnDefinition = "TEXT default ''"  )
  private String	remoteDevice;
  @Column( name = "metadata_vm_volume_status" )
  private String	status;
  @Column( name = "metadata_vm_volume_at_startup", columnDefinition = "boolean default false" )
  private Boolean	attachedAtStartup;
  @Column( name = "metadata_vm_volume_attach_time" )
  private Date attachTime;
  @Column( name = "metadata_vm_vol_delete_on_terminate" )
  private Boolean	deleteOnTerminate;
  @Column( name = "metadata_vm_volume_is_root_device", columnDefinition = "boolean default false" )
  private Boolean	isRootDevice;
  
  //  @OneToOne
//  @JoinTable( name = "metadata_vm_has_volume", joinColumns = { @JoinColumn( name = "metadata_vm_id" ) }, inverseJoinColumns = { @JoinColumn( name = "metadata_volume_id" ) } )
//  //  private Volume     volume;
  
  VmVolumeAttachment( ) {
    super( );
  }
  
  public VmVolumeAttachment( VmInstance vmInstance, String volumeId, String device, String remoteDevice, String status,
                             Date attachTime, Boolean deleteOnTerminate, Boolean attachedAtStartup ) {
    this( vmInstance, volumeId, device, remoteDevice, status, attachTime, deleteOnTerminate, Boolean.FALSE, attachedAtStartup );
  }

  public VmVolumeAttachment(VmInstance vmInstance, String volumeId, String device, String remoteDevice, String status, Date attachTime,
			Boolean deleteOnTerminate, Boolean rootDevice, Boolean attachedAtStartup) {
    super();
	this.vmInstance = vmInstance;
	this.volumeId = volumeId;
	this.device = device;
	this.remoteDevice = remoteDevice;
	this.status = status;
	this.attachTime = attachTime;
	this.deleteOnTerminate = deleteOnTerminate;
	this.isRootDevice = rootDevice;
	this.attachedAtStartup = attachedAtStartup;
  }
  
  public static Function<VmVolumeAttachment, com.eucalyptus.compute.common.AttachedVolume> asAttachedVolume( final VmInstance vm ) {
    return new Function<VmVolumeAttachment, com.eucalyptus.compute.common.AttachedVolume>( ) {
      @Override
      public com.eucalyptus.compute.common.AttachedVolume apply( VmVolumeAttachment vol ) {
        com.eucalyptus.compute.common.AttachedVolume attachment = null;
        if ( vm == null && vol.getVmInstance( ) == null ) {
          throw new NoSuchElementException( "Failed to transform volume attachment because it no longer exists: " + vol );
        } else if ( vm == null ) {
          attachment = new com.eucalyptus.compute.common.AttachedVolume( vol.getVolumeId( ), vol.getVmInstance( ).getInstanceId( ), vol.getDevice( ) );
        } else {
          attachment = new com.eucalyptus.compute.common.AttachedVolume( vol.getVolumeId( ), vm.getInstanceId( ), vol.getDevice( ) );
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

  public String getShortDeviceName() {
	return device.startsWith("/dev/") ? device.substring(5) : device;
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
  
  public void setDeleteOnTerminate( Boolean value ) {
    this.deleteOnTerminate = value;
  }
  
  public int compareTo( VmVolumeAttachment that ) {
    return this.volumeId.compareTo( that.getVolumeId( ) );
  }
  
  /**
   * @param instanceId
   */
  public void setInstanceId( String instanceId ) {}

  public void setVmInstance( VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
  }
  
  public Boolean getIsRootDevice() {
	return isRootDevice;
  }
	
  public void setIsRootDevice(Boolean isRootDevice) {
	this.isRootDevice = isRootDevice;
  }

  public Boolean getAttachedAtStartup() {
	return attachedAtStartup;
  }

  public void setAttachedAtStartup(Boolean attachedAtStartup) {
	this.attachedAtStartup = attachedAtStartup;
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
  
  static Predicate<VmVolumeAttachment> volumeDeviceFilter( final String deviceName ) {
    return new Predicate<VmVolumeAttachment>( ) {
      @Override
      public boolean apply( VmVolumeAttachment input ) {
        return input.getDevice( ).replaceAll( "unknown,requested:", "" ).equals( deviceName );
      }
    };
  }

  public static Predicate<VmVolumeAttachment> volumeIdFilter( final String volumeId ) {
    return new Predicate<VmVolumeAttachment>( ) {
      @Override
      public boolean apply( VmVolumeAttachment input ) {
        return input.getVolumeId().equals( volumeId );
      }
    };
  }

  public static Predicate<VmVolumeAttachment> deleteOnTerminateFilter( final Boolean deleteOnTerminate ) {
    return new Predicate<VmVolumeAttachment>( ) {
      @Override
      public boolean apply( VmVolumeAttachment input ) {
        return Objects.equals( deleteOnTerminate, input.getDeleteOnTerminate( ) );
      }
    };
  }

  public static Function<VmVolumeAttachment,String> volumeId( ) {
    return VmVolumeAttachmentStringProperties.VOLUME_ID;
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

  private enum VmVolumeAttachmentStringProperties implements Function<VmVolumeAttachment,String> {
    VOLUME_ID {
      @Nullable
      @Override
      public String apply( @Nullable final VmVolumeAttachment volumeAttachment ) {
        return volumeAttachment == null ? null : volumeAttachment.getVolumeId( );
      }
    }
  }

  public static class SchemaUpgradeForEntitySupport implements Callable<Boolean> {
    private final String table;
    private final String primaryKeyConstraintName;
    private final String permUuidUniqueConstraintName;

    public SchemaUpgradeForEntitySupport(
        final String table,
        final String primaryKeyConstraintName,
        final String permUuidUniqueConstraintName
    ) {
      this.table = table;
      this.primaryKeyConstraintName = primaryKeyConstraintName;
      this.permUuidUniqueConstraintName = permUuidUniqueConstraintName;
    }

    private boolean columnExists( Sql sql, String name ) throws SQLException {
      return !sql.rows(
          "select column_name from information_schema.columns where table_name=? and column_name=?",
          new Object[]{ table, name } ).isEmpty( );
    }

    @Override
    public Boolean call( ) throws Exception {
      final Logger LOG = Logger.getLogger( getClass( ) );
      LOG.info( "Checking "+table+" for upgrade" );
      Sql sql = null;
      try {
        sql = Bootstrap.isFinished( ) ? // if finished assume manual (re-)run
            Databases.getBootstrapper( ).getConnection( "eucalyptus_cloud" ) :
            Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloud" );

        sql.withTransaction( new Closure( this ) {
          {
            this.maximumNumberOfParameters = 1;
          }

          @Override
          public Object call( final Object... args ) {
            try {
              final Sql sql = new Sql( (Connection) args[ 0 ] );

              if ( !columnExists( sql, "id" ) ) {
                sql.execute( "alter table " + table + " add column id character varying(255)" );
                if ( !columnExists( sql, "creation_timestamp" ) ) {
                  sql.execute( "alter table " + table + " add column creation_timestamp timestamp without time zone" );
                }
                if ( !columnExists( sql, "last_update_timestamp" ) ) {
                  sql.execute( "alter table " + table + " add column last_update_timestamp timestamp without time zone" );
                }
                if ( !columnExists( sql, "metadata_perm_uuid" ) ) {
                  sql.execute( "alter table " + table + " add column metadata_perm_uuid character varying(255)" );
                }
                if ( !columnExists( sql, "version" ) ) {
                  sql.execute( "alter table " + table + " add column version integer" );
                }

                final List<GroovyRowResult> volumeAndInstanceIds = // instance id is foreign key
                    sql.rows( "select metadata_vm_volume_id, vminstance_id from " + table + " order by metadata_vm_volume_id, vminstance_id" );

                final UUIDHexGenerator generator = new UUIDHexGenerator( );
                final java.sql.Date date = new java.sql.Date( System.currentTimeMillis( ) );
                for ( final GroovyRowResult volumeAndInstanceIdRow : volumeAndInstanceIds ) {
                  sql.execute( "update " + table + " set id = ?, creation_timestamp = ?, last_update_timestamp = ?, metadata_perm_uuid = ?, version = 1 where metadata_vm_volume_id = ? and vminstance_id = ?",
                      new Object[]{
                          generator.generate( null, null ),
                          date,
                          date,
                          UUID.randomUUID( ).toString( ),
                          volumeAndInstanceIdRow.getAt( 0 ),
                          volumeAndInstanceIdRow.getAt( 1 ),
                      } );
                }

                sql.execute( "alter table " + table + " alter column id set not null" );
                sql.execute( "alter table " + table + " alter column metadata_perm_uuid set not null" );
                sql.execute( "alter table " + table + " add constraint " + primaryKeyConstraintName + " primary key (id)" );
                sql.execute( "alter table " + table + " add constraint " + permUuidUniqueConstraintName + " unique (metadata_perm_uuid)" );
              } else {
                LOG.info( "New columns exist, skipping upgrade" );
              }
            } catch ( Exception e ) {
              throw Exceptions.toUndeclared( "Failed to upgrade "+table+" schema", e );
            }
            return null;
          }
        } );
      } finally {
        if ( sql != null ) {
          sql.close( );
        }
      }

      return Boolean.TRUE;
    }
  }
}
