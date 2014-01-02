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

package com.eucalyptus.blockstorage.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.StorageManagers;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableIdentifier;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.MultiDatabasePropertyEntry;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.Version;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@Entity
@PersistenceContext(name="eucalyptus_config")
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ComponentPart(Storage.class)
@ConfigurableClass( root = "storage", alias = "basic", description = "Basic cluster controller configuration.", singleton = false, deferred = true )
public class StorageControllerConfiguration extends ComponentConfiguration implements Serializable {
	
	@Transient
	private static String DEFAULT_SERVICE_PATH = "/services/Storage";

	@Transient
	@ConfigurableIdentifier
	private String propertyPrefix;

	@ConfigurableField( description = "EBS Block Storage Manager to use for backend", displayName = "EBS Block Storage Manager", changeListener = StorageBackendChangeListener.class)
	@Column( name = "system_storage_ebs_backend")
	private String blockStorageManager;
	
	/*
	 * Available Backends is used *ONLY* for allowing the CLC to do sanity checks on the value being set for the block storage backend
	 * by the user via a modify-property call. This obviates the need to have the CLC call the SC somehow and check the set of valid
	 * values. The SC should set the value when the service is constructed.
	*/
	@Column( name = "available_storage_backends")
	private String availableBackends;
	
	/*
	 * Change Listener for san provider and backend config options.
	 * The semantics enforced are that they can be set once per registered SC. If the value has already been configured it will
	 * throw an exception if the user tries to reconfigure them.
	 *
	 * This is done to prevent data loss and database corruption that can occur if the user tries to change the backend after it
	 * already has some state.
	 * 
	 * The semantics are that the value is valid if *any* of the SCs in the specified partition have proposed value
	 * listed as an available backend manager.
	 */
	public static class StorageBackendChangeListener implements PropertyChangeListener<String> {
		@Override
		public void fireChange(ConfigurableProperty t, String newValue) throws ConfigurablePropertyException {			
			String existingValue = (String)t.getValue();
			if(existingValue != null && !"<unset>".equals(existingValue)) {
				throw new ConfigurablePropertyException("Cannot change extant storage backend configuration. You must deregister all SCs in the partition before you can change the configuration value");
			} else {
				//Try to figure out the partition name for the request
				String probablePartitionName = ((MultiDatabasePropertyEntry)t).getEntrySetName();
				if(probablePartitionName == null) {
					throw new ConfigurablePropertyException("Could not determing partition name from property to check validity");
				}
				
				String[] parts = probablePartitionName.split("\\.");
				if(parts == null || parts.length == 0) {
					throw new ConfigurablePropertyException("Could not determing partition name from property to check validity: " + probablePartitionName);
				}
				probablePartitionName = parts[0];
				
				/*Look through the service configurations for each SC in the partition and see if the value is valid.
				 * This step must work if we don't allow the user to change it once set.
				 * The difficulty here is if 2 SCs are in an HA pair but have different backends installed (i.e. packages)
				 * The implemented semantic is that if the proposed value is valid in either SC, then allow the change.
				*/
				List<ServiceConfiguration> scConfigs = null;
				try {
					scConfigs = ServiceConfigurations.listPartition(Storage.class, probablePartitionName);					
				} catch(NoSuchElementException e) {
					throw new ConfigurablePropertyException("No Storage Controller configurations found for partition: " + probablePartitionName);
				}
				
				final String proposedValue = newValue;
				final Set<String> validEntries = Sets.newHashSet();
				EntityTransaction tx = Entities.get(StorageControllerConfiguration.class);
				try {
					if(!Iterables.any(scConfigs, new Predicate<ServiceConfiguration>( ) {
						@Override
						public boolean apply(ServiceConfiguration config) {
							if(config.isVmLocal()) {
								//Service is local, so add entries to the valid list (in case of HA configs)
								// and then check the local memory state
								validEntries.addAll(StorageManagers.list());
								return StorageManagers.contains(proposedValue);
							} else {
								try {
									//Remote SC, so check the db for the list of valid entries.
									StorageControllerConfiguration scConfig = Entities.uniqueResult((StorageControllerConfiguration)config);
									for(String entry : Splitter.on(",").split(scConfig.getAvailableBackends())) {
										validEntries.add(entry);
									}									
									return validEntries.contains(proposedValue);
								} catch(Exception e) {
									return false;
								}
							}
						}
					})) {
						//Nothing matched.
						throw new ConfigurablePropertyException("Cannot modify " + t.getQualifiedName() + "." + t.getFieldName() + " new value is not a valid value.  " +				
								"Legal values are: " + Joiner.on( "," ).join( validEntries) );
					}
				} finally {
					tx.rollback();
				}
			}
		}
	}

	@PrePersist
	private void updateRedundantConfig( ) {
		//Checks to see if other SCs exist in the same partition and uses the same backend config if it exists.
		if ( this.blockStorageManager == null && this.getPartition( ) != null ) { 
			for ( ServiceConfiguration s : ServiceConfigurations.listPartition( Storage.class, this.getPartition( ) ) ) {
				StorageControllerConfiguration otherSc = ( StorageControllerConfiguration ) s;
				this.blockStorageManager = otherSc.getBlockStorageManager( ) != null ? otherSc.getBlockStorageManager( ) : null;
			}
		}
  }

	public StorageControllerConfiguration( ) {

	}
	public StorageControllerConfiguration( String name ) {
		super.setName(name);
	}
	public StorageControllerConfiguration( String partition, String name, String hostName, Integer port ) {
		super( partition, name, hostName, port, DEFAULT_SERVICE_PATH );
	}

	public StorageControllerConfiguration( String partition, String name, String hostName, Integer port , String storageManager) {
		super( partition, name, hostName, port, DEFAULT_SERVICE_PATH );
		this.blockStorageManager = storageManager;
	}
	
	public String getBlockStorageManager() {
		return this.blockStorageManager;
	}
	public void setBlockStorageManager(String m) {
		this.blockStorageManager = m;
	}
		
	public String getPropertyPrefix() {
		return this.getPartition();
	}
	
	public void setPropertyPrefix(String p) {
		this.setPartition(p);
	}
	
	private static final String BLOCK_STORAGE_MANAGER_OVERLAY = "overlay";
	private static final String BLOCK_STORAGE_MANAGER_DAS = "das";
	private static final String BLOCK_STORAGE_MANAGER_EQUALLOGIC = "equallogic";
	private static final String BLOCK_STORAGE_MANAGER_NETAPP = "netapp";
	
	private static final Pattern EBS_STORAGE_MANAGER_PATTERN = Pattern.compile(".*-Debs\\.storage\\.manager=(\\w+).*");
	private static final Pattern EBS_SAN_PROVIDER_PATTERN = Pattern.compile(".*-Debs\\.san\\.provider=(\\w+).*");
	
	private static String matchParameter(Pattern pattern, String text) {
	  Matcher matcher = pattern.matcher(text);
	  if (matcher.matches()) {
	    return matcher.group(1);
	  }
	  return null;
	}
	
  public String getAvailableBackends() {
		return availableBackends;
	}

	public void setAvailableBackends(String availableBackends) {
		this.availableBackends = availableBackends;
	}

	@EntityUpgrade( entities = { StorageControllerConfiguration.class }, since = Version.v3_2_0, value = Storage.class )
  public enum StorageControllerConfigurationUpgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StorageControllerConfiguration.StorageControllerConfigurationUpgrade.class );
    
    private static String loadLocalBlockStorageManagerConfig() throws Exception {
      String manager = BLOCK_STORAGE_MANAGER_OVERLAY; // default
      BufferedReader fileReader = new BufferedReader(new FileReader(
          BaseDirectory.HOME + "/etc/eucalyptus/eucalyptus.conf"));
      String ebsStorageManager = null;
      String ebsSanProvider = null;
      String line;
      while ((line = fileReader.readLine()) != null) {
        line.trim();
        if (line.startsWith("CLOUD_OPTS")) {
          ebsStorageManager = matchParameter(EBS_STORAGE_MANAGER_PATTERN, line);
          ebsSanProvider = matchParameter(EBS_SAN_PROVIDER_PATTERN, line);
          break;
        }
      }
      fileReader.close();
      if (Strings.isNullOrEmpty(ebsStorageManager)) {
        manager = BLOCK_STORAGE_MANAGER_OVERLAY;
      } else if ("DASManager".equals(ebsStorageManager)) {
        manager = BLOCK_STORAGE_MANAGER_DAS;
      } else if ("OverlayManager".equals(ebsStorageManager)) {
        manager = BLOCK_STORAGE_MANAGER_OVERLAY;
      } else if ("SANManager".equals(ebsStorageManager)){
        if ("EquallogicProvider".equals(ebsSanProvider)) {
          manager = BLOCK_STORAGE_MANAGER_EQUALLOGIC;
        } else if ("NetappProvider".equals(ebsSanProvider)) {
          manager = BLOCK_STORAGE_MANAGER_NETAPP;
        } else {
          LOG.error("Invalid SAN provider name: " + ebsSanProvider);
        }
      } else {
        LOG.error("Invalid storage manager name: " + ebsStorageManager);
      }
      return manager;
    }
    
    @Override
    public boolean apply( Class arg0 ) {
      EntityTransaction db = Entities.get( StorageControllerConfiguration.class );
      try {
        // Get local IP addresses or host names
        Set<String> localAddresses = Internets.getAllLocalHostNamesIps();
        List<StorageControllerConfiguration> entities = Entities.query( new StorageControllerConfiguration( ) );
        for ( StorageControllerConfiguration entry : entities ) {
          // This SC is running on the local machine, upgrade its block storage manager config
          if (localAddresses.contains(entry.getHostName())) {
            LOG.debug("Upgrading SC config " + entry.getPartition());
            entry.setBlockStorageManager(loadLocalBlockStorageManagerConfig());
            LOG.debug("Set storage manager " + entry.getBlockStorageManager() + " for SC " + entry.getPartition());
            break;
          }
        }
        db.commit( );
        return true;
      } catch ( Exception ex ) {
        db.rollback();
        throw Exceptions.toUndeclared( ex );
      }
    }
  }
}
