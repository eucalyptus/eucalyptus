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
package com.eucalyptus.crypto.util;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;

/**
 *
 */
public class X509ExtendedTrustManagerSupport extends X509ExtendedTrustManager {

  @Override
  public void checkServerTrusted( final X509Certificate[] x509Certificates, final String s, final Socket socket ) throws CertificateException {
    throw new CertificateException( "Server cert not trusted" );
  }

  @Override
  public void checkServerTrusted( final X509Certificate[] x509Certificates, final String s, final SSLEngine sslEngine ) throws CertificateException {
    throw new CertificateException( "Server cert not trusted" );
  }

  @Override
  public void checkServerTrusted( final X509Certificate[] x509Certificates, final String s ) throws CertificateException {
    throw new CertificateException( "Server cert not trusted" );
  }

  @Override
  public void checkClientTrusted( final X509Certificate[] x509Certificates, final String s, final Socket socket ) throws CertificateException {
    throw new CertificateException( "Client cert not trusted" );
  }

  @Override
  public void checkClientTrusted( final X509Certificate[] x509Certificates, final String s, final SSLEngine sslEngine ) throws CertificateException {
    throw new CertificateException( "Client cert not trusted" );
  }

  @Override
  public void checkClientTrusted( final X509Certificate[] x509Certificates, final String s ) throws CertificateException {
    throw new CertificateException( "Client cert not trusted" );
  }

  @Override
  public X509Certificate[] getAcceptedIssuers( ) {
    return new X509Certificate[ 0 ];
  }
}
