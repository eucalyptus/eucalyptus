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

public class ReportingS3ObjectEventStore extends EventStoreSupport
{
  private static ReportingS3ObjectEventStore instance = new ReportingS3ObjectEventStore();

  public static ReportingS3ObjectEventStore getInstance() {
    return instance;
  }

  protected ReportingS3ObjectEventStore() {
  }

  public void insertS3ObjectCreateEvent(String s3BucketName, String s3ObjectKey, String objectVersion,
		  long size, long timestampMs, String userId)
  {
    persist( new ReportingS3ObjectCreateEvent(s3BucketName, s3ObjectKey, objectVersion, size, timestampMs, userId) );
  }

  public void insertS3ObjectDeleteEvent(String s3BucketName, String s3ObjectKey, String objectVersion,
		  long timestampMs)
  {
    persist( new ReportingS3ObjectDeleteEvent(s3BucketName, s3ObjectKey, objectVersion, timestampMs) );
  }
}
