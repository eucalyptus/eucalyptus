/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.euare;

import javax.annotation.Nonnull;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.DatabaseIdentityProvider;
import com.eucalyptus.auth.api.IdentityProvider;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurationManager;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurations;
import com.eucalyptus.auth.euare.identity.region.RegionInfo;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.NonNullFunction;
import com.google.common.base.Optional;

/**
 *
 */
public class RegionDelegatingIdentityProvider implements IdentityProvider {

  private final IdentityProvider localProvider = new DatabaseIdentityProvider( );
  private final RegionConfigurationManager regionConfigurationManager = new RegionConfigurationManager( );

  @Override
  public UserPrincipal lookupPrincipalByUserId( final String userId, final String nonce ) throws AuthException {
    return regionDispatch( userId, new NonNullFunction<IdentityProvider, UserPrincipal>() {
      @Nonnull
      @Override
      public UserPrincipal apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.lookupPrincipalByUserId( userId, nonce );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByRoleId( final String roleId, final String nonce ) throws AuthException {
    return regionDispatch( roleId, new NonNullFunction<IdentityProvider, UserPrincipal>() {
      @Nonnull
      @Override
      public UserPrincipal apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.lookupPrincipalByRoleId( roleId, nonce );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccessKeyId( final String keyId, final String nonce ) throws AuthException {
    return regionDispatch( keyId, new NonNullFunction<IdentityProvider, UserPrincipal>() {
      @Nonnull
      @Override
      public UserPrincipal apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.lookupPrincipalByAccessKeyId( keyId, nonce );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByCertificateId( final String certificateId ) throws AuthException {
    return localProvider.lookupPrincipalByCertificateId( certificateId ); //TODO:STEVE: remote for certificate lookup
  }

  @Override
  public UserPrincipal lookupPrincipalByCanonicalId( final String canonicalId ) throws AuthException {
    if ( regionConfigurationManager.getRegionInfo().isPresent() && Contexts.exists( ) && Contexts.lookup( ).getUser( ).getCanonicalId( ).equals( canonicalId ) ) {
      return Contexts.lookup( ).getUser( ); //TODO:STEVE: remove this temporary lookup hack
    }
    return localProvider.lookupPrincipalByCanonicalId( canonicalId ); //TODO:STEVE: remote for canonical id lookup
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumber( final String accountNumber ) throws AuthException {
    if ( regionConfigurationManager.getRegionInfo().isPresent() && Contexts.exists( ) && Contexts.lookup( ).getUser( ).getAccountNumber( ).equals( accountNumber ) ) {
      return Contexts.lookup( ).getUser( ); //TODO:STEVE: remove this temporary lookup hack
    }
    return localProvider.lookupPrincipalByAccountNumber( accountNumber ); //TODO:STEVE: remote for account number lookup
  }

  private UserPrincipal regionDispatch( final String identifier, final NonNullFunction<IdentityProvider, UserPrincipal> invoker ) throws AuthException {
    final Optional<RegionInfo> regionInfo = regionConfigurationManager.getRegionByIdentifier( identifier );
    try {
      if ( regionInfo.isPresent( ) &&
          !RegionConfigurations.getRegionName( ).asSet( ).contains( regionInfo.get( ).getName( ) ) ) {
        for ( final RegionInfo.RegionService service : regionInfo.get( ).getServices( ) ) {
          if ( "identity".equals( service.getType( ) ) ) {
            final IdentityProvider remoteProvider = new RemoteIdentityProvider( service.getEndpoints( ) );
            return invoker.apply( remoteProvider );
          }
        }
        return invoker.apply( localProvider );
      } else {
        return invoker.apply( localProvider );
      }
    } catch ( final RuntimeException e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw e;
    }
  }
}
