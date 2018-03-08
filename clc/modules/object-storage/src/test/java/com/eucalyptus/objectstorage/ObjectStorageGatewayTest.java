/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

import com.eucalyptus.objectstorage.auth.OsgAuthorizationHandler;
import com.eucalyptus.objectstorage.auth.RequestAuthorizationHandler;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.metadata.BucketLifecycleManager;
import com.eucalyptus.objectstorage.metadata.BucketMetadataManager;
import com.eucalyptus.objectstorage.metadata.BucketNameValidatorRepo;
import com.eucalyptus.objectstorage.metadata.Validator;
import com.eucalyptus.objectstorage.msgs.GetBucketLifecycleResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLifecycleType;

public class ObjectStorageGatewayTest {

  @Rule
  public JUnitRuleMockery context = new JUnitRuleMockery();

  @Test
  public void getBucketLifecycleTest() throws Exception {

    final BucketLifecycleManager bucketLifecycleManagerMock = context.mock(BucketLifecycleManager.class);
    final BucketMetadataManager bucketManagerMock = context.mock(BucketMetadataManager.class);
    final RequestAuthorizationHandler requestAuthorizationHandlerMock = context.mock(RequestAuthorizationHandler.class);

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

    context.checking(new Expectations() {
      {
        oneOf(bucketManagerMock).lookupExtantBucket(bucketName);
        will(returnValue(bucket));
        oneOf(requestAuthorizationHandlerMock).operationAllowed(request, bucket, null, 0);
        will(returnValue(true));
        oneOf(bucketLifecycleManagerMock).getLifecycleRules(fakeUuid);
        will(returnValue(Collections.EMPTY_LIST));
      }
    });

    ObjectStorageGateway osg = new ObjectStorageGateway();
    GetBucketLifecycleResponseType response = osg.getBucketLifecycle(request);
    assertTrue("expected a response", response != null);
    assertTrue("expected response to contain an empty set of lifecycle rules", response.getLifecycleConfiguration().getRules().size() == 0);

  }

  @AfterClass
  public static void tearDown() {
    BucketLifecycleManagers.setInstance(null);
    BucketMetadataManagers.setInstance(null);
    OsgAuthorizationHandler.setInstance(null);
  }

  @Test
  public void testCheckBucketName() {
    final Validator<String> bucketNameValidator =
        BucketNameValidatorRepo.getBucketNameValidator( "extended" );
    assertTrue( bucketNameValidator.check("bucket"));
    assertTrue( bucketNameValidator.check("bucket.bucket.bucket"));
    assertTrue( bucketNameValidator.check("bu_ckeS-adsad"));
    assertTrue( bucketNameValidator.check("aou12naofdufan1421_-123oiasd-afasf.asdfas"));
    assertTrue( bucketNameValidator.check("bucket_name"));
    //assertFalse( bucketNameValidator.check("10.100.1.1"));
    assertFalse( bucketNameValidator.check("bucke<t>%12313"));
    assertFalse( bucketNameValidator.check("b*u&c%k#et"));
  }

}
