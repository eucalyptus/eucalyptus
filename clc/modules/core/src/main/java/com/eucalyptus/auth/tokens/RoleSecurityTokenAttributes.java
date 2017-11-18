/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.tokens;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.util.Parameters;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 *
 */
@SuppressWarnings( "Guava" )
public class RoleSecurityTokenAttributes {

  private final String sessionName;

  private RoleSecurityTokenAttributes(
      final String sessionName
  ) {
    Parameters.checkParamNotNullOrEmpty( "sessionName", sessionName );
    this.sessionName = sessionName;
  }

  public static <T extends RoleSecurityTokenAttributes> Optional<T> fromContext( Class<T> type  ) {
    try {
      final Context context = Contexts.lookup( );
      final UserPrincipal principal = context.getUser( );
      if ( principal != null  ) {
        final Optional<RoleSecurityTokenAttributes> attributes = RoleSecurityTokenAttributes.forUser( principal );
        if ( attributes.isPresent( ) && type.isInstance( attributes.get( ) ) ) {
          return Optional.of( type.cast( attributes.get( ) ) );
        }
      }
    } catch ( final IllegalContextAccessException e ) {
      // absent
    }
    return Optional.absent( );

  }

  public static Optional<RoleSecurityTokenAttributes> forUser( final User user ) {
    if ( user instanceof UserPrincipal ) {
      final UserPrincipal principal = (UserPrincipal) user;
      if ( Accounts.isRoleIdentifier( principal.getAuthenticatedId( ) ) ) {
        final List<AccessKey> keys = principal.getKeys( );
        if ( keys.size( ) == 1 ) {
          return forKey( keys.get( 0 ) );
        }
      }
    }
    return Optional.absent( );
  }

  public static Optional<RoleSecurityTokenAttributes> forKey( final AccessKey accessKey ) {
    if ( accessKey instanceof TemporaryAccessKey ) {
      final TemporaryAccessKey temporaryAccessKey = (TemporaryAccessKey) accessKey;
      if ( temporaryAccessKey.getType( ) == TemporaryAccessKey.TemporaryKeyType.Role ) {
        return forMap( temporaryAccessKey.getAttributes( ) );
      }
    }
    return Optional.absent( );
  }

  public static Optional<RoleSecurityTokenAttributes> forMap( final Map<String,String> attributes ) {
    final String sessionName = attributes.get( "ses" );
    final String instanceArn = attributes.get( "ins" );
    final String providerUrl = attributes.get( "url" );
    final String aud = attributes.get( "aud" );
    final String sub = attributes.get( "sub" );
    try {
      if ( instanceArn != null ) {
        return Optional.of( instance( sessionName, instanceArn ) );
      } else if ( providerUrl != null ) {
        return Optional.of( webIdentity( sessionName, providerUrl, aud, sub ) );
      } else if ( sessionName != null ) {
        return Optional.of( basic( sessionName ) );
      }
    } catch ( final IllegalArgumentException e ) {
      // so absent
    }
    return Optional.absent( );
  }

  public static RoleSecurityTokenAttributes basic(
      @Nonnull final String sessionName
  ) {
    return new RoleSecurityTokenAttributes( sessionName );
  }

  public static RoleSecurityTokenAttributes instance(
      @Nonnull final String sessionName,
      @Nonnull final String instanceArn
  ) {
    return new RoleInstanceProfileSecurityTokenAttributes( sessionName, instanceArn );
  }

  public static RoleSecurityTokenAttributes webIdentity(
      @Nonnull final String sessionName,
      @Nonnull final String providerUrl,
      @Nonnull final String aud,
      @Nonnull final String sub
  ) {
    return new RoleWithWebIdSecurityTokenAttributes( sessionName, providerUrl, aud, sub );
  }

  public final String getSessionName( ) {
    return sessionName;
  }

  public final Map<String,String> asMap( ) {
    return populate( ImmutableMap.builder( ) ).build( );
  }

  protected ImmutableMap.Builder<String, String> populate( final ImmutableMap.Builder<String, String> builder ) {
    return builder.put( "ses", sessionName );
  }

  public static class RoleInstanceProfileSecurityTokenAttributes extends RoleSecurityTokenAttributes {
    private final String instanceArn;

    public RoleInstanceProfileSecurityTokenAttributes(
        final String sessionName,
        final String instanceArn
    ) {
      super( sessionName );
      Parameters.checkParamNotNullOrEmpty( "instanceArn", instanceArn );
      this.instanceArn = instanceArn;
    }

    @Override
    protected ImmutableMap.Builder<String, String> populate( final ImmutableMap.Builder<String, String> builder ) {
      return super.populate( builder )
          .put( "ins", instanceArn );
    }

    public String getInstanceArn( ) {
      return instanceArn;
    }
  }

  public static class RoleWithWebIdSecurityTokenAttributes extends RoleSecurityTokenAttributes {
    private final String providerUrl;
    private final String aud;
    private final String sub;

    public RoleWithWebIdSecurityTokenAttributes(
        final String sessionName,
        final String providerUrl,
        final String aud,
        final String sub
    ) {
      super( sessionName );
      Parameters.checkParamNotNullOrEmpty( "providerUrl", providerUrl );
      Parameters.checkParamNotNullOrEmpty( "aud", aud );
      Parameters.checkParamNotNullOrEmpty( "sub", sub);
      this.providerUrl = providerUrl;
      this.aud = aud;
      this.sub = sub;
    }

    @Override
    protected ImmutableMap.Builder<String, String> populate( final ImmutableMap.Builder<String, String> builder ) {
      return super.populate( builder )
          .put( "url", providerUrl )
          .put( "aud", aud )
          .put( "sub", sub );
    }

    public String getProviderUrl( ) {
      return providerUrl;
    }

    public String getAud( ) {
      return aud;
    }

    public String getSub( ) {
      return sub;
    }
  }
}
