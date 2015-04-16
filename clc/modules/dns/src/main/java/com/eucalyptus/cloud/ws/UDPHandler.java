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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright (c) 1999-2004, Brian Wellington.
 *   All rights reserved.
 *
 *   Redistribution and use in source and binary forms, with or without
 *   modification, are permitted provided that the following conditions
 *   are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.cloud.ws;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;
import java.util.concurrent.Callable;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;

import org.apache.log4j.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.id.Dns;
import com.eucalyptus.system.Threads;

@ConfigurableClass( root = "dns.udp",
		description = "Handles udp listeners." )

public class UDPHandler extends Thread {
	@ConfigurableField( description = "Parameter controlling the number of UDP worker threads." )
	public static Integer num_worker_threads = 128;
	private static Logger LOG = Logger.getLogger( UDPHandler.class );

	DatagramSocket socket;
	UDPHandler(DatagramSocket s) {
		this.socket = s;
	}

	public void run() {
		final short udpLength = 512;
		while (Bootstrap.isOperational( )) {
			if ( socket.isClosed( ) ) {
				LOG.info( "Exiting due to closed socket" );
				return;
			}
			try {
			  byte [] in = new byte[udpLength];
			  DatagramPacket indp = new DatagramPacket(in, in.length);
				indp.setLength(in.length);
				try {
					socket.receive(indp);
				}catch (InterruptedIOException e) {
					continue;
				}
				try{
				  Threads.enqueue(Dns.class, UDPHandler.class, num_worker_threads, new UDPWorker(this.socket, indp, in));
				}catch(Exception ex){
				  LOG.error("failed to run dns UDP worker");
				}
			} catch (IOException e) {
			    LOG.trace(e);
			}
		}
	}

	private static class UDPWorker extends ConnectionHandler implements Callable<Boolean>{
	  private DatagramSocket socket = null;
	  private DatagramPacket packet = null;
	  private byte[] data = null;
	  private Long requestedTime = null;
	  private final long DISCARD_AFTER_MS = 10000L;
	  // Most (if not all) DNS client will timeout after 5 seconds.
	  private UDPWorker(final DatagramSocket socket, final DatagramPacket dpin, byte[] data){
	    this.socket = socket;
	    this.packet = dpin;
	    this.data = data;
	    this.requestedTime = (new Date()).getTime();
	  }

	  @Override
	  public Boolean call() throws Exception {
	    // This request already timed-out; don't even try resolving it.
	    if((new Date()).getTime() - this.requestedTime > DISCARD_AFTER_MS) {
	      return false;
	    }
	    
	    final byte[] in = this.data;
	    Message query;
	    byte [] response = null;
	    try {
	      query = new Message(in);
	      ConnectionHandler.setLocalAndRemoteInetAddresses( this.socket.getLocalAddress( ), this.packet.getAddress( ) );
	      try {
	        response = generateReply( query, in,
	            this.packet.getLength( ),
	            null );
	      } catch ( RuntimeException ex ) {
	        response = errorMessage(query, Rcode.SERVFAIL);
	        throw ex;
	      } finally {
	        ConnectionHandler.clearInetAddresses( );
	      }
	      if (response == null)
	        return false;
	    } catch (Exception e) {
	      if ( response != null ) {
	        response = formerrMessage(in);
	      } else {
	        LOG.trace(e);
	        return false;
	      }
	    }
	    try{
	      final DatagramPacket outdp = new DatagramPacket(response,
	          response.length,
	          this.packet.getAddress(),
	          this.packet.getPort());
	      this.socket.send(outdp);
	    }catch(Exception e){
	      LOG.trace(e);
	    }
	    return true;
	  }
	}
}
