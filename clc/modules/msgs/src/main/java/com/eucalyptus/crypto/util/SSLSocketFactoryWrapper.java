/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.crypto.util;

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
