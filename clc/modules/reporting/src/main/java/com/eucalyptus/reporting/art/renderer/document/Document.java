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