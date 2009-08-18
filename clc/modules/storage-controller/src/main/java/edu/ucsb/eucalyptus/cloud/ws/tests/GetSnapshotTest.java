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

import com.eucalyptus.auth.Hashes;

import edu.ucsb.eucalyptus.cloud.ws.BlockStorage;
import edu.ucsb.eucalyptus.cloud.ws.WalrusControl;
import edu.ucsb.eucalyptus.msgs.ListAllMyBucketsResponseType;
import edu.ucsb.eucalyptus.msgs.ListAllMyBucketsType;
import junit.framework.TestCase;

public class GetSnapshotTest extends TestCase {

    static BlockStorage blockStorage;
    static WalrusControl bukkit;

    public void testGetSnapshot() throws Throwable {

        String userId = "admin";
        ListAllMyBucketsType listBucketsRequest = new ListAllMyBucketsType();

        listBucketsRequest.setUserId(userId);
        ListAllMyBucketsResponseType response =  bukkit.ListAllMyBuckets(listBucketsRequest);
        System.out.println(response);

        String volumeId = "vol-" + Hashes.getRandom(10);
        String snapshotBucket = "snapset-1234"; //irrelevant
        String snapshotId = "snap-" + Hashes.getRandom(10);
        snapshotId = "snap-xsO2qaH9xGSxYQ..";

        blockStorage.GetSnapshots(volumeId, snapshotBucket, snapshotId);


//        if ( EntityWrapper.getEntityManagerFactory().isOpen() )
  //   EntityWrapper.getEntityManagerFactory().close();
    }
    public void setUp() {
        blockStorage = new BlockStorage();
        bukkit = new WalrusControl();
    }
}