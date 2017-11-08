/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
package com.eucalyptus.reporting.export;

import static com.eucalyptus.reporting.export.ReportingExport.ReportingExportLoadListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.event_store.ReportingEventSupport;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;

/**
 *
 */
public class Import {
  private static final Logger logger = Logger.getLogger(Import.class);
  private int count;
  private long minTimestamp;
  private long maxTimestamp;

  public static int deleteAll() {
    return deleteAll( ExportUtils.getPersistentClasses(), null );
  }

  public static int deleteAll( @Nullable final Date createdTimestamp ) {
    return deleteAll( ExportUtils.getTimestampedClasses(), createdTimestamp );
  }

  public static ImportResult importData( final File exportFile,
                                         final Runnable preImportCallback ) throws Exception {
    FileInputStream in = null;
    try {
      in = new FileInputStream( exportFile );
      return importData( in, preImportCallback );
    } finally {
      Closeables.closeQuietly( in );
    }
  }

  public static ImportResult importData( final InputStream in,
                                        final Runnable preImportCallback ) throws Exception {
    return new Import().doImport( in, preImportCallback );
  }

  protected void resetStats() {
    count = 0;
    minTimestamp = Long.MAX_VALUE;
    maxTimestamp = Long.MIN_VALUE;
  }

  protected ImportResult getStats() {
    return new ImportResult( count, minTimestamp, maxTimestamp );
  }

  protected ImportResult doImport( final InputStream in,
                                   final Runnable preImportCallback ) throws Exception {
    final Supplier<Void> callback = Suppliers.memoize( new Supplier<Void>() {
      @Override
      public Void get() {
        if ( preImportCallback != null ) preImportCallback.run();
        return null;
      }
    } );
    resetStats();
    return transactional( new Callable<ImportResult>() {
      @Override
      public ImportResult call() throws Exception {
        ReportingExport.setLoadListenerSupplier(
            Suppliers.<ReportingExportLoadListener>ofInstance( new ReportingExportLoadListener() {
              private final Set<String> userIds = Sets.newHashSet();
              private final Set<String> accountIds = Sets.newHashSet();

              @Override
              public void addAction( final ReportedAction action ) {
                callback.get();
                checkUserAndAccountDependencies( action );
                add( action, ExportUtils.fromExportAction() );
              }

              @Override
              public void addUsage( final ReportedUsage usage ) {
                callback.get();
                add( usage, ExportUtils.fromExportUsage() );
              }

              private void checkUserAndAccountDependencies( final ReportedAction action ) {
                if ( !Strings.isNullOrEmpty(action.getUserId()) &&
                    !Strings.isNullOrEmpty(action.getUserName()) &&
                    !Strings.isNullOrEmpty(action.getAccountId()) &&
                    !userIds.contains(action.getUserId())) {
                  merge( new ReportingUser(
                      action.getUserId(),
                      action.getAccountId(),
                      action.getUserName() ) );
                  userIds.add(action.getUserId());
                }
                if ( !Strings.isNullOrEmpty(action.getAccountId()) &&
                    !Strings.isNullOrEmpty(action.getAccountName()) &&
                    !accountIds.contains(action.getAccountId()) ) {
                  merge( new ReportingAccount(
                      action.getAccountId(),
                      action.getAccountName() ) );
                  accountIds.add(action.getAccountId());
                }
              }

              private <T> void add( final T item, final Function<T,ReportingEventSupport> transform ) {
                final ReportingEventSupport event = transform.apply( item );
                count ++;
                minTimestamp = Math.min( minTimestamp, event.getTimestampMs() );
                maxTimestamp = Math.max( maxTimestamp, event.getTimestampMs() );
                merge( event );
              }
            } ) );

        final String bindingName = "www_eucalyptus_com_ns_reporting_export_2012_08_24";
        BindingManager.seedBinding( bindingName, ReportingExport.class );
        final Binding binding = BindingManager.getBinding( bindingName );
        binding.fromStream( ReportingExport.class, new BufferedInputStream( in ) );
        return getStats();
      }
    });
  }

  protected <T> T transactional( final Callable<T> callable ) throws Exception {
    final EntityTransaction transaction = Entities.get( ExportUtils.getTemplateClass() );
    try {
      return callable.call();
    } finally {
      transaction.commit();
    }
  }

  protected void merge( final Object object ) {
    Entities.mergeDirect( object );
  }

  public static int deleteAll( @Nonnull  final Iterable<Class<?>> entityClasses,
                               @Nullable final Date createdTimestamp ) {
    int deleted = 0;
    for ( final Class<?> reportingClass : entityClasses ) {
      deleted += deleteAll( reportingClass, createdTimestamp );
    }
    return deleted;
  }

  private static int deleteAll( @Nonnull  final Class<?> persistentClass,
                                @Nullable final Date createdTimestamp ) {
    final EntityTransaction transaction = Entities.get( ExportUtils.getTemplateClass() );
    try {
      return createdTimestamp == null ?
          Entities.deleteAll( persistentClass ) :
          Entities.deleteAllMatching( persistentClass,
              "where creationTimestamp < :creationTimestamp",
              Collections.singletonMap( "creationTimestamp", createdTimestamp ));
    } finally {
      transaction.commit();
    }
  }

  public static class ImportResult {
    private final int items;
    private final long minTimestamp;
    private final long maxTimestamp;

    public ImportResult( final int items,
                         final long minTimestamp,
                         final long maxTimestamp ) {
      this.items = items;
      this.minTimestamp = minTimestamp;
      this.maxTimestamp = maxTimestamp;
    }

    public int getItems() {
      return items;
    }

    public long getMinTimestamp() {
      return minTimestamp;
    }

    public long getMaxTimestamp() {
      return maxTimestamp;
    }
  }
}
