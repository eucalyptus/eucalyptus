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
package com.eucalyptus.compute.common.internal.account;

import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 *
 */
public class ComputeAccounts {

  private static final Logger logger = Logger.getLogger( ComputeAccounts.class );

  private static final Cache<String,Object> accountCache = CacheBuilder
      .<String,Object>newBuilder( )
      .maximumSize( 1_000 )
      .expireAfterWrite( 1, TimeUnit.HOURS )
      .build( );

  private static final Iterable<ComputeAccountInitializer> initializers =
      ServiceLoader.load( ComputeAccountInitializer.class );

  public static void ensureInitialized( final String accountNumber ) {
    if ( accountCache.getIfPresent( accountNumber ) == null ) {
      boolean initialized = false;
      synchronized ( sync( accountNumber ) ) {
        if ( accountCache.getIfPresent( accountNumber ) == null ) {
          try ( final TransactionResource tx = Entities.transactionFor( ComputeAccount.class ) ) {
            try {
              Entities.uniqueResult( ComputeAccount.exampleWithAccountNumber( accountNumber ) );
            } catch ( NoSuchElementException e ) {
              Entities.persist( ComputeAccount.create( accountNumber ) );
              tx.commit( );
              initialized = true;
            }
            accountCache.put( accountNumber, accountNumber );
          } catch ( TransactionException e ) {
            logger.error( "Error checking for account initialization " + accountNumber, e );
          }
        }
      }
      if ( initialized ) {
        initialize( accountNumber );
      }
    }
  }

  private static Object sync( final String accountNumber ) {
    return ( "account-create-sync-" + accountNumber ).intern( );
  }

  private static void initialize( final String accountNumber ) {
    for ( final ComputeAccountInitializer initializer : initializers ) {
      try {
        initializer.initialize( accountNumber );
      } catch ( final Exception e ) {
        logger.error( "Error in account initializer", e );
      }
    }
  }

  public interface ComputeAccountInitializer {
    void initialize( String accountNumber );
  }
}
