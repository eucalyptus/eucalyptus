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

package com.eucalyptus.reporting.modules.instance;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import com.eucalyptus.reporting.service.ReportingService;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.InstanceUsageEvent;
import com.eucalyptus.reporting.event_store.ReportingInstanceEventStore;
import com.eucalyptus.system.Threads;

@ConfigurableClass(root = "reporting", description = "Parameters controlling reporting")
public class InstanceUsageEventListener implements
	EventListener<InstanceUsageEvent> {
    private static final Logger log = Logger
	    .getLogger(InstanceUsageEventListener.class);

    @ConfigurableField(initial = "15", description = "How often the reporting system requests information from the cluster controller")
    public static long DEFAULT_WRITE_INTERVAL_MINS = 15;

    private static final ScheduledExecutorService eventFlushTimer = Executors
	    .newSingleThreadScheduledExecutor( Threads.threadFactory( "reporting-flush-pool-%d" ) );

    private static AtomicBoolean busy = new AtomicBoolean(false);

    private static LinkedBlockingQueue<InstanceUsageEvent> eventQueue = new LinkedBlockingQueue<InstanceUsageEvent>();

    public static void register() {
	Listeners.register(InstanceUsageEvent.class,
		new InstanceUsageEventListener());
    }

    @Override
    public void fireEvent(@Nonnull final InstanceUsageEvent event) {
      if (!ReportingService.DATA_COLLECTION_ENABLED) {
        ReportingService.faultDisableReportingServiceIfNecessary();
        log.trace("Reporting service data collection disabled....InstanceUsageEvent discarded");
        return;
      }

	if (log.isDebugEnabled()) {
	    log.debug("Received instance usage event:" + event);
	}

	try {
	    eventQueue.offer(event, DEFAULT_WRITE_INTERVAL_MINS + 1,
		    TimeUnit.MINUTES);
	} catch (InterruptedException e) {
	    log.debug("Unable to queue usage event " + event, e);
	}

	if (!busy.get()) {
	    flushEventQueue();
	}
    }

    private void flushEventQueue() {

	busy.set(true);

	Runnable safeRunner = new Runnable() {
	    @Override
	    public void run() {
		
		Set<InstanceUsageEvent> eventBatch = new HashSet<InstanceUsageEvent>();
		eventQueue.drainTo(eventBatch);
		
		for (final InstanceUsageEvent event : eventBatch) {
		    insertEvent(event);
		}
		
		eventBatch.clear();
		busy.set(false);
	    }
	};

	eventFlushTimer.schedule(safeRunner, DEFAULT_WRITE_INTERVAL_MINS,
		TimeUnit.MINUTES);

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
