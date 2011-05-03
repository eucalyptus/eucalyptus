package com.eucalyptus.webui.client.view;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class FooterViewImpl extends Composite {
  
  private static Logger LOG = Logger.getLogger( "FooterViewImpl" );
  
  private static FooterViewImplUiBinder uiBinder = GWT.create( FooterViewImplUiBinder.class );
  interface FooterViewImplUiBinder extends UiBinder<Widget, FooterViewImpl> {}
  
  private static final int HEIGHT = 150;
  
  @UiField
  LogSwitch logSwitch;

  private PopupPanel logPanel;
  
  public FooterViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
    logPanel = new LogViewImpl( );
  }
  
  @UiHandler( "logSwitch" )
  void handleLogSwitchClickedEvent( ClickEvent e ) {
    if ( logSwitch.isClicked( ) ) {
      logPanel.setPixelSize( RootPanel.get( ).getOffsetWidth( ) - 15, HEIGHT );
      logPanel.setPopupPosition( 5, this.getAbsoluteTop( ) - HEIGHT );
      logPanel.show( );
    } else {
      logPanel.hide( );
    }
  }
  
}
