package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;

public class ShellViewImpl extends Composite implements StartView {
  
  private static ShellViewImplUiBinder uiBinder = GWT.create( ShellViewImplUiBinder.class );
  interface ShellViewImplUiBinder extends UiBinder<Widget, ShellViewImpl> {}
  
  interface ShellStyle extends CssResource {
    String hoverLeft( );
    String hoverRight( );
  }
  
  private static final int ANIMATE_DURATION = 200;//ms
  private static final int DIRECTORY_WIDTH = 240; //px
  
  @UiField
  HeaderViewImpl header;
  
  @UiField
  DirectoryViewImpl directory;
  
  @UiField
  ContentViewImpl content;
  
  @UiField
  DetailViewImpl detail;
  
  @UiField
  FooterViewImpl footer;
  
  @UiField
  Button splitter;
  
  @UiField
  ShellStyle shellStyle;
  
  private boolean directoryHidden = false;
  
  public ShellViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @UiHandler( "splitter" )
  void handleSplitterMouseOver( MouseOverEvent e ) {
    if ( directoryHidden ) {
      splitter.addStyleName( shellStyle.hoverRight( ) );
    } else {
      splitter.addStyleName( shellStyle.hoverLeft( ) );
    }    
  }
  
  @UiHandler( "splitter" )
  void handleSplitterMouseOut( MouseOutEvent e ) {
    splitter.removeStyleName( shellStyle.hoverRight( ) );
    splitter.removeStyleName( shellStyle.hoverLeft( ) );
  }
  
  @UiHandler( "splitter" )
  void handleSplitterClick( ClickEvent e ) {
    DockLayoutPanel parent = (DockLayoutPanel) this.getWidget( );
    if ( directoryHidden ) {
      parent.setWidgetSize( directory, DIRECTORY_WIDTH );
      directoryHidden = false;
    } else {
      parent.setWidgetSize( directory, 0 );
      directoryHidden = true;      
    }
    parent.animate( ANIMATE_DURATION );
  }
  
  public void openDetail( ) {
    DockLayoutPanel parent = (DockLayoutPanel) this.getWidget( );
    parent.setWidgetSize( detail, 300 );
  }

  public void closeDetail( ) {
    DockLayoutPanel parent = (DockLayoutPanel) this.getWidget( );
    parent.setWidgetSize( detail, 0 );
  }

  @Override
  public void setPresenter( Presenter listener ) {
    // TODO Auto-generated method stub
    
  }
  
}
