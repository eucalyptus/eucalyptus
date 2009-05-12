/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2009, Eucalyptus Systems, Inc.
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Neil Soman neil@eucalyptus.com
 *
 * Parts of this code are licensed under the BSD license and carry the following copyright,
 * Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)
 * @author Brian Wellington &lt;bwelling@xbill.org&gt;         
 *
 */

package edu.ucsb.eucalyptus.cloud.ws;

import org.xbill.DNS.Message;
import org.apache.log4j.Logger;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.InterruptedIOException;
import java.io.IOException;


public class UDPHandler extends ConnectionHandler {
    private static Logger LOG = Logger.getLogger( UDPHandler.class );

    DatagramSocket socket;
    public UDPHandler(DatagramSocket s) {
        this.socket = s;
    }

    public void run() {
        try {
            final short udpLength = 512;
            byte [] in = new byte[udpLength];
            DatagramPacket indp = new DatagramPacket(in, in.length);
            DatagramPacket outdp = null;
            while (true) {
                indp.setLength(in.length);
                try {
                    socket.receive(indp);
                }
                catch (InterruptedIOException e) {
                    continue;
                }
                Message query;
                byte [] response = null;
                try {
                    query = new Message(in);
                    response = generateReply(query, in,
                            indp.getLength(),
                            null);
                    if (response == null)
                        continue;
                }
                catch (IOException e) {
                    response = formerrMessage(in);
                }
                if (outdp == null)
                    outdp = new DatagramPacket(response,
                            response.length,
                            indp.getAddress(),
                            indp.getPort());
                else {
                    outdp.setData(response);
                    outdp.setLength(response.length);
                    outdp.setAddress(indp.getAddress());
                    outdp.setPort(indp.getPort());
                }
                socket.send(outdp);
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }

    }
}