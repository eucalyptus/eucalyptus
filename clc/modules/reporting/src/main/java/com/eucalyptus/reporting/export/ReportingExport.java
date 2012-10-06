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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Reporting data export model.
 *
 * <p>Due to dependencies between usage and actions in an export the usage
 * MUST be accessed first.</p>
 */
public class ReportingExport {

  private static Supplier<ReportingExportLoadListener> loadListenerSupplier = null;
  private final List<Iterable<ReportedAction>> actions = Lists.newArrayList();
  private final List<Iterable<ReportedUsage>> usage = Lists.newArrayList();
  private final ReportingExportLoadListener listener;

  public static void setLoadListenerSupplier( final Supplier<ReportingExportLoadListener> supplier ) {
    loadListenerSupplier = supplier;
  }

  public ReportingExport() {
    this.listener = getListenerSupplier().get();
  }

  private Supplier<ReportingExportLoadListener> getListenerSupplier() {
    return Objects.firstNonNull(
        loadListenerSupplier,
        Suppliers.<ReportingExportLoadListener>ofInstance(new DefaultLoadListener()));
  }

  public ReportingExport( final ReportingExportLoadListener listener ) {
    this.listener = listener;
  }

  public void addActions( final Iterable<ReportedAction> actions ) {
    this.actions.add( actions );
  }

  public void addUsage( final Iterable<ReportedUsage> usage ) {
    this.usage.add( usage );
  }

  public void addAction( final ReportedAction action ) {
    listener.addAction( action );
  }

  public Iterator<ReportedAction> iterateActions() {
    return iterateAll( actions );
  }

  public void addUsage( final ReportedUsage usage ) {
    listener.addUsage( usage );
  }

  public Iterator<ReportedUsage> iterateUsage() {
    return iterateAll( usage );
  }

  private <T> Iterator<T> iterateAll( Iterable<Iterable<T>> data ) {
    List<Iterator<T>> iterators = Lists.newArrayList();
    for ( final Iterable<T> iterable : data ) {
      iterators.add( iterable.iterator() );
    }
    return Iterators.unmodifiableIterator( Iterators.concat( iterators.iterator() ) );
  }

  public static interface ReportingExportLoadListener {
    void addAction( ReportedAction action );
    void addUsage( ReportedUsage usage );
  }

  private class DefaultLoadListener implements ReportingExportLoadListener {
    @Override
    public void addAction( final ReportedAction action ) {
      ReportingExport.this.actions.add( Collections.singleton( action ) );
    }

    @Override
    public void addUsage( final ReportedUsage usage ) {
      ReportingExport.this.usage.add( Collections.singleton( usage ) );
    }
  }
}
