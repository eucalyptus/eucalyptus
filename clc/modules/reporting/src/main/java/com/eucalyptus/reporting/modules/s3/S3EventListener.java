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

package com.eucalyptus.reporting.modules.s3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.S3Event;
import com.eucalyptus.reporting.domain.ReportingAccountCrud;
import com.eucalyptus.reporting.domain.ReportingUserCrud;
import com.google.common.base.Preconditions;

@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class S3EventListener implements EventListener<S3Event> {
  private static final Logger LOG = Logger.getLogger( S3EventListener.class );

  private static final long WRITE_INTERVAL_MS = TimeUnit.HOURS.toMillis( 3L );
  private final ReadWriteLock usageDataLock = new ReentrantReadWriteLock();
  private ConcurrentMap<S3SummaryKey, S3UsageData> usageDataMap; //TODO Expire data
  private final AtomicLong lastAllSnapshotMs = new AtomicLong( 0L );
  private final Object allSnapshotLock = new Object();

  public static void register() {
    Listeners.register(S3Event.class, new S3EventListener());
  }

  public S3EventListener() {
  }

  @Override
  public void fireEvent( @Nonnull final S3Event event ) {
    Preconditions.checkNotNull( event, "Event is required" );

    /* Retain records of all account and user id's and names encountered
    * even if they're subsequently deleted.
    */
    ReportingAccountCrud.getInstance().createOrUpdateAccount(
        event.getAccountId(), event.getAccountName());
    ReportingUserCrud.getInstance().createOrUpdateUser(event.getOwnerId(), event.getAccountId(),
        event.getOwnerName());

    final long timeMillis = getCurrentTimeMillis();
    final S3UsageLog usageLog = S3UsageLog.getS3UsageLog();

    final EntityTransaction entityTransaction = Entities.get(S3UsageSnapshot.class);
    try {
      // Load usageDataMap if starting up
      ConcurrentMap<S3SummaryKey, S3UsageData> usageDataMap;
      usageDataLock.readLock().lock();
      try {
        usageDataMap = this.usageDataMap;
      } finally {
        usageDataLock.readLock().unlock();
      }

      if ( usageDataMap == null ) {
          usageDataLock.writeLock().lock();
          try {
            if ( this.usageDataMap == null ) {
              usageDataMap = this.usageDataMap =
                  new ConcurrentHashMap<S3SummaryKey,S3UsageData>( usageLog.findLatestUsageData() );
            } else {
              usageDataMap = this.usageDataMap;
            }
            LOG.info("Loaded usageDataMap");
          } finally {
            usageDataLock.writeLock().unlock();
          }
      }

      // Update usageDataMap
      final S3SummaryKey key = new S3SummaryKey( event.getOwnerId(), event.getAccountId() );
      final S3UsageData usageData = usageDataMap.putIfAbsent(key, new S3UsageData());
      final long addNum = (event.isCreateOrDelete()) ? 1 : -1;

      if ( event.isObjectOrBucket() ) {
        synchronized ( usageData ) {
          final long addAmountMegs = (event.isCreateOrDelete())
              ? event.getSizeMegs()
              : -event.getSizeMegs();
          LOG.debug("Receive event:" + event.toString() + " usageData:" + usageData + " addNum:" + addNum + " addAmountMegs:" + addAmountMegs);

          final Long newObjectsNum = usageData.getObjectsNum() + addNum;
          if (newObjectsNum != null && newObjectsNum < 0) {
            throw new IllegalStateException("Objects num cannot be negative");
          }
          usageData.setObjectsNum(newObjectsNum);
          final Long newObjectsMegs = usageData.getObjectsMegs() + addAmountMegs;
          if (newObjectsMegs != null && newObjectsMegs < 0) {
            throw new IllegalStateException("Objects megs cannot be negative");
          }
          usageData.setObjectsMegs(newObjectsMegs);
        }
      } else synchronized ( usageData ) {
        final Long newBucketsNum = usageData.getBucketsNum() + addNum;
        if (newBucketsNum != null && newBucketsNum < 0) {
          throw new IllegalStateException("Buckets num cannot be negative");
        }
        usageData.setBucketsNum(newBucketsNum);
        LOG.debug("Receive event:" + event.toString() + " usageData:" + usageData + " addNum:" + addNum);
      }

      // Write data to DB
      boolean wroteAll = false;
      if ( shouldWriteAll(timeMillis) ) {
        // Write all snapshots
        synchronized ( allSnapshotLock ) {
          if ( shouldWriteAll(timeMillis) ) {
            LOG.info("Starting allSnapshot...");
            for ( Map.Entry<S3SummaryKey,S3UsageData> entry : usageDataMap.entrySet() ) {
              final S3UsageSnapshot sus = toUsageSnapshot(timeMillis, entry.getKey(), entry.getValue());
              sus.setAllSnapshot(true);
              LOG.info("Storing part of allSnapshot:" + sus);
              Entities.persist(sus);
              lastAllSnapshotMs.set( timeMillis );
              wroteAll = true;
            }
            LOG.info("Ending allSnapshot...");
          }
        }
      }

      if ( !wroteAll ) {
        // Write this snapshot
        final S3UsageSnapshot sus = toUsageSnapshot(timeMillis, key, usageData);
        LOG.info("Storing:" + sus);
        Entities.persist(sus);
      }

      entityTransaction.commit();
    } catch (Exception ex) {
      entityTransaction.rollback();
      LOG.error(ex);
    }
  }

  private S3UsageSnapshot toUsageSnapshot( final long timeMillis,
                                           final S3SummaryKey key,
                                           final S3UsageData usageData) {
    final S3SnapshotKey snapshotKey = new S3SnapshotKey(
        key.getOwnerId(), key.getAccountId(),
        timeMillis);
    final S3UsageSnapshot sus;
    synchronized ( usageData ) {
        sus = new S3UsageSnapshot(snapshotKey, usageData);
    }
    return sus;
  }

  private boolean shouldWriteAll( final long timeMillis ) {
    return (timeMillis - lastAllSnapshotMs.get()) > WRITE_INTERVAL_MS;
  }

  /**
   * Overridable for the purpose of testing.
   */
  protected long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }
}
