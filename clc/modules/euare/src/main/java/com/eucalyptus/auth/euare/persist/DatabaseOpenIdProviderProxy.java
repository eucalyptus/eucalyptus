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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.euare.checker.InvalidValueException;
import com.eucalyptus.auth.euare.checker.ValueChecker;
import com.eucalyptus.auth.euare.checker.ValueCheckerFactory;
import com.eucalyptus.auth.euare.persist.entities.AccountEntity_;
import com.eucalyptus.auth.euare.persist.entities.OpenIdProviderEntity;
import com.eucalyptus.auth.euare.persist.entities.OpenIdProviderEntity_;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareOpenIdConnectProvider;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Tx;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

/**
 * OpenIdConnectProvider implementation backed by OpenIdProviderEntity
 */
public class DatabaseOpenIdProviderProxy implements EuareOpenIdConnectProvider {

  private static final long serialVersionUID = 1L;

  private static Logger LOG = Logger.getLogger( DatabaseOpenIdProviderProxy.class );

  private OpenIdProviderEntity delegate;
  private transient Supplier<String> accountNumberSupplier =
      DatabaseAuthUtils.getAccountNumberSupplier( this );

  public DatabaseOpenIdProviderProxy( OpenIdProviderEntity delegate ) {
    this.delegate = delegate;
  }

  @Override
  public String getName() {
    return this.delegate.getUrl();
  }

  @Override
  public String getDisplayName() {
    return this.delegate.getUrl();
  }

  @Override
  public OwnerFullName getOwner() {
    try {
      return AccountFullName.getInstance( getAccount().getAccountNumber() );
    } catch ( AuthException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      DatabaseAuthUtils.invokeUnique( OpenIdProviderEntity.class, OpenIdProviderEntity_.url, this.delegate.getUrl(), new Tx<OpenIdProviderEntity>( ) {
        @Override
        public void fire( OpenIdProviderEntity t ) {
          sb.append( t.toString( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to toString for " + this.delegate );
    }
    return sb.toString();
  }

  @Override
  public String getUrl() {
    return this.delegate.getUrl();
  }

  @Override
  public void setUrl( String url ) {
    this.delegate.setUrl(url);
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
    return DatabaseAuthUtils.extract( accountNumberSupplier );
  }

  public EuareAccount getAccount( ) throws AuthException {
    if ( Entities.isReadable( delegate.getAccount( ) ) ) {
      return new DatabaseAccountProxy( delegate.getAccount( ) );  
    } else {
      final List<EuareAccount> results = Lists.newArrayList( );
      dbCallback( "getAccount", new Callback<OpenIdProviderEntity>( ) {
        @Override
        public void fire( final OpenIdProviderEntity openidproviderEntity ) {
          results.add( new DatabaseAccountProxy( openidproviderEntity.getAccount( ) ) );
        }
      } );
      return results.get( 0 );
    }
  }

  private void check( ValueChecker checker, String error, String value ) throws AuthException {
    try {
      checker.check( value );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, error + " " + value );
      throw new AuthException( error, e );
    }
  }

  private void dbCallback( final String description,
                           final Callback<OpenIdProviderEntity> updateCallback ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( OpenIdProviderEntity.class, OpenIdProviderEntity_.url, getUrl(), new Tx<OpenIdProviderEntity>( ) {
        @Override
        public void fire( final OpenIdProviderEntity openidproviderEntity ) {
          updateCallback.fire( openidproviderEntity );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to " + description + " for " + this.delegate );
      throw new AuthException( e );
    }
  }

  private OpenIdProviderEntity getOpenIdProviderEntity( ) throws Exception {
    return DatabaseAuthUtils.getUnique( OpenIdProviderEntity.class, OpenIdProviderEntity_.url, getUrl() );
  }

  private void readObject( ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject( );
    this.accountNumberSupplier = DatabaseAuthUtils.getAccountNumberSupplier( this );
  }
}
