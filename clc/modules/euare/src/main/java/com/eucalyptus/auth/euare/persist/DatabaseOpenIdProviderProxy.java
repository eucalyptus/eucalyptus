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
package com.eucalyptus.auth.euare.persist;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.euare.persist.entities.OpenIdProviderEntity;
import com.eucalyptus.auth.euare.persist.entities.OpenIdProviderEntity_;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareOpenIdConnectProvider;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

/**
 * OpenIdConnectProvider implementation backed by OpenIdProviderEntity
 */
public class DatabaseOpenIdProviderProxy implements EuareOpenIdConnectProvider {

  private static final long serialVersionUID = 1L;

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
