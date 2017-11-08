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

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.async.VolumeCreator;
import com.eucalyptus.blockstorage.exceptions.ThreadPoolNotInitializedException;
import com.eucalyptus.system.Threads;

public class VolumeThreadPool {
  private static Logger LOG = Logger.getLogger(VolumeThreadPool.class);
  private static ThreadPoolExecutor pool;
  private static final ReentrantLock RLOCK = new ReentrantLock();

  private VolumeThreadPool() {}

  public static void initialize(Integer poolSize) {
    RLOCK.lock();
    try {
      shutdown();
      LOG.info("Initializing SC thread pool catering to volume creation");
      pool =
          new ThreadPoolExecutor(poolSize, poolSize, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), Threads.lookup(Storage.class,
              VolumeCreator.class), new ThreadPoolExecutor.AbortPolicy());
    } finally {
      RLOCK.unlock();
    }
  }

  public static void add(VolumeCreator volumeCreator) throws ThreadPoolNotInitializedException {
    if (pool != null && !pool.isShutdown()) {
      pool.execute(volumeCreator);
    } else {
      LOG.warn("SC thread pool catering to volume creation is either not initalized or shut down");
      throw new ThreadPoolNotInitializedException("SC thread pool catering to volume creation is either not initalized or shut down");
    }
  }

  public static Integer getPoolSize() {
    if (pool != null && !pool.isShutdown()) {
      return pool.getCorePoolSize();
    } else {
      return null;
    }
  }

  public static void updatePoolSize(Integer newSize) {
    if (pool != null && !pool.isShutdown() && newSize != null && pool.getCorePoolSize() != newSize) {
      pool.setCorePoolSize(newSize);
      pool.setMaximumPoolSize(newSize);
    } else {
      // nothing to do here
    }
  }

  public static void shutdown() {
    RLOCK.lock();
    try {
      if (pool != null) {
        LOG.info("Shutting down SC thread pool catering to volume creation");
        LOG.debug("Number of volumes in progress on block storage backend: " + pool.getActiveCount());
        List<Runnable> awaitingExecution = pool.shutdownNow();
        LOG.debug("Number of queued volumes: " + awaitingExecution.size());
        pool = null;
      }
    } finally {
      RLOCK.unlock();
    }
  }
}
