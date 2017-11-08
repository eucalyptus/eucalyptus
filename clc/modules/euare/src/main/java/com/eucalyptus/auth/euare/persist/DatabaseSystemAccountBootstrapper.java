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

import java.util.ServiceLoader;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.euare.Accounts;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareRole;
import com.eucalyptus.auth.euare.principal.EuareUser;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.util.SystemAccountProvider;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Sets;

/**
 *
 */
@Provides( Empyrean.class )
@RunDuring( Bootstrap.Stage.SystemAccountsInit )
public class DatabaseSystemAccountBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( DatabaseSystemAccountBootstrapper.class );

  public boolean load( ) {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
   */
  @Override
  public boolean enable( ) throws Exception {
    return true;
  }

  public boolean start( ) throws Exception {
    if( ComponentIds.lookup( Eucalyptus.class ).isAvailableLocally( ) ) {
      for ( final SystemAccountProvider systemAccountProvider : ServiceLoader.load( SystemAccountProvider.class ) ) {
        ensureSystemAccountExists( systemAccountProvider );
      }
    }
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
   */
  @Override
  public boolean stop( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
   */
  @Override
  public void destroy( ) throws Exception {
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
   */
  @Override
  public boolean disable( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#check()
   */
  @Override
  public boolean check( ) throws Exception {
    return true;
  }

  private static void ensureSystemAccountExists( final SystemAccountProvider provider ) {
    try {
      EuareAccount account;
      try {
        account = Accounts.lookupAccountByName( provider.getAlias( ) );
      } catch ( final AuthException e ) {
        LOG.info( "Could not find system account '" + provider.getAlias( ) + "', creating.");
        try {
          account = Accounts.addSystemAccountWithAdmin( provider.getAlias() );
        } catch ( final Exception e1 ) {
          throw new EucalyptusCloudException( "Error creating system account " + provider.getAlias() );
        }
      }
      EuareUser user = account.lookupAdmin( );
      if ( provider.isCreateAdminAccessKey( ) && user.getKeys( ).isEmpty( ) ) {
        LOG.info( "Creating system account '"+provider.getAlias()+"', admin access key" );
        account.lookupAdmin( ).createKey( );
      }

      for ( final SystemAccountProvider.SystemAccountRole systemAccountRole : provider.getRoles( ) ) {
        try {
          final EuareRole role = account.lookupRoleByName( systemAccountRole.getName( ) );
          ensureSystemRolePoliciesExist( role, systemAccountRole, account.getAccountNumber() );
        } catch ( final AuthException e ) {
          addSystemRole( account, systemAccountRole );
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Error checking system account '" + provider.getAlias( ) + "'", e );
    }
  }

  private static void addSystemRole( final EuareAccount account,
                                     final SystemAccountProvider.SystemAccountRole systemAccountRole ) {
    LOG.info( String.format( "Creating system role: %s", systemAccountRole.getName( ) ) );
    try {
      final String name = systemAccountRole.getName( );
      final String path = systemAccountRole.getPath( );
      final String assumeRolePolicy = systemAccountRole.getAssumeRolePolicy();
      final EuareRole role = account.addRole( name, path, assumeRolePolicy );
      ensureSystemRolePoliciesExist( role, systemAccountRole, account.getAccountNumber() );
    } catch ( Exception e ) {
      LOG.error( String.format( "Error adding system role: %s", systemAccountRole.getName( ) ), e );
    }
  }

  private static void ensureSystemRolePoliciesExist(
      final EuareRole role,
      final SystemAccountProvider.SystemAccountRole systemAccountRole,
      final String accountId) throws AuthException, PolicyParseException {
    final Set<String> existingPolicies = Sets.newHashSet();
    for ( final Policy policy : role.getPolicies( ) ) {
      existingPolicies.add( policy.getName( ) );
    }
    for ( final SystemAccountProvider.AttachedPolicy policy : systemAccountRole.getPolicies( ) ) {
      if ( !existingPolicies.contains( policy.getName( ) ) ) {
        LOG.info( "Creating system role policy '"+policy.getName( )+"'" );
        String p = policy.getPolicy( );
        p = p != null ? p.replaceAll("<AccountNumber>", accountId) : p;
        role.putPolicy( policy.getName( ), p );
      }
    }
  }

  public static class DatabaseAuthSystemAccountInitializer implements SystemAccountProvider.SystemAccountInitializer {
    @Override
    public void initialize( final SystemAccountProvider provider ) throws AuthException {
      ensureSystemAccountExists( provider );
    }
  }
}
