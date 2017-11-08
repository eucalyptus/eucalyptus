/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
