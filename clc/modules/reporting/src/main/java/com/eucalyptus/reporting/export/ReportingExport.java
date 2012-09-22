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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

/**
 * Reporting data export model.
 */
public class ReportingExport implements Serializable, Iterable<Serializable> {
  private static final long serialVersionUID = 1L;

  private final List<Serializable> exportData = Lists.newArrayList();

  public ReportingExport() {
  }

  public ReportingExport( final String data ) {
    try {
      final ReportingExport export = (ReportingExport)
          new ObjectInputStream( new ByteArrayInputStream( B64.standard.dec(data) ) ).readObject();

      exportData.addAll( export.exportData );
    } catch ( Exception e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  public ReportingExport( final File file ) throws IOException {
    this( Files.toString( file, Charsets.UTF_8 ) );
  }

  public void add( final Serializable item ) {
    exportData.add( item );
  }

  @Override
  public Iterator<Serializable> iterator() {
    return Iterators.unmodifiableIterator( exportData.iterator() );
  }

  public String toString() {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      final ObjectOutputStream oout = new ObjectOutputStream( out );
      oout.writeObject( this );
      oout.flush();
      oout.close();
      return B64.standard.encString( out.toByteArray() );
    } catch ( IOException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }
}
