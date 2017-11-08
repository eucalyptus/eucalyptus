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
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.amazonaws.services.s3.model.PartETag;
import com.eucalyptus.blockstorage.S3SnapshotTransfer.CompleteUpload;
import com.eucalyptus.blockstorage.S3SnapshotTransfer.StorageWriter;
import com.eucalyptus.blockstorage.S3SnapshotTransfer.UploadPart;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.exceptions.ThreadPoolNotInitializedException;
import com.eucalyptus.system.Threads;

public class SnapshotTransferThreadPool {
  private static Logger LOG = Logger.getLogger(SnapshotTransferThreadPool.class);
  private static ThreadPoolExecutor uploadPartPool;
  private static ThreadPoolExecutor completeMpuPool;
  private static ThreadPoolExecutor backendWriterPool;

  private static final ReentrantLock RLOCK = new ReentrantLock();

  private SnapshotTransferThreadPool() {}

  public static void initialize(Integer poolSize) {
    RLOCK.lock();
    try {
      shutdown();
      LOG.info("Initializing SC thread pool catering to snapshot transfers");

      uploadPartPool =
          new ThreadPoolExecutor(poolSize, poolSize, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), Threads.lookup(Storage.class,
              UploadPart.class), new ThreadPoolExecutor.AbortPolicy());
      completeMpuPool =
          new ThreadPoolExecutor(poolSize, poolSize, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), Threads.lookup(Storage.class,
              CompleteUpload.class), new ThreadPoolExecutor.AbortPolicy());
      backendWriterPool =
          new ThreadPoolExecutor(poolSize, poolSize, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), Threads.lookup(Storage.class,
              StorageWriter.class), new ThreadPoolExecutor.AbortPolicy());
    } finally {
      RLOCK.unlock();
    }
  }

  public static Future<List<PartETag>> add(UploadPart task) throws ThreadPoolNotInitializedException {
    if (uploadPartPool != null && !uploadPartPool.isShutdown()) {
      return uploadPartPool.submit(task);
    } else {
      LOG.warn("SC thread pool catering to snapshot transfers (upload part pool) is either not initalized or shut down");
      throw new ThreadPoolNotInitializedException(
          "SC thread pool catering to snapshot transfers (upload part pool) is either not initalized or shut down");
    }
  }

  public static Future<String> add(CompleteUpload task) throws ThreadPoolNotInitializedException {
    if (completeMpuPool != null && !completeMpuPool.isShutdown()) {
      return completeMpuPool.submit(task);
    } else {
      LOG.warn("SC thread pool catering to snapshot transfers (complete mpu pool) is either not initalized or shut down");
      throw new ThreadPoolNotInitializedException(
          "SC thread pool catering to snapshot transfers (complete mpu pool) is either not initalized or shut down");
    }
  }

  public static Future<String> add(StorageWriter task) throws ThreadPoolNotInitializedException {
    if (backendWriterPool != null && !backendWriterPool.isShutdown()) {
      return backendWriterPool.submit(task);
    } else {
      LOG.warn("SC thread pool catering to snapshot transfers (backend writer pool) is either not initalized or shut down");
      throw new ThreadPoolNotInitializedException(
          "SC thread pool catering to snapshot transfers (backend writer pool) is either not initalized or shut down");
    }
  }

  public static Integer getPoolSize() {
    if (uploadPartPool != null && !uploadPartPool.isShutdown() && completeMpuPool != null && !completeMpuPool.isShutdown()
        && backendWriterPool != null && !backendWriterPool.isShutdown()) {
      return uploadPartPool.getCorePoolSize();
    } else {
      return null;
    }
  }

  public static void updatePoolSize(Integer newSize) {
    if (uploadPartPool != null && !uploadPartPool.isShutdown() && completeMpuPool != null && !completeMpuPool.isShutdown()
        && backendWriterPool != null && !backendWriterPool.isShutdown() && newSize != null && uploadPartPool.getCorePoolSize() != newSize) {
      uploadPartPool.setCorePoolSize(newSize);
      uploadPartPool.setMaximumPoolSize(newSize);
      completeMpuPool.setCorePoolSize(newSize);
      completeMpuPool.setMaximumPoolSize(newSize);
      backendWriterPool.setCorePoolSize(newSize);
      backendWriterPool.setMaximumPoolSize(newSize);
    }
  }

  public static void shutdown() {
    RLOCK.lock();
    try {
      if (uploadPartPool != null) {
        LOG.info("Shutting down SC thread pool catering to snapshot transfers (upload part pool)");
        LOG.debug("Number of snapshots in progress for upload: " + uploadPartPool.getActiveCount());
        List<Runnable> awaitingExecution = uploadPartPool.shutdownNow();
        LOG.debug("Number of queued snapshots for upload: " + awaitingExecution.size());
        uploadPartPool = null;
      }
      if (completeMpuPool != null) {
        LOG.info("Shutting down SC thread pool catering to snapshot transfers (complete mpu pool)");
        LOG.debug("Number of snapshots in progress for multipart upload completion: " + completeMpuPool.getActiveCount());
        List<Runnable> awaitingExecution = completeMpuPool.shutdownNow();
        LOG.debug("Number of queued snapshots for multipart upload completion: " + awaitingExecution.size());
        completeMpuPool = null;
      }
      if (backendWriterPool != null) {
        LOG.info("Shutting down SC thread pool catering to snapshot transfers (backend writer pool)");
        LOG.debug("Number of snapshots in progress for download: " + backendWriterPool.getActiveCount());
        List<Runnable> awaitingExecution = backendWriterPool.shutdownNow();
        LOG.debug("Number of queued snapshots for download: " + awaitingExecution.size());
        backendWriterPool = null;
      }
    } finally {
      RLOCK.unlock();
    }
  }
}
