/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
