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
package com.eucalyptus.database.activities;

import static com.eucalyptus.crypto.util.SslUtils.getEnabledCipherSuites;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.toArray;
import static java.lang.System.getProperty;

import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.DatabaseInfo;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.crypto.util.SSLSocketFactoryWrapper;
import com.eucalyptus.crypto.util.SimpleClientX509TrustManager;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Charsets;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;

/**
 * @author Sang-Min Park
 *
 */
public class VmDatabaseSSLSocketFactory extends SSLSocketFactoryWrapper {
  private static Logger  LOG = Logger.getLogger( VmDatabaseSSLSocketFactory.class );

  private static final String PROP_VMDB_SSL_PROVIDER = "com.eucalyptus.database.vmSslProvider";
  private static final String PROP_VMDB_SSL_PROTOCOL = "com.eucalyptus.database.vmSslProtocol";
  private static final String PROP_VMDB_SSL_CIPHER_SUITES = "com.eucalyptus.database.vmSslCipherSuites";
  private static final String DEFAULT_CIPHER_STRINGS = "RSA+AES:+SHA:!EXPORT:!EXPORT1025:!MD5";
  private static final String DEFAULT_PROTOCOL = "TLS";
  private final List<String> cipherSuites;
  
  public VmDatabaseSSLSocketFactory() {
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
    return getProperty( PROP_VMDB_SSL_CIPHER_SUITES, DEFAULT_CIPHER_STRINGS );
  }

  private static SSLSocketFactory buildDelegate() {
    final String provider = getProperty( PROP_VMDB_SSL_PROVIDER );
    final String protocol = getProperty( PROP_VMDB_SSL_PROTOCOL, DEFAULT_PROTOCOL );

    final X509Certificate certificate;
    try{
      String bodyPem = DatabaseInfo.getDatabaseInfo().getAppendOnlySslCert();
      bodyPem  = bodyPem.trim();
      certificate = PEMFiles.getCert(bodyPem.getBytes( Charsets.UTF_8 ));
    }catch(final Exception ex){
      LOG.error("Failed to read the server certificate", ex);
      throw Exceptions.toUndeclared(ex);
    }
    
    final SSLContext sslContext;
    try {
      sslContext = provider!=null ?
          SSLContext.getInstance( protocol, provider ) :
          SSLContext.getInstance( protocol );
      final TrustManager trustManager =
          new SimpleClientX509TrustManager(certificate, false);
      sslContext.init( null, new TrustManager[]{ trustManager }, Crypto.getSecureRandomSupplier( ).get( ) );
      return sslContext.getSocketFactory();
    } catch ( NoSuchAlgorithmException | KeyManagementException | NoSuchProviderException e) {
      throw propagate(e);
    }
  }
}
