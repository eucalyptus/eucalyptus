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

package com.eucalyptus.reporting.modules.instance;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.InstanceUsageEvent;
import com.eucalyptus.reporting.event_store.ReportingInstanceEventStore;

@ConfigurableClass(root = "reporting", description = "Parameters controlling reporting")
public class InstanceUsageEventListener implements
	EventListener<InstanceUsageEvent> {
    private static final Logger log = Logger
	    .getLogger(InstanceUsageEventListener.class);

    @ConfigurableField(initial = "15", description = "How often the reporting system requests information from the cluster controller")
    public static long DEFAULT_WRITE_INTERVAL_MINS = 15;

    private static final ScheduledExecutorService eventFlushTimer = Executors
	    .newSingleThreadScheduledExecutor();

    private static AtomicBoolean busy = new AtomicBoolean(false);

    private static LinkedBlockingQueue<InstanceUsageEvent> eventQueue = new LinkedBlockingQueue<InstanceUsageEvent>();

    public static void register() {
	Listeners.register(InstanceUsageEvent.class,
		new InstanceUsageEventListener());
    }

    @Override
    public void fireEvent(@Nonnull final InstanceUsageEvent event) {

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
