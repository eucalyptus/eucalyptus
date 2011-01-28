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
package edu.ucsb.eucalyptus.cloud.entities;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableIdentifier;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.BlockStorageUtil;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;

import javax.persistence.*;

@Entity
@PersistenceContext(name="eucalyptus_storage")
@Table( name = "san_info" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ConfigurableClass(root = "storage", alias="san", description = "Basic storage controller configuration for SAN.", singleton=false, deferred = true)
public class
SANInfo {
	private static Logger LOG = Logger.getLogger( SANInfo.class );

	@Id
	@GeneratedValue
	@Column( name = "storage_san_id" )
	private Long id = -1l;
	@ConfigurableIdentifier
	@Column( name = "storage_name", unique=true)
	private String name;
	@ConfigurableField( description = "Hostname for SAN device.", displayName = "SAN Host" )
	@Column(name = "san_host")
	private String sanHost;
	@ConfigurableField( description = "Username for SAN device.", displayName = "SAN Username" )
	@Column(name = "san_user")
	private String sanUser;
	@ConfigurableField( description = "Password for SAN device.", displayName = "SAN Password", type = ConfigurableFieldType.KEYVALUEHIDDEN )
	@Column(name = "san_password")
	@Lob
	private String sanPassword;

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

	public Long getId()
	{
		return id;
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
		EntityWrapper<SANInfo> storageDb = new EntityWrapper<SANInfo>(StorageProperties.DB_NAME);
		SANInfo conf = null;
		try {
			conf = storageDb.getUnique(new SANInfo(StorageProperties.NAME));
			storageDb.commit();
		}
		catch ( EucalyptusCloudException e ) {
			LOG.warn("Failed to get storage info for: " + StorageProperties.NAME + ". Loading defaults.");
			conf =  new SANInfo(StorageProperties.NAME, 
					StorageProperties.SAN_HOST,
					StorageProperties.SAN_USERNAME,
					StorageProperties.SAN_PASSWORD);
			storageDb.add(conf);
			storageDb.commit();
		}
		catch (Throwable t) {
			LOG.error("Unable to get storage info for: " + StorageProperties.NAME);
			storageDb.rollback();
			return new SANInfo(StorageProperties.NAME, 
					StorageProperties.SAN_HOST,
					StorageProperties.SAN_USERNAME,
					StorageProperties.SAN_PASSWORD);
		}
		return conf;
	}
}
