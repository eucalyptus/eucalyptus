package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.TableDisplay;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.service.SearchResult;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.cellview.client.CellTable.Style;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionModel;

public class SearchResultTable extends Composite {
  
  private static final String MULTI_SELECTION_TIP = "Use 'Ctrl' or 'Shift' for multiple selections.";

  private static SearchResultTableUiBinder uiBinder = GWT.create( SearchResultTableUiBinder.class );
  
  interface SearchResultTableUiBinder extends UiBinder<Widget, SearchResultTable> {}

  public static interface TableResources extends Resources {
    @Source( "SearchResultTable.css" )
    Style cellTableStyle( );
  }  
  
  @UiField
  Label tip;
  
  @UiField( provided = true )
  CellTable<SearchResultRow> cellTable;
  
  @UiField( provided = true )
  SimplePager pager;

  private ArrayList<SearchResultFieldDesc> fieldDescs;
  private final SearchRangeChangeHandler changeHandler;
  private SelectionModel<SearchResultRow> selectionModel;
  // Not all column are displayed in the table. This maps table column to data field index.
  private final ArrayList<Integer> tableColIdx = new ArrayList<Integer>( );
  
  public SearchResultTable( int pageSize, ArrayList<SearchResultFieldDesc> fieldDescs, SearchRangeChangeHandler changeHandler, SelectionModel<SearchResultRow> selectionModel ) {
    this.changeHandler = changeHandler;
    this.fieldDescs = fieldDescs;
    this.selectionModel = selectionModel;
    
    buildTable( pageSize );
    buildPager( );
    
    initWidget( uiBinder.createAndBindUi( this ) );
    
    if ( selectionModel instanceof MultiSelectionModel ) {
      this.tip.setText( MULTI_SELECTION_TIP );
    }
  }
  
  public void setData( SearchResult data ) {    
    if ( cellTable != null ) {
      cellTable.setRowCount( data.getTotalSize( ), true );
      //cellTable.setVisibleRange( data.getStart( ), data.getLength( ) );
      cellTable.setRowData( data.getRange( ).getStart( ), data.getRows( ) );
    }
  }
  
  private void buildTable( int pageSize ) {
    CellTable.Resources resources = GWT.create( TableResources.class );
    
    cellTable = new CellTable<SearchResultRow>( pageSize, resources );
    cellTable.setWidth( "100%", true );
    // Initialize columns
    for ( int i = 0; i < this.fieldDescs.size( ); i++ ) {
      SearchResultFieldDesc desc = this.fieldDescs.get( i );
      if ( desc.getTableDisplay( ) != TableDisplay.MANDATORY ) {
        continue;
      }
      final int index = i;
      TextColumn<SearchResultRow> col = new TextColumn<SearchResultRow>( ) {
        @Override
        public String getValue( SearchResultRow data ) {
          if ( data == null ) {
            return "";
          } else {
            return data.getField( index );
          }
        }
      };
      col.setSortable( desc.getSortable( ) );
      cellTable.addColumn( col, desc.getTitle( ) );
      cellTable.setColumnWidth( col, desc.getWidth( ) );
      tableColIdx.add( i );
    }
    
    cellTable.setSelectionModel( selectionModel );
  }
  
  private void buildPager( ) {
    SimplePager.Resources pagerResources = GWT.create( SimplePager.Resources.class );
    pager = new SimplePager( TextLocation.CENTER, pagerResources, false, 0, true );
    pager.setDisplay( cellTable );
  }
  
  public void load( ) {
    AsyncDataProvider<SearchResultRow> dataProvider = new AsyncDataProvider<SearchResultRow>( ) {
      @Override
      protected void onRangeChanged( HasData<SearchResultRow> display ) {
        SearchRange sr = new SearchRange( -1 );
        Range range = display.getVisibleRange( );
        if ( range != null ) {
          sr.setStart( range.getStart( ) );
          sr.setLength( range.getLength( ) );
        }
        ColumnSortList sortList = cellTable.getColumnSortList( );
        if ( sortList != null && sortList.size( ) > 0 ) {
          ColumnSortInfo sort = sortList.get( 0 );
          if ( sort != null ) {
            sr.setSortField( tableColIdx.get( cellTable.getColumnIndex( ( Column<SearchResultRow, ?> ) sort.getColumn( ) ) ) );
            sr.setAscending( sort.isAscending( ) );
          }
        }
        changeHandler.handleRangeChange( sr );
      }
    };
    dataProvider.addDataDisplay( cellTable );
    
    AsyncHandler sortHandler = new AsyncHandler( cellTable );
    cellTable.addColumnSortHandler( sortHandler );    
  }
  
}
