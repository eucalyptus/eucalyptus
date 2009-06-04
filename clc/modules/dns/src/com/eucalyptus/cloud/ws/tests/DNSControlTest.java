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
 */

package com.eucalyptus.cloud.ws.tests;

import edu.ucsb.eucalyptus.msgs.*;
import com.eucalyptus.cloud.ws.DNSControl;
import junit.framework.TestCase;

public class DNSControlTest extends TestCase {

    private static DNSControl dnsControl;

	public void testAddARecord() throws Throwable {
        String userId = "admin";
        UpdateARecordType request = new UpdateARecordType();
        request.setUserId(userId);
        request.setAddress("192.168.7.1");
        request.setName("rich.walrus.localhost.");
        request.setTtl(604800);
        request.setZone("localhost.");
        UpdateARecordResponseType reply = dnsControl.UpdateARecord(request);
        System.out.println(reply);
	}

    public void setUp() {
        dnsControl = new DNSControl();
    }
}
