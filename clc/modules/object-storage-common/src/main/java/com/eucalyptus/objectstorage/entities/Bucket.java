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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.msgs.s3.BucketListEntry;

@Entity
@PersistenceContext(name="eucalyptus_osg")
@Table( name = "buckets" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class Bucket extends S3AccessControlledEntity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Column( name = "bucket_name", unique=true )
	private String bucketName;

	@Column( name = "bucket_creation_date" )
	private Date creationDate;

	@Column(name="bucket_location")
	private String location;

	@Column(name="hidden")
	private Boolean hidden;

	@Column(name="logging_enabled")
	private Boolean loggingEnabled;

	@Column(name="target_bucket")
	private String targetBucket;

	@Column(name="target_prefix")
	private String targetPrefix;

	@Column(name="versioning")
	private String versioning;
		
	//Needed for enforcing IAM size quotas, to prevent having to scan all objects
	@Column(name="bucket_size")
	private Long bucketSize;
		
	public Long getBucketSize() {
		return bucketSize;
	}

	public void setBucketSize(Long bucketSize) {
		this.bucketSize = bucketSize;
	}
	
	@Override
	public String getResourceFullName() {
		return getBucketName();
	}

	public Bucket() {
	}

	public Bucket(String bucketName) {
		this.bucketName = bucketName;
	}

	public Bucket(String ownerId, String ownerIamUserId, String bucketName, Date creationDate) {
		this.bucketName = bucketName;
		this.creationDate = creationDate;
		setOwnerCanonicalId(ownerId);
		setOwnerIamUserId(ownerIamUserId);
	}

	public String getBucketName()
	{
		return this.bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}
	
	public Date getCreationDate() {
		return creationDate;
	}
	
	public void setCreationDate(Date date){
		this.creationDate = date;
	}
		
	public boolean hasLoggingPerms() {
		//Logging requires write and readACP
		return this.can(ObjectStorageProperties.Permission.READ_ACP, ObjectStorageProperties.S3_GROUP.LOGGING_GROUP.toString())
				&& this.can(ObjectStorageProperties.Permission.WRITE, ObjectStorageProperties.S3_GROUP.LOGGING_GROUP.toString());
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public Boolean getLoggingEnabled() {
		return loggingEnabled;
	}

	public void setLoggingEnabled(Boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
	}

	public String getTargetBucket() {
		return targetBucket;
	}

	public void setTargetBucket(String targetBucket) {
		this.targetBucket = targetBucket;
	}

	public String getTargetPrefix() {
		return targetPrefix;
	}

	public void setTargetPrefix(String targetPrefix) {
		this.targetPrefix = targetPrefix;
	}

	public String getVersioning() {
		return versioning;
	}

	public void setVersioning(String versioning) {
		this.versioning = versioning;
	}

	public boolean isVersioningEnabled() {
		return ObjectStorageProperties.VersioningStatus.Enabled.toString().equals(versioning);	
	}
	
	public boolean isVersioningDisabled() {
		return ObjectStorageProperties.VersioningStatus.Disabled.toString().equals(versioning);	
	}
	
	public boolean isVersioningSuspended() {
		return ObjectStorageProperties.VersioningStatus.Suspended.toString().equals(versioning);	
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
		+ ((bucketName == null) ? 0 : bucketName.hashCode());
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
		Bucket other = (Bucket) obj;
		if (bucketName == null) {
			if (other.bucketName != null)
				return false;
		} else if (!bucketName.equals(other.bucketName))
			return false;
		return true;
	}
	
	public BucketListEntry toBucketListEntry() {
		return new BucketListEntry(this.getBucketName(), OSGUtil.dateToListingFormattedString(this.getCreationDate()));
	}
	
}
