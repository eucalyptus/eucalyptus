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

import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.*;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
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
import com.eucalyptus.objectstorage.providers.ObjectStorageProviders;
import com.eucalyptus.storage.config.CacheableConfiguration;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.upgrade.Upgrades.DatabaseFilters;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.walrus.entities.WalrusInfo;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * The OSG global configuration parameters. These are common for all OSG instances.
 */
@Entity
@PersistenceContext(name = "eucalyptus_osg")
@Table(name = "osg_config")
@ConfigurableClass(root = "ObjectStorage", description = "Object Storage Gateway configuration.", deferred = true, singleton = true)
public class ObjectStorageGlobalConfiguration extends AbstractPersistent implements CacheableConfiguration<ObjectStorageGlobalConfiguration> {
  @Transient
  private static final Logger LOG = Logger.getLogger(ObjectStorageGlobalConfiguration.class);
  @Transient
  private static final int DEFAULT_MAX_BUCKETS_PER_ACCOUNT = 100;
  @Transient
  private static final int DEFAULT_MAX_METADATA_REQUEST_SIZE = 1024 * 300; // 300 KB
  @Transient
  private static final int DEFAULT_PUT_TIMEOUT_HOURS = 168; // An upload not marked completed or deleted in 24 hours from record creation will be
                                                            // considered 'failed'
  @Transient
  private static final int DEFAULT_CLEANUP_INTERVAL_SEC = 60; // 60 seconds between cleanup tasks.
  @Transient
  private static final String DEFAULT_BUCKET_NAMING_SCHEME = "extended";
  @Transient
  private static final Boolean DEFAULT_COPY_UNSUPPORTED_STRATEGY = Boolean.FALSE;

  @Override
  public ObjectStorageGlobalConfiguration getLatest() {
    return getConfiguration();
  }

  @Column
  @ConfigurableField(description = "Maximum allowed size of metadata request bodies",
      displayName = "Maximum allowed size of metadata requests",
      initialInt = DEFAULT_MAX_METADATA_REQUEST_SIZE )
  protected Integer max_metadata_request_size;

  @Column
  @ConfigurableField(description = "Maximum number of buckets per account",
      displayName = "Maximum buckets per account",
      initialInt = DEFAULT_MAX_BUCKETS_PER_ACCOUNT )
  protected Integer max_buckets_per_account;

  @Column
  @ConfigurableField(
      description = "Total ObjectStorage storage capacity for Objects soley for reporting usage percentage. Not a size restriction. No enforcement of this value",
      displayName = "ObjectStorage object capacity (GB)", initialInt = Integer.MAX_VALUE )
  protected Integer max_total_reporting_capacity_gb;

  @Column
  @ConfigurableField(description = "Number of hours to wait for object PUT operations to be allowed to complete before cleanup.",
      displayName = "Object PUT failure cleanup (Hours)", initialInt = DEFAULT_PUT_TIMEOUT_HOURS )
  protected Integer failed_put_timeout_hrs;

  @Column
  @ConfigurableField(description = "Interval, in seconds, at which cleanup tasks are initiated for removing old/stale objects.",
      displayName = "Cleanup interval (seconds)",
      initialInt = DEFAULT_CLEANUP_INTERVAL_SEC )
  protected Integer cleanup_task_interval_seconds;

  @Column
  @ConfigurableField(
      description = "Interval, in seconds, during which buckets in creating-state are valid. After this interval, the operation is assumed failed.",
      displayName = "Operation wait interval (seconds)",
      initialInt = DEFAULT_CLEANUP_INTERVAL_SEC )
  protected Integer bucket_creation_wait_interval_seconds;

  @Column
  @ConfigurableField(description = "The S3 bucket naming restrictions to enforce. Values are 'dns-compliant' or 'extended'. "
      + "Default is 'extended'. dns_compliant is non-US region S3 names, extended is for US-Standard Region naming. "
      + "See http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html", displayName = "Bucket Naming restrictions",
      changeListener = BucketNamingRestrictionsValidator.class,
      initial = DEFAULT_BUCKET_NAMING_SCHEME )
  protected String bucket_naming_restrictions;

  @Column
  @ConfigurableField(description = "Should provider client attempt a GET / PUT when backend does not support Copy operation",
      displayName = "attempt GET/PUT on Copy fail", type = ConfigurableFieldType.BOOLEAN, initial = "false" )
  protected Boolean doGetPutOnCopyFail;

  @Column
  @ConfigurableField(description = "Object Storage Provider client to use for backend", displayName = "Object Storage Provider Client",
      changeListener = ObjectStorageProviderChangeListener.class)
  protected String providerClient; // configured by user to specify which back-end client to use

  @Column
  @ConfigurableField(description = "List of host names that may not be used as bucket cnames")
  protected String bucket_reserved_cnames;

  protected ObjectStorageGlobalConfiguration initializeDefaults() {
    this.setBucket_creation_wait_interval_seconds(DEFAULT_CLEANUP_INTERVAL_SEC);
    this.setBucket_naming_restrictions(DEFAULT_BUCKET_NAMING_SCHEME);
    this.setCleanup_task_interval_seconds(DEFAULT_CLEANUP_INTERVAL_SEC);
    this.setDoGetPutOnCopyFail(DEFAULT_COPY_UNSUPPORTED_STRATEGY);
    this.setFailed_put_timeout_hrs(DEFAULT_PUT_TIMEOUT_HOURS);
    this.setMax_buckets_per_account(DEFAULT_MAX_BUCKETS_PER_ACCOUNT);
    this.setMax_metadata_request_size(DEFAULT_MAX_METADATA_REQUEST_SIZE);
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
      if (Strings.isNullOrEmpty(proposed) || (!"extended".equals(proposed) && !"dns-compliant".equals(proposed))) {
        throw Exceptions.toUndeclared("Invalid property value: " + proposed + " Acceptabled values are: 'extended' and 'dns-compliant'",
            new NoSuchElementException(proposed));
      }
    };

  }

  public Integer getMax_metadata_request_size() {
    return max_metadata_request_size != null ? max_metadata_request_size : DEFAULT_MAX_METADATA_REQUEST_SIZE;
  }

  public void setMax_metadata_request_size(Integer max_metadata_request_size) {
    this.max_metadata_request_size = max_metadata_request_size;
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

  public String getProviderClient() {
    return providerClient;
  }

  public void setProviderClient(String providerClient) {
    this.providerClient = providerClient;
  }

  public String getBucket_reserved_cnames( ) {
    return bucket_reserved_cnames;
  }

  public void setBucket_reserved_cnames( final String bucket_reserved_cnames ) {
    this.bucket_reserved_cnames = bucket_reserved_cnames;
  }

  @PrePersist
  @PreUpdate
  public void updateDefaults() {
    if (max_metadata_request_size == null) {
      max_metadata_request_size = DEFAULT_MAX_METADATA_REQUEST_SIZE;
    }
  }

  /**
   * Gets this config from the DB. May throw an exception on db failure
   * 
   * @return
   * @throws Exception
   */
  public static ObjectStorageGlobalConfiguration getConfiguration() {
    try {
      try {
        return Transactions.find(new ObjectStorageGlobalConfiguration());
      } catch (NoSuchElementException e) {
        // Initialize;
        LOG.info("No extant S3 provider configuration found. Initializing defaults");
        return Transactions.saveDirect(new ObjectStorageGlobalConfiguration().initializeDefaults());
      }
    } catch (Throwable f) {
      throw Exceptions.toUndeclared("Failed getting and/or initializing OSG global configuration", f);
    }
  }

  @Override
  public String toString() {
    String value =
        "[OSG Global configuration: " + "MaxTotalCapacity=" + max_total_reporting_capacity_gb + " , " + "MaxBucketsPerAccount="
            + max_buckets_per_account + " , " + "FailedPutTimeoutHrs=" + failed_put_timeout_hrs + " , " + "CleanupTaskIntervalSec="
            + cleanup_task_interval_seconds + " , " + "BucketCreationWaitIntervalSec=" + bucket_creation_wait_interval_seconds + " , "
            + "BucketNamingRestrictions=" + bucket_naming_restrictions + " , " + "DoGetPutOnCopyFail=" + doGetPutOnCopyFail + " , "
            + "ProviderClient=" + providerClient + "]";
    return value;
  }

  /**
   * Upgrade code to copy walrus config into osg
   * 
   * @throws Exception
   */
  @EntityUpgrade(entities = {ObjectStorageGlobalConfiguration.class}, since = Upgrades.Version.v4_0_0, value = ObjectStorage.class)
  public static enum OSGConfigUpgrade implements Predicate<Class> {
    INSTANCE;

    @Override
    public boolean apply(@Nullable Class arg0) {
      try (TransactionResource trans = Entities.transactionFor(arg0)) {
        // Set defaults to the values from Walrus in 3.4.x
        ObjectStorageGlobalConfiguration config = new ObjectStorageGlobalConfiguration().initializeDefaults();
        config = Entities.merge(config);
        WalrusInfo walrusConfig = WalrusInfo.getWalrusInfo();
        config.setMax_buckets_per_account(walrusConfig.getStorageMaxBucketsPerAccount());
        config.setMax_total_reporting_capacity_gb(walrusConfig.getStorageMaxTotalCapacity());
        config.setBucket_naming_restrictions((walrusConfig.getBucketNamesRequireDnsCompliance() ? "dns-compliant" : "extended"));
        config.setProviderClient("walrus"); // set the provider client to walrus since its an upgrade to 4.0.0
        trans.commit();
        return true;
      } catch (Exception e) {
        LOG.error("Error saving upgrade global osg configuration", e);
        throw Exceptions.toUndeclared("Error upgrading walrus config to OSG config", e);
      }
    }
  }

  /**
   * Upgrade code to copy global provider client to osg config
   *
   */
  @EntityUpgrade(entities = {ObjectStorageGlobalConfiguration.class}, since = Upgrades.Version.v4_1_0, value = ObjectStorage.class)
  public static enum ProviderClientUpgrade implements Predicate<Class> {
    INSTANCE;

    private static final Logger LOG = Logger.getLogger(ProviderClientUpgrade.class);

    @Override
    public boolean apply(@Nullable Class arg0) {
      try (TransactionResource tr = Entities.transactionFor(arg0)) {
        // look for global config
        ObjectStorageGlobalConfiguration osgc = null;
        try {
          osgc = Entities.uniqueResult(new ObjectStorageGlobalConfiguration());
        } catch (NoSuchElementException e) {
          osgc = new ObjectStorageGlobalConfiguration().initializeDefaults();
          Entities.persist(osgc); // create global config with defaults
        }

        // check if provider client is set
        if (StringUtils.isNotBlank(osgc.getProviderClient())) {
          // This could be the case if cloud was upgraded from 3.4.x release. Upgrade logic to 4.0.0 (OSGConfigUpgrade) may have populated the
          // provider client. Don't overwrite the provider client, just move on
          LOG.info("Nothing to upgrade as objectstorage provider client is already configured to " + osgc.getProviderClient());
        } else {
          // Cloud was upgraded from 4.0.x release, look for provider client property in static global properties table and copy it
          String prevClient = getStaticProviderClient();
          if (StringUtils.isNotBlank(prevClient)) {
            LOG.info("Found global objectstorage.providerclient=" + prevClient
                + ". Copying value to providerClient of ObjectStorageGlobalConfiguration entity");
            osgc.setProviderClient(prevClient);
          } else { // else block should never be hit
            LOG.info("Global objectstorage.providerclient not found. Defaulting providerClient of ObjectStorageGlobalConfiguration entity to walrus");
            osgc.setProviderClient("walrus");
          }
        }
        tr.commit();
        return true;
      } catch (Exception e) {
        LOG.error("Error upgrading global osg configuration", e);
        throw Exceptions.toUndeclared("Error upgrading global osg configuration", e);
      }

    }

    /*
     * Get the value of objectstorage.providerclient stored in config_static_property table. Had to use sql directly as StaticDatabasePropertyEntry
     * has a private constructor that restricts its usage for lookup/delete operations
     */
    private String getStaticProviderClient() {
      String propertyValue = null;
      Sql sql = null;

      try {
        sql = DatabaseFilters.NEWVERSION.getConnection("eucalyptus_config");
        String query = "select config_static_field_value from config_static_property where config_static_prop_name='objectstorage.providerclient'";

        try {
          // Look for static property objectstorage.providerclient
          List<GroovyRowResult> result = sql.rows(query);
          if (result != null && result.size() == 1 && result.get(0) != null) {
            propertyValue = (String) result.get(0).getProperty("config_static_field_value");
            // Delete the static property
            query = "delete from config_static_property where config_static_prop_name='objectstorage.providerclient'";
            sql.execute(query);
          } else {
            // static property not found, nothing to return here
          }
        } catch (Exception e) {
          LOG.warn("Failed to execute query: " + query, e);
        }
      } catch (Exception e) {
        LOG.warn("Failed to connect to database", e);
      } finally {
        try {
          if (sql != null) {
            sql.close();
          }
        } catch (Exception e) {
          LOG.warn("Failed to close database connection", e);
        }
      }

      return propertyValue;
    }
  }

  @EntityUpgrade(entities = {ObjectStorageGlobalConfiguration.class}, since = Upgrades.Version.v4_4_0, value = ObjectStorage.class)
  public enum OSG44ConfigUpgrade implements Predicate<Class> {
    INSTANCE;

    @Override
    public boolean apply(@Nullable Class arg0) {
      try (TransactionResource trans = Entities.transactionFor(arg0)) {
        ObjectStorageGlobalConfiguration config;
        try {
          config = Entities.uniqueResult(new ObjectStorageGlobalConfiguration());
        } catch (NoSuchElementException e) {
          config = new ObjectStorageGlobalConfiguration().initializeDefaults();
        }
        config.updateDefaults();
        Entities.persist(config);
        trans.commit();
        return true;
      } catch (Exception e) {
        String msg = "Error saving OSG 4.4 configuration upgrade";
        LOG.error(msg, e);
        throw Exceptions.toUndeclared(msg, e);
      }
    }
  }

  @EntityUpgrade(entities = ObjectStorageGlobalConfiguration.class, since = Upgrades.Version.v5_0_0, value = ObjectStorage.class)
  public enum OSG50ConfigUpgrade implements Predicate<Class> {
    INSTANCE;

    @Override
    public boolean apply( @Nullable Class arg0 ) {
      try ( TransactionResource trans = Entities.transactionFor( ObjectStorageGlobalConfiguration.class ) ) {
        final Optional<ObjectStorageGlobalConfiguration> config =
            Entities.criteriaQuery( ObjectStorageGlobalConfiguration.class ).uniqueResultOption( );
        config.transform( it -> { it.setBucket_reserved_cnames( "*" ); return it; } );
        trans.commit( );
        return true;
      } catch (Exception e) {
        String msg = "Error saving OSG 5.0 configuration upgrade";
        LOG.error( msg, e );
        throw Exceptions.toUndeclared(msg, e);
      }
    }
  }

  /**
   * Change listener for the osg provider client setting.
   * 
   * @author zhill
   *
   */
  public static class ObjectStorageProviderChangeListener implements PropertyChangeListener<String> {
    /*
     * Ensures that the proposed value is valid based on the set of valid values for OSGs Additional DB lookup required for remote OSGs where the CLC
     * doesn't have the OSG bits installed and therefore doesn't have the same view of the set of valid values. (non-Javadoc)
     * 
     * @see com.eucalyptus.configurable.PropertyChangeListener#fireChange(com.eucalyptus.configurable.ConfigurableProperty, java.lang.Object)
     */
    @Override
    public void fireChange(ConfigurableProperty t, String newValue) throws ConfigurablePropertyException {
      String existingValue = (String) t.getValue();

      List<ServiceConfiguration> objConfigs = null;
      try {
        objConfigs = ServiceConfigurations.list(ObjectStorage.class);
      } catch (NoSuchElementException e) {
        throw new ConfigurablePropertyException("No ObjectStorage configurations found");
      }

      final String proposedValue = newValue;
      final Set<String> validEntries = Sets.newHashSet();
      try (TransactionResource tr = Entities.transactionFor(ObjectStorageConfiguration.class)) {
        boolean match = Iterables.any(Components.lookup(ObjectStorage.class).services(), new Predicate<ServiceConfiguration>() {
          @Override
          public boolean apply(ServiceConfiguration config) {
            if (config.isVmLocal()) {
              // Service is local, so add entries to the valid list (in case of HA configs)
              // and then check the local memory state
              validEntries.addAll(ObjectStorageProviders.list());
              return ObjectStorageProviders.contains(proposedValue);
            } else {
              try {
                // Remote OSG, so check the db for the list of valid entries.
                ObjectStorageConfiguration objConfig = Entities.uniqueResult((ObjectStorageConfiguration) config);
                for (String entry : Splitter.on(",").split(objConfig.getAvailableClients())) {
                  validEntries.add(entry);
                }
                return validEntries.contains(proposedValue);
              } catch (Exception e) {
                return false;
              }
            }
          }
        });
        tr.commit();
        if (!match) {
          // Nothing matched.
          throw new ConfigurablePropertyException("Cannot modify " + t.getQualifiedName() + "." + t.getFieldName()
              + " new value is not a valid value.  " + "Legal values are: " + Joiner.on(",").join(validEntries));
        } else {
          // matching provider client found
        }
      }
    }
  }
}
