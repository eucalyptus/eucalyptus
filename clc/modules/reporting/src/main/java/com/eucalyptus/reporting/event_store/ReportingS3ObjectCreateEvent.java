/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
package com.eucalyptus.reporting.event_store;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import com.eucalyptus.component.annotation.RemotablePersistence;

@Entity
@PersistenceContext(name="eucalyptus_reporting_backend")
@RemotablePersistence
@Table(name="reporting_s3_object_create_events")
public class ReportingS3ObjectCreateEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;

	@Column(name="s3_bucket_name", nullable=false)
	protected String s3BucketName;
	@Column(name="s3_object_name", nullable=false)
	protected String s3ObjectKey;
	@Column(name="s3_object_version", nullable=true) //version can be null as per disc with Zach
	protected String objectVersion;
	@Column(name="size", nullable=false)
	protected Long size;
	@Column(name="user_id", nullable=false)
	protected String userId;
	
	/**
 	 * <p>Do not instantiate this class directly; use the ReportingS3ObjectCrud class.
 	 */
	protected ReportingS3ObjectCreateEvent()
	{
	}

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingS3ObjectCrud class.
 	 */
	ReportingS3ObjectCreateEvent(String s3BucketName, String s3ObjectKey, String objectVersion,
			Long size, Long timestampMs, String userId)
	{
		this.s3BucketName = s3BucketName;
		this.s3ObjectKey = s3ObjectKey;
		this.objectVersion = objectVersion;
		this.size = size;
		this.timestampMs = timestampMs;
		this.userId = userId;
	}

	public String getS3BucketName()
	{
		return this.s3BucketName;
	}

	public String getS3ObjectKey()
	{
		return this.s3ObjectKey;
	}

	public String getUserId()
	{
		return this.userId;
	}
	
	public Long getSize()
	{
	    	return this.size;
	}

	public String getObjectVersion()
	{
		return objectVersion;
	}

	@Override
	public EventDependency asDependency() {
		return asDependency( "s3ObjectName", s3ObjectKey );
	}

  @Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.user( userId )
				.set();
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = super.hashCode();
	    result = prime * result
		    + ((s3BucketName == null) ? 0 : s3BucketName.hashCode());
	    result = prime * result
		    + ((s3ObjectKey == null) ? 0 : s3ObjectKey.hashCode());
	    result = prime * result
		    + ((size == null) ? 0 : size.hashCode());
	    result = prime * result
		    + ((timestampMs == null) ? 0 : timestampMs.hashCode());
	    result = prime * result
		    + ((userId == null) ? 0 : userId.hashCode());
	    return result;
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
		return true;
	    if (!super.equals(obj))
		return false;
	    if (getClass() != obj.getClass())
		return false;
	    ReportingS3ObjectCreateEvent other = (ReportingS3ObjectCreateEvent) obj;
	    if (s3BucketName == null) {
		if (other.s3BucketName != null)
		    return false;
	    } else if (!s3BucketName.equals(other.s3BucketName))
		return false;
	    if (s3ObjectKey == null) {
		if (other.s3ObjectKey != null)
		    return false;
	    } else if (!s3ObjectKey.equals(other.s3ObjectKey))
		return false;
	    if (size == null) {
		if (other.size != null)
		    return false;
	    } else if (!size.equals(other.size))
		return false;
	    if (timestampMs == null) {
		if (other.timestampMs != null)
		    return false;
	    } else if (!timestampMs.equals(other.timestampMs))
		return false;
	    if (userId == null) {
		if (other.userId != null)
		    return false;
	    } else if (!userId.equals(other.userId))
		return false;
	    return true;
	}

	@Override
	public String toString() {
	    return "ReportingS3ObjectCreateEvent [s3BucketName=" + s3BucketName
		    + ", s3ObjectName=" + s3ObjectKey + ", timestampMs="
		    + timestampMs + ", userId=" + userId + ", s3ObjectSize="
		    + size + "]";
	}


}
