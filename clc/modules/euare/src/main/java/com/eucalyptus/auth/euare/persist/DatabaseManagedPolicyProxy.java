/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.euare.persist;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.euare.persist.entities.GroupEntity;
import com.eucalyptus.auth.euare.persist.entities.ManagedPolicyEntity;
import com.eucalyptus.auth.euare.persist.entities.ManagedPolicyEntity_;
import com.eucalyptus.auth.euare.persist.entities.RoleEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareGroup;
import com.eucalyptus.auth.euare.principal.EuareManagedPolicy;
import com.eucalyptus.auth.euare.principal.EuareRole;
import com.eucalyptus.auth.euare.principal.EuareUser;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Tx;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;

/**
 *
 */
public class DatabaseManagedPolicyProxy implements EuareManagedPolicy {

  private static Logger LOG = Logger.getLogger( DatabaseManagedPolicyProxy.class );

  private ManagedPolicyEntity delegate;
  private Supplier<String> accountNumberSupplier = getAccountNumberSupplier( this );

  public DatabaseManagedPolicyProxy( final ManagedPolicyEntity delegate ) {
    this.delegate = delegate;
  }

  @Override
  public String getAccountNumber() throws AuthException {
    return DatabaseAuthUtils.extract( accountNumberSupplier );
  }

  @Override
  public EuareAccount getAccount() throws AuthException {
    if ( Entities.isReadable( delegate.getAccount( ) ) ) {
      return new DatabaseAccountProxy( delegate.getAccount( ) );
    } else {
      final List<EuareAccount> results = Lists.newArrayList( );
      dbCallback( "getAccount", new Callback<ManagedPolicyEntity>( ) {
        @Override
        public void fire( final ManagedPolicyEntity policyEntity ) {
          results.add( new DatabaseAccountProxy( policyEntity.getAccount( ) ) );
        }
      } );
      return results.get( 0 );
    }
  }

  @Override
  public String getPolicyId( ) {
    return delegate.getPolicyId( );
  }

  @Override
  public Integer getPolicyVersion( ) {
    return delegate.getVersion( );
  }

  @Override
  public String getName() {
    return delegate.getName( );
  }

  @Override
  public String getPath() {
    return delegate.getPath( );
  }

  @Override
  public String getDescription() {
    return delegate.getDescription( );
  }

  @Override
  public String getText() {
    return delegate.getText();
  }

  @Override
  public Date getCreateDate() {
    return delegate.getCreationTimestamp( );
  }

  @Override
  public Date getUpdateDate() {
    return delegate.getLastUpdateTimestamp();
  }

  @Override
  public List<EuareGroup> getGroups( ) throws AuthException {
    final List<EuareGroup> groups = Lists.newArrayList( );
    if ( Entities.isReadable( delegate.getGroups( ) ) ) {
      for ( final GroupEntity group : delegate.getGroups( ) ) {
        groups.add( new DatabaseGroupProxy( group ) );
      }
    } else {
      dbCallback( "getGroups", new Callback<ManagedPolicyEntity>( ) {
        @Override
        public void fire( final ManagedPolicyEntity policyEntity ) {
          for ( final GroupEntity group : policyEntity.getGroups( ) ) {
            groups.add( new DatabaseGroupProxy( group ) );
          }
        }
      } );
    }
    return groups;
  }

  @Override
  public List<EuareRole> getRoles( ) throws AuthException {
    final List<EuareRole> roles = Lists.newArrayList( );
    if ( Entities.isReadable( delegate.getRoles( ) ) ) {
      for ( final RoleEntity role : delegate.getRoles( ) ) {
        roles.add( new DatabaseRoleProxy( role ) );
      }
    } else {
      dbCallback( "getRoles", new Callback<ManagedPolicyEntity>( ) {
        @Override
        public void fire( final ManagedPolicyEntity policyEntity ) {
          for ( final RoleEntity role : policyEntity.getRoles( ) ) {
            roles.add( new DatabaseRoleProxy( role ) );
          }
        }
      } );
    }
    return roles;
  }

  @Override
  public List<EuareUser> getUsers( ) throws AuthException {
    final List<EuareUser> users = Lists.newArrayList( );
    if ( Entities.isReadable( delegate.getUsers( ) ) ) {
      for ( final UserEntity user : delegate.getUsers( ) ) {
        users.add( new DatabaseUserProxy( user ) );
      }
    } else {
      dbCallback( "getRoles", new Callback<ManagedPolicyEntity>( ) {
        @Override
        public void fire( final ManagedPolicyEntity policyEntity ) {
          for ( final UserEntity user : policyEntity.getUsers( ) ) {
            users.add( new DatabaseUserProxy( user ) );
          }
        }
      } );
    }
    return users;
  }

  private void dbCallback( final String description,
                           final Callback<ManagedPolicyEntity> updateCallback ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( ManagedPolicyEntity.class, ManagedPolicyEntity_.policyId, getPolicyId( ), new Tx<ManagedPolicyEntity>( ) {
        @Override
        public void fire( final ManagedPolicyEntity policyEntity ) {
          updateCallback.fire( policyEntity );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to " + description + " for " + this.delegate );
      throw new AuthException( e );
    }
  }

  static Supplier<String> getAccountNumberSupplier( final DatabaseManagedPolicyProxy policy ){
    return Suppliers.memoize( new Supplier<String>() {
      @Override
      public String get() {
        try {
          return policy.getAccount().getAccountNumber();
        } catch ( final AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }
}
