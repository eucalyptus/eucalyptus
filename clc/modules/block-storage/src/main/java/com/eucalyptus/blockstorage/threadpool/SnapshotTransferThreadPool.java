/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

package com.eucalyptus.blockstorage.threadpool;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.amazonaws.services.s3.model.PartETag;
import com.eucalyptus.blockstorage.S3SnapshotTransfer.CompleteMpu;
import com.eucalyptus.blockstorage.S3SnapshotTransfer.StorageWriter;
import com.eucalyptus.blockstorage.S3SnapshotTransfer.UploadPart;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.exceptions.ThreadPoolNotInitializedException;
import com.eucalyptus.system.Threads;

public class SnapshotTransferThreadPool {
  private static Logger LOG = Logger.getLogger(SnapshotTransferThreadPool.class);
  private static ExecutorService uploadPartPool;
  private static ExecutorService completeMpuPool;
  private static ExecutorService backendWriterPool;

  private static final ReentrantLock RLOCK = new ReentrantLock();

  private SnapshotTransferThreadPool() {}

  public static void initialize() {
    RLOCK.lock();
    try {
      shutdown();

      LOG.info("Initializing SC thread pool catering to snapshot transfers");
      StorageInfo info = StorageInfo.getStorageInfo();
      Integer size = info.getMaxConcurrentSnapshotTransfers();

      uploadPartPool = Executors.newFixedThreadPool(size, Threads.lookup(Storage.class, UploadPart.class).limitTo(size));
      completeMpuPool = Executors.newFixedThreadPool(size, Threads.lookup(Storage.class, CompleteMpu.class).limitTo(size));
      backendWriterPool = Executors.newFixedThreadPool(size, Threads.lookup(Storage.class, StorageWriter.class).limitTo(size));
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

  public static Future<String> add(CompleteMpu task) throws ThreadPoolNotInitializedException {
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

  public static void shutdown() {
    RLOCK.lock();
    try {
      if (uploadPartPool != null) {
        LOG.info("Shutting down SC thread pool catering to snapshot transfers (upload part pool)");
        uploadPartPool.shutdownNow();
        uploadPartPool = null;
      }
      if (completeMpuPool != null) {
        LOG.info("Shutting down SC thread pool catering to snapshot transfers (complete mpu pool)");
        completeMpuPool.shutdownNow();
        completeMpuPool = null;
      }
      if (backendWriterPool != null) {
        LOG.info("Shutting down SC thread pool catering to snapshot transfers (backend writer pool)");
        backendWriterPool.shutdownNow();
        backendWriterPool = null;
      }
    } finally {
      RLOCK.unlock();
    }
  }
}
