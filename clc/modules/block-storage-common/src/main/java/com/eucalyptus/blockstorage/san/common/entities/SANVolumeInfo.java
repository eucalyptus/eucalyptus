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
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.blockstorage.san.common.entities;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.Version;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;

@Entity
@PersistenceContext(name="eucalyptus_storage")
@Table( name = "san_volume_info" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class SANVolumeInfo extends AbstractPersistent {
	protected String volumeId;
	private String scName;
	private String iqn;
	private String storeUser;
	@Type(type="org.hibernate.type.StringClobType")
	@Lob
	private String encryptedPassword;
	@Column(name = "size")
	protected Integer size;
	@Column(name = "status")
	private String status;
	@Column(name = "snapshot_of")
	private String snapshotOf;
	@Column(name = "san_volume_Id")
	private String sanVolumeId;

	public SANVolumeInfo() {
		this.scName = StorageProperties.NAME;
	}

	public SANVolumeInfo(String volumeId) {
		this();
		this.volumeId = volumeId;
	}

	public SANVolumeInfo(String volumeId, String iqn, int size) {
		this();
		this.volumeId = volumeId;
		this.iqn = iqn;
		this.size = size;
	}

	public String getIqn() {
		return iqn;
	}

	public void setIqn(String iqn) {
		this.iqn = iqn;
	}

	public String getStoreUser() {
		return storeUser;
	}

	public void setStoreUser(String storeUser) {
		this.storeUser = storeUser;
	}

	public String getEncryptedPassword() {
		return encryptedPassword;
	}

	public void setEncryptedPassword(String encryptedPassword) {
		this.encryptedPassword = encryptedPassword;
	}

	public String getVolumeId() {
		return volumeId;
	}

	public void setVolumeId(String volumeId) {
		this.volumeId = volumeId;
	}

	public String getScName() {
		return scName;
	}

	public void setScName(String scName) {
		this.scName = scName;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}
	
	public SANVolumeInfo withSize(Integer size) {
		this.size = size;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSnapshotOf() {
		return snapshotOf;
	}

	public void setSnapshotOf(String snapshotOf) {
		this.snapshotOf = snapshotOf;
	}
	
	public SANVolumeInfo withSnapshotOf(String snapshotOf) {
		this.snapshotOf = snapshotOf;
		return this;
	}

	public String getSanVolumeId() {
		return sanVolumeId;
	}

	public void setSanVolumeId(String sanVolumeId) {
		this.sanVolumeId = sanVolumeId;
	}

	public SANVolumeInfo withSanVolumeId(String sanVolumeId) {
		this.sanVolumeId = sanVolumeId;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((volumeId == null) ? 0 : volumeId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SANVolumeInfo other = (SANVolumeInfo) obj;
		if (volumeId == null) {
			if (other.volumeId != null)
				return false;
		} else if (!volumeId.equals(other.volumeId))
			return false;
		return true;
	}
	
	/**
	   * This upgrade is to push the snapshot size from this entity to the SnapshoInfo entity because
	   * Snapshot info cannot have a dependency on the backend volume entities.
	   *
	   */
	  @EntityUpgrade( entities = { SANVolumeInfo.class }, since = Version.v3_4_0, value = Storage.class )
	  public enum SANSnapshotSizeUpgrade3_4 implements Predicate<Class> {
		  INSTANCE;
		  private static Logger LOG = Logger.getLogger( SANVolumeInfo.SANSnapshotSizeUpgrade3_4.class );

		  @Override
		  public boolean apply( Class arg0 ) {
			  EntityTransaction db = Entities.get( SANVolumeInfo.class );
			  try {
				  SANVolumeInfo example = new SANVolumeInfo( );
				  example.setScName(null);
				  List<SANVolumeInfo> entities = Entities.query(example); 
				  for ( SANVolumeInfo entry : entities ) {
					  if(entry.getVolumeId().startsWith("snap-")) {						  
						  EntityTransaction snapDb = Entities.get(SnapshotInfo.class);
						  try {
							  SnapshotInfo exampleSnap = new SnapshotInfo(entry.getVolumeId());
							  exampleSnap.setScName(null); //all clusters.
							  List<SnapshotInfo> snaps = Entities.query(exampleSnap);
							  for(SnapshotInfo snap : snaps) {
								  if(snap.getSizeGb() == null) {
									  snap.setSizeGb(entry.getSize());								  
									  LOG.debug( "Upgrading: " + entry.getVolumeId() + " putting size from back-end to SnapshotInfo. Setting size to " + snap.getSizeGb());
								  } else {
									  //Do nothing, already set.
								  }
							  }
							  snapDb.commit();
						  } finally {
							  snapDb.rollback();
							  snapDb = null;
						  }
					  } else {
						  //Skip because not a snapshot record
						  LOG.debug("Skipping snapshot upgrade of " + entry.getVolumeId() + " because not a snapshot");
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