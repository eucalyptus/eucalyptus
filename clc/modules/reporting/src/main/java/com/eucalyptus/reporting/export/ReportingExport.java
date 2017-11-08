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
