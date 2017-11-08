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
