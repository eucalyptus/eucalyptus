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
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;
import java.util.NoSuchElementException;

@Entity
@PersistenceContext(name = "eucalyptus_osg")
@Table(name = "provider_config")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@ConfigurableClass( root = "objectstorage.s3provider", description = "Configuration for S3-compatible backend")
public class S3ProviderConfiguration extends AbstractPersistent {

    @Transient
    private static Logger LOG = Logger.getLogger(S3ProviderConfiguration.class);

    private static final String NAME = "osg_provider_config";

    @Column(name = "config_name", unique = true)
    private String name = NAME;

	@ConfigurableField( description = "External S3 endpoint.",
			displayName = "s3_endpoint",
            changeListener = S3ProviderConfiguration.S3ProviderConfigurationListener.class,
            initial = "s3.amazonaws.com" )
    @Column(name = "endpoint")
	public String S3Endpoint;

	@ConfigurableField( description = "Local Store S3 Access Key.",
			displayName = "s3_access_key", 
			type = ConfigurableFieldType.KEYVALUEHIDDEN,
            changeListener = S3ProviderConfiguration.S3ProviderConfigurationListener.class )
    @Column(name = "access_key")
	public String S3AccessKey;

	@ConfigurableField( description = "Local Store S3 Secret Key.",
			displayName = "s3_secret_key", 
			type = ConfigurableFieldType.KEYVALUEHIDDEN,
            changeListener = S3ProviderConfiguration.S3ProviderConfigurationListener.class )
    @Column(name = "secret_key")
	public String S3SecretKey;
	
	@ConfigurableField( description = "Use HTTPS for communication to service backend.",
			displayName = "use_https",
			initial="false",
            changeListener = S3ProviderConfiguration.S3ProviderConfigurationListener.class )
    @Column(name = "use_https")
	public boolean S3UseHttps;
	
	@ConfigurableField( description = "Use DNS virtual-hosted-style bucket names for communication to service backend.",
			displayName = "use_backend_dns",
			initial="false",
            changeListener = S3ProviderConfiguration.S3ProviderConfigurationListener.class )
    @Column(name = "use_backend_dns")
	public boolean S3UseBackendDns;

    @ConfigurableField( description = "Number of seconds to wait between checks for configuration change.",
            displayName = "prop_change_check_interval",
            initial = "15",
            changeListener = S3ProviderConfiguration.S3ProviderConfigurationListener.class )
    @Column(name = "prop_change_check_interval")
    public Integer propertyChangeCheckInterval;

    @ConfigurableField( description = "cap on the number of \"sleeping\" instances in the pool.",
            displayName = "pool_max_idle",
            initial = "5",
            changeListener = S3ProviderConfiguration.S3ProviderConfigurationListener.class )
    @Column(name = "pool_max_idle")
    public Integer poolMaxIdle;

    @ConfigurableField( description = "cap on the total number of active instances from the pool.",
            displayName = "pool_max_active",
            initial = "20",
            changeListener = S3ProviderConfiguration.S3ProviderConfigurationListener.class )
    @Column(name = "pool_max_active")
    public Integer poolMaxActive;

    @Column(name = "prop_change")
    private Date propertyChange;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

	public String getS3SecretKey() {
		return S3SecretKey;
	}

	public void setS3SecretKey(String s3SecretKey) {
		S3SecretKey = s3SecretKey;
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

    public Date getPropertyChange() {
        return propertyChange;
    }

    public void setPropertyChange(Date propertyChange) {
        this.propertyChange = propertyChange;
    }

    public String getS3EndpointHost() {
		String[] s3EndpointParts = S3Endpoint.split(":");
		if (s3EndpointParts.length > 0) {
			return s3EndpointParts[0];
		} else {
			return null;
		}
	}

	public int getS3EndpointPort() {
		String[] s3EndpointParts = S3Endpoint.split(":");
		if (s3EndpointParts.length > 1) {
			try {
				return Integer.parseInt(s3EndpointParts[1]);
			} catch (NumberFormatException e) {
				return 80;
			}
		} else {
			return 80; //default http port
		}
	}

    public Integer getPropertyChangeCheckInterval() {
        return propertyChangeCheckInterval;
    }

    public void setPropertyChangeCheckInterval(Integer propertyChangeCheckInterval) {
        this.propertyChangeCheckInterval = propertyChangeCheckInterval;
    }

    public Integer getPoolMaxIdle() {
        return poolMaxIdle;
    }

    public void setPoolMaxIdle(Integer poolMaxIdle) {
        this.poolMaxIdle = poolMaxIdle;
    }

    public Integer getPoolMaxActive() {
        return poolMaxActive;
    }

    public void setPoolMaxActive(Integer poolMaxActive) {
        this.poolMaxActive = poolMaxActive;
    }

    public static S3ProviderConfiguration getS3ProviderConfiguration() {
        S3ProviderConfiguration example = new S3ProviderConfiguration();
        example.setName(NAME);
        S3ProviderConfiguration retrieved = null;
        boolean needsCreated = false;
        try (TransactionResource tran = Entities.transactionFor(S3ProviderConfiguration.class)) {
            retrieved = Entities.uniqueResult(example);
            tran.commit();
            return retrieved;
        }
        catch (NoSuchElementException enf ){
            needsCreated = true;
        }
        catch (TransactionException e) {
            LOG.error("exception occurred while retrieving S3 provider configuration", e);
            return null;
        }
        if (needsCreated) {
            try (TransactionResource tran = Entities.transactionFor(S3ProviderConfiguration.class)) {
                example.setS3Endpoint("s3.amazonaws.com");
                example.setPropertyChangeCheckInterval( new Integer(15) );
                example.setPoolMaxActive( new Integer(20) );
                example.setPoolMaxIdle( new Integer(5) );
                example = Entities.persist(example);
                tran.commit();
                return example;
            }
            catch (Exception ex) {
                LOG.error("exception occurred while creating S3 provider configuration", ex);
                return null;
            }
        }
        return null;
    }

    public static final class S3ProviderConfigurationListener implements PropertyChangeListener {
        @Override
        public void fireChange(ConfigurableProperty t, Object newValue) throws ConfigurablePropertyException {
            S3ProviderConfiguration configuration = S3ProviderConfiguration.getS3ProviderConfiguration();
            try (TransactionResource tran = Entities.transactionFor(S3ProviderConfiguration.class)) {
                configuration = Entities.uniqueResult(configuration);
                configuration.setPropertyChange(new Date());
                tran.commit();
            } catch (TransactionException e) {
                LOG.error("exception occurred while retrieving S3 provider configuration, your configuration " +
                        "change may require a cloud restart before taking effect");
            }
        }
    }


}
