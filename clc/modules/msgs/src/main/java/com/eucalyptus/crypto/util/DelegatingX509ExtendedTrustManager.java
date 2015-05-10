/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.crypto.util;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import com.eucalyptus.util.CollectionUtils;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 *
 */
public class DelegatingX509ExtendedTrustManager extends X509ExtendedTrustManager {

  private final List<X509ExtendedTrustManager> delegates;
  private final Supplier<Boolean> enabled;

  public DelegatingX509ExtendedTrustManager( final Iterable<X509ExtendedTrustManager> delegates ) {
    this( delegates, Suppliers.ofInstance( true ) );
  }

  public DelegatingX509ExtendedTrustManager(
      final Iterable<X509ExtendedTrustManager> delegates,
      final Supplier<Boolean> enabled
  ) {
    this.delegates = ImmutableList.copyOf( delegates );
    this.enabled = enabled;
  }

  @Override
  public void checkClientTrusted(
      final X509Certificate[] chain,
      final String authType
  ) throws CertificateException {
    checkTrusted( new DelegatedTrustChecker( ) {
      @Override
      public void checkTrusted( final X509ExtendedTrustManager trustManager ) throws CertificateException {
        trustManager.checkClientTrusted( chain, authType );
      }
    } );
  }

  @Override
  public void checkServerTrusted(
      final X509Certificate[] chain,
      final String authType
  ) throws CertificateException {
    checkTrusted( new DelegatedTrustChecker( ) {
      @Override
      public void checkTrusted( final X509ExtendedTrustManager trustManager ) throws CertificateException {
        trustManager.checkServerTrusted( chain, authType );
      }
    } );
  }

  @Override
  public void checkClientTrusted(
      final X509Certificate[] chain,
      final String authType,
      final Socket socket
  ) throws CertificateException {
    checkTrusted( new DelegatedTrustChecker( ) {
      @Override
      public void checkTrusted( final X509ExtendedTrustManager trustManager ) throws CertificateException {
        trustManager.checkClientTrusted( chain, authType, socket );
      }
    } );
  }

  @Override
  public void checkServerTrusted(
      final X509Certificate[] chain,
      final String authType,
      final Socket socket
  ) throws CertificateException {
    checkTrusted( new DelegatedTrustChecker( ) {
      @Override
      public void checkTrusted( final X509ExtendedTrustManager trustManager ) throws CertificateException {
        trustManager.checkServerTrusted( chain, authType, socket );
      }
    } );
  }

  @Override
  public void checkClientTrusted(
      final X509Certificate[] chain,
      final String authType,
      final SSLEngine sslEngine ) throws CertificateException {
    checkTrusted( new DelegatedTrustChecker( ) {
      @Override
      public void checkTrusted( final X509ExtendedTrustManager trustManager ) throws CertificateException {
        trustManager.checkClientTrusted( chain, authType, sslEngine );
      }
    } );
  }

  @Override
  public void checkServerTrusted(
      final X509Certificate[] chain,
      final String authType,
      final SSLEngine sslEngine
  ) throws CertificateException {
    checkTrusted( new DelegatedTrustChecker( ) {
      @Override
      public void checkTrusted( final X509ExtendedTrustManager trustManager ) throws CertificateException {
        trustManager.checkServerTrusted( chain, authType, sslEngine );
      }
    } );
  }

  @Override
  public X509Certificate[] getAcceptedIssuers( ) {
    final Set<X509Certificate> issuers = Sets.newLinkedHashSet( );
    if ( enabled.get( ) ) for ( final X509ExtendedTrustManager delegate : delegates ) {
      issuers.addAll( Arrays.asList( delegate.getAcceptedIssuers( ) ) );
    }
    return issuers.toArray( new X509Certificate[ issuers.size( ) ] );
  }

  private static interface DelegatedTrustChecker {
    void checkTrusted( X509ExtendedTrustManager trustManager ) throws CertificateException;
  }

  private void checkTrusted( final DelegatedTrustChecker checker ) throws CertificateException {
    if ( !enabled.get( ) ) {
      throw new CertificateException( "Delegates disabled" );
    }
    final Optional<CertificateException> resultException = CollectionUtils.reduce(
      delegates,
      Optional.of( new CertificateException( "No delegates" ) ),
      new Function<Optional<CertificateException>,Function<X509ExtendedTrustManager,Optional<CertificateException>>>( ){
        @Override
        public Function<X509ExtendedTrustManager, Optional<CertificateException>> apply( final Optional<CertificateException> previousException ) {
          return new Function<X509ExtendedTrustManager, Optional<CertificateException>>(){
            @Override
            public Optional<CertificateException> apply( final X509ExtendedTrustManager trustManager ) {
              Optional<CertificateException> resultException = Optional.absent( );
              if ( previousException.isPresent( ) ) {
                try {
                  checker.checkTrusted( trustManager );
                } catch ( final CertificateException e ) {
                  resultException = Optional.of( e );
                }
              }
              return resultException;
            }
          };
        }
      }
    );
    if ( resultException.isPresent( ) ) {
      throw resultException.get( );
    }
  }
}
