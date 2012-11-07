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
package com.eucalyptus.reporting.art.generator;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.reporting.art.entity.AccountArtEntity;
import com.eucalyptus.reporting.art.entity.BucketUsageArtEntity;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.art.entity.UserArtEntity;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.event_store.ReportingS3ObjectCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingS3ObjectDeleteEvent;
import com.eucalyptus.reporting.units.SizeUnit;
import com.eucalyptus.reporting.units.TimeUnit;
import com.eucalyptus.reporting.units.UnitUtil;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class S3ArtGenerator
	extends AbstractArtGenerator
{
	private static Logger log = Logger.getLogger( S3ArtGenerator.class );

	@Override
  public ReportArtEntity generateReportArt(final ReportArtEntity report)
	{
		log.debug("Generating S3 report ART");

		/* NOTE: careful! This is subtler than it appears at first. A single object can
		 * be repeatedly created and deleted within the period, so we cannot just hang on
		 * to start times and end times then subtract at the end. Furthermore, an object
		 * can be created but never deleted.
		 */

		/* Find end timestamps for objects which are subsequently deleted, including
				 * objects which are created and deleted repeatedly during a single period.
				 */
		final Map<S3ObjectKey,List<Long>> endTimesMap = Maps.newHashMap();
		foreachReportingS3ObjectDeleteEvent( report.getEndMs(), buildTimestampMap( report, endTimesMap, key() ) );

		/* Generate a tree of zones, accounts, users, and bucket usages.
		 * Retain a Map of bucket usages at the leaf nodes.
		 * Find and retain start times and sizes for S3 objects.
		 * Set default object duration to the remaining of report (in case no
		 *   subsequent delete event is ever encountered), to be overwritten
		 *   if there is later found a delete event (or multiple delete events).
		 */
		final Map<String,ReportingUser> users = Maps.newHashMap();
		final Map<String,String> accounts = Maps.newHashMap();
		final Map<BucketUsageArtEntity,List<S3ObjectData>> bucketObjectData = Maps.newHashMap();
		foreachReportingS3ObjectCreateEvent( report.getEndMs(), new Predicate<ReportingS3ObjectCreateEvent>() {
			@Override
			public boolean apply( final ReportingS3ObjectCreateEvent createEvent ) {
				final S3ObjectKey objectKey = new S3ObjectKey(createEvent.getS3BucketName(),
						createEvent.getS3ObjectKey(), createEvent.getObjectVersion());
				final Long objectEndTime = Math.min( findTimeAfter( endTimesMap, objectKey, createEvent.getTimestampMs() ), report.getEndMs() );
				if ( objectEndTime < report.getBeginMs() ) {
					return true; // usage not relevant for this report
				}
				final ReportingUser reportingUser = getUserById( users, createEvent.getUserId() );
				if (reportingUser==null) {
					log.error("No user corresponding to event:" + createEvent.getUserId());
					return true;
				}
				final String accountName = getAccountNameById( accounts, reportingUser.getAccountId() );
				if (accountName==null) {
					log.error("No account corresponding to user:" + reportingUser.getAccountId());
					return true;
				}
				if (!report.getAccounts().containsKey( accountName )) {
					report.getAccounts().put(accountName, new AccountArtEntity());
				}
				final AccountArtEntity account = report.getAccounts().get(accountName);
				if (! account.getUsers().containsKey(reportingUser.getName())) {
					account.getUsers().put(reportingUser.getName(), new UserArtEntity());
				}
				final UserArtEntity user = account.getUsers().get(reportingUser.getName());
				if (! user.getBucketUsage().containsKey(createEvent.getS3BucketName())) {
					user.getBucketUsage().put(createEvent.getS3BucketName(), new BucketUsageArtEntity());
				}
				final BucketUsageArtEntity bucketUsage = user.getBucketUsage().get(createEvent.getS3BucketName());

				/* Retain the information necessary to calculate GB-secs from object durations
				 * later.
				 */
				final S3ObjectData data = new S3ObjectData(
						createEvent.getSize(),
						objectEndTime - Math.max(createEvent.getTimestampMs(),report.getBeginMs()) );
				List<S3ObjectData> bucketUserData = bucketObjectData.get( bucketUsage );
				if ( bucketUserData == null ) {
					bucketUserData = Lists.newLinkedList();
					bucketObjectData.put( bucketUsage, bucketUserData );
				}
				bucketUserData.add(data);
				return true;
			}
		});
		
		/* Perform totals and summations for user, account, zone, and bucket
		 */
		for ( final String accountName : report.getAccounts().keySet() ) {
			final AccountArtEntity account = report.getAccounts().get(accountName);
			for ( final String userName : account.getUsers().keySet() ) {
				final UserArtEntity user = account.getUsers().get(userName);
				for ( final String bucketName : user.getBucketUsage().keySet() ) {
					final BucketUsageArtEntity usage = user.getBucketUsage().get(bucketName);
					final List<S3ObjectData> objectUsages = bucketObjectData.get( usage );
					if ( objectUsages == null ) {
						log.error("Missing object usage for bucket");
						continue;
					}

					usage.setObjectsNum( objectUsages.size() );
					long size = 0;
					long KBSecs = 0;
					for ( final S3ObjectData data : objectUsages ) {
						size += data.size;
						KBSecs += (data.durationMs/1000) *
								UnitUtil.convertSize( data.size, SizeUnit.B, SizeUnit.KB );
					}

					long gBSecs =
							UnitUtil.convertSizeTime( KBSecs, SizeUnit.KB,  SizeUnit.GB, TimeUnit.SECS,TimeUnit.SECS );
					usage.setGBSecs( gBSecs );
					usage.setSize( size );
					updateUsageTotals(user.getUsageTotals().getBucketTotals(), usage);
					updateUsageTotals(account.getUsageTotals().getBucketTotals(), usage);
				}
			}
		}

		return report;
	}
	
	private static void updateUsageTotals(
			BucketUsageArtEntity totalEntity,
			BucketUsageArtEntity newEntity)
	{
		totalEntity.setObjectsNum( totalEntity.getObjectsNum() + newEntity.getObjectsNum() );
		totalEntity.setSize( totalEntity.getSize() + newEntity.getSize() );
		totalEntity.setGBSecs( totalEntity.getGBSecs() + newEntity.getGBSecs() );
	}


	private Function<ReportingS3ObjectDeleteEvent, S3ObjectKey> key() {
		return new Function<ReportingS3ObjectDeleteEvent, S3ObjectKey>() {
			@Override
			public S3ObjectKey apply( final ReportingS3ObjectDeleteEvent reportingEventSupport ) {
				return new S3ObjectKey(
						reportingEventSupport.getS3BucketName(),
						reportingEventSupport.getS3ObjectKey(),
						reportingEventSupport.getObjectVersion() );
			}
		};
	}

	private class S3ObjectKey
	{
		private final String bucketName;
		private final String objectKey;
		private final String objectVer;

		private S3ObjectKey(String bucketName, String objectKey,
				String objectVer)
		{
			this.bucketName = bucketName;
			this.objectKey = objectKey;
			this.objectVer = objectVer;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((bucketName == null) ? 0 : bucketName.hashCode());
			result = prime * result
					+ ((objectKey == null) ? 0 : objectKey.hashCode());
			result = prime * result
					+ ((objectVer == null) ? 0 : objectVer.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			S3ObjectKey other = (S3ObjectKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (bucketName == null) {
				if (other.bucketName != null)
					return false;
			} else if (!bucketName.equals(other.bucketName))
				return false;
			if (objectKey == null) {
				if (other.objectKey != null)
					return false;
			} else if (!objectKey.equals(other.objectKey))
				return false;
			if (objectVer == null) {
				if (other.objectVer != null)
					return false;
			} else if (!objectVer.equals(other.objectVer))
				return false;
			return true;
		}

		private S3ArtGenerator getOuterType() {
			return S3ArtGenerator.this;
		}	
	}
	
	private static class S3ObjectData
	{
		private final long durationMs;
		private final long size;
		
		private S3ObjectData( final long size, final long durationMs )
		{
			this.durationMs = durationMs;
			this.size = size;
		}
	}
	
	protected void foreachReportingS3ObjectCreateEvent( final long endExclusive, final Predicate<ReportingS3ObjectCreateEvent> callback ) {
		foreach( ReportingS3ObjectCreateEvent.class, before( endExclusive ), true, callback );
	}

	protected void foreachReportingS3ObjectDeleteEvent( final long endExclusive, final Predicate<ReportingS3ObjectDeleteEvent> callback ) {
		foreach( ReportingS3ObjectDeleteEvent.class, before( endExclusive ), true, callback );
	}
}
