/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.auth.euare.persist;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.euare.persist.entities.InstanceProfileEntity;
import com.eucalyptus.auth.euare.persist.entities.InstanceProfileEntity_;
import com.eucalyptus.auth.euare.persist.entities.RoleEntity;
import com.eucalyptus.auth.euare.persist.entities.RoleEntity_;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareInstanceProfile;
import com.eucalyptus.auth.euare.principal.EuareRole;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

/**
 * Instance profile implementation backed by InstanceProfileEntity
 */
public class DatabaseInstanceProfileProxy implements EuareInstanceProfile {

  private static Logger LOG = Logger.getLogger( DatabaseInstanceProfileProxy.class );

  private InstanceProfileEntity delegate;

  public DatabaseInstanceProfileProxy( InstanceProfileEntity delegate ) {
    this.delegate = delegate;
  }

  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      DatabaseAuthUtils.invokeUnique( InstanceProfileEntity.class, InstanceProfileEntity_.instanceProfileId, this.delegate.getInstanceProfileId(), new Tx<InstanceProfileEntity>( ) {
        @Override
        public void fire( InstanceProfileEntity t ) {
          sb.append( t.toString( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to toString for " + this.delegate );
    }
    return sb.toString();
  }

  @Override
  public String getAccountNumber() throws AuthException {
    return getAccount( ).getAccountNumber( );
  }

  @Override
  public EuareAccount getAccount() throws AuthException {
    if ( Entities.isReadable( delegate.getAccount( ) ) ) {
      return new DatabaseAccountProxy( delegate.getAccount( ) );
    } else {
      final List<EuareAccount> results = Lists.newArrayList( );
      dbCallback( "getAccount", new Callback<InstanceProfileEntity>( ) {
        @Override
        public void fire( final InstanceProfileEntity instanceProfileEntity ) {
          results.add( new DatabaseAccountProxy( instanceProfileEntity.getAccount( ) ) );
        }
      } );
      return results.get( 0 );
    }
  }

  @Override
  public String getInstanceProfileId() {
    return delegate.getInstanceProfileId();
  }

  @Override
  public String getInstanceProfileArn( ) throws AuthException {
    return Accounts.getInstanceProfileArn( this );
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public String getPath() {
    return delegate.getPath();
  }

  @Override
  public EuareRole getRole( ) throws AuthException {
    if ( Entities.isPersistent( delegate ) && delegate.getRole( ) == null ) {
      return null;
    } else if ( Entities.isReadable( delegate.getRole( ) ) ) {
      return new DatabaseRoleProxy( delegate.getRole( ) );
    } else {
      final List<EuareRole> results = Lists.newArrayList( );
      dbCallback( "getRole", new Callback<InstanceProfileEntity>( ) {
        @Override
        public void fire( final InstanceProfileEntity instanceProfileEntity ) {
          if ( instanceProfileEntity.getRole( ) == null ) {
            results.add( null );
          } else {
            results.add( new DatabaseRoleProxy( instanceProfileEntity.getRole( ) ) );
          }
        }
      } );
      return results.get( 0 );
    }
  }

  @Override
  public void setRole( @Nullable final EuareRole role ) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( InstanceProfileEntity.class ) ) {
      final InstanceProfileEntity instanceProfileEntity =
          DatabaseAuthUtils.getUnique( InstanceProfileEntity.class, InstanceProfileEntity_.instanceProfileId, getInstanceProfileId() );
      final RoleEntity roleEntity = role == null ?
          null :
          DatabaseAuthUtils.getUnique( RoleEntity.class, RoleEntity_.roleId, role.getRoleId() );
      instanceProfileEntity.setRole( roleEntity );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to assign role for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to assign role", e );
    }
  }

  @Override
  public Date getCreationTimestamp() {
    return delegate.getCreationTimestamp();
  }

  private void dbCallback( final String description,
                           final Callback<InstanceProfileEntity> updateCallback ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( InstanceProfileEntity.class, InstanceProfileEntity_.instanceProfileId, getInstanceProfileId(), new Tx<InstanceProfileEntity>( ) {
        @Override
        public void fire( final InstanceProfileEntity instanceProfileEntity ) {
          updateCallback.fire( instanceProfileEntity );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to " + description + " for " + this.delegate );
      throw new AuthException( e );
    }
  }

}
