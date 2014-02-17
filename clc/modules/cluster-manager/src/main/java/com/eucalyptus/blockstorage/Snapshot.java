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

package com.eucalyptus.blockstorage;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.compute.common.CloudMetadata.SnapshotMetadata;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.Version;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Predicate;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_snapshots" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class Snapshot extends UserMetadata<State> implements SnapshotMetadata {
  @Column( name = "metadata_snapshot_vol_size" )
  private Integer  volumeSize;
  @Column( name = "metadata_snapshot_parentvolume", updatable = false )
  private String   parentVolume;
  /**
   * @deprecated srsly. dont use it.
   */
  @Deprecated
  @Column( name = "metadata_snapshot_vol_sc", updatable = false )
  private String   volumeSc;
  @Column( name = "metadata_snapshot_vol_partition", updatable = false )
  private String   volumePartition;
  @Column( name = "metadata_snapshot_progress" )
  private String   progress;
  @Column( name =  "metadata_snapshot_description", updatable = false )
  private String   description;
  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "snapshot" )
  private Collection<SnapshotTag> tags;

  protected Snapshot( ) {
    super( );
  }
  
  Snapshot( final OwnerFullName ownerFullName, final String displayName ) {
    super( ownerFullName, displayName );
  }
  
  Snapshot( final OwnerFullName ownerFullName,
            final String displayName,
            final String description,
            final String parentVolume,
            final Integer volumeSize,
            final String volumeScName,
            final String volumePartition ) {
    this( ownerFullName, displayName );
    this.description = description;
    this.parentVolume = parentVolume;
    this.volumeSc = volumeScName;
    this.volumePartition = volumePartition;
    this.volumeSize = volumeSize;
    this.progress = "0%";
    super.setState( State.NIHIL );
  }

  public static Snapshot named( @Nullable final OwnerFullName ownerFullName,
                                @Nullable final String snapshotId ) {
    return new Snapshot( ownerFullName, snapshotId );
  }

  public static Snapshot naturalId( final String naturalId ) {
    final Snapshot snapshot = new Snapshot();
    snapshot.setNaturalId( naturalId );
    return snapshot;
  }

  public String mapState( ) {
    switch ( this.getState( ) ) {
      case GENERATING:
      case NIHIL:
        return "pending";
      case EXTANT:
        return "completed";
      default:
        return "failed";
    }
  }
  
  public void setMappedState( final String state ) {
    if ( StorageProperties.Status.creating.toString( ).equals( state ) )
      this.setState( State.GENERATING );
    else if ( StorageProperties.Status.pending.toString( ).equals( state ) )
      this.setState( State.GENERATING );
    else if ( StorageProperties.Status.completed.toString( ).equals( state ) )
      this.setState( State.EXTANT );
    else if ( StorageProperties.Status.available.toString( ).equals( state ) )
      this.setState( State.EXTANT );
    else if ( StorageProperties.Status.failed.toString( ).equals( state ) ) this.setState( State.FAIL );
  }
  
  public edu.ucsb.eucalyptus.msgs.Snapshot morph( final edu.ucsb.eucalyptus.msgs.Snapshot snap ) {
    snap.setSnapshotId( this.getDisplayName( ) );
    snap.setDescription( this.getDescription() );
    snap.setStatus( this.mapState( ) );
    snap.setStartTime( this.getCreationTimestamp( ) );
    snap.setVolumeId( this.getParentVolume( ) );
    snap.setVolumeSize( Integer.toString( this.getVolumeSize( ) ) );
    if ( this.getProgress( ) != null ) {
      snap.setProgress( this.getProgress( ) );
    } else {
      snap.setProgress( State.EXTANT.equals( this.getState( ) ) ? "100%" : "0%" );//GRZE: sigh @ this crap.
    }
    return snap;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getParentVolume( ) {
    return parentVolume;
  }
  
  protected void setParentVolume( final String parentVolume ) {
    this.parentVolume = parentVolume;
  }
  
  @Override
  public String getPartition( ) {
    return this.volumePartition;
  }
  
  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" )
                          .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                          .namespace( this.getOwnerAccountNumber( ) )
                          .relativeId( "snapshot", this.getDisplayName( ) );
  }
  
  public Integer getVolumeSize( ) {
    return this.volumeSize;
  }
  
  public void setVolumeSize( Integer integer ) {
    this.volumeSize = integer;
  }
  
  public void setPartition( String partition ) {
    this.volumePartition = partition;
  }
  
  public String getVolumeCluster( ) {
    return this.volumeSc;
  }
  
  public void setVolumeCluster( String volumeCluster ) {
    this.volumeSc = volumeCluster;
  }
  
  public String getVolumePartition( ) {
    return this.volumePartition;
  }
  
  public void setVolumePartition( String volumePartition ) {
    this.volumePartition = volumePartition;
  }
  
  public String getVolumeSc( ) {
    return this.volumeSc;
  }

  protected String getProgress( ) {
    return this.progress;
  }

  protected void setProgress( String progress ) {
    this.progress = progress;
  }
  
  @EntityUpgrade( entities = { Snapshot.class }, since = Version.v3_2_0, value = Storage.class )
  public enum SnapshotUpgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( Snapshot.SnapshotUpgrade.class );
    @Override
    public boolean apply( Class arg0 ) {
      EntityTransaction db = Entities.get( Snapshot.class );
      try {
        List<Snapshot> entities = Entities.query( new Snapshot( ) );
        for ( Snapshot entry : entities ) {
          LOG.debug( "Upgrading: " + entry.getDisplayName() );
          entry.setDescription(null);
        }
        db.commit( );
        return true;
      } catch ( Exception ex ) {
        db.rollback();
        throw Exceptions.toUndeclared( ex );
      }
    }
  }
  
}
