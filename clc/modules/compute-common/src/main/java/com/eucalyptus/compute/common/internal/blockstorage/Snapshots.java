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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.google.common.base.Predicate;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.CloudMetadata.SnapshotMetadata;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;
import com.google.common.base.Function;

public class Snapshots {
  private static Logger           LOG                     = Logger.getLogger( Snapshots.class );

  public static final String SELF = "self";

  public static Predicate<Snapshot> filterRestorableBy( final Collection<String> restorableSet,
                                                        final String callerAccountNumber ) {
    final boolean restorableSelf = restorableSet.remove( SELF );
    final boolean restorableAll = restorableSet.remove( "all" );
    return new Predicate<Snapshot>( ) {
      @Override
      public boolean apply( Snapshot snapshot ) {
        return restorableSet.isEmpty( ) && !restorableSelf && !restorableAll ||
            ( restorableAll && snapshot.getSnapshotPublic() ) ||
            ( restorableSelf && snapshot.hasPermission( callerAccountNumber ) ) ||
            snapshot.hasPermission( restorableSet.toArray( new String[ restorableSet.size() ] ) );
      }
    };
  }

  @QuantityMetricFunction( SnapshotMetadata.class )
  public enum CountSnapshots implements Function<OwnerFullName, Long> {
    INSTANCE;
    
    @Override
    public Long apply( OwnerFullName input ) {
      final EntityTransaction db = Entities.get( Snapshot.class );
      try {
        return Entities.count( Snapshot.named( input, null ) );
      } finally {
        db.rollback( );
      }
    }
  }
  
  public static Snapshot named( final String snapshotId ) {
    return new Snapshot( null, snapshotId );
  }
  
  public static Snapshot lookup( @Nullable OwnerFullName accountFullName, String snapshotId ) throws ExecutionException {
    return Transactions.find(Snapshot.named(accountFullName, snapshotId));
  }
  
  public static List<Snapshot> list( ) throws TransactionException {
    return Transactions.findAll( Snapshot.named( null, null ) );
  }

  public static class SnapshotFilterSupport extends FilterSupport<Snapshot> {
    public SnapshotFilterSupport() {
      super( builderFor( Snapshot.class )
          .withTagFiltering( SnapshotTag.class, "snapshot" )
          .withStringProperty( "description", FilterFunctions.DESCRIPTION )
          .withLikeExplodedProperty( "owner-alias", FilterFunctions.ACCOUNT_ID, accountAliasExploder() )
          .withStringProperty( "owner-id", FilterFunctions.ACCOUNT_ID )
          .withStringProperty( "progress", FilterFunctions.PROGRESS )
          .withStringProperty( "snapshot-id", CloudMetadatas.toDisplayName() )
          .withStringProperty( "status", FilterFunctions.STATUS )
          .withDateProperty( "start-time", FilterDateFunctions.START_TIME )
          .withStringProperty( "volume-id", FilterFunctions.VOLUME_ID )
          .withStringProperty( "volume-size", FilterFunctions.VOLUME_SIZE )
          .withPersistenceFilter( "description" )
          .withLikeExplodingPersistenceFilter( "owner-alias", "ownerAccountNumber", accountAliasExploder() )
          .withPersistenceFilter( "owner-id", "ownerAccountNumber" )
          .withPersistenceFilter( "progress" )
          .withPersistenceFilter( "snapshot-id", "displayName" )
          .withPersistenceFilter( "start-time", "creationTimestamp", PersistenceFilter.Type.Date )
          .withPersistenceFilter( "volume-id", "parentVolume" )
          .withPersistenceFilter( "volume-size", "volumeSize", PersistenceFilter.Type.Integer )
      );
    }
  }

  private static Function<String,Collection> accountAliasExploder() {
    return new Function<String,Collection>() {
      @Override
      public Collection<String> apply( final String accountAliasExpression ) {
        try {
          return Accounts.listAccountNumbersForName( accountAliasExpression );
        } catch ( AuthException e ) {
          LOG.error( e, e );
          return Collections.emptySet();
        }
      }
    };
  }

    /** True if owner, euca administrator,
     * snap is public, or
     * granted explicit permission by snap owner via ModifySnapshotAttributes
     */
    public enum FilterPermissions implements Predicate<Snapshot> {
        INSTANCE;

        @Override
        public boolean apply( Snapshot input ) {
            try {
                Context ctx = Contexts.lookup();
                if ( ctx.isAdministrator( ) ) {
                    return true;
                } else {
                    UserFullName luser = ctx.getUserFullName( );
                    if ( input.getSnapshotPublic( ) ) {
                        // Granted by 'all' permission on the snapshot
                        return true;
                    } else if ( input.getOwnerAccountNumber( ).equals( luser.getAccountNumber( ) ) ) {
                        // Owning account
                        return true;
                    } else if ( input.hasPermission( luser.getAccountNumber( ) ) ) {
                        //Explicitly granted via createVolumePermission
                        return true;
                    }
                    return false;
                }
            } catch ( Exception ex ) {
                return false;
            }
        }
    }

  private enum FilterFunctions implements Function<Snapshot,String> {
    DESCRIPTION {
      @Override
      public String apply( final Snapshot snapshot ) {
        return snapshot.getDescription();
      }
    },
    ACCOUNT_ID {
      @Override
      public String apply( final Snapshot snapshot ) {
        return snapshot.getOwnerAccountNumber();
      }
    },
    PROGRESS {
      @Override
      public String apply( final Snapshot snapshot ) {
        return snapshot.getProgress();
      }
    },
    STATUS {
      @Override
      public String apply( final Snapshot snapshot ) {
        return snapshot.mapState();
      }
    },
    VOLUME_ID {
      @Override
      public String apply( final Snapshot snapshot ) {
        return snapshot.getParentVolume();
      }
    },
    VOLUME_SIZE {
      @Override
      public String apply( final Snapshot snapshot ) {
        Integer size = snapshot.getVolumeSize();
        return size == null ? null : String.valueOf( size );
      }
    }
  }

  private enum FilterDateFunctions implements Function<Snapshot,Date> {
    START_TIME {
      @Override
      public Date apply( final Snapshot snapshot ) {
        return snapshot.getCreationTimestamp();
      }
    }
  }
}
