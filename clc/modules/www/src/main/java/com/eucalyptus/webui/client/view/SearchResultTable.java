package com.eucalyptus.webui.client.view;

import com.eucalyptus.webui.client.service.DataRow;
import com.eucalyptus.webui.client.service.SearchResult;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class SearchResultTable extends Composite {
  
  private static SearchResultTableUiBinder uiBinder = GWT.create( SearchResultTableUiBinder.class );
  
  interface SearchResultTableUiBinder extends UiBinder<Widget, SearchResultTable> {}
  
  @UiField( provided = true )
  CellTable<DataRow> cellTable;
  
  @UiField( provided = true )
  SimplePager pager;
  
  // First page of data to get the table going
  private SearchResult initialData;
    
  public SearchResultTable( SearchResult initialData ) {
    this.initialData = initialData;
    
    buildTable( );
    
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
  private void buildTable( ) {
    cellTable = new CellTable<DataRow>( );
    
    
  }
  
}
