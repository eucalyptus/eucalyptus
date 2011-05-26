package com.eucalyptus.webui.client.view;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;

public class ShellViewImpl extends Composite implements ShellView {
  
  private static final Logger LOG = Logger.getLogger( ShellViewImpl.class.getName( ) );
  
  private static ShellViewImplUiBinder uiBinder = GWT.create( ShellViewImplUiBinder.class );
  interface ShellViewImplUiBinder extends UiBinder<Widget, ShellViewImpl> {}
  
  interface ShellStyle extends CssResource {
    String left( );
    String right( );
  }
  
  private static final int ANIMATE_DURATION = 200;//ms
  
  public static final int DIRECTORY_WIDTH = 240;//px
  public static final int LOG_HEIGHT = 160;//px
  
  @UiField
  HeaderViewImpl header;
  
  @UiField
  DirectoryViewImpl directory;
  
  @UiField
  DetailViewImpl detail;
  
  @UiField
  FooterViewImpl footer;
  
  @UiField
  Anchor splitter;
  
  @UiField
  LogViewImpl log;

  @UiField
  ContentViewImpl content;
  
  @UiField
  ShellStyle shellStyle;
  
  private boolean directoryHidden = false;
  
  public ShellViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
  @UiHandler( "splitter" )
  void handleSplitterClick( ClickEvent e ) {
    directoryHidden = !directoryHidden;
    DockLayoutPanel parent = (DockLayoutPanel) this.getWidget( );
    if ( directoryHidden ) {
      parent.setWidgetSize( directory, 0 );
      splitter.removeStyleName( shellStyle.left( ) );
      splitter.addStyleName( shellStyle.right( ) );
    } else {
      parent.setWidgetSize( directory, DIRECTORY_WIDTH );
      splitter.removeStyleName( shellStyle.right( ) );
      splitter.addStyleName( shellStyle.left( ) );
    }
    parent.animate( ANIMATE_DURATION );
  }
  
  @Override
  public void showDetail( int width ) {
    DockLayoutPanel parent = (DockLayoutPanel) this.getWidget( );
    parent.setWidgetSize( detail, width );
  }

  @Override
  public void hideDetail( ) {
    LOG.log( Level.INFO, "Hiding detail pane." );
    DockLayoutPanel parent = (DockLayoutPanel) this.getWidget( );
    parent.setWidgetSize( detail, 0 );
  }

  @Override
  public DirectoryView getDirectoryView( ) {
    return this.directory;
  }

  @Override
  public ContentView getContentView( ) {
    return this.content;
  }

  @Override
  public FooterView getFooterView( ) {
    return this.footer;
  }

  @Override
  public HeaderView getHeaderView( ) {
    return this.header;
  }
  
  @Override
  public void showLogConsole( ) {
    DockLayoutPanel parent = (DockLayoutPanel) this.getWidget( );
    parent.setWidgetSize( this.log, LOG_HEIGHT );
    parent.animate( ANIMATE_DURATION );
  }

  @Override
  public void hideLogConsole( ) {
    DockLayoutPanel parent = (DockLayoutPanel) this.getWidget( );
    parent.setWidgetSize( this.log, 0 );
    parent.animate( ANIMATE_DURATION );
  }

  @Override
  public DetailView getDetailView( ) {
    return this.detail;
  }

  @Override
  public LogView getLogView( ) {
    return this.log;
  }

}
