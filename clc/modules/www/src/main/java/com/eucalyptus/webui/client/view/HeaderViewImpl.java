package com.eucalyptus.webui.client.view;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class HeaderViewImpl extends Composite {
  
  private static Logger LOG = Logger.getLogger( "HeaderViewImpl" );
  
  private static HeaderViewImplUiBinder uiBinder = GWT.create( HeaderViewImplUiBinder.class );
  interface HeaderViewImplUiBinder extends UiBinder<Widget, HeaderViewImpl> {}
  
  @UiField
  UserLink userLink;
  
  private PopupPanel settingPopup;
  
  public HeaderViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
    settingPopup = new UserSettingViewImpl( );
    settingPopup.setAutoHideEnabled( true );
  }
  
  @UiHandler( "userLink" )
  void handleClickOnUserAnchor( ClickEvent e ) {
    LOG.log( Level.INFO, "CLICK" );
    settingPopup.setPopupPosition( userLink.getAbsoluteLeft( ) - 1,
                                   userLink.getAbsoluteTop( ) - 1 );
    settingPopup.show( );
  }
  
}
