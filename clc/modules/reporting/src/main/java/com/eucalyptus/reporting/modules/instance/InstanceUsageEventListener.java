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

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import org.apache.log4j.*;
import org.hibernate.exception.ConstraintViolationException;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.*;
import com.eucalyptus.reporting.event_store.ReportingInstanceEventStore;
import com.google.common.collect.Lists;

@ConfigurableClass( root = "reporting", description = "Parameters controlling reporting")
public class InstanceUsageEventListener implements EventListener<InstanceUsageEvent> {
    private static final Logger log = Logger.getLogger( InstanceUsageEventListener.class );

    @ConfigurableField(initial = "49", description = "How often the reporting system requests information from the cluster controller")
    public static long DEFAULT_WRITE_INTERVAL_MINS = 49;
    
    private static final ScheduledExecutorService eventFlushTimer   = Executors.newScheduledThreadPool( 50 );
    
    private static InstanceUsageEvent lastEvent = new InstanceUsageEvent("-1", "-1", "-1", -1L, "-1", -1D, -1L);

    private static List<InstanceUsageEvent> eventQueue = Lists.newArrayList();
    
    private static int MAX_QUEUE_SIZE = 50;
    
    public static void register( ) {
	Listeners.register( InstanceUsageEvent.class, new InstanceUsageEventListener( ) );
    }

    @Override
    public void fireEvent(@Nonnull final InstanceUsageEvent event) {
	
	if (log.isDebugEnabled()) {
	    log.debug("Received instance usage event:" + event);
	}
	if (lastEvent.getInstanceId().equals("-1")) {
	    queueEvent(event);
	    lastEvent = new InstanceUsageEvent(event.getUuid(),event.getInstanceId(), event.getMetric(), event.getSequenceNum(), event.getDimension(), event.getValue(), event.getValueTimestamp());
	} else if (lastEvent.getInstanceId().equals(event.getInstanceId())
		&& !lastEvent.getMetric().equals(event.getMetric())
		&& !lastEvent.getDimension().equals(event.getDimension())
		&& lastEvent.getValue() != event.getValue()) {
	    queueEvent(event);
	    lastEvent = new InstanceUsageEvent("-1", "-1", "-1", -1L, "-1", -1D, -1L);
	} else {
	    log.debug("Instance Usage Event : " + event.getInstanceId() + " : "
		    + event.getValue() + "has already been record.");
	}

	if (checkEventQueue()) {
	    flushEventQueue();   
	}
	
    }

    private void queueEvent(InstanceUsageEvent event) {
	eventQueue.add(event);
    }
    
    private void flushEventQueue() {

	final List<InstanceUsageEvent> copyEventQueue = Lists.newArrayList(eventQueue);

	Runnable safeRunner = new Runnable() {
	    @Override
	    public void run() {
		for (final InstanceUsageEvent event : copyEventQueue) {
		    insertEvent(event);
		}
		eventQueue.clear();
		copyEventQueue.clear();
	    }
	};

	final long delayTime = TimeUnit.MINUTES.toMillis(DEFAULT_WRITE_INTERVAL_MINS);
	
	eventFlushTimer.scheduleAtFixedRate(safeRunner, delayTime, 500,
		TimeUnit.MILLISECONDS);
	
    }

    private boolean checkEventQueue() {
	
	if (eventQueue.size() >= MAX_QUEUE_SIZE ) {
	    return true;
	} else {
	    return false; 
	}
	
    }
    private void insertEvent(InstanceUsageEvent event) {
	try {
	    final ReportingInstanceEventStore eventStore = getReportingInstanceEventStore();
	    eventStore.insertUsageEvent(event.getUuid(),
		    event.getValueTimestamp(), event.getMetric(),
		    event.getSequenceNum(), event.getDimension(),
		    event.getValue());
	} catch (ConstraintViolationException ex) {
	    log.debug(ex, ex); // info already exists for instance
	} catch (Exception ex) {
	    log.error(ex, ex);
	}
    }

    protected ReportingInstanceEventStore getReportingInstanceEventStore() {
	return ReportingInstanceEventStore.getInstance();
    }
}
