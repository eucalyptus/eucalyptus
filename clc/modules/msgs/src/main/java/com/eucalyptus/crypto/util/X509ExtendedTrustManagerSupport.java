/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
