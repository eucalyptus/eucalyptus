/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
