/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.blockstorage.threadpool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.exceptions.ThreadPoolNotInitializedException;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.system.Threads;

public class CheckerThreadPool {
  private static Logger LOG = Logger.getLogger(CheckerThreadPool.class);
  private static ScheduledExecutorService executor;
  private static ConcurrentHashMap<String, CheckerTask> checkers;
  private static Map<String, ScheduledFuture<?>> futures;

  private static final ReentrantLock RLOCK = new ReentrantLock();

  private CheckerThreadPool() {}

  public static void initialize() { // synchronizing at the class level
    RLOCK.lock();
    try {
      shutdown();
      LOG.info("Initializing SC thread pool catering to blockstorage checker service");
      executor = Executors.newSingleThreadScheduledExecutor(Threads.lookup(Storage.class, CheckerThreadPool.class));
      checkers = new ConcurrentHashMap<String, CheckerTask>();
      futures = new HashMap<String, ScheduledFuture<?>>();
    } finally {
      RLOCK.unlock();
    }
  }

  public static void add(CheckerTask checker) throws ThreadPoolNotInitializedException {
    if (executor != null && !executor.isShutdown()) {
      if (checkers.putIfAbsent(checker.getName(), checker) == null) {
        LOG.info("Adding task " + checker.getName() + " to blockstorage checker service");
        ScheduledFuture<?> future = null;
        if (checker.getIsFixedDelay()) {
          future = executor.scheduleWithFixedDelay(checker, checker.getRunInterval(), checker.getRunInterval(), checker.getRunIntervalUnit());
        } else {
          future = executor.scheduleAtFixedRate(checker, checker.getRunInterval(), checker.getRunInterval(), checker.getRunIntervalUnit());
        }
        futures.put(checker.getName(), future);
      } else {
        LOG.info("Checker " + checker.getName() + " has already been added to Storage Checker Service");
      }
    } else {
      LOG.warn("SC thread pool catering to blockstorage checker service is either not initalized or shut down");
      throw new ThreadPoolNotInitializedException("SC thread pool catering to blockstorage checker service is either not initalized or shut down");
    }
  }

  public static void shutdown() {
    RLOCK.lock();
    try {
      if (checkers != null) {
        checkers.clear();
        checkers = null;
      }
      if (futures != null) {
        for (ScheduledFuture<?> future : futures.values()) {
          future.cancel(true);
        }
        futures.clear();
        futures = null;
      }
      if (executor != null) {
        LOG.info("Shutting down SC thread pool catering to blockstorage checker service");
        executor.shutdownNow();
        executor = null;
      }
    } finally {
      RLOCK.unlock();
    }
  }
}
