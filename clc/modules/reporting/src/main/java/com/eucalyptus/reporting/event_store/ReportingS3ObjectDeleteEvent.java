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
@Table(name="reporting_s3_object_delete_events")
public class ReportingS3ObjectDeleteEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;

	@Column(name="s3_bucket_name", nullable=false)
	private String s3BucketName;
	@Column(name="s3_object_key", nullable=false)
	private String s3ObjectKey;
	@Column(name="s3_object_version", nullable=true) //version can be null as per disc with Zach
	protected String objectVersion;

	protected ReportingS3ObjectDeleteEvent()
	{
	}
	
	protected ReportingS3ObjectDeleteEvent(String s3BucketName, String s3ObjectKey, String objectVersion,
			Long timestampMs)
	{
		this.s3BucketName = s3BucketName;
		this.s3ObjectKey = s3ObjectKey;
		this.objectVersion = objectVersion;
		this.timestampMs = timestampMs;
	}

	public String getS3BucketName()
	{
		return s3BucketName;
	}
	
	public String getS3ObjectKey()
	{
		return s3ObjectKey;
	}
	
	public String getObjectVersion()
	{
		return objectVersion;
	}
	
	@Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.relation( ReportingS3ObjectCreateEvent.class, "s3ObjectName", s3ObjectKey )
				.set();
	}

	@Override
	public String toString() {
	    return "ReportingS3ObjectDeleteEvent [s3BucketName=" + s3BucketName
		    + ", s3ObjectKey=" + s3ObjectKey + ", s3ObjectSize="
		    + ", timestampMs=" + timestampMs + "]";
	}

}
