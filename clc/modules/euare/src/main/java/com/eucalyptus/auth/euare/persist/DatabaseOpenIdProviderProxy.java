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
package com.eucalyptus.auth.euare.persist;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.euare.persist.entities.OpenIdProviderEntity;
import com.eucalyptus.auth.euare.persist.entities.OpenIdProviderEntity_;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareOpenIdConnectProvider;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

/**
 * OpenIdConnectProvider implementation backed by OpenIdProviderEntity
 */
@SuppressWarnings( "WeakerAccess" )
public class DatabaseOpenIdProviderProxy implements EuareOpenIdConnectProvider {

  private static Logger LOG = Logger.getLogger( DatabaseOpenIdProviderProxy.class );

  private OpenIdProviderEntity delegate;

  @SuppressWarnings( "WeakerAccess" )
  public DatabaseOpenIdProviderProxy( final OpenIdProviderEntity delegate ) {
    this.delegate = delegate;
  }

  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      dbCallback( "toString", openIDProviderEntity -> sb.append( openIDProviderEntity.toString( ) ) );
    } catch ( AuthException e ) {
      Debugging.logError( LOG, e, "Failed to toString for " + this.delegate );
    }
    return sb.toString();
  }

  @Override
  public String getArn() throws AuthException {
    return Accounts.getOpenIdConnectProviderArn( this );
  }

  @Override
  public String getUrl() {
    return this.delegate.getUrl();
  }

  @Override
  public String getHost() {
    return this.delegate.getHost();
  }

  @Override
  public Integer getPort( ) {
    return this.delegate.getPort();
  }

  @Override
  public String getPath() {
    return this.delegate.getPath();
  }

  @Override
  public List<String> getThumbprints() {
    return this.delegate.getThumbprints();
  }

  @Override
  public List<String> getClientIds() {
    return this.delegate.getClientIDs();
  }

  @Override
  public Date getCreationTimestamp() {
    return delegate.getCreationTimestamp();
  }

  @Override
  public String getAccountNumber() throws AuthException {
    return getAccount( ).getAccountNumber( );
  }

  public EuareAccount getAccount( ) throws AuthException {
    if ( Entities.isReadable( delegate.getAccount( ) ) ) {
      return new DatabaseAccountProxy( delegate.getAccount( ) );  
    } else {
      final List<EuareAccount> results = Lists.newArrayList( );
      dbCallback(
          "getAccount",
          openIDProviderEntity -> results.add( new DatabaseAccountProxy( openIDProviderEntity.getAccount( ) ) ) );
      return results.get( 0 );
    }
  }

  private void dbCallback( final String description,
                           final Callback<OpenIdProviderEntity> updateCallback ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique(
          OpenIdProviderEntity.class,
          OpenIdProviderEntity_.id,
          delegate.getEntityId( ),
          (Tx<OpenIdProviderEntity>) updateCallback::fire );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to " + description + " for " + this.delegate );
      throw new AuthException( e );
    }
  }
}
