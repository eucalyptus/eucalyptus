/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */
package edu.ucsb.eucalyptus.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;

// A concurrent hash map that holds a map of queues, which can be used for passing data
// Currently, the queues are a LinkedBlockingQueue and producers/consumers do not timeout.

public class WalrusDataMessenger {

    private static final int DATA_QUEUE_SIZE = 100;

    private ConcurrentHashMap<String, ConcurrentHashMap<String,LinkedBlockingQueue<WalrusDataMessage>>> queueMap;
    private ConcurrentHashMap<String, WalrusMonitor> monitorMap;
    private ConcurrentHashMap<String, WalrusSemaphore> semaphoreMap;

    public WalrusDataMessenger() {
        queueMap = new ConcurrentHashMap<String, ConcurrentHashMap<String,LinkedBlockingQueue<WalrusDataMessage>>>();
        monitorMap = new ConcurrentHashMap<String, WalrusMonitor>();
        semaphoreMap = new ConcurrentHashMap<String, WalrusSemaphore>();
    }

    public LinkedBlockingQueue<WalrusDataMessage> getQueue(String key1, String key2) {
        ConcurrentHashMap<String,LinkedBlockingQueue<WalrusDataMessage>> queues = queueMap.putIfAbsent(key1, new ConcurrentHashMap<String, LinkedBlockingQueue<WalrusDataMessage>>());
        if (queues == null) {
            queues = queueMap.get(key1);
        }
        LinkedBlockingQueue<WalrusDataMessage> queue = queues.putIfAbsent(key2, new LinkedBlockingQueue<WalrusDataMessage>(DATA_QUEUE_SIZE));
        if (queue == null) {
            queue = queues.get(key2);
        }
        return queue;
    }

    public synchronized LinkedBlockingQueue<WalrusDataMessage> interruptAllAndGetQueue(String key1, String key2) {
        ConcurrentHashMap<String,LinkedBlockingQueue<WalrusDataMessage>> queues = queueMap.get(key1);
        if(queues != null) {
            for (LinkedBlockingQueue<WalrusDataMessage> queue: queues.values()) {
                queue.add(WalrusDataMessage.InterruptTransaction());
            }
        }
        return getQueue(key1, key2);
    }

    public void removeQueue(String key1, String key2) {
        if(queueMap.containsKey(key1)) {
            ConcurrentHashMap<String, LinkedBlockingQueue<WalrusDataMessage>> queues = queueMap.get(key1);
            if(queues.containsKey(key2)) {
                queues.remove(key2);
            }
        }
    }

    public WalrusMonitor getMonitor(String key) {
        WalrusMonitor monitor = monitorMap.putIfAbsent(key, new WalrusMonitor());
        if (monitor == null) {
            monitor = monitorMap.get(key);
        }
        return monitor;
    }

    public void removeMonitor(String key) {
        if(monitorMap.containsKey(key)) {
            monitorMap.remove(key);
        }
    }

    public WalrusSemaphore getSemaphore(String key) {
        WalrusSemaphore semaphore = semaphoreMap.putIfAbsent(key, new WalrusSemaphore());
        if (semaphore == null) {
            semaphore = semaphoreMap.get(key);
        }
        return semaphore;
    }

    public void removeSemaphore(String key) {
        if(semaphoreMap.containsKey(key)) {
            semaphoreMap.remove(key);
        }
    }
}
