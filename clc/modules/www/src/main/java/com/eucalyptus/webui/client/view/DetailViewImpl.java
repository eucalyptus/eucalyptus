package com.eucalyptus.webui.client.view;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class DetailViewImpl extends Composite implements DetailView {
  
  private static Logger LOG = Logger.getLogger( DetailViewImpl.class.getName( ) );
  
  private static DetailViewImplUiBinder uiBinder = GWT.create( DetailViewImplUiBinder.class );
  
  interface DetailViewImplUiBinder extends UiBinder<Widget, DetailViewImpl> {}
  
  interface GridStyle extends CssResource {
    String grid( );
  }
  
  @UiField
  GridStyle gridStyle;
  
  @UiField
  SpanElement title;
  
  @UiField
  Anchor save;
  
  @UiField
  ScrollPanel content;
  
  private Presenter presenter;
  
  public DetailViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
    createGrid( );
  }
  
  private TextBox getTextBox( ) {
    TextBox textBox = new TextBox( );
    textBox.setEnabled( false );
    //textBox.addStyleName( gridStyle.textbox( ) );
    return textBox;
  }
  
  private CheckBox getCheckBox( ) {
    CheckBox checkBox = new CheckBox( );
    //checkBox.addStyleName( gridStyle.checkbox( ) );
    return checkBox;
  }
  
  private void createGrid( ) {
    Grid grid = new Grid( 3, 2 );
    grid.addStyleName( gridStyle.grid( ) );
    grid.getColumnFormatter( ).setWidth( 0, "40%" );
    grid.setWidget( 0, 0, new Label( "Name" ) );
    grid.setWidget( 0, 1, getTextBox( ) );
    grid.setWidget( 1, 0, new Label( "Enable" ) );
    grid.setWidget( 1, 1, getCheckBox( ) );
    grid.setWidget( 2, 0, new Label( "Name" ) );
    grid.setWidget( 2, 1, getTextBox( ) );
    
    this.content.clear( );
    this.content.add( grid );        
  }

  @UiHandler( "close" )
  void handleCloseEvent( ClickEvent e ) {
    closeSelf( );
  }
  
  @UiHandler( "save" )
  void handleSave( ClickEvent e ) {
    LOG.log( Level.INFO, "Save!" );
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }
  
  @Override
  public void setTitle( String title ) {
    this.title.setInnerText( title );
  }
  
  private void closeSelf( ) {
    this.presenter.hideDetail( );
  }
}
