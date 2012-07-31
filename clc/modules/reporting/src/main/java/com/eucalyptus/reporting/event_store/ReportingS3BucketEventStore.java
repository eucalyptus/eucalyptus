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
 ************************************************************************/

package com.eucalyptus.reporting.event_store;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

public class ReportingS3BucketEventStore
{
	private static Logger LOG = Logger.getLogger( ReportingS3BucketEventStore.class );

	private static ReportingS3BucketEventStore instance = null;
	
	public static synchronized ReportingS3BucketEventStore getInstance()
	{
		if (instance == null) {
			instance = new ReportingS3BucketEventStore();
		}
		return instance;
	}
	
	private ReportingS3BucketEventStore()
	{
		
	}

	public void insertS3BucketCreateEvent(String s3BucketName, String userId, String availabilityZone)
	{
		EntityWrapper<ReportingS3BucketCreateEvent> entityWrapper =
			EntityWrapper.get(ReportingS3BucketCreateEvent.class);

		try {
			ReportingS3BucketCreateEvent s3Bucket = new ReportingS3BucketCreateEvent(s3BucketName, userId, availabilityZone);
			entityWrapper.add(s3Bucket);
			entityWrapper.commit();
			LOG.debug("Added event to db:" + s3Bucket);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

	public void insertS3BucketDeleteEvent(String s3BucketName, long timestampMs)
	{
		EntityWrapper<ReportingS3BucketDeleteEvent> entityWrapper =
			EntityWrapper.get(ReportingS3BucketDeleteEvent.class);

		try {
			ReportingS3BucketDeleteEvent s3Bucket = new ReportingS3BucketDeleteEvent(s3BucketName, timestampMs);
			entityWrapper.add(s3Bucket);
			entityWrapper.commit();
			LOG.debug("Added event to db:" + s3Bucket);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
		
	}
}

