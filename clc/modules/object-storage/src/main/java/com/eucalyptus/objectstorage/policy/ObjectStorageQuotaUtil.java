/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage.policy;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.objectstorage.BucketMetadataManagers;
import com.eucalyptus.objectstorage.MpuPartMetadataManagers;
import com.eucalyptus.objectstorage.ObjectMetadataManagers;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.metadata.MpuPartMetadataManager;

import java.util.List;

public class ObjectStorageQuotaUtil {

    public static long countBucketsByAccount(String accountId) throws AuthException {
        try {
            return BucketMetadataManagers.getInstance().countBucketsByAccount(Accounts.lookupAccountById(accountId).getCanonicalId());
        } catch (Exception e) {
            throw new AuthException("Failed to search bucket", e);
        }
    }

    public static long countBucketsByUser(String userId) throws AuthException {
        try {
            return BucketMetadataManagers.getInstance().countBucketsByUser(userId);
        } catch (Exception e) {
            throw new AuthException("Failed to search bucket", e);
        }
    }

    public static long countBucketObjects(String bucketName) throws AuthException {
        try {
            return ObjectMetadataManagers.getInstance().countValid(BucketMetadataManagers.getInstance().lookupBucket(bucketName));
        } catch (Exception e) {
            throw new AuthException("Failed to search object", e);
        }
    }

    public static long getBucketSize(String bucketName) throws AuthException {
        try (TransactionResource db = Entities.transactionFor(Bucket.class)) {
            Bucket bucket = BucketMetadataManagers.getInstance().lookupBucket(bucketName);
            long objectSize = ObjectMetadataManagers.getInstance().getTotalSize(bucket);
            long mpuPartsSize = MpuPartMetadataManagers.getInstance().getTotalSize(bucket);
            return objectSize + mpuPartsSize;
        } catch (Exception e) {
            throw new AuthException("Failed to get bucket total size", e);
        }
    }

    public static long getTotalObjectsSizeByAccount(String accountId) throws AuthException {
        String canonicalId = Accounts.lookupAccountById(accountId).getCanonicalId();
        try (TransactionResource db = Entities.transactionFor(Bucket.class)) {
            List<Bucket> bucketList = BucketMetadataManagers.getInstance().lookupBucketsByOwner(canonicalId);
            long size = 0;
            for (Bucket b : bucketList) {
                size += ObjectMetadataManagers.getInstance().getTotalSize(b);
                size += MpuPartMetadataManagers.getInstance().getTotalSize(b);
            }
            return size;
        } catch (Exception e) {
            throw new AuthException("Failed to search bucket", e);
        }
    }

    public static long getTotalObjectsSizeByUser(String userId) throws AuthException {
        try (TransactionResource db = Entities.transactionFor(Bucket.class)) {
            List<Bucket> bucketList = BucketMetadataManagers.getInstance().lookupBucketsByUser(userId);
            long size = 0;
            for (Bucket b : bucketList) {
                size += ObjectMetadataManagers.getInstance().getTotalSize(b);
                size += MpuPartMetadataManagers.getInstance().getTotalSize(b);
            }
            return size;
        } catch (Exception e) {
            throw new AuthException("Failed to search bucket", e);
        }
    }

    /**
     * Return the total size in bytes of objects in the ObjectStorage.
     *
     * @return The size or -1 if the size could not be determined.
     */
    public static long getTotalObjectSize() throws Exception {
        //return BucketMetadataManagers.getInstance().totalSizeOfAllBuckets();
        return ObjectMetadataManagers.getInstance().getTotalSize(null) +
                MpuPartMetadataManagers.getInstance().getTotalSize(null);
    }
}
