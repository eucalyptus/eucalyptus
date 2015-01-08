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
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.storage.config.CacheableConfiguration;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.walrus.entities.WalrusInfo;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.NoSuchElementException;

/**
 * The OSG global configuration parameters. These are common
 * for all OSG instances.
 */
@Entity
@PersistenceContext(name = "eucalyptus_osg")
@Table(name = "osg_config")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@ConfigurableClass(root = "ObjectStorage", description = "Object Storage Gateway configuration.", deferred = true, singleton = true)
public class ObjectStorageGlobalConfiguration extends AbstractPersistent implements CacheableConfiguration<ObjectStorageGlobalConfiguration> {
    @Transient
    private static final Logger LOG = Logger.getLogger(ObjectStorageGlobalConfiguration.class);
    @Transient
    private static final int DEFAULT_MAX_BUCKETS_PER_ACCOUNT = 100;
    @Transient
    private static final int DEFAULT_PUT_TIMEOUT_HOURS = 168; //An upload not marked completed or deleted in 24 hours from record creation will be considered 'failed'
    @Transient
    private static final int DEFAULT_CLEANUP_INTERVAL_SEC = 60; //60 seconds between cleanup tasks.
    @Transient
    private static final String DEFAULT_BUCKET_NAMING_SCHEME = "extended";
    @Transient
    private static final Boolean DEFAULT_COPY_UNSUPPORTED_STRATEGY = Boolean.FALSE;

    @Override
    public ObjectStorageGlobalConfiguration getLatest() {
        return getConfiguration();
    }

    @Column
    @ConfigurableField(description = "Maximum number of buckets per account", displayName = "Maximum buckets per account")
    protected Integer max_buckets_per_account;

    @Column
    @ConfigurableField(description = "Total ObjectStorage storage capacity for Objects soley for reporting usage percentage. Not a size restriction. No enforcement of this value", displayName = "ObjectStorage object capacity (GB)")
    protected Integer max_total_reporting_capacity_gb;

    @Column
    @ConfigurableField(description = "Number of hours to wait for object PUT operations to be allowed to complete before cleanup.", displayName = "Object PUT failure cleanup (Hours)")
    protected Integer failed_put_timeout_hrs;

    @Column
    @ConfigurableField(description = "Interval, in seconds, at which cleanup tasks are initiated for removing old/stale objects.", displayName = "Cleanup interval (seconds)")
    protected Integer cleanup_task_interval_seconds;

    @Column
    @ConfigurableField(description = "Interval, in seconds, during which buckets in creating-state are valid. After this interval, the operation is assumed failed.", displayName = "Operation wait interval (seconds)")
    protected Integer bucket_creation_wait_interval_seconds;

    @Column
    @ConfigurableField( description = "The S3 bucket naming restrictions to enforce. Values are 'dns-compliant' or 'extended'. " +
            "Default is 'extended'. dns_compliant is non-US region S3 names, extended is for US-Standard Region naming. " +
            "See http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html", displayName = "Bucket Naming restrictions", changeListener = BucketNamingRestrictionsValidator.class)
    protected String bucket_naming_restrictions;

    @Column
    @ConfigurableField(description = "Should provider client attempt a GET / PUT when backend does not support Copy operation", displayName = "attempt GET/PUT on Copy fail", type = ConfigurableFieldType.BOOLEAN)
    protected Boolean doGetPutOnCopyFail;

    protected ObjectStorageGlobalConfiguration initializeDefaults() {
        this.setBucket_creation_wait_interval_seconds(DEFAULT_CLEANUP_INTERVAL_SEC);
        this.setBucket_naming_restrictions(DEFAULT_BUCKET_NAMING_SCHEME);
        this.setCleanup_task_interval_seconds(DEFAULT_CLEANUP_INTERVAL_SEC);
        this.setDoGetPutOnCopyFail(DEFAULT_COPY_UNSUPPORTED_STRATEGY);
        this.setFailed_put_timeout_hrs(DEFAULT_PUT_TIMEOUT_HOURS);
        this.setMax_buckets_per_account(DEFAULT_MAX_BUCKETS_PER_ACCOUNT);
        this.setMax_total_reporting_capacity_gb(Integer.MAX_VALUE);
        return this;
    }

    /**
     * Validator for values to set restrictions to
     */
    public static class BucketNamingRestrictionsValidator implements PropertyChangeListener {
        @Override
        public void fireChange(ConfigurableProperty t, Object newValue) throws ConfigurablePropertyException {
            String proposed = (String) newValue;
            if(Strings.isNullOrEmpty(proposed) ||
                    (!"extended".equals(proposed) && !"dns-compliant".equals(proposed))) {
                throw Exceptions.toUndeclared("Invalid property value: " + proposed + " Acceptabled values are: 'extended' and 'dns-compliant'", new NoSuchElementException(proposed));
            }
        };

    }

    public Integer getMax_buckets_per_account() {
        return max_buckets_per_account;
    }

    public void setMax_buckets_per_account(Integer max_buckets_per_account) {
        this.max_buckets_per_account = max_buckets_per_account;
    }

    public Integer getMax_total_reporting_capacity_gb() {
        return max_total_reporting_capacity_gb;
    }

    public void setMax_total_reporting_capacity_gb(Integer max_total_reporting_capacity_gb) {
        this.max_total_reporting_capacity_gb = max_total_reporting_capacity_gb;
    }

    public Integer getFailed_put_timeout_hrs() {
        return failed_put_timeout_hrs;
    }

    public void setFailed_put_timeout_hrs(Integer failed_put_timeout_hrs) {
        this.failed_put_timeout_hrs = failed_put_timeout_hrs;
    }

    public Integer getCleanup_task_interval_seconds() {
        return cleanup_task_interval_seconds;
    }

    public void setCleanup_task_interval_seconds(Integer cleanup_task_interval_seconds) {
        this.cleanup_task_interval_seconds = cleanup_task_interval_seconds;
    }

    public Integer getBucket_creation_wait_interval_seconds() {
        return bucket_creation_wait_interval_seconds;
    }

    public void setBucket_creation_wait_interval_seconds(Integer bucket_creation_wait_interval_seconds) {
        this.bucket_creation_wait_interval_seconds = bucket_creation_wait_interval_seconds;
    }

    public String getBucket_naming_restrictions() {
        return bucket_naming_restrictions;
    }

    public void setBucket_naming_restrictions(String bucket_naming_restrictions) {
        this.bucket_naming_restrictions = bucket_naming_restrictions;
    }

    public Boolean getDoGetPutOnCopyFail() {
        return doGetPutOnCopyFail;
    }

    public void setDoGetPutOnCopyFail(Boolean doGetPutOnCopyFail) {
        this.doGetPutOnCopyFail = doGetPutOnCopyFail;
    }

    /**
     * Gets this config from the DB. May throw an exception on db failure
     * @return
     * @throws Exception
     */
    public static ObjectStorageGlobalConfiguration getConfiguration() {
        try {
            try {
                return Transactions.find(new ObjectStorageGlobalConfiguration());
            } catch(NoSuchElementException e) {
                //Initialize;
                LOG.info("No extant S3 provider configuration found. Initializing defaults");
                return Transactions.saveDirect(new ObjectStorageGlobalConfiguration().initializeDefaults());
            }
        } catch(Throwable f) {
            throw Exceptions.toUndeclared("Failed getting and/or initializing OSG global configuration", f);
        }
    }

    @Override
    public String toString() {
        String value = "[OSG Global configuration: " +
                "MaxTotalCapacity=" + max_total_reporting_capacity_gb + " , " +
                "MaxBucketsPerAccount=" + max_buckets_per_account + " , " +
                "FailedPutTimeoutHrs=" + failed_put_timeout_hrs + " , " +
                "CleanupTaskIntervalSec=" + cleanup_task_interval_seconds + " , " +
                "BucketCreationWaitIntervalSec=" + bucket_creation_wait_interval_seconds + " , " +
                "BucketNamingRestrictions=" + bucket_naming_restrictions + "]";
        return value;
    }

    /**
     * Upgrade code to copy walrus config into osg
     * @throws Exception
     */
    @Upgrades.EntityUpgrade(entities = { ObjectStorageGlobalConfiguration.class }, since = Upgrades.Version.v4_0_0, value = ObjectStorage.class)
    public static enum OSGConfigUpgrade implements Predicate<Class> {
        INSTANCE;

        @Override
        public boolean apply(@Nullable Class arg0) {
            try(TransactionResource trans = Entities.transactionFor(arg0)) {
                //Set defaults to the values from Walrus in 3.4.x
                ObjectStorageGlobalConfiguration config = new ObjectStorageGlobalConfiguration().initializeDefaults();
                config = Entities.merge(config);
                WalrusInfo walrusConfig = WalrusInfo.getWalrusInfo();
                config.setMax_buckets_per_account(walrusConfig.getStorageMaxBucketsPerAccount());
                config.setMax_total_reporting_capacity_gb(walrusConfig.getStorageMaxTotalCapacity());
                config.setBucket_naming_restrictions((walrusConfig.getBucketNamesRequireDnsCompliance() ? "dns-compliant" : "extended"));
                trans.commit();
                return true;
            } catch(Exception e) {
                LOG.error("Error saving upgrade global osg configuration", e);
                throw Exceptions.toUndeclared("Error upgrading walrus config to OSG config", e);
            }
        }
    }

}
