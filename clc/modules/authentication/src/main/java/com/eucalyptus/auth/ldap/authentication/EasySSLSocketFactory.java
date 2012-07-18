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
 ************************************************************************/

package com.eucalyptus.auth.ldap.authentication;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.log4j.Logger;

/**
 * An SSLSocketFactory that ignores certificate validation.
 */
public class EasySSLSocketFactory extends SSLSocketFactory {
  
  public static class DummyTrustManager implements X509TrustManager {

    @Override
    public void checkClientTrusted( X509Certificate[] arg0, String arg1 ) throws CertificateException {
      // do nothing
    }

    @Override
    public void checkServerTrusted( X509Certificate[] arg0, String arg1 ) throws CertificateException {
      // do nothing
    }

    @Override
    public X509Certificate[] getAcceptedIssuers( ) {
      return new X509Certificate[0];
    }
    
  }
  
  public static SocketFactory getDefault( ) {
    return new EasySSLSocketFactory( );
  }
  
  private static final Logger LOG = Logger.getLogger( EasySSLSocketFactory.class );
  
  private SSLSocketFactory socketFactory;
  
  public EasySSLSocketFactory( ) {
    try {
      SSLContext ctx = SSLContext.getInstance( "TLS" );
      ctx.init( null, new TrustManager[]{ new DummyTrustManager( ) }, new SecureRandom( ) );
      socketFactory = ctx.getSocketFactory( );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
  }
  @Override
  public Socket createSocket( Socket socket, String host, int port, boolean autoClose ) throws IOException {
    return socketFactory != null ? socketFactory.createSocket( socket, host, port, autoClose ) : null;
  }
  
  @Override
  public String[] getDefaultCipherSuites( ) {
    return socketFactory != null ? socketFactory.getDefaultCipherSuites( ) : null;
  }
  
  @Override
  public String[] getSupportedCipherSuites( ) {
    return socketFactory != null ? socketFactory.getSupportedCipherSuites( ) : null;
  }
  
  @Override
  public Socket createSocket( String host, int port ) throws IOException, UnknownHostException {
    return socketFactory != null ? socketFactory.createSocket( host, port ) : null;
  }
  
  @Override
  public Socket createSocket( InetAddress host, int port ) throws IOException {
    return socketFactory != null ? socketFactory.createSocket( host, port ) : null;
  }
  
  @Override
  public Socket createSocket( String host, int port, InetAddress localHost, int localPort ) throws IOException, UnknownHostException {
    return socketFactory != null ? socketFactory.createSocket( host, port, localHost, localPort ) : null;
  }
  
  @Override
  public Socket createSocket( InetAddress address, int port, InetAddress localAddress, int localPort ) throws IOException {
    return socketFactory != null ? socketFactory.createSocket( address, port, localAddress, localPort ) : null;
  }
  
}
