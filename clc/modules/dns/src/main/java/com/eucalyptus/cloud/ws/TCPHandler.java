/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cloud.ws;

import com.eucalyptus.component.id.Dns;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.DNSProperties;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.apache.log4j.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@ConfigurableClass( root = "dns.tcp",
        description = "Handles dns TCP requests." )
public class TCPHandler extends ConnectionHandler {
  @ConfigurableField( description = "Parameter controlling tcp handler timeout in seconds." )
  public static Integer timeout_seconds = 30;
  private static Logger LOG = Logger.getLogger( TCPHandler.class );

  private final Socket socket;

  public TCPHandler( final Socket s ) {
    this.socket = s;
  }

  public void run() {
    try ( final Socket socket = this.socket ) {
      socket.setSoTimeout( timeout_seconds * 1000 );
      final TimeLimiter limiter = new SimpleTimeLimiter( Threads.lookup( Dns.class, TCPHandler.class ) );
      final byte[] inBytes = limiter.callWithTimeout( new Callable<byte[]>( ) {
        @Override
        public byte[] call() throws Exception {
          final DataInputStream inStream = new DataInputStream(socket.getInputStream());
          final int inputLength = inStream.readUnsignedShort();
          if( inputLength > DNSProperties.MAX_MESSAGE_SIZE ) {
            throw new IOException("Maximum message size exceeded. Ignoring request.");
          }
          byte[] inBytes = new byte[inputLength];
          inStream.readFully(inBytes);
          return inBytes;
        }
      }, timeout_seconds, TimeUnit.SECONDS, false );

      byte [] response = null;
      try {
        final Message query = new Message(inBytes);
        ConnectionHandler.setRemoteInetAddress( socket.getInetAddress( ) );
        try {
          response = generateReply(query, inBytes, inBytes.length, socket);
        } catch ( RuntimeException ex ) {
          response = errorMessage(query, Rcode.SERVFAIL);
          throw ex;
        } finally {
          ConnectionHandler.removeRemoteInetAddress( );
        }
        if (response == null)
          return;
      } catch ( IOException exception ) {
        LOG.error(exception);
      }
      final DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
      outStream.writeShort(response.length);
      outStream.write(response);
      outStream.flush();
    } catch ( UncheckedTimeoutException e ) {
      LOG.debug( "Timeout reading request." );
    } catch(Exception ex) {
      LOG.error(ex);
    }
  }
}
