/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
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
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import org.apache.log4j.Logger;
import edu.ucsb.eucalyptus.msgs.UpdateARecordResponseType;
import edu.ucsb.eucalyptus.msgs.UpdateARecordType;
import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.util.DNSProperties;

public class DNSControl {

    private static Logger LOG = Logger.getLogger( DNSControl.class );
    static {
        initializeDNS();
    }

    private static void initializeUDP() {
        UDPHandler udpHandler = new UDPHandler(DNSProperties.ADDRESS, DNSProperties.PORT);
        udpHandler.start();
    }

    private static void initializeTCP() {
        TCPHandler tcpHandler = new TCPHandler(DNSProperties.ADDRESS, DNSProperties.PORT);
        tcpHandler.start();
    }

    private static void initializeDNS() {
        initializeUDP();
        initializeTCP();
    }

    public DNSControl() {}

    public UpdateARecordResponseType UpdateARecord(UpdateARecordType request)  throws EucalyptusCloudException {
        UpdateARecordResponseType reply = (UpdateARecordResponseType) request.getReply();
        String name = request.getName();
        String address = request.getAddress();
        int ttl = request.getTtl();
        return reply;
    }
}
