/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.blockstorage.san.common.entities;

import java.util.List;
import java.util.TreeMap;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import com.eucalyptus.blockstorage.san.common.SANProperties;
import com.eucalyptus.blockstorage.util.BlockStorageUtil;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableIdentifier;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.HostSpecifier;

@Entity
@PersistenceContext(name="eucalyptus_storage")
@Table( name = "san_info" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ConfigurableClass(root = "storage", alias="san", description = "Basic storage controller configuration for SAN.", singleton=false, deferred = true)
public class SANInfo extends AbstractPersistent {
	private static Logger LOG = Logger.getLogger( SANInfo.class );

	// The SAN controller management port address cache
	private static String currentSanHosts = null;
	private static TreeMap<String, Long> sanControllerAddresses = Maps.newTreeMap();
	private static final Long ADDRESS_FAILURE_RETRY_INTERVAL_IN_MILLIS = 300 * 1000L; // 5 mins
	private static final long TASK_TIMEOUT = 5 * 60 * 1000;
	public static final String DEFAULT_CHAP_USER = "nouser";

	@ConfigurableIdentifier
	@Column( name = "storage_name", unique=true)
	protected String name;
	@ConfigurableField( description = "Hostname for SAN device.", displayName = "SAN Host" )
	@Column(name = "san_host")
	private String sanHost;
	@ConfigurableField( description = "Username for SAN device.", displayName = "SAN Username" )
	@Column(name = "san_user")
	private String sanUser;
	@ConfigurableField( description = "Password for SAN device.", displayName = "SAN Password", type = ConfigurableFieldType.KEYVALUEHIDDEN )
	@Column(name = "san_password")
	@Type(type="org.hibernate.type.StringClobType")
	@Lob
	private String sanPassword;
	@ConfigurableField( description = "User ID for CHAP authentication", displayName = "CHAP user", type = ConfigurableFieldType.KEYVALUE )
	@Column(name = "chap_user")
	private String chapUser;
	@ConfigurableField( description = "iSCSI Paths for NC", displayName = "NC paths", type = ConfigurableFieldType.KEYVALUE, changeListener = PathsChangeListener.class )
	@Column(name = "ncpaths")
	private String ncPaths;
	@ConfigurableField( description = "iSCSI Paths for SC", displayName = "SC paths", type = ConfigurableFieldType.KEYVALUE, changeListener = PathsChangeListener.class )
	@Column(name = "scpaths")
	private String scPaths;
	@ConfigurableField( description = "Timeout for SAN commands.", displayName = "SAN Task Timeout" )
	@Column(name = "task_timeout")
	private Long taskTimeout;

	public static final String DEFAULT_PATHS = "nopath";

	public SANInfo(){
		this.name = StorageProperties.NAME;
	}

	public SANInfo( final String name )
	{
		this.name = name;
	}

	public SANInfo(final String name, 
			final String sanHost,
			final String sanUser,
			final String sanPassword) {
		this.name = name;
		this.sanHost = sanHost;
		this.sanUser = sanUser;
		this.sanPassword = sanPassword;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSanHost() {
		return sanHost;
	}

	public void setSanHost(String sanHost) {
		this.sanHost = sanHost;
	}

	public String getSanUser() {
		return sanUser;
	}

	public void setSanUser(String sanUser) {
		this.sanUser = sanUser;
	}

	public String getSanPassword() {
		try {
			return BlockStorageUtil.decryptSCTargetPassword(sanPassword);
		} catch(EucalyptusCloudException ex) {
			LOG.error(ex);
			return null;
		}
	}

	public void setSanPassword(String sanPassword) {
		try {
			this.sanPassword = BlockStorageUtil.encryptSCTargetPassword(sanPassword);
		} catch(EucalyptusCloudException ex) {
			LOG.error(ex);
		}
	}

	public String getChapUser() {
		return chapUser;
	}

	public void setChapUser(String chapUser) {
		this.chapUser = chapUser;
	}

	public String getNcPaths() {
		return this.ncPaths;
	}

	public void setNcPaths(String paths) {
		this.ncPaths = paths;
	}

	public String getScPaths() {
		return this.scPaths;
	}

	public void setScPaths(String scPaths) {
		this.scPaths = scPaths;
	}

	public Long getTaskTimeout() {
		return taskTimeout;
	}

	public void setTaskTimeout(Long taskTimeout) {
		this.taskTimeout = taskTimeout;
	}

	@PreUpdate
	@PostLoad
	public void setDefaults() {
		if(this.taskTimeout == null) {
			this.taskTimeout = TASK_TIMEOUT;
		}
		if(this.chapUser == null) {
			this.chapUser = DEFAULT_CHAP_USER;
		}
	}

	private static SANInfo newDefault() {
		SANInfo sanInfo = new SANInfo();
		sanInfo.setName(StorageProperties.NAME); 
		sanInfo.setSanHost(SANProperties.SAN_HOST);
		sanInfo.setSanUser(SANProperties.SAN_USERNAME);
		sanInfo.setSanPassword(SANProperties.SAN_PASSWORD);
		sanInfo.setChapUser(DEFAULT_CHAP_USER);
		sanInfo.setNcPaths(SANInfo.DEFAULT_PATHS);
		sanInfo.setScPaths(SANInfo.DEFAULT_PATHS);
		sanInfo.setTaskTimeout(TASK_TIMEOUT);
		return sanInfo;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SANInfo other = (SANInfo) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public String toString()
	{
		return this.name;
	}

	public static SANInfo getStorageInfo() {
		EntityWrapper<SANInfo> storageDb = EntityWrapper.get(SANInfo.class);
		SANInfo conf = null;
		try {
			conf = storageDb.getUnique(new SANInfo(StorageProperties.NAME));
			storageDb.commit();
		}
		catch ( EucalyptusCloudException e ) {
			LOG.warn("Failed to get storage info for: " + StorageProperties.NAME + ". Loading defaults.");
			conf = newDefault();
			storageDb.add(conf);
			storageDb.commit();
		}
		catch (Exception t) {
			LOG.error("Unable to get storage info for: " + StorageProperties.NAME);
			storageDb.rollback();
			conf = newDefault();
		}
		return conf;
	}

	private static void loadSanControllerAddressesIfChanged() {
		String hosts = getStorageInfo().getSanHost();
		if (currentSanHosts == null || !currentSanHosts.equals(hosts)) {
			sanControllerAddresses.clear();
			if (SANProperties.DUMMY_SAN_HOST.equals(hosts) || Strings.isNullOrEmpty(hosts)) {
				return;
			}
			for (String host : hosts.split(",")) {
				host = host.trim();
				if (!Strings.isNullOrEmpty(host)) {
					sanControllerAddresses.put(host, 0L);
				}
			}
			currentSanHosts = hosts;
		}
	}

	/**
	 * Get a usable SAN controller management port address:
	 * find the address whose last failure time is earlier than the retry limit.
	 * 
	 * @return the working address. Empty if none is available.
	 */
	public static synchronized String getSanControllerAddress() {
		loadSanControllerAddressesIfChanged();
		for (String addr : sanControllerAddresses.keySet()) {
			Long lastFailureTime = sanControllerAddresses.get(addr);
			if (System.currentTimeMillis() - lastFailureTime > ADDRESS_FAILURE_RETRY_INTERVAL_IN_MILLIS) {
				return addr;
			}
		}
		return null;
	}

	/**
	 * Register an address failure. This will cause the next address retrieval to
	 * fail over to another address.
	 * 
	 * @param address The failed address.
	 */
	public static synchronized void setSanControllerAddressFailure(String address) {
		sanControllerAddresses.put(address, System.currentTimeMillis());
	}

	/**
	 * The internal representation of an iSCSI path:
	 * 
	 * Host iface -> SAN device port
	 * 
	 * @author wenye
	 *
	 */
	public static class Path {
		public String iface;  // The host iface
		public String ip;     // The SAN port IP
		public String sp;     // The SAN device SP
		public String portId; // The SAN device SP port
		public String iqn;    // The SAN port IQN

		public Path(String iface, String ip) {
			this.iface = iface;
			this.ip = ip;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(iface).append(',').append(ip);
			return sb.toString();
		}
	}


	public static class PathsChangeListener implements PropertyChangeListener {

		@Override
		public void fireChange(ConfigurableProperty t, Object newValue) throws ConfigurablePropertyException {
			try {
				if (!SANInfo.DEFAULT_PATHS.equals(newValue)) {
					parsePaths((String)newValue);
				}
			} catch (IllegalArgumentException e) {
				throw new ConfigurablePropertyException("Invalid paths: " + e, e);
			}
		}

	}


	/**
	 * Parsing the iSCSI paths string into the internal representation. The iSCSI paths string format
	 * as in the system property value:
	 * 
	 * <NC host interface name>:<device port IP address>,<NC host interface name>:<device port IP address>;...
	 * 
	 * A typical example:
	 * 
	 * iface0:192.168.25.182,iface1:10.109.25.186
	 * 
	 * Interface can be omitted (which means using default interface). So it will become:
	 * 
	 * 192.168.25.182,10.109.25.186
	 * 
	 * @param paths Paths string
	 * @return The internal paths structure
	 * @throws IllegalArgumentException If the paths string has wrong format. Note that we only check syntax
	 * correctness.
	 */
	private static final String PATH_SEPARATOR = ",";
	private static final String PATH_FIELD_SEPARATOR = ":";

	public static List<SANInfo.Path> parsePaths(String paths) {
		if (Strings.isNullOrEmpty(paths)) {
			throw new IllegalArgumentException("Empty paths");
		}
		String[] splitPaths = paths.split(PATH_SEPARATOR);
		if (splitPaths == null || splitPaths.length < 1) {
			throw new IllegalArgumentException("Invalid paths " + paths);
		}
		List<SANInfo.Path> parsed = Lists.newArrayList();
		for (String path : splitPaths) {
			String[] splitOnePath = path.split(PATH_FIELD_SEPARATOR);
			if (splitOnePath == null || splitOnePath.length < 1 || splitOnePath.length > 2) {
				throw new IllegalArgumentException("Invalid path " + path);
			}
			String iface;
			String ip;
			if (splitOnePath.length == 1) {
				iface = "";
				ip = splitOnePath[0];
			} else {
				iface = splitOnePath[0];
				ip = splitOnePath[1];
			}
			HostSpecifier.fromValid(ip);
			parsed.add(new SANInfo.Path(iface, ip));
		}
		return parsed;
	}

}
