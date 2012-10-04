/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

  public static int deleteAll() {
    return deleteAll( ExportUtils.getPersistentClasses(), null );
  }

  public static int deleteAll( @Nullable final Date createdTimestamp ) {
    return deleteAll( ExportUtils.getTimestampedClasses(), createdTimestamp );
  }

  public static ReportingExport importData( final File exportFile,
                                            final Runnable preImportCallback ) throws Exception {
    FileInputStream in = null;
    try {
      in = new FileInputStream( exportFile );
      return importData( in, preImportCallback );
    } finally {
      Closeables.closeQuietly( in );
    }
  }

  public static ReportingExport importData( final InputStream in,
                                            final Runnable preImportCallback ) throws Exception {
    return new Import().doImport( in, preImportCallback );
  }

  protected ReportingExport doImport( final InputStream in,
                                      final Runnable preImportCallback ) throws Exception {
    final Supplier<Void> callback = Suppliers.memoize( new Supplier<Void>() {
      @Override
      public Void get() {
        if ( preImportCallback != null ) preImportCallback.run();
        return null;
      }
    } );
    return transactional( new Callable<ReportingExport>() {
      @Override
      public ReportingExport call() throws Exception {
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
                merge( event );
              }
            } ) );

        final String bindingName = "www_eucalyptus_com_ns_reporting_export_2012_08_24";
        BindingManager.seedBinding( bindingName, ReportingExport.class );
        final Binding binding = BindingManager.getBinding( bindingName );
        return binding.fromStream( ReportingExport.class, new BufferedInputStream( in ) );      }
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
}
