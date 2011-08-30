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

package com.eucalyptus.blockstorage;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Example;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.CloudMetadata.SnapshotMetadata;
import com.eucalyptus.cloud.util.DuplicateMetadataException;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.reporting.event.StorageEvent;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.base.Function;
import edu.ucsb.eucalyptus.msgs.CreateStorageSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.CreateStorageSnapshotType;

public class Snapshots {
  private static Logger LOG = Logger.getLogger( Snapshots.class );
  
  @QuantityMetricFunction( SnapshotMetadata.class )
  public enum CountSnapshots implements Function<OwnerFullName, Long> {
    INSTANCE;
    
    @Override
    public Long apply( OwnerFullName input ) {
      EntityWrapper<Snapshot> db = EntityWrapper.get( Snapshot.class );
      int ret = db.createCriteria( Snapshot.class ).add( Example.create( Snapshots.named( input, null ) ) ).setReadOnly( true ).setCacheable( false ).list( ).size( );
      db.rollback( );
      return new Long( ret );
    }
    
  }
  
  static Snapshot initializeSnapshot( UserFullName userFullName, Volume vol, ServiceConfiguration sc ) throws EucalyptusCloudException {
    String newId = null;
    Snapshot snap = null;
    EntityWrapper<Snapshot> db = EntityWrapper.get( Snapshot.class );
    try {
      while ( true ) {
        newId = Crypto.generateId( userFullName.getUniqueId( ), SnapshotManager.ID_PREFIX );
        try {
          db.getUnique( Snapshots.named( newId ) );
        } catch ( EucalyptusCloudException e ) {
          snap = new Snapshot( userFullName, newId, vol.getDisplayName( ), sc.getName( ), sc.getPartition( ) );
          snap.setVolumeSize( vol.getSize( ) );
          db.add( snap );
          db.commit( );
          return snap;
        }
      }
    } catch ( Exception ex ) {
      db.rollback( );
      throw new EucalyptusCloudException( "Failed to initialize snapshot state because of: " + ex.getMessage( ), ex );
    }
  }
  
  static Snapshot startCreateSnapshot( final Volume vol, final Snapshot snap ) throws EucalyptusCloudException, DuplicateMetadataException {
    final ServiceConfiguration sc = Partitions.lookupService( Storage.class, vol.getPartition( ) );
    try {
      Snapshot snapState = Transactions.save( snap, new Callback<Snapshot>( ) {
        
        @Override
        public void fire( Snapshot s ) {
          try {
            CreateStorageSnapshotType scRequest = new CreateStorageSnapshotType( vol.getDisplayName( ), snap.getDisplayName( ) );
            CreateStorageSnapshotResponseType scReply = ServiceDispatcher.lookup( sc ).send( scRequest );
            s.setMappedState( scReply.getStatus( ) );
          } catch ( EucalyptusCloudException ex ) {
            throw new UndeclaredThrowableException( ex );
          }
        }
      } );
    } catch ( ConstraintViolationException ex ) {
      throw new DuplicateMetadataException( "Duplicate snapshot creation: " + snap + ": " + ex.getMessage( ), ex );
    } catch ( ExecutionException ex ) {
      LOG.error( ex.getCause( ), ex.getCause( ) );
      throw new EucalyptusCloudException( ex );
    }
    try {
      ListenerRegistry.getInstance( ).fireEvent( new StorageEvent( StorageEvent.EventType.EbsSnapshot, true, snap.getVolumeSize( ),
                                                                   snap.getOwnerUserId( ), snap.getOwnerUserName( ),
                                                                   snap.getOwnerAccountNumber( ), snap.getOwnerAccountName( ),
                                                                   snap.getVolumeCluster( ), snap.getVolumePartition( ) ) );
    } catch ( EventFailedException ex ) {
      LOG.error( ex, ex );
    }
    return snap;
  }
  
  /**
   * @param snapshotId
   * @return
   * @throws ExecutionException
   */
  public static Snapshot lookup( String snapshotId ) throws ExecutionException {
    return Transactions.find( Snapshots.named( snapshotId ) );
  }
  
  public static Snapshot named( final String snapshotId ) {
    return new Snapshot( ( UserFullName ) null, snapshotId );
  }
  
  public static Snapshot lookup( OwnerFullName accountFullName, String snapshotId ) throws ExecutionException {
    return Transactions.find( Snapshots.named( accountFullName, snapshotId ) );
  }
  
  public static Snapshot named( final OwnerFullName ownerFullName, String snapshotId ) {
    return new Snapshot( ownerFullName, snapshotId );
  }
}
