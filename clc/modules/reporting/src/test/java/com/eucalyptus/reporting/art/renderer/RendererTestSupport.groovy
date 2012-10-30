package com.eucalyptus.reporting.art.renderer

import com.eucalyptus.reporting.units.Units
import com.eucalyptus.reporting.units.TimeUnit
import com.eucalyptus.reporting.units.SizeUnit
import java.text.SimpleDateFormat
import com.google.common.base.Splitter
import com.google.common.collect.Maps
import com.google.common.collect.Lists
import com.google.common.base.Strings
import com.google.common.collect.Iterables
import com.google.common.base.Predicates
import com.google.common.base.Function
import static org.junit.Assert.*
import com.eucalyptus.reporting.ReportType
import com.eucalyptus.reporting.ReportFormat
import com.google.common.base.Charsets
import com.eucalyptus.reporting.art.entity.ReportArtEntity

/**
 * 
 */
class RendererTestSupport {

  protected String render( ReportType type, ReportFormat format, ReportArtEntity report ) {
    Renderer renderer = RendererFactory.getRenderer( type, format )
    ByteArrayOutputStream out = new ByteArrayOutputStream()
    renderer.render( report, out , units() )
    new String( out.toByteArray(), Charsets.UTF_8 )
  }

  protected void assertCsvColumns( String csvReport ) {
    Iterable<String> rows = Splitter.on("\n").split( csvReport )
    Map<Integer,List<String>> columns = Maps.newTreeMap()
    int rowCount = 0;
    for ( final String row : rows ) {
      int columnCount = 0;
      for ( final column : Splitter.on(",").split( row ) ) {
        List<String> columnValues = columns.get(columnCount)
        if ( columnValues == null ) {
          columns.put( columnCount, columnValues = Lists.newArrayList() )
          Iterables.addAll( columnValues, Iterables.limit( Iterables.cycle(""), rowCount ) )
        }
        columnValues.add(column)
        columnCount++
      }
      for ( int column = columnCount; column < columns.size(); column++ ) {
        List<String> columnValues = columns.get(column)
        columnValues.add( "" )
      }
      rowCount++;
    }

    // Print out report in a fixed width format for visual validation
    for ( int i=0; i<columns.size(); i++ ) {
      System.out.print( Strings.padEnd( Integer.toString(i), 16, (char)' ' ) )
    }
    System.out.println()
    for ( int row = 0; row < Integer.MAX_VALUE; row++ ) {
      boolean sawValue = false;
      for ( String column : Iterables.transform( columns.values(), new Function<List<String>,String>(){
          @Override
          String apply(final List<String> f) {
            f.size() > row ? f.get(row) : ""
          }
        } ) ) {
        System.out.print( Strings.padEnd( column, 16, (char)' ' ) )
        if ( !column.isEmpty() ) sawValue = true;
      }
      System.out.println()
      if ( !sawValue ) break;
    }

    // Check for an empty column
    for ( Map.Entry<Integer,List<String>> entry : columns.entrySet() ) {
      assertFalse( "Column " + entry.getKey() + " empty", Iterables.all( entry.getValue(), Predicates.equalTo("")) )
    }

    // Check for a column without a header
    for ( Map.Entry<Integer,List<String>> entry : columns.entrySet() ) {
      assertFalse( "Column " + entry.getKey() + " missing header ", Iterables.all( Iterables.transform( entry.getValue(), new Function<String, String>(){
        @Override
        String apply(final String value) {
          return value.replaceAll( "(cumul.|0)", "" )
        }
      } ), Predicates.equalTo("")) )
    }
  }

  protected void assertHtmlColumns( String htmlReport ) {
    Node htmlDoc = new XmlParser().parseText( htmlReport.replaceAll("&nbsp;"," ") )

    // Print out tables in a fixed width format for visual validation
    for ( int i=0; i<20; i++ ) {
      print( Strings.padEnd( Integer.toString(i), 16, (char)' ' ) )
    }
    println()
    htmlDoc.body.table.each{ table ->
      table.tr.each{ row ->
        row.td.each{ cell ->
          print( Strings.padEnd( cell.text(), 16, (char)' ' ) )
          if ( cell.attributes()['colspan'] ) {
            int pad = 16 * (Integer.parseInt(cell.attributes()['colspan'])-1)
            print( Strings.padEnd( "", pad, (char)' ' ) )
          }
        }
        println()
      }
      println()
    }

    // verify rows have the same number of columns
    htmlDoc.body.table.each{ table ->
      Integer columns
      table.tr.each{ row ->
        int columnsForRow = 0
        row.td.each{ cell ->
          if ( cell.attributes()['colspan'] )
            columnsForRow += Integer.parseInt(cell.attributes()['colspan'])
          else
            columnsForRow += 1
        }
        if ( columns == null ) {
          columns = columnsForRow
        } else {
          assertEquals( "Column count check", columns, columnsForRow )
        }
      }
    }
  }

  protected Units units() {
    new Units( TimeUnit.DAYS, SizeUnit.MB, TimeUnit.HOURS, SizeUnit.MB )
  }

  protected long millis( String timestamp ) {
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    sdf.parse( timestamp ).getTime()
  }
}
