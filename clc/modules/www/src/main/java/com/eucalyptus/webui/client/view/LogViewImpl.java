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
  
  @UiField
  ScrollPanel panel;
  
  @UiField
  GridStyle gridStyle;
  
  private Resources resources;
  
  private Grid currentGrid;
  private Image errorImage;
  private Image infoImage;
  
  public LogViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
    
    resources = GWT.create( Resources.class );
    
    createGrid( );
    
    this.currentGrid.setHTML( 0, 0, getTimeString( new Date( ) ) );
    this.currentGrid.setWidget( 0, 1, new Image( resources.error( ) ) );
    this.currentGrid.setHTML( 0, 2, "Loading this message and write to log window" );
    
    this.currentGrid.setHTML( 1, 0, getTimeString( new Date( ) ) );
    this.currentGrid.setWidget( 1, 1,  new Image( resources.info( ) ) );
    this.currentGrid.setHTML( 1, 2, "Again loading this message and write to log window" );
    
    this.currentGrid.setHTML( 2, 0, getTimeString( new Date( ) ) );
    this.currentGrid.setWidget( 2, 1,  new Image( resources.info( ) ) );
    this.currentGrid.setHTML( 2, 2, "Again loading this message and write to log window" );
    
    this.currentGrid.setHTML( 3, 0, getTimeString( new Date( ) ) );
    this.currentGrid.setWidget( 3, 1,  new Image( resources.info( ) ) );
    this.currentGrid.setHTML( 3, 2, "Again loading this message and write to log window" );

    this.panel.setWidget( currentGrid );
  }

  private void createGrid( ) {
    this.currentGrid = new Grid( 7, 3 );
    this.currentGrid.addStyleName( gridStyle.grid( ) );
    this.currentGrid.getColumnFormatter( ).setWidth( 0, "160px" );
    this.currentGrid.getColumnFormatter( ).setWidth( 1, "30px" );
  }
  
  private String getTimeString( Date date ) {
    return DateTimeFormat.getFormat( DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM ).format( date );
  }
  
}
