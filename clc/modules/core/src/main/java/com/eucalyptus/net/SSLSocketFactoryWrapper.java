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

package com.eucalyptus.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLSocketFactory;
import com.google.common.base.Supplier;

/**
 * Wrapper for SSLSocketFactory.
 */
public class SSLSocketFactoryWrapper extends SSLSocketFactory {

  private final Supplier<SSLSocketFactory> delegate;

  public SSLSocketFactoryWrapper( final Supplier<SSLSocketFactory> delegate ) {
    this.delegate = delegate;
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return delegate().getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return delegate().getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket( final Socket socket,
                              final String host,
                              final int port,
                              final boolean autoClose ) throws IOException {
    return notifyCreated(delegate().createSocket(socket, host, port, autoClose));
  }

  @Override
  public Socket createSocket( final String host,
                              final int port ) throws IOException {
    return notifyCreated(delegate().createSocket(host, port));
  }

  @Override
  public Socket createSocket( final String host,
                              final int port,
                              final InetAddress localHost,
                              final int localPort ) throws IOException {
    return notifyCreated(delegate().createSocket(host, port, localHost, localPort));
  }

  @Override
  public Socket createSocket( final InetAddress host,
                              final int port ) throws IOException {
    return notifyCreated(delegate().createSocket(host, port));
  }

  @Override
  public Socket createSocket( final InetAddress address,
                              final int port,
                              final InetAddress localAddress,
                              final int localPort ) throws IOException {
    return notifyCreated(delegate().createSocket(address, port, localAddress, localPort));
  }

  protected Socket notifyCreated( final Socket socket ) {
    return socket;
  }

  private SSLSocketFactory delegate() {
    return delegate.get();
  }
}
