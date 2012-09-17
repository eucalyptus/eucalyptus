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

package com.eucalyptus.reporting.modules.instance;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import org.apache.log4j.*;
import org.hibernate.exception.ConstraintViolationException;

import com.eucalyptus.bootstrap.OrderedShutdown;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.*;
import com.eucalyptus.reporting.event_store.ReportingInstanceEventStore;
import com.eucalyptus.reporting.event_store.ReportingInstanceUsageEvent;
import com.eucalyptus.reporting.domain.ReportingAccountCrud;
import com.eucalyptus.reporting.domain.ReportingUserCrud;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;

@ConfigurableClass( root = "reporting", description = "Parameters controlling reporting")
public class InstanceUsageEventListener implements EventListener<InstanceUsageEvent> {
  private static final Logger log = Logger.getLogger( InstanceUsageEventListener.class );

  @ConfigurableField( initial = "1200", description = "How often the reporting system writes instance snapshots" )
  public static long DEFAULT_WRITE_INTERVAL_SECS = 1200;

  private final Set<String> recentlySeenUuids = Sets.newSetFromMap( getExpiringMapMaker().<String,Boolean>makeMap() );
  private final Map<String, DatedInstanceEvent> recentUsageEvents = getExpiringMapMaker().makeMap();
  private final ReadWriteLock persistenceLock = new ReentrantReadWriteLock(); // lock for recentUsageEvents bulk updates
  private final AtomicLong lastWriteMs = new AtomicLong( 0L );

  public static void register( ) {
    final InstanceUsageEventListener listener = new InstanceUsageEventListener( );
    Listeners.register( InstanceUsageEvent.class, listener );
    OrderedShutdown.registerPreShutdownHook( new Runnable() {
      @Override
      public void run( ) {
        listener.flush();
      }
    } );
  }

  public void fireEvent( @Nonnull final InstanceUsageEvent event ) {
    final long receivedEventMs = getCurrentTimeMillis();

    log.debug("Received instance usage event:" + event);

    final String uuid = event.getUuid();
    if ( uuid == null ) {
      log.warn("Received null uuid");
      return;
    }

    /* Retain records of all account and user id's and names encountered
     * even if they're subsequently deleted.
     */
    //getReportingAccountCrud().createOrUpdateAccount( event.getAccountId(), event.getAccountName() );
    //getReportingUserCrud().createOrUpdateUser( event.getUserId(), event.getAccountId(), event.getUserName() );

    // Write the instance attributes, but only if we don't have it already.
    if ( !recentlySeenUuids.contains(uuid) ) {
      try {
        log.info( "Wrote Reporting Instance:" + uuid );
        final ReportingInstanceEventStore eventStore = getReportingInstanceEventStore();
        eventStore.insertUsageEvent(
            event.getUuid(),
            receivedEventMs,
            event.getResourceName(),
            event.getMetric(),
            event.getSequenceNum(),
            event.getDimension(),
            event.getValue(),
            event.getValueTimestamp()
        );
      } catch ( ConstraintViolationException ex ) {
        log.debug( ex, ex ); // info already exists for instance
      } catch ( Exception ex ) {
        log.error( ex, ex );
      } finally {
        recentlySeenUuids.add( uuid );
      }
    }

    /* Gather the latest usage snapshots (they're cumulative, so
     * intermediate ones don't matter except for granularity), and
     * write them all to the database at once every n secs.
     */
    boolean accepted = false;
    persistenceLock.readLock().lock();
    try {
      final DatedInstanceEvent oldInstanceEvent = recentUsageEvents.get(uuid);
      if ( oldInstanceEvent==null || oldInstanceEvent.getTimestamp() < receivedEventMs ) {
        recentUsageEvents.put( uuid, new DatedInstanceEvent( receivedEventMs, event ) );
        accepted = true;
      }
    } finally {
      persistenceLock.readLock().unlock();
    }
    if (!accepted) log.error( "Events are arriving out of order" );

    if ( receivedEventMs > ( lastWriteMs.get() + (DEFAULT_WRITE_INTERVAL_SECS * 1000) ) ) {
      try {
        flush();
        lastWriteMs.set( receivedEventMs );
      } catch ( Exception ex ) {
        log.error( ex, ex );
      }
    }
  }

  //TODO: shutdown hook
  public void flush() {
    final ReportingInstanceEventStore eventStore = getReportingInstanceEventStore();
    persistenceLock.writeLock().lock();
    try {
      transactional( ReportingInstanceUsageEvent.class, new Function<Collection<DatedInstanceEvent>,Void>(){
        @Override
        public Void apply( final Collection<DatedInstanceEvent> datedInstanceEvents ) {
          for ( final DatedInstanceEvent datedEvent : datedInstanceEvents ) {
            final InstanceUsageEvent event = datedEvent.getInstanceEvent();
            
            eventStore.insertUsageEvent(
                event.getUuid(),
                event.getTimestamp(),
                event.getResourceName(),
                event.getMetric(),
                event.getSequenceNum(),
                event.getDimension(),
                event.getValue(),
                event.getValueTimestamp()
            );
            log.debug( "Wrote instance usage for: " + event.getUuid() );
          }
          return null;
        }
      } ).apply( recentUsageEvents.values() );
      recentUsageEvents.clear();
    } finally {
      persistenceLock.writeLock().unlock();
    }
  }

  /*
  protected ReportingAccountCrud getReportingAccountCrud() {
    return ReportingAccountCrud.getInstance();
  }

  protected ReportingUserCrud getReportingUserCrud() {
    return ReportingUserCrud.getInstance();
  }

  */
  protected ReportingInstanceEventStore getReportingInstanceEventStore() {
    return ReportingInstanceEventStore.getInstance();
  }

  
  /**
   * Get the current time which will be used for recording when an event
   * occurred. This can be overridden if you have some alternative method
   * of timekeeping (synchronized, test times, etc).
   */
  protected long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

  protected MapMaker getExpiringMapMaker() {
    return new MapMaker().expireAfterAccess( 1, TimeUnit.HOURS );
  }

  protected <P,R> Function<P,R> transactional( final Class<?> clazz,
                                               final Function<P,R> callback ) {
    return Entities.asTransaction( clazz, callback );
  }

  private static final class DatedInstanceEvent {
    private final long timestamp;
    private final InstanceUsageEvent instanceEvent;

    private DatedInstanceEvent( final long timestamp,
                                final InstanceUsageEvent instanceEvent ) {
      this.timestamp = timestamp;
      this.instanceEvent = instanceEvent;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public InstanceUsageEvent getInstanceEvent() {
      return instanceEvent;
    }
  }
}
