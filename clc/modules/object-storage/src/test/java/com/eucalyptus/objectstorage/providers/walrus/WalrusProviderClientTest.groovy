/*
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
 */

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
                    HttpResponseStatus.NOT_ACCEPTABLE)
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
                    HttpResponseStatus.NOT_FOUND)
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
                    HttpResponseStatus.NOT_ACCEPTABLE)
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
                    HttpResponseStatus.NOT_FOUND)
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
                    HttpResponseStatus.NOT_ACCEPTABLE);
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
                    HttpResponseStatus.CONFLICT)
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
                    HttpResponseStatus.NOT_ACCEPTABLE);
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
                    HttpResponseStatus.FORBIDDEN);
            WalrusProviderClient.handleRemoteFault(testFault)
        } catch(S3Exception e) {
            assert(e instanceof AccessDeniedException)
            assert(e.getCode() == "AccessDenied")
            assert(e.getStatus() == HttpResponseStatus.FORBIDDEN)
        }
    }
}
