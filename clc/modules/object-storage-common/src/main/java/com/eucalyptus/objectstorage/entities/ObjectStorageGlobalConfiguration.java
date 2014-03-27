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

package com.eucalyptus.objectstorage.entities;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;

/**
 * The OSG global configuration parameters. These are common
 * for all OSG instances.
 */
@ConfigurableClass(root = "ObjectStorage", description = "Object Storage Gateway configuration.", deferred = true, singleton = true)
public class ObjectStorageGlobalConfiguration {
    private static final int DEFAULT_MAX_BUCKETS_PER_ACCOUNT = 100;
    private static final int DEFAULT_MAX_BUCKET_SIZE_MB = 5000;
    private static final int DEFAULT_PUT_TIMEOUT_HOURS = 168; //An upload not marked completed or deleted in 24 hours from record creation will be considered 'failed'
    private static final int DEFAULT_CLEANUP_INTERVAL_SEC = 60; //60 seconds between cleanup tasks.
    private static final int DEFAULT_BUCKET_CREATION_INTERVAL_SEC = 30; //10 seconds for 'creating' or 'deleting' transitions.
    private static final String DEFAULT_BUCKET_NAMING_SCHEME = "extended";

    @ConfigurableField(description = "Maximum number of buckets per account", displayName = "Maximum buckets per account")
    public static Integer max_buckets_per_account = DEFAULT_MAX_BUCKETS_PER_ACCOUNT;

    @ConfigurableField(description = "Maximum size per bucket", displayName = "Maximum bucket size (MB)")
    public static Integer max_bucket_size_mb = DEFAULT_MAX_BUCKET_SIZE_MB;

    @ConfigurableField(description = "Total ObjectStorage storage capacity for Objects", displayName = "ObjectStorage object capacity (GB)")
    public static Integer max_total_reporting_capacity_gb = Integer.MAX_VALUE;

    @ConfigurableField(description = "Number of hours to wait for object PUT operations to be allowed to complete before cleanup.", displayName = "Object PUT failure cleanup (Hours)")
    public static Integer failed_put_timeout_hrs = DEFAULT_PUT_TIMEOUT_HOURS;

    @ConfigurableField(description = "Interval, in seconds, at which cleanup tasks are initiated for removing old/stale objects.", displayName = "Cleanup interval (seconds)")
    public static Integer cleanup_task_interval_seconds = DEFAULT_CLEANUP_INTERVAL_SEC;

    @ConfigurableField(description = "Interval, in seconds, during which buckets in creating-state are valid. After this interval, the operation is assumed failed.", displayName = "Operation wait interval (seconds)")
    public static Integer bucket_creation_wait_interval_seconds = DEFAULT_BUCKET_CREATION_INTERVAL_SEC;

    @ConfigurableField( description = "The S3 bucket naming restrictions to enforce. Values are 'dns-compliant' or 'extended'. Default is 'extended'. dns-compliant is non-US region S3 names, extended is for US-Standard Region naming. See http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html", displayName = "Bucket Naming restrictions")
    public static String bucket_naming_restrictions = DEFAULT_BUCKET_NAMING_SCHEME;

    @ConfigurableField(description = "Should provider client attempt a GET / PUT when backend does not support Copy operation", displayName = "attempt GET/PUT on Copy fail", type = ConfigurableFieldType.BOOLEAN)
    public static volatile Boolean doGetPutOnCopyFail = Boolean.FALSE;

    @Override
    public String toString() {
        String value = "[OSG Global configuration: " +
                "MaxTotalCapacity=" + max_total_reporting_capacity_gb + " , " +
                "MaxBucketsPerAccount=" + max_buckets_per_account + " , " +
                "MaxBucketSizeMB=" + max_bucket_size_mb + " , " +
                "FailedPutTimeoutHrs=" + failed_put_timeout_hrs + " , " +
                "CleanupTaskIntervalSec=" + cleanup_task_interval_seconds + " , " +
                "BucketCreationWaitIntervalSec=" + bucket_creation_wait_interval_seconds + " , " +
                "BucketNamingRestrictions=" + bucket_naming_restrictions + "]";
        return value;
    }

	/* Different type of upgrade required since this is not an explicit entity
    @EntityUpgrade(entities = { ObjectStorageGlobalConfiguration.class }, since = Version.v4_0_0, value = ObjectStorage.class)
	public static void upgrade3_4_To4_0() throws Exception {
		//Set defaults to the values from Walrus in 3.4.x
		ObjectStorageGlobalConfiguration config = getConfiguration();
		WalrusInfo walrusConfig = WalrusInfo.getWalrusInfo();		
		config.setStorageMaxBucketSizeInMB(walrusConfig.getStorageMaxBucketSizeInMB());
		config.setStorageMaxBucketsPerAccount(walrusConfig.getStorageMaxBucketsPerAccount());
		config.setStorageMaxTotalCapacity(walrusConfig.getStorageMaxTotalCapacity());
		
		try {
			Transactions.save(config);
		} catch(TransactionException e) {
			LOG.error("Error saving upgrade global osg configuration", e);
			throw e;
		}
	}
	*/
}
