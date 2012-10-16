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

package com.eucalyptus.config;

import java.io.Serializable;
import java.util.List;

import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Column;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;

import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableIdentifier;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.storage.StorageManagers;
import com.google.common.base.Joiner;

@Entity
@javax.persistence.Entity
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
	 * Change Listener for san provider and backend config options.
	 * The semantics enforced are that they can be set once per registered SC. If the value has already been configured it will
	 * throw an exception if the user tries to reconfigure them.
	 *
	 * This is done to prevent data loss and database corruption that can occur if the user tries to change the backend after it
	 * already has some state.
	 */
	public static class StorageBackendChangeListener implements PropertyChangeListener<String> {
		@Override
		public void fireChange(ConfigurableProperty t, String newValue) throws ConfigurablePropertyException {
			String existingValue = (String)t.getValue();
			if(existingValue != null && !"<unset>".equals(existingValue)) {
				throw new ConfigurablePropertyException("Cannot change extant storage backend configuration.");
			}
			else if(!StorageManagers.contains(newValue)){
				throw new ConfigurablePropertyException("Cannot modify " + t.getAlias() + " new value is not a valid value.  " +
						"Legal values are: " + Joiner.on( "," ).join( StorageManagers.list( ) ) );
			}
		}
	}

	@PrePersist
	private void updateRedundantConfig( ) {
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
		//return this.propertyPrefix;
		return this.getPartition();
	}
	
	public void setPropertyPrefix(String p) {
		//this.propertyPrefix = p;
		this.setPartition(p);
	}
	
}
