/* Copyright (c) 2001-2008, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hsqldb;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.eucalyptus.auth.util.SslSetup;

/**
 * We need to override the way that hsqldb gets its secure sockets to use our internally mananged credentials.
 */
public final class HsqlSocketFactorySecure extends HsqlSocketFactory {
  protected SSLSocketFactory       socketFactory;
  protected SSLServerSocketFactory serverSocketFactory;
  
  protected HsqlSocketFactorySecure( ) throws Exception {}
  
  public void configureSocket( Socket socket ) {
    super.configureSocket( socket );
  }
  
  public ServerSocket createServerSocket( int port ) throws Exception {
    return ( SSLServerSocket ) getServerSocketFactoryImpl( ).createServerSocket( port );
  }
  
  public ServerSocket createServerSocket( int port, String address ) throws Exception {
    InetAddress addr = InetAddress.getByName( address );
    return ( SSLServerSocket ) getServerSocketFactoryImpl( ).createServerSocket( port, 128, addr );
  }
  
  public Socket createSocket( String host, int port ) throws Exception {
    SSLSocket socket = ( SSLSocket ) getSocketFactoryImpl( ).createSocket( host, port );
    socket.startHandshake( );
    return socket;
  }
  
  public boolean isSecure( ) {
    return true;
  }
  
  protected SSLServerSocketFactory getServerSocketFactoryImpl( ) throws Exception {
    synchronized ( HsqlSocketFactorySecure.class ) {
      if ( serverSocketFactory == null ) {
        serverSocketFactory = SslSetup.getServerContext( ).getServerSocketFactory( );
      }
    }
    return ( SSLServerSocketFactory ) serverSocketFactory;
  }
  
  protected SSLSocketFactory getSocketFactoryImpl( ) throws Exception {
    synchronized ( HsqlSocketFactorySecure.class ) {
      if ( socketFactory == null ) {
        socketFactory = SslSetup.getClientContext( ).getSocketFactory( );
      }
    }
    return ( SSLSocketFactory ) socketFactory;
  }
  
}
