/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.AuthenticationLimitProvider;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.euare.Accounts;
import com.eucalyptus.auth.euare.persist.entities.GroupEntity;
import com.eucalyptus.auth.euare.persist.entities.ManagedPolicyEntity;
import com.eucalyptus.auth.euare.persist.entities.ManagedPolicyEntity_;
import com.eucalyptus.auth.euare.persist.entities.ManagedPolicyVersionEntity;
import com.eucalyptus.auth.euare.persist.entities.RoleEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareGroup;
import com.eucalyptus.auth.euare.principal.EuareManagedPolicy;
import com.eucalyptus.auth.euare.principal.EuareManagedPolicyVersion;
import com.eucalyptus.auth.euare.principal.EuareRole;
import com.eucalyptus.auth.euare.principal.EuareUser;
import com.eucalyptus.auth.policy.PolicyParser;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

/**
 *
 */
public class DatabaseManagedPolicyProxy implements EuareManagedPolicy {

  private static Logger LOG = Logger.getLogger( DatabaseManagedPolicyProxy.class );

  private ManagedPolicyEntity delegate;

  public DatabaseManagedPolicyProxy( final ManagedPolicyEntity delegate ) {
    this.delegate = delegate;
  }

  @Override
  public String getAccountNumber( ) {
    return delegate.accountNumber( );
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
    return delegate.getDefaultPolicyVersionNumber( );
  }

  @Override
  public void setPolicyVersion( @Nonnull final Integer versionId ) throws AuthException {
    dbCallback( "setPolicyVersion", policyEntity -> {
      for ( final ManagedPolicyVersionEntity versionEntity : policyEntity.getVersions( ) ) {
        if ( versionId.equals( versionEntity.getPolicyVersion( ) ) ) {
          policyEntity.applyDefaultPolicyVersion( versionEntity );
          return;
        }
      }
      throw Exceptions.toUndeclared( new AuthException( AuthException.INVALID_ID ) );
    } );
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
    return delegate.getText( );
  }

  @Override
  public Date getCreateDate() {
    return delegate.getCreationTimestamp( );
  }

  @Override
  public Date getUpdateDate() {
    return delegate.getPolicyUpdated( );
  }

  @Override
  public String getDisplayName( ) {
    return Accounts.getManagedPolicyFullName( this );
  }

  @Override
  public OwnerFullName getOwner( ) {
    return AccountFullName.getInstance( getAccountNumber( ) );
  }

  @Override
  public Integer getAttachmentCount( ) {
    return delegate.getAttachmentCount( );
  }

  @Override
  public EuareManagedPolicyVersion addPolicyVersion( final String policy, final boolean setAsDefault ) throws AuthException {
    final EuareManagedPolicyVersion[] policyVersion = new EuareManagedPolicyVersion[1];
    try {
      PolicyParser.getInstance( ).parse( policy, "2012-10-17" );
    } catch ( final PolicyParseException e ) {
      throw new AuthException( "Invalid policy: " + e.getMessage( ) );
    }
    dbCallback( "addPolicyVersion", policyEntity -> {
      if ( policyEntity.getVersions( ).size( ) >= 5 ) {
        throw Exceptions.toUndeclared( new AuthException( AuthException.QUOTA_EXCEEDED ) );
      }
      final ManagedPolicyVersionEntity newManagedPolicyVersionEntity = new ManagedPolicyVersionEntity( policyEntity );
      newManagedPolicyVersionEntity.setText( policy );
      if ( setAsDefault ) {
        policyEntity.applyDefaultPolicyVersion( newManagedPolicyVersionEntity );
      }
      policyVersion[0] = new DatabaseManagedPolicyVersionProxy( Entities.persist( newManagedPolicyVersionEntity ) );
    } );
    return policyVersion[0];
  }

  @Override
  public void deletePolicyVersion( final Integer versionId ) throws AuthException {
    dbCallback( "deletePolicyVersion", policyEntity -> {
      for ( final ManagedPolicyVersionEntity versionEntity : policyEntity.getVersions( ) ) {
        if ( versionId.equals( versionEntity.getPolicyVersion( ) ) ) {
          if ( versionEntity.getDefaultPolicy( ) ) {
            throw Exceptions.toUndeclared( new AuthException( AuthException.CONFLICT ) );
          }
          Entities.delete( versionEntity );
          policyEntity.setPolicyUpdated( new Date( ) );
          return;
        }
      }
      throw Exceptions.toUndeclared( new AuthException( AuthException.INVALID_ID ) );

    } );
  }

  @Override
  public List<EuareManagedPolicyVersion> getVersions( ) throws AuthException {
    final List<EuareManagedPolicyVersion> versions = Lists.newArrayList( );
    if ( Entities.isReadable( delegate.getVersions( ) ) ) {
      for ( final ManagedPolicyVersionEntity version : delegate.getVersions( ) ) {
        versions.add( new DatabaseManagedPolicyVersionProxy( version ) );
      }
    } else {
      dbCallback( "getVersions", new Callback<ManagedPolicyEntity>( ) {
        @Override
        public void fire( final ManagedPolicyEntity policyEntity ) {
          for ( final ManagedPolicyVersionEntity version : policyEntity.getVersions( ) ) {
            versions.add( new DatabaseManagedPolicyVersionProxy( version ) );
          }
        }
      } );
    }
    return versions;
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
      Exceptions.findAndRethrow( e, AuthException.class );
      throw new AuthException( e );
    }
  }
}
