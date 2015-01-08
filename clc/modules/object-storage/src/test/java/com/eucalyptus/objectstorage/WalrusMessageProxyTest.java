/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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
 ************************************************************************/

package com.eucalyptus.objectstorage;

import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.providers.walrus.MessageMapper;
import com.eucalyptus.storage.msgs.s3.BucketListEntry;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.ListAllMyBucketsList;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

public class WalrusMessageProxyTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void listBucketsTest() throws Exception {
        com.eucalyptus.objectstorage.msgs.ListAllMyBucketsType bucketRequest = new com.eucalyptus.objectstorage.msgs.ListAllMyBucketsType();
        bucketRequest.setCorrelationId("test-correlation-id");

        com.eucalyptus.walrus.msgs.ListAllMyBucketsType walrusRequest = MessageMapper.INSTANCE.proxyWalrusRequest(com.eucalyptus.walrus.msgs.ListAllMyBucketsType.class, bucketRequest);
        assert (walrusRequest.getCorrelationId() != bucketRequest.getCorrelationId());

        System.out.println("Request: " + bucketRequest.toSimpleString() + "\nProxied: " + walrusRequest.toSimpleString());

        com.eucalyptus.walrus.msgs.ListAllMyBucketsResponseType walrusResponse = (com.eucalyptus.walrus.msgs.ListAllMyBucketsResponseType) walrusRequest.getReply();
        ListAllMyBucketsList bucketList = new ListAllMyBucketsList();
        ArrayList<BucketListEntry> bucketArray = Lists.newArrayList();
        bucketArray.add(new BucketListEntry("bucket1", "2013-01-01 12:00:00"));
        bucketList.setBuckets(bucketArray);
        walrusResponse.setBucketList(bucketList);
        CanonicalUser usr = new CanonicalUser("123-456-789", "user1");
        walrusResponse.setOwner(usr);

        com.eucalyptus.objectstorage.msgs.ListAllMyBucketsResponseType bucketResponse = MessageMapper.INSTANCE.proxyWalrusResponse(bucketRequest, walrusResponse);

        assert (bucketResponse.getCorrelationId().equals(bucketRequest.getCorrelationId()));
        assert (bucketResponse.getBucketList().equals(walrusResponse.getBucketList()));
        assert (bucketResponse.getOwner().equals(walrusResponse.getOwner()));

        System.out.println("Response: " + bucketResponse.toSimpleString() + "\nFrom: " + walrusResponse.toSimpleString());
        System.out.println("Response: " + bucketResponse.getBucketList().toString() + "\nFrom: " + walrusResponse.getBucketList().toString());

    }

    @Test
    public void createBucketTest() throws Exception {
        com.eucalyptus.objectstorage.msgs.CreateBucketType bucketRequest = new com.eucalyptus.objectstorage.msgs.CreateBucketType();
        bucketRequest.setCorrelationId("test-correlation-id");
        bucketRequest.setBucket("testbucket123");

        com.eucalyptus.walrus.msgs.CreateBucketType walrusRequest = MessageMapper.INSTANCE.proxyWalrusRequest(com.eucalyptus.walrus.msgs.CreateBucketType.class, bucketRequest);
        assert (walrusRequest.getCorrelationId() != bucketRequest.getCorrelationId());
        assert (walrusRequest.getBucket().equals(bucketRequest.getBucket()));

        System.out.println("Request: " + bucketRequest.toSimpleString() + "\nProxied: " + walrusRequest.toSimpleString());

        com.eucalyptus.walrus.msgs.CreateBucketResponseType walrusResponse = (com.eucalyptus.walrus.msgs.CreateBucketResponseType) walrusRequest.getReply();
        walrusResponse.setLogData(new com.eucalyptus.storage.msgs.BucketLogData(walrusRequest.getCorrelationId()));
        walrusResponse.setBucket(walrusRequest.getBucket());

        com.eucalyptus.objectstorage.msgs.CreateBucketResponseType bucketResponse = MessageMapper.INSTANCE.proxyWalrusResponse(bucketRequest, walrusResponse);
        assert (bucketResponse.getCorrelationId().equals(bucketRequest.getCorrelationId()));
        assert (bucketResponse.getBucket().equals(bucketRequest.getBucket()));
        assert (bucketResponse.getLogData().equals(walrusResponse.getLogData()));

        System.out.println("Response: " + bucketResponse.toSimpleString() + "\nFrom: " + walrusResponse.toSimpleString());
        System.out.println("Response: " + bucketResponse.getBucket() + "\nFrom: " + walrusResponse.getBucket().toString());
        System.out.println("Response: " + bucketResponse.getLogData() + "\nFrom: " + walrusResponse.getLogData().toString());

    }

    @Test
    public void proxyWalrusExceptionTest() throws Exception {
        com.eucalyptus.walrus.exceptions.BucketNotEmptyException walrusBucketNotEmptyException
                = new com.eucalyptus.walrus.exceptions.BucketNotEmptyException();
        assertTrue("Expected walrus's BucketNotEmptyException to come back as an OSG S3ClientException",
                MessageMapper.INSTANCE.proxyWalrusException(walrusBucketNotEmptyException) instanceof S3Exception);
    }

}
