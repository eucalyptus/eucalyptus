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
 ************************************************************************/

package com.eucalyptus.objectstorage;


import com.eucalyptus.objectstorage.auth.OsgAuthorizationHandler;
import com.eucalyptus.objectstorage.auth.RequestAuthorizationHandler;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.metadata.BucketLifecycleManager;
import com.eucalyptus.objectstorage.metadata.BucketMetadataManager;
import com.eucalyptus.objectstorage.msgs.GetBucketLifecycleResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLifecycleType;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class ObjectStorageGatewayTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Test
    public void getBucketLifecycleTest() throws Exception {

        final BucketLifecycleManager bucketLifecycleManagerMock
                = context.mock(BucketLifecycleManager.class);
        final BucketMetadataManager bucketManagerMock
                = context.mock(BucketMetadataManager.class);
        final RequestAuthorizationHandler requestAuthorizationHandlerMock
                = context.mock(RequestAuthorizationHandler.class);

        BucketLifecycleManagers.setInstance(bucketLifecycleManagerMock);
        BucketMetadataManagers.setInstance(bucketManagerMock);
        OsgAuthorizationHandler.setInstance(requestAuthorizationHandlerMock);

        final String bucketName = "my-test-bucket";
        final Bucket bucket = new Bucket();
        bucket.setBucketName(bucketName);
        bucket.setState(BucketState.extant);
        final String fakeUuid = "fakeuuid";
        bucket.withUuid(fakeUuid);
        final GetBucketLifecycleType request = new GetBucketLifecycleType();
        request.setBucket(bucketName);

        context.checking(new Expectations() {{
            oneOf(bucketManagerMock).lookupExtantBucket(bucketName);
            will(returnValue(bucket));
            oneOf(requestAuthorizationHandlerMock).operationAllowed(request, bucket, null, 0);
            will(returnValue(true));
            oneOf(bucketLifecycleManagerMock).getLifecycleRules(fakeUuid);
            will(returnValue(Collections.EMPTY_LIST));
        }});

        ObjectStorageGateway osg = new ObjectStorageGateway();
        GetBucketLifecycleResponseType response = osg.getBucketLifecycle(request);
        assertTrue("expected a response", response != null);
        assertTrue("expected response to contain an empty set of lifecycle rules",
                response.getLifecycleConfiguration().getRules().size() == 0);

    }

    @AfterClass
    public static void tearDown() {
        BucketLifecycleManagers.setInstance(null);
        BucketMetadataManagers.setInstance(null);
        OsgAuthorizationHandler.setInstance(null);
    }

    @Test
    public void testCheckBucketName() {
        assert (ObjectStorageGateway.checkBucketNameValidity("bucket"));
        assert (ObjectStorageGateway.checkBucketNameValidity("bucket.bucket.bucket"));
        assert (!ObjectStorageGateway.checkBucketNameValidity("10.100.1.1"));
        assert (ObjectStorageGateway.checkBucketNameValidity("bu_ckeS-adsad"));
        assert (ObjectStorageGateway.checkBucketNameValidity("aou12naofdufan1421_-123oiasd-afasf.asdfas"));
        assert (!ObjectStorageGateway.checkBucketNameValidity("bucke<t>%12313"));
        assert (!ObjectStorageGateway.checkBucketNameValidity("b*u&c%k#et"));
        assert (ObjectStorageGateway.checkBucketNameValidity("bucket_name"));
    }

}
