package com.eucalyptus.webui.client.view;

import java.util.Date;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class LogViewImpl extends Composite implements LogView {
  
  private static LogViewImplUiBinder uiBinder = GWT.create( LogViewImplUiBinder.class );
  
  interface LogViewImplUiBinder extends UiBinder<Widget, LogViewImpl> {}
  
  interface GridStyle extends CssResource {
    String grid( );
  }
  
  interface Resources extends ClientBundle {
    @Source( "image/info_12x12_blue.png" )
    ImageResource info( );
    
    @Source( "image/x_alt_12x12_red.png" )
    ImageResource error( );
  }
  
  private static final int MAX_LOG_LINES = 1024;
  private static final String TIME_COL_WIDTH = "160px";
  private static final String ICON_COL_WIDTH = "30px";
  
  @UiField
  ScrollPanel panel;
  
  @UiField
  GridStyle gridStyle;
  
  private Resources resources;
  
  private Grid currentGrid;
  
  public LogViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
    resources = GWT.create( Resources.class );
    createGrid( );
    this.panel.setWidget( currentGrid );
  }

  private void createGrid( ) {
    this.currentGrid = new Grid( 1, 3 );
    this.currentGrid.addStyleName( gridStyle.grid( ) );
    this.currentGrid.getColumnFormatter( ).setWidth( 0, TIME_COL_WIDTH );
    this.currentGrid.getColumnFormatter( ).setWidth( 1, ICON_COL_WIDTH );
    this.currentGrid.setText( 0, 0, getTimeString( new Date( ) ) );
    this.currentGrid.setWidget( 0, 1, getLogIcon( LogType.INFO ) );
    this.currentGrid.setText( 0, 2, "Main screen loaded" );
  }
  
  private String getTimeString( Date date ) {
    return DateTimeFormat.getFormat( DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM ).format( date );
  }

  private Image getLogIcon( LogType type ) {
    switch ( type ) {
      case ERROR:
        return new Image( resources.error( ) );
      default:
        return new Image( resources.info( ) );
    }
  }
  
  @Override
  public void log( LogType type, String content ) {
    this.currentGrid.insertRow( 0 );
    this.currentGrid.setText( 0, 0, getTimeString( new Date( ) ) );
    this.currentGrid.setWidget( 0, 1, getLogIcon( type ) );
    this.currentGrid.setText( 0, 2, content != null ? content : "" );
    truncateLog( );
  }

  private void truncateLog( ) {
    if ( this.currentGrid.getRowCount( ) > MAX_LOG_LINES ) {    
      this.currentGrid.removeRow( this.currentGrid.getRowCount( ) - 1 );
    }
  }

  @Override
  public void clear( ) {
    while ( this.currentGrid.getRowCount( ) > 0 ) {
      this.currentGrid.removeRow( 0 );
    }
  }
  
}
