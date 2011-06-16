package com.eucalyptus.webui.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.shared.query.SearchQuery;
import com.google.common.collect.Lists;

public class SearchUtil {

  public static List<SearchResultRow> getRange( List<SearchResultRow> results, SearchRange range ) {
    final int sortField = range.getSortField( );
    if ( sortField >= 0 ) {
      final boolean ascending = range.isAscending( );
      Collections.sort( results, new Comparator<SearchResultRow>( ) {
        @Override
        public int compare( SearchResultRow r1, SearchResultRow r2 ) {
          if ( r1 == r2 ) {
            return 0;
          }
          // Compare the name columns.
          int diff = -1;
          if ( r1 != null ) {
            diff = ( r2 != null ) ? r1.getField( sortField ).compareTo( r2.getField( sortField ) ) : 1;
          }
          return ascending ? diff : -diff;
        }
      } );
    }
    int resultLength = Math.min( range.getLength( ), results.size( ) - range.getStart( ) );
    return results.subList( range.getStart( ), range.getStart( ) + resultLength );
  }

}
