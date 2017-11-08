/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.objectstorage.providers.walrus

import com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException
import com.eucalyptus.objectstorage.exceptions.s3.BucketNotEmptyException
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchBucketException
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception
import com.eucalyptus.ws.EucalyptusRemoteFault
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.junit.Test

/**
 * Created by zhill on 4/9/14.
 */
class WalrusProviderClientTest {
  @Test
  public void testExceptionHandler() {
    EucalyptusRemoteFault testFault

    //NoSuchBucket - 406
    try {
      testFault = new EucalyptusRemoteFault('noaction',
          'norelates',
          'NoSuchBucket',
          'NoSuchBucketException',
          'Bucket not found',
          HttpResponseStatus.NOT_ACCEPTABLE.getCode())
      WalrusProviderClient.handleRemoteFault(testFault)
    } catch(S3Exception e) {
      assert(e instanceof NoSuchBucketException)
      assert(e.getCode() == "NoSuchBucket")
      assert(e.getStatus() == HttpResponseStatus.NOT_FOUND)
    }

    //NoSuchBucket - 404
    try {
      testFault = new EucalyptusRemoteFault('noaction',
          'norelates',
          'NoSuchBucket',
          'NoSuchBucketException',
          'Bucket not found',
          HttpResponseStatus.NOT_FOUND.getCode())
      WalrusProviderClient.handleRemoteFault(testFault)
    } catch(S3Exception e) {
      assert(e instanceof NoSuchBucketException)
      assert(e.getCode() == "NoSuchBucket")
      assert(e.getStatus() == HttpResponseStatus.NOT_FOUND)
    }

    //NoSuchKey - 406
    try {
      testFault = new EucalyptusRemoteFault('noaction',
          'norelates',
          'NoSuchBucket',
          'NoSuchBucketException',
          'Bucket not found',
          HttpResponseStatus.NOT_ACCEPTABLE.getCode())
      WalrusProviderClient.handleRemoteFault(testFault)
    } catch(S3Exception e) {
      assert(e instanceof NoSuchBucketException)
      assert(e.getCode() == "NoSuchBucket")
      assert(e.getStatus() == HttpResponseStatus.NOT_FOUND)
    }

    //NoSuchKey - 404
    try {
      testFault = new EucalyptusRemoteFault('noaction',
          'norelates',
          'NoSuchBucket',
          'NoSuchBucketException',
          'Bucket not found',
          HttpResponseStatus.NOT_FOUND.getCode())
      WalrusProviderClient.handleRemoteFault(testFault)
    } catch(S3Exception e) {
      assert(e instanceof NoSuchBucketException)
      assert(e.getCode() == "NoSuchBucket")
      assert(e.getStatus() == HttpResponseStatus.NOT_FOUND)
    }

    //BucketNotEmpty - 406
    try {
      testFault = new EucalyptusRemoteFault('noaction',
          'norelates',
          'BucketNotEmpty',
          'BucketNotEmptyException',
          'Bucket is not empty',
          HttpResponseStatus.NOT_ACCEPTABLE.getCode());
      WalrusProviderClient.handleRemoteFault(testFault)
    } catch(S3Exception e) {
      assert(e instanceof BucketNotEmptyException)
      assert(e.getCode() == "BucketNotEmpty")
      assert(e.getStatus() == HttpResponseStatus.CONFLICT)
    }

    //BucketNotEmpty - 405
    try {
      testFault = new EucalyptusRemoteFault('noaction',
          'norelates',
          'BucketNotEmpty',
          'BucketNotEmptyException',
          'Bucket is not empty',
          HttpResponseStatus.CONFLICT.getCode())
      WalrusProviderClient.handleRemoteFault(testFault)
    } catch(S3Exception e) {
      assert(e instanceof BucketNotEmptyException)
      assert(e.getCode() == "BucketNotEmpty")
      assert(e.getStatus() == HttpResponseStatus.CONFLICT)
    }

    //AccessDenied - 406
    try {
      testFault = new EucalyptusRemoteFault('noaction',
          'norelates',
          'AccessDenied',
          'AccessDeniedException',
          'Access to resource is denied',
          HttpResponseStatus.NOT_ACCEPTABLE.getCode());
      WalrusProviderClient.handleRemoteFault(testFault)
    } catch(S3Exception e) {
      assert(e instanceof AccessDeniedException)
      assert(e.getCode() == "AccessDenied")
      assert(e.getStatus() == HttpResponseStatus.FORBIDDEN)
    }

    //AccessDenied - 403
    try {
      testFault = new EucalyptusRemoteFault('noaction',
          'norelates',
          'AccessDenied',
          'AccessDeniedException',
          'Access to resource is denied',
          HttpResponseStatus.FORBIDDEN.getCode());
      WalrusProviderClient.handleRemoteFault(testFault)
    } catch(S3Exception e) {
      assert(e instanceof AccessDeniedException)
      assert(e.getCode() == "AccessDenied")
      assert(e.getStatus() == HttpResponseStatus.FORBIDDEN)
    }
  }
}
