/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.postgresql;

import static java.lang.System.getProperty;
import static com.eucalyptus.component.auth.SystemCredentials.lookup;
import static com.eucalyptus.component.id.Eucalyptus.Database;
import static com.eucalyptus.crypto.util.SslSetup.getEnabledCipherSuites;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.toArray;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import com.eucalyptus.net.SSLSocketFactoryWrapper;
import com.eucalyptus.net.SimpleClientX509TrustManager;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;

/**
 * SSLSocketFactory for use with PostgreSQL.
 *
 * <p>This expects the server to use the certificate with the "db" alias.</p>
 */
public class PostgreSQLSSLSocketFactory extends SSLSocketFactoryWrapper {

  private static final String PROP_POSTGRESQL_SSL_PROVIDER = "com.eucalyptus.postgresql.sslProvider";
  private static final String PROP_POSTGRESQL_SSL_PROTOCOL = "com.eucalyptus.postgresql.sslProtocol";
  private static final String PROP_POSTGRESQL_SSL_CIPHER_SUITES = "com.eucalyptus.postgresql.sslCipherSuites";
  private static final String DEFAULT_CIPHER_STRINGS = "RSA+AES:+SHA:!EXPORT:!EXPORT1025:!MD5";
  private static final String DEFAULT_PROTOCOL = "TLS";
  private final List<String> cipherSuites;

  public PostgreSQLSSLSocketFactory() {
    super(Suppliers.ofInstance(buildDelegate()) );

    this.cipherSuites = ImmutableList.copyOf(getEnabledCipherSuites(getCipherStrings(), getSupportedCipherSuites()));
  }

  @Override
  protected Socket notifyCreated(final Socket socket) {
    if ( socket instanceof SSLSocket) {
      final SSLSocket sslSocket = (SSLSocket) socket;
      sslSocket.setEnabledCipherSuites( toArray(cipherSuites,String.class) );
    }
    return socket;
  }

  private static String getCipherStrings() {
    return getProperty( PROP_POSTGRESQL_SSL_CIPHER_SUITES, DEFAULT_CIPHER_STRINGS );
  }

  private static SSLSocketFactory buildDelegate() {
    final String provider = getProperty( PROP_POSTGRESQL_SSL_PROVIDER );
    final String protocol = getProperty( PROP_POSTGRESQL_SSL_PROTOCOL, DEFAULT_PROTOCOL );

    final SSLContext sslContext;
    try {
      sslContext = provider!=null ?
          SSLContext.getInstance( protocol, provider ) :
          SSLContext.getInstance( protocol );
      final TrustManager trustManager =
          new SimpleClientX509TrustManager(lookup(Database.class).getCertificate(), false);
      sslContext.init( null, new TrustManager[]{ trustManager }, null );
      return sslContext.getSocketFactory();
    } catch ( NoSuchAlgorithmException e ) {
      throw propagate(e);
    } catch (KeyManagementException e) {
      throw propagate(e);
    } catch (NoSuchProviderException e) {
      throw propagate(e);
    }
  }
}
