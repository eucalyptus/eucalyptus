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

package edu.ucsb.eucalyptus.cloud.ws.tests;

import edu.ucsb.eucalyptus.cloud.ws.Storage;
import edu.ucsb.eucalyptus.keys.Hashes;
import edu.ucsb.eucalyptus.msgs.CreateStorageSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.CreateStorageSnapshotType;
import edu.ucsb.eucalyptus.util.WalrusProperties;
import junit.framework.TestCase;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;

import java.util.Date;

public class CreateSnapshotTest extends TestCase {

    static Storage storage;
    public void testCreateSnapshot() throws Throwable {

        storage = new Storage();

        String userId = "admin";

        String volumeId = "vol-yCqCbrweuVviYQxx";
        String snapshotId = "snap-" + Hashes.getRandom(10);

        CreateStorageSnapshotType createSnapshotRequest = new CreateStorageSnapshotType();

        createSnapshotRequest.setUserId(userId);
        createSnapshotRequest.setVolumeId(volumeId);
        createSnapshotRequest.setSnapshotId(snapshotId);
        CreateStorageSnapshotResponseType createSnapshotResponse = storage.CreateStorageSnapshot(createSnapshotRequest);
        System.out.println(createSnapshotResponse);

        while(true);
    }

    public void testTransferSnapshot() throws Throwable {
        storage = new Storage();


        String volumeId = "vol-yCqCbrweuVviYQxx";
        String snapshotId = "snap-zVl2kZJmjhxnEg..";
        String dupSnapshotId = "snap-zVl2kZJmjhxnEg...SrZ5iA..";

        storage.transferSnapshot(volumeId, snapshotId, dupSnapshotId, true);
        while(true);
    }

    public void testSendDummy() throws Throwable {
        HttpClient httpClient = new HttpClient();
        String addr = System.getProperty(WalrusProperties.URL_PROPERTY) + "/meh/ttt.wsl?gg=vol&hh=snap";

        HttpMethodBase method = new PutMethod(addr);
        method.setRequestHeader("Authorization", "Euca");
        method.setRequestHeader("Date", (new Date()).toString());
        method.setRequestHeader("Expect", "100-continue");

        httpClient.executeMethod(method);
        String responseString = method.getResponseBodyAsString();
        System.out.println(responseString);
        method.releaseConnection();
    }

    public void testGetSnapshotInfo() throws Throwable {
        HttpClient httpClient = new HttpClient();
        String addr = System.getProperty(WalrusProperties.URL_PROPERTY) + "/snapset-FuXLn1MUHJ66BkK0/snap-zVl2kZJmjhxnEg..";

        HttpMethodBase method = new GetMethod(addr);
        method.setRequestHeader("Authorization", "Euca");
        method.setRequestHeader("Date", (new Date()).toString());
        method.setRequestHeader("Expect", "100-continue");
        method.setRequestHeader("EucaOperation", "GetSnapshotInfo");
        httpClient.executeMethod(method);
        String responseString = method.getResponseBodyAsString();
        System.out.println(responseString);
        method.releaseConnection();         
    }

    public CreateSnapshotTest() {
        super();
    }

}