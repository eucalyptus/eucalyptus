/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage.entities;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.storage.config.CacheableConfiguration;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.NoSuchElementException;

@Entity
@PersistenceContext(name="eucalyptus_osg")
@Table(name="s3provider_config")
@ConfigurableClass( root = "objectstorage.s3provider", alias="backendconfig", description = "Configuration for S3-compatible backend", singleton = true)
public class S3ProviderConfiguration extends AbstractPersistent implements CacheableConfiguration<S3ProviderConfiguration> {
    @Transient
    private static final String DEFAULT_S3_ENDPOINT = "uninitialized-s3-endpoint";
    @Transient
    private static final Logger LOG = Logger.getLogger(S3ProviderConfiguration.class);
    @Transient
    private static final boolean DEFAULT_BACKEND_DNS = false;
    @Transient
    private static final Boolean DEFAULT_BACKEND_HTTPS = false;

    @ConfigurableField( description = "External S3 endpoint.",
            displayName = "s3_endpoint",
            initial = "s3.amazonaws.com" )
    @Column(name = "endpoint")
    protected String S3Endpoint;

	@ConfigurableField( description = "Local Store S3 Access Key.",
			displayName = "s3_access_key", 
			type = ConfigurableFieldType.KEYVALUEHIDDEN)
    @Column(name = "access_key")
	protected String S3AccessKey;

	@ConfigurableField( description = "Local Store S3 Secret Key.",
			displayName = "s3_secret_key", 
			type = ConfigurableFieldType.KEYVALUEHIDDEN)
    @Column(name = "secret_key")
    @Lob
    @Type(type="org.hibernate.type.StringClobType")
	protected String S3SecretKey;
	
	@ConfigurableField( description = "Use HTTPS for communication to service backend.",
			displayName = "use_https",
			initial="false")
    @Column(name = "use_https")
	protected Boolean S3UseHttps;
	
	@ConfigurableField( description = "Use DNS virtual-hosted-style bucket names for communication to service backend.",
			displayName = "use_backend_dns",
			initial="false")
    @Column(name = "use_backend_dns")
	protected Boolean S3UseBackendDns;

    public boolean getS3UseBackendDns() {
		return S3UseBackendDns;
	}

	public void setS3UserBackendDns(boolean useDns) {
		S3UseBackendDns = useDns;
	}	

	public String getS3AccessKey() {
		return S3AccessKey;
	}

	public void setS3AccessKey(String s3AccessKey) {
		S3AccessKey = s3AccessKey;
	}

	public String getS3SecretKey() throws Exception {
        if(this.S3SecretKey != null) {
            try {
                return OSGUtil.decryptWithComponentPrivateKey(ObjectStorage.class, this.S3SecretKey);
            } catch(EucalyptusCloudException ex) {
                LOG.error(ex);
                throw ex;
            }
        } else {
            return this.S3SecretKey;
        }
	}

	public void setS3SecretKey(String s3SecretKey) throws Exception {
        if(s3SecretKey != null) {
            try {
                s3SecretKey = OSGUtil.encryptWithComponentPublicKey(ObjectStorage.class, s3SecretKey);
            } catch(EucalyptusCloudException ex) {
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

    public S3ProviderConfiguration initializeDefaults() {
        this.setS3Endpoint(DEFAULT_S3_ENDPOINT);
        this.setS3UserBackendDns(DEFAULT_BACKEND_DNS);
        this.setS3UseHttps(DEFAULT_BACKEND_HTTPS);
        return this;
    }

    public static S3ProviderConfiguration getS3ProviderConfiguration() {
        try {
            try {
                return Transactions.find(new S3ProviderConfiguration());
            }
            catch (NoSuchElementException enf ){
                LOG.info("No extant S3 provider configuration found. Initializing defaults");
                return Transactions.saveDirect(new S3ProviderConfiguration().initializeDefaults());
            }
        }
        catch (Throwable f) {
            LOG.error("exception occurred while retrieving S3 provider configuration", f);
            throw Exceptions.toUndeclared("Error getting/initializing s3 provider configuration", f);
        }
    }

    @Override
    public S3ProviderConfiguration getLatest() {
        return getS3ProviderConfiguration();
    }
}
