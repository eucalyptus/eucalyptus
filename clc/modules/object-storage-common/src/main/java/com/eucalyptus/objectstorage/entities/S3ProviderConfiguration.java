/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage.entities;

import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableInit;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.storage.config.CacheableConfiguration;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.Version;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LockResource;

import com.google.common.base.Predicate;

@Entity
@PersistenceContext(name = "eucalyptus_osg")
@Table(name = "s3provider_config")
@ConfigurableClass(root = "objectstorage.s3provider", alias = "backendconfig", description = "Configuration for S3-compatible backend",
    singleton = true)
public class S3ProviderConfiguration extends AbstractPersistent implements CacheableConfiguration<S3ProviderConfiguration> {
  @Transient
  private static final String DEFAULT_S3_ENDPOINT = "uninitialized-s3-endpoint";
  @Transient
  private static final Logger LOG = Logger.getLogger(S3ProviderConfiguration.class);
  @Transient
  private static final boolean DEFAULT_BACKEND_DNS = false;
  @Transient
  private static final Boolean DEFAULT_BACKEND_HTTPS = false;
  @Transient
  private static final String DEFAULT_S3_HEAD_RESPONSE = "405";

  @ConfigurableField(description = "External S3 endpoint.", displayName = "s3_endpoint", initial = DEFAULT_S3_ENDPOINT)
  @Column(name = "endpoint")
  protected String S3Endpoint;

  @ConfigurableField(description = "Local Store S3 Access Key.", displayName = "s3_access_key", type = ConfigurableFieldType.KEYVALUEHIDDEN)
  @Column(name = "access_key")
  protected String S3AccessKey;

  @ConfigurableField(description = "Local Store S3 Secret Key.", displayName = "s3_secret_key", type = ConfigurableFieldType.KEYVALUEHIDDEN)
  @Column(name = "secret_key")
  @Type(type="text")
  protected String S3SecretKey;

  @Transient
  private String decryptedS3SecretKey = null;
  @Transient
  private ReentrantReadWriteLock S3SecretKeyLock = new ReentrantReadWriteLock();

  @ConfigurableField(description = "Use HTTPS for communication to service backend.", displayName = "use_https", initial = "false",
      type = ConfigurableFieldType.BOOLEAN)
  @Column(name = "use_https")
  protected Boolean S3UseHttps;

  @ConfigurableField(description = "Use DNS virtual-hosted-style bucket names for communication to service backend.", displayName = "use_backend_dns",
      initial = "false", type = ConfigurableFieldType.BOOLEAN)
  @Column(name = "use_backend_dns")
  protected Boolean S3UseBackendDns;

  @ConfigurableField(description = "HTTP response code for HEAD operation on S3 endpoint. Default value is 405",
      displayName = "s3_endpoint_heartbeat_response", initial = DEFAULT_S3_HEAD_RESPONSE)
  @Column(name = "endpoint_head_response")
  protected Integer S3EndpointHeadResponse;

  public Boolean getS3UseBackendDns() {
    return S3UseBackendDns;
  }

  public void setS3UseBackendDns(Boolean useDns) {
    S3UseBackendDns = useDns;
  }

  public String getS3AccessKey() {
    return S3AccessKey;
  }

  public void setS3AccessKey(String s3AccessKey) {
    S3AccessKey = s3AccessKey;
  }

  /* Returns decrypted S3SecretKey */
  public String getS3SecretKey() throws Exception {
    if (this.S3SecretKey != null) {
      try(final LockResource rlock = LockResource.lock(S3SecretKeyLock.readLock())) {
        if (decryptedS3SecretKey == null) {
          rlock.close();
          LOG.trace("There is no stored decrypted S3 Secret Key. Decrypting...");
          try(final LockResource lock = LockResource.lock(S3SecretKeyLock.writeLock())) {
            decryptedS3SecretKey = OSGUtil.decryptWithComponentPrivateKey(ObjectStorage.class, this.S3SecretKey);
            return decryptedS3SecretKey;
          } catch (EucalyptusCloudException ex) {
            LOG.error(ex);
            throw ex;
          }
        }
        return decryptedS3SecretKey;
      }
    } else {
      return null;
    }
  }

  public void setS3SecretKey(String s3SecretKey) throws Exception {
    if (s3SecretKey != null) {
      try(final LockResource lock = LockResource.lock(S3SecretKeyLock.writeLock())) {
        String val = s3SecretKey;
        s3SecretKey = OSGUtil.encryptWithComponentPublicKey(ObjectStorage.class, s3SecretKey);
        decryptedS3SecretKey = val;
      } catch (EucalyptusCloudException ex) {
        LOG.error(ex);
        throw ex;
      }
    }
    this.S3SecretKey = s3SecretKey;
  }

  public Boolean getS3UseHttps() {
    return S3UseHttps;
  }

  public void setS3UseHttps(Boolean s3UseHttps) {
    S3UseHttps = s3UseHttps;
  }

  public String getS3Endpoint() {
    return S3Endpoint;
  }

  public void setS3Endpoint(String endPoint) {
    S3Endpoint = endPoint;
  }

  public Integer getS3EndpointHeadResponse() {
    return S3EndpointHeadResponse;
  }

  public void setS3EndpointHeadResponse(Integer s3EndpointHeadResponse) {
    S3EndpointHeadResponse = s3EndpointHeadResponse;
  }

  @ConfigurableInit
  public S3ProviderConfiguration initializeDefaults() {
    this.setS3Endpoint(DEFAULT_S3_ENDPOINT);
    this.setS3UseBackendDns(DEFAULT_BACKEND_DNS);
    this.setS3UseHttps(DEFAULT_BACKEND_HTTPS);
    this.setS3EndpointHeadResponse(Integer.valueOf(DEFAULT_S3_HEAD_RESPONSE));
    return this;
  }

  @PrePersist
  @PreUpdate
  public void updateDefaults() {
    if (this.S3EndpointHeadResponse == null) {
      this.S3EndpointHeadResponse = Integer.valueOf(DEFAULT_S3_HEAD_RESPONSE);
    }
  }

  public static S3ProviderConfiguration getS3ProviderConfiguration() {
    try {
      try {
        return Transactions.find(new S3ProviderConfiguration());
      } catch (NoSuchElementException enf) {
        LOG.info("No extant S3 provider configuration found. Initializing defaults");
        return Transactions.saveDirect(new S3ProviderConfiguration().initializeDefaults());
      }
    } catch (Throwable f) {
      LOG.error("exception occurred while retrieving S3 provider configuration", f);
      throw Exceptions.toUndeclared("Error getting/initializing s3 provider configuration", f);
    }
  }

  @Override
  public S3ProviderConfiguration getLatest() {
    return getS3ProviderConfiguration();
  }

  @EntityUpgrade( entities = S3ProviderConfiguration.class, since = Version.v4_3_0, value = ObjectStorage.class )
  public enum S3ProviderConfigurationUpgrade430 implements Predicate<Class> {
    INSTANCE;

    private static final Logger LOG = Logger.getLogger(S3ProviderConfigurationUpgrade430.class);

    @Override
    public boolean apply( final Class entityClass ) {
      try ( final TransactionResource tx = Entities.transactionFor( S3ProviderConfiguration.class ) ) {
        final S3ProviderConfiguration configuration = S3ProviderConfiguration.getS3ProviderConfiguration( );
        if ( configuration.getS3EndpointHeadResponse( ) == null ) {
          LOG.info( "Initializing S3EndpointHeadResponse to default value " + DEFAULT_S3_HEAD_RESPONSE );
          configuration.updateDefaults( );
          tx.commit( );
        }
        return true;
      } catch (final Exception ex) {
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
}
