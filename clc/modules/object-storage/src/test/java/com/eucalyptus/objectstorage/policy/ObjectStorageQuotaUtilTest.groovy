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

package com.eucalyptus.objectstorage.policy

import com.eucalyptus.auth.Accounts
import com.eucalyptus.auth.principal.Account
import com.eucalyptus.auth.principal.User
import com.eucalyptus.objectstorage.BucketMetadataManagers
import com.eucalyptus.objectstorage.BucketState
import com.eucalyptus.objectstorage.ObjectMetadataManagers
import com.eucalyptus.objectstorage.ObjectState
import com.eucalyptus.objectstorage.UnitTestSupport
import com.eucalyptus.objectstorage.entities.Bucket
import com.eucalyptus.objectstorage.entities.ObjectEntity
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by zhill on 3/13/14.
 */
@CompileStatic
public class ObjectStorageQuotaUtilTest {
    static List<Bucket> buckets = Lists.newArrayList()
    static List<ObjectEntity> objects = Lists.newArrayList()
    static int bucketCount = 3
    static int objectCount = 5
    static int size = 1024 * 1024
    static Account account1
    static Account account2
    static User a1u1
    static User a1u2
    static User a2u1
    static User a2u2

    private static void initMetaData() {
        def name = "bucket"
        account1 = Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().first())
        a1u1 = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
        a1u2 = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).getAt(1))

        account2 = Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().getAt(1))
        a2u1 = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().getAt(1)).first())
        a2u2 = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().getAt(1)).getAt(1))

        def acl = ""
        def location = ""
        def b

        for (int i = 0; i < bucketCount ; i++) {
            b = Bucket.getInitializedBucket(name + i, account1.getCanonicalId(), account1.getName(), a1u1.getUserId(), acl, location)
            b = BucketMetadataManagers.getInstance().transitionBucketToState(b, BucketState.creating)
            b = BucketMetadataManagers.getInstance().transitionBucketToState(b, BucketState.extant)
            buckets.add(b)

            def obj
            def key = 'key'
            for (int j = 0; j < objectCount ; j++) {
                obj = ObjectEntity.newInitializedForCreate(b, key + j, size, a1u1)
                obj = ObjectMetadataManagers.getInstance().transitionObjectToState(obj, ObjectState.creating)
                obj.setObjectModifiedTimestamp(new Date())
                obj.seteTag("etag" + j)
                obj = ObjectMetadataManagers.getInstance().transitionObjectToState(obj, ObjectState.extant)
                objects.add(obj)
            }
        }
    }

    @BeforeClass
    public static void setup() {
        UnitTestSupport.setupAuthPersistenceContext()
        UnitTestSupport.setupOsgPersistenceContext()
        UnitTestSupport.initializeAuth(2, 2)

        initMetaData()
    }

    @AfterClass
    public static void teardown() {
        UnitTestSupport.tearDownOsgPersistenceContext()
        UnitTestSupport.tearDownAuthPersistenceContext()
    }

    @Before
    public void preTest() {}

    @After
    public void postTest() {}

    @Test
    public void testCountBucketsByAccount() throws Exception {
        assert(ObjectStorageQuotaUtil.countBucketsByAccount(account1.getAccountNumber()) == bucketCount)
        assert(ObjectStorageQuotaUtil.countBucketsByAccount(account2.getAccountNumber()) == 0)
    }

    @Test
    public void testCountBucketsByUser() throws Exception {
        assert(ObjectStorageQuotaUtil.countBucketsByUser(a1u1.getUserId()) == bucketCount)
        assert(ObjectStorageQuotaUtil.countBucketsByUser(a1u2.getUserId()) == 0)
        assert(ObjectStorageQuotaUtil.countBucketsByUser(a2u1.getUserId()) == 0)
        assert(ObjectStorageQuotaUtil.countBucketsByUser(a2u2.getUserId()) == 0)
    }

    @Test
    public void testCountBucketObjects() throws Exception {
        buckets.each {
            assert(ObjectStorageQuotaUtil.countBucketObjects(((Bucket)it).getBucketName()) == objectCount)
        }
    }

    @Test
    public void testGetBucketSize() throws Exception {
        buckets.each {
            assert(ObjectStorageQuotaUtil.getBucketSize(((Bucket)it).getBucketName()) == objectCount * size)
        }
    }

    @Test
    public void testGetTotalObjectsSizeByAccount() throws Exception {
        assert(ObjectStorageQuotaUtil.getTotalObjectsSizeByAccount(account1.getAccountNumber()) ==  bucketCount * objectCount * size)
        assert(ObjectStorageQuotaUtil.getTotalObjectsSizeByAccount(account2.getAccountNumber()) == 0)
    }

    @Test
    public void testGetTotalObjectsSizeByUser() throws Exception {
        assert(ObjectStorageQuotaUtil.getTotalObjectsSizeByUser(a1u1.getUserId()) ==  bucketCount * objectCount * size)
        assert(ObjectStorageQuotaUtil.getTotalObjectsSizeByUser(a1u2.getUserId()) == 0)
        assert(ObjectStorageQuotaUtil.getTotalObjectsSizeByUser(a2u1.getUserId()) == 0)
        assert(ObjectStorageQuotaUtil.getTotalObjectsSizeByUser(a2u2.getUserId()) == 0)

    }

    @Test
    public void testGetTotalObjectSize() throws Exception {
        assert(ObjectStorageQuotaUtil.getTotalObjectSize() == bucketCount * objectCount * size)
    }
}
