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

package com.eucalyptus.blockstorage.entities;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.walrus.entities.WalrusSnapshotInfo;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

@Entity
@PersistenceContext(name="eucalyptus_storage")
@Table( name = "Snapshots" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class SnapshotInfo extends AbstractPersistent {
	private static final String AUTHORITY = "snapshots";
	
	@Column(name = "snapshot_user_name")
    private String userName;
    @Column(name = "sc_name")
    private String scName;
    @Column(name = "snapshot_name")
    String snapshotId;
    @Column(name = "volume_name")
    String volumeId;
    @Column(name = "status")
    String status;
    @Column(name = "start_time")
    Date startTime;
    @Column(name = "progress")
    private String progress;
    @Column(name = "should_transfer")
    private Boolean shouldTransfer;
    //TODO: zhill, persist the snapshot consistency point id here for cleanup. Should be removed upon snapshot completion
    @Column(name = "snapshot_point_id")
    private String snapPointId;
    @Column(name = "snapshot_size_gb")
    private Integer sizeGb;
    // Location of the snapshot in URI format storagebackend://bucket#key 
    @Column(name = "snapshot_location")
    private String snapshotLocation;
    
    public SnapshotInfo() {
    	this.scName = StorageProperties.NAME;
    }
    
    public SnapshotInfo(String snapshotId) {
    	this();
    	this.snapshotId = snapshotId;
    }
    
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getScName() {
		return scName;
	}

	public void setScName(String scName) {
		this.scName = scName;
	}

	public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

	public Boolean getShouldTransfer() {
		return shouldTransfer;
	}

	public void setShouldTransfer(Boolean shouldTransfer) {
		this.shouldTransfer = shouldTransfer;
	}
	
	public String getSnapPointId() {
		return snapPointId;
	}

	public void setSnapPointId(String snapPointId) {
		this.snapPointId = snapPointId;
	}
	
	public Integer getSizeGb() {
		return sizeGb;
	}

	public void setSizeGb(Integer snapSizeGb) {
		this.sizeGb = snapSizeGb;
	}
	
	public String getSnapshotLocation() {
		return snapshotLocation;
	}

	public void setSnapshotLocation(String snapshotLocation) {
		this.snapshotLocation = snapshotLocation;
	}
	
	/**
	 * Generate snapshot location uri in the format: snapshots://server/bucket/key
	 * 
	 * @param server
	 * @param bucket
	 * @param key
	 * @return
	 * @throws URISyntaxException
	 * @throws EucalyptusCloudException 
	 */
	public static String generateSnapshotLocationURI(String server, String bucket, String key) throws URISyntaxException, EucalyptusCloudException {
		if(StringUtils.isNotBlank(bucket) && StringUtils.isNotBlank(key)) {
			URI snapshotUri = new URI(AUTHORITY, server, ('/' + bucket + '/' + key), null, null);
			return snapshotUri.toString();
		} else {
			throw new EucalyptusCloudException("Invalid bucket and or key names. Bucket: " + bucket + ", Key: " + key);
		}
	}
	
	/**
	 * Parse the snapshot location uri such as snapshots://server/bucket/key and return bucket and key
	 * 
	 * @param snapshotUriString
	 * @return 
	 * @throws URISyntaxException
	 * @throws EucalyptusCloudException
	 */
	public static String[] getSnapshotBucketKeyNames(String snapshotUriString) throws URISyntaxException, EucalyptusCloudException {
		if(StringUtils.isNotBlank(snapshotUriString)) {
			URI snapshotUri = new URI(snapshotUriString);
			String[] parts = snapshotUri.getPath().split("/");
			// Path would be in the format /snapshot-bucket/snapshot-key. Splitting it on '/' returns 3 strings, the first should be blank
			if(parts.length >= 3) {
				if(StringUtils.isBlank(parts[0]) && StringUtils.isNotBlank(parts[1]) && StringUtils.isNotBlank(parts[2])) {
					return new String[]{parts[1], parts[2]};
				} else {
					throw new EucalyptusCloudException("Bucket and or key name not found in snapshot location: " + snapshotUriString);
				}
			} else {
				throw new EucalyptusCloudException("Failed to parse bucket and key names from snapshot location: " + snapshotUriString);
			} 
		} else {
			throw new EucalyptusCloudException("Invalid snapshot location: " + snapshotUriString);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((scName == null) ? 0 : scName.hashCode());
		result = prime * result
				+ ((snapshotId == null) ? 0 : snapshotId.hashCode());
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
		SnapshotInfo other = (SnapshotInfo) obj;
		if (scName == null) {
			if (other.scName != null)
				return false;
		} else if (!scName.equals(other.scName))
			return false;
		if (snapshotId == null) {
			if (other.snapshotId != null)
				return false;
		} else if (!snapshotId.equals(other.snapshotId))
			return false;
		return true;
	}

	@EntityUpgrade( entities = { SnapshotInfo.class }, since = Upgrades.Version.v3_4_0, value = Storage.class)
	public enum SnapshotEntityUpgrade implements Predicate<Class> {
		INSTANCE;
		private static Logger LOG = Logger.getLogger(SnapshotInfo.SnapshotEntityUpgrade.class);
		
		@Override
		public boolean apply(@Nullable Class aClass) {
			EntityTransaction db = Entities.get(SnapshotInfo.class);
			try {
				SnapshotInfo example = new SnapshotInfo();
				List<SnapshotInfo> snaps = Entities.query(example);
				if (snaps != null && snaps.size() > 0) {
					for (SnapshotInfo snapshot : snaps) {
						if (snapshot.getSizeGb() == null) {
							//Do lookup for source volume
							EntityTransaction volDb = Entities.get(VolumeInfo.class);
							try {
								VolumeInfo vol = Entities.uniqueResult(new VolumeInfo(snapshot.getVolumeId()));
								if(vol != null) {
									snapshot.setSizeGb(vol.getSize());
									LOG.debug("Setting snapshot size on entity: " + snapshot.getScName() + snapshot.getSnapshotId() + " to : " + snapshot.getSizeGb());
									db.commit();
								} else {
									//No volume record found. May have been deleted. Only source of data now is the snapshot backend record.
									LOG.debug("No volume record found for snapshot " + snapshot.getScName() + ", " + snapshot.getSnapshotId() + " will have size set on upgrade of the backend snapshot entity");
								}
								
							} finally {
								volDb.rollback();
								volDb = null;
							}							
						} else {
							//Already set, do nothing.
						}
					}
				}
				db.commit();
			} catch (Exception ex) {
				LOG.error("caught exception during upgrade, while attempting to set snapshot size");
				throw Exceptions.toUndeclared(ex);				
			} finally {
				db.rollback();
			}
			return true;
		}
	}
	
	@EntityUpgrade(entities = { SnapshotInfo.class }, since = Upgrades.Version.v4_0_0, value = Storage.class)
	public enum SnapshotEntity400Upgrade implements Predicate<Class> {
		INSTANCE;
		private static Logger LOG = Logger.getLogger(SnapshotInfo.SnapshotEntity400Upgrade.class);

		@Override
		public boolean apply(@Nullable Class arg0) {

			EntityTransaction db = Entities.get(SnapshotInfo.class);
			try {
				SnapshotInfo example = new SnapshotInfo();
				example.setScName(null); // neccessary as it defaults to the SC name
				List<SnapshotInfo> snaps = Entities.query(example);

				if (snaps != null & !snaps.isEmpty()) {
					Map<String, String> snapIDBucketNameMap = Maps.newHashMap();
					Map<String, Integer> snapIDSizeMap = Maps.newHashMap();

					// Populate snapshot IDs and buckets into a map
					EntityTransaction walrusSnapshotsdb = Entities.get(WalrusSnapshotInfo.class);
					try {
						List<WalrusSnapshotInfo> walrusSnapshots = Entities.query(new WalrusSnapshotInfo(), Boolean.TRUE);
						for (WalrusSnapshotInfo walrusSnapshot : walrusSnapshots) {
							snapIDBucketNameMap.put(walrusSnapshot.getSnapshotId(), walrusSnapshot.getNaturalId());
							snapIDSizeMap.put(walrusSnapshot.getSnapshotId(), walrusSnapshot.getSize());
						}
					} catch (Exception e) {
						walrusSnapshotsdb.rollback();
						return false;
					} finally {
						if (walrusSnapshotsdb.isActive()) {
							walrusSnapshotsdb.commit();
						}
					}

					// Populate the snapshot location and size if they are not already populated
					for (SnapshotInfo snap : snaps) {
						String snapId = snap.getSnapshotId();
						if (snapIDBucketNameMap.containsKey(snapId)) {
							if (snap.getSnapshotLocation() == null) {
								snap.setSnapshotLocation(SnapshotInfo.generateSnapshotLocationURI(SnapshotTransferConfiguration.OSG,
										snapIDBucketNameMap.get(snapId), snapId));
							}

							if (snap.getSizeGb() == null) {
								snap.setSizeGb(snapIDSizeMap.get(snapId));
							}
						}
					}
				} else {
					// nothing to upgrade
				}
				db.commit();
			} catch (Exception e) {
				LOG.error("Failed to upgrade location/size during SnapshotInfo entity upgrade", e);
				db.rollback();
				return false;
			} finally {
				if (db.isActive()) {
					db.commit();
				}
			}

			return true;
		}
	}
}
