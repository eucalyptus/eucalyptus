/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 ************************************************************************/
package com.eucalyptus.auth;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.InstanceProfileEntity;
import com.eucalyptus.auth.entities.RoleEntity;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

/**
 * Instance profile implementation backed by InstanceProfileEntity
 */
public class DatabaseInstanceProfileProxy implements InstanceProfile {

  private static Logger LOG = Logger.getLogger( DatabaseInstanceProfileProxy.class );

  private InstanceProfileEntity delegate;

  public DatabaseInstanceProfileProxy( InstanceProfileEntity delegate ) {
    this.delegate = delegate;
  }

  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      DatabaseAuthUtils.invokeUnique( InstanceProfileEntity.class, "instanceProfileId", this.delegate.getInstanceProfileId(), new Tx<InstanceProfileEntity>( ) {
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
  public Account getAccount() throws AuthException {
    final List<Account> results = Lists.newArrayList();
    dbCallback( "getAccount", new Callback<InstanceProfileEntity>() {
      @Override
      public void fire( final InstanceProfileEntity instanceProfileEntity ) {
        results.add( new DatabaseAccountProxy( instanceProfileEntity.getAccount() ) );
      }
    } );
    return results.get( 0 );
  }

  @Override
  public String getInstanceProfileId() {
    return delegate.getInstanceProfileId();
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
  public Role getRole() throws AuthException {
    final List<Role> results = Lists.newArrayList();
    dbCallback( "getRole", new Callback<InstanceProfileEntity>() {
      @Override
      public void fire( final InstanceProfileEntity instanceProfileEntity ) {
        if ( instanceProfileEntity.getRole() == null ) {
          results.add( null );
        } else {
          results.add( new DatabaseRoleProxy( instanceProfileEntity.getRole() ) );
        }
      }
    } );
    return results.get( 0 );
  }

  @Override
  public void setRole( @Nullable final Role role ) throws AuthException {
    final EntityWrapper<InstanceProfileEntity> db = EntityWrapper.get( InstanceProfileEntity.class );
    try {
      final InstanceProfileEntity instanceProfileEntity =
          DatabaseAuthUtils.getUnique( db, InstanceProfileEntity.class, "instanceProfileId", getInstanceProfileId() );
      final RoleEntity roleEntity = role == null ?
          null :
          DatabaseAuthUtils.getUnique( db.recast( RoleEntity.class ), RoleEntity.class, "roleId", role.getRoleId() );
      instanceProfileEntity.setRole( roleEntity );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to assign role for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to assign role", e );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  @Override
  public Date getCreationTimestamp() {
    return delegate.getCreationTimestamp();
  }

  private void dbCallback( final String description,
                           final Callback<InstanceProfileEntity> updateCallback ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( InstanceProfileEntity.class, "instanceProfileId", getInstanceProfileId(), new Tx<InstanceProfileEntity>( ) {
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
