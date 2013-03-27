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

package com.eucalyptus.reporting.art.renderer.document;

import java.io.IOException;
import java.io.Writer;

public interface Document {

  Document open() throws IOException;

  Document close() throws IOException;

  Document tableOpen() throws IOException;

  Document tableClose() throws IOException;

  Document textLine( String text, int emphasis ) throws IOException;

  Document newRow() throws IOException;

  Document addLabelCol( int indent, String val ) throws IOException;

  Document addValCol( String val ) throws IOException;

  Document addValCol( Long val ) throws IOException;

  Document addValCol( Integer val ) throws IOException;

  Document addValCol( Double val ) throws IOException;

  Document addValCol( String val, int colspan, String align ) throws IOException;

  Document addEmptyValCols( int num ) throws IOException;

  Document addEmptyLabelCols( int num ) throws IOException;

  void setUnlabeledRowIndent( int num );

  void setWriter( Writer writer );

}
