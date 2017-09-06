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

package com.eucalyptus.compute.common.internal.blockstorage;

import java.util.Collection;
import java.util.Date;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.CloudMetadata.VolumeMetadata;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_volumes", indexes = {
    @Index( name = "metadata_volumes_user_id_idx", columnList = "metadata_user_id" ),
    @Index( name = "metadata_volumes_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "metadata_volumes_display_name_idx", columnList = "metadata_display_name" ),
} )
public class Volume extends UserMetadata<State> implements VolumeMetadata {
  public  static final String ID_PREFIX = "vol";

  @Column( name = "metadata_volume_size" )
  private Integer  size;
  @Deprecated
  @Column( name = "metadata_volume_sc_name" )
  private String   scName;
  @Column( name = "metadata_volume_partition" )
  private String   partition;     //TODO:GRZE: change to injected ref.
  @Column( name = "metadata_volume_parentsnapshot" )
  private String   parentSnapshot;
  @Column( name = "metadata_volume_remotedevice" )
  @Type(type="text")
  private String   remoteDevice;
  @Column( name = "metadata_volume_localdevice" )
  private String   localDevice;
  @Column( name = "metadata_system_managed" )
  private Boolean  systemManaged;
  @Transient
  private FullName fullName;
  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "volume" )
  private Collection<VolumeTag> tags;

  protected Volume( ) {
    super( );
  }
  
  private Volume( final UserFullName userFullName, final String displayName, final Integer size, final String scName, final String partitionName,
                  final String parentSnapshot ) {
    super( userFullName, displayName );
    this.size = size;
    this.scName = scName;
    this.partition = partitionName;
    this.parentSnapshot = parentSnapshot;
    super.setState( State.NIHIL );
    super.setCreationTimestamp( new Date( ) );
  }
  
  private Volume( final OwnerFullName userFullName, String displayName ) {
    super( userFullName, displayName );
  }
  
  public static Volume create( final ServiceConfiguration sc, final UserFullName owner, final String snapId, final Integer newSize, final String newId ) {
    return new Volume( owner, newId, newSize, sc.getName( ), sc.getPartition( ), snapId );
  }
  
  public static Volume named( @Nullable final OwnerFullName fullName,
                              @Nullable final String volumeId ) {
    return new Volume( fullName, volumeId );
  }

  public static Volume naturalId( final String naturalId ) {
    final Volume volume = new Volume();
    volume.setNaturalId( naturalId );
    return volume;
  }

  public static Volume exampleResource( final OwnerFullName owner,
                                        final String parentSnapshot,
                                        final String partition,
                                        final int size ) {
    return new Volume( owner, "" ) {
      @Override public String getParentSnapshot( ) { return parentSnapshot; }
      @Override public String getPartition( ) { return partition; }
      @Override public Integer getSize( ) { return size; }
    };
  }

  public String mapState( ) {
    switch ( this.getState( ) ) {
      case GENERATING:
        return "creating";
      case EXTANT:
        return "available";
      case ANNIHILATING:
        return "deleting";
      case ANNIHILATED:
        return "deleted";
      case FAIL:
        return "failed";
      case BUSY:
        return "in-use";
      case ERROR:
          return "error";
      default:
        return "unavailable";
    }
  }
  
  public com.eucalyptus.compute.common.Volume morph( final com.eucalyptus.compute.common.Volume vol ) {
    vol.setAvailabilityZone( this.getPartition( ) );
    vol.setCreateTime( this.getCreationTimestamp( ) );
    vol.setVolumeId( this.getDisplayName( ) );
    vol.setSnapshotId( this.getParentSnapshot( ) );
    vol.setStatus( this.mapState( ) );
    vol.setSize( ( this.getSize( ) == null ) || ( this.getSize( ) == -1 )
      ? null
      : this.getSize( ).toString( ) );
    return vol;
  }
  
  public Integer getSize( ) {
    return this.size;
  }
  
  public String getScName( ) {
    return this.scName;
  }
  
  public void setSize( final Integer size ) {
    this.size = size;
  }
  
  protected void setScName( final String scName ) {
    this.scName = scName;
  }
  
  public String getParentSnapshot( ) {
    return this.parentSnapshot;
  }
  
  public void setParentSnapshot( final String parentSnapshot ) {
    this.parentSnapshot = parentSnapshot;
  }
  
  public String getRemoteDevice( ) {
    return this.remoteDevice;
  }
  
  public void setRemoteDevice( final String remoteDevice ) {
    this.remoteDevice = remoteDevice;
  }
  
  public String getLocalDevice( ) {
    return this.localDevice;
  }
  
  public void setLocalDevice( final String localDevice ) {
    this.localDevice = localDevice;
  }
  
  public boolean isReady( ) {
    return this.getState( ).equals( State.EXTANT ) || this.getState( ).equals( State.BUSY );
  }
  
  @Override
  public String getPartition( ) {
    return this.partition;
  }
  
  protected void setPartition( String partition ) {
    this.partition = partition;
  }

  public String getType( ) {
    return "standard";
  }

  public Integer getIops( ) {
    return null;
  }

  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" )
                          .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                          .namespace( this.getOwnerAccountNumber( ) )
                          .relativeId( "volume", this.getDisplayName( ) );
  }

  public Boolean getSystemManaged( ) {
    return systemManaged == null ? false : systemManaged;
  }

  public void setSystemManaged( Boolean systemManaged ) {
    this.systemManaged = systemManaged;
  }
}
