/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.blockstorage;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import com.eucalyptus.blockstorage.entities.LVMVolumeInfo;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class ThreadPoolDispatchingStorageExportManager implements StorageExportManager {

  private final StorageExportManager delegate;

  public ThreadPoolDispatchingStorageExportManager( final StorageExportManager delegate ) {
    this.delegate = delegate;
  }

  @Override
  public void checkPreconditions( ) throws EucalyptusCloudException {
    delegate.checkPreconditions( );
  }

  @Override
  public void configure( ) {
    delegate.configure( );
  }

  @Override
  public void allocateTarget( final LVMVolumeInfo volumeInfo ) throws EucalyptusCloudException {
    doThreads( (Callable<Void>) () -> { delegate.allocateTarget( volumeInfo ); return null; });
  }

  @Override
  public void cleanup( final LVMVolumeInfo volume ) throws EucalyptusCloudException {
    doThreads( (Callable<Void>) () -> { delegate.cleanup( volume ); return null; });
  }

  @Override
  public void stop( ) {
    delegate.stop( );
  }

  @Override
  public void check( ) throws EucalyptusCloudException {
    delegate.check( );
  }

  @Override
  public boolean isExported( final LVMVolumeInfo foundLVMVolumeInfo ) throws EucalyptusCloudException {
    return doThreads( () -> delegate.isExported( foundLVMVolumeInfo ) );
  }

  private <T> T doThreads( final Callable<T> callable ) throws EucalyptusCloudException {
    try {
      return Threads.enqueue( Storage.class, ThreadPoolDispatchingStorageExportManager.class, callable ).get( );
    } catch ( InterruptedException e ) {
      throw Exceptions.toUndeclared( e );
    } catch ( ExecutionException e ) {
      Exceptions.findAndRethrow( e, EucalyptusCloudException.class );
      throw Exceptions.toUndeclared( MoreObjects.firstNonNull( e.getCause( ), e ) );
    }
  }
}
