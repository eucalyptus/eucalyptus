/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.stats;

import com.eucalyptus.stats.configuration.StatsConfiguration;
import com.eucalyptus.stats.emitters.EventEmitter;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import org.apache.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The service to which monitoring events are submitted for
 * propagation outside the JVM.
 */
public class EventEmitterService {
    private static final Logger LOG = Logger.getLogger(EventEmitterService.class);
    protected LinkedBlockingQueue<SystemMetric> eventQueue;
    protected ScheduledExecutorService writerThreadPool;
    protected volatile boolean acceptNewEvents;

    private static EventEmitterService serviceInstance;

    public EventEmitterService() {
        this.eventQueue = new LinkedBlockingQueue<>();
        this.writerThreadPool =
            Executors.newSingleThreadScheduledExecutor( Threads.threadFactory( "ts-event-emitter-pool-%d" ) );
        this.acceptNewEvents = false;
    }

    public static synchronized EventEmitterService getInstance() {
        if (serviceInstance == null) {
            serviceInstance = new EventEmitterService();
        }

        return serviceInstance;
    }

    public void init() {
        //No-op
    }

    public void start() {
        this.acceptNewEvents = true;
        this.writerThreadPool.execute(new EventWriterThread(eventQueue, getEmitterInstance()));
    }

    /**
     * Is the emitter service ready and able to operate. Does not guarantee successful
     * delivery, just checks prereqs
     */
    public void check() throws Exception {
        if(this.eventQueue == null ||
                this.writerThreadPool == null ||
                this.writerThreadPool.isShutdown() ||
                this.writerThreadPool.isTerminated() || getEmitterInstance() == null) {
            throw new Exception("Event emitter check failed. Event emitter service not ready");
        }
        try {
            getEmitterInstance().check();
        } catch(Exception e) {
            LOG.error("Event emitter service failed check because of exception from emitter instance", e);
            throw new Exception("Event emitter service not ready", e);
        }
    }

    protected static EventEmitter getEmitterInstance() {
        try {
            Class emitterClass = StatsConfiguration.getEmitterClass();
            return (EventEmitter) emitterClass.newInstance();
        } catch (Exception e) {
            LOG.error("Could not load event emitter config, cannot initialize the monitoring system", e);
            throw Exceptions.toUndeclared("Error loading emitter configuration", e);
        }
    }

    public boolean offer(SystemMetric systemMetric) {
        return this.acceptNewEvents && this.eventQueue.offer(systemMetric);
    }

    public void stop() {
        this.acceptNewEvents = false;
        this.writerThreadPool.shutdownNow();
    }

    /**
     * Dangerous! Use for testing only
     * Used for testing and special cases where the entire queue should be flushed before shutdown. Will cause events to be rejected.
     */
    protected void softStop() {
        this.acceptNewEvents = false;
        while (!this.eventQueue.isEmpty()) ; //wait for flushing
        this.writerThreadPool.shutdown();
    }

    private class EventWriterThread implements Runnable {
        BlockingQueue<SystemMetric> eventQ;
        EventEmitter emitter;

        public EventWriterThread(BlockingQueue<SystemMetric> eventQueueToWatch, EventEmitter eventEmitter) {
            this.eventQ = eventQueueToWatch;
            this.emitter = eventEmitter;
        }

        @Override
        public void run() {
            //blocking wait for an event to write
            try {
                while (true) {
                    SystemMetric event = this.eventQ.take();
                    emitter.emit(event);
                }
            } catch (InterruptedException ex) {
                LOG.warn("Event emitter interrupted");
            }
        }
    }
}
