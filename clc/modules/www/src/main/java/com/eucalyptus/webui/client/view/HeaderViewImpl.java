package com.eucalyptus.webui.client.view;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class HeaderViewImpl extends Composite implements HeaderView {
  
  private static Logger LOG = Logger.getLogger( HeaderViewImpl.class.getName( ) );
  
  private static HeaderViewImplUiBinder uiBinder = GWT.create( HeaderViewImplUiBinder.class );
  interface HeaderViewImplUiBinder extends UiBinder<Widget, HeaderViewImpl> {}
  
  @UiField
  UserLink userLink;
  
  @UiField
  SearchBox searchBox;
  
  @UiField
  Label logoTitle;
  
  @UiField
  Label logoSubtitle;
  
  private UserSettingViewImpl settingPopup;
  
  private SearchHandler searchHandler;
    
  public HeaderViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
    settingPopup = new UserSettingViewImpl( );
    settingPopup.setAutoHideEnabled( true );
  }
  
  @UiHandler( "userLink" )
  void handleClickOnUserAnchor( ClickEvent e ) {
    settingPopup.setPopupPosition( userLink.getAbsoluteLeft( ) - 1,
                                   userLink.getAbsoluteTop( ) - 1 );
    settingPopup.show( );
  }

  @UiHandler( "searchBox" )
  void handleSearchBoxKeyPressed( KeyPressEvent e ) {
    if ( KeyCodes.KEY_ENTER == e.getNativeEvent( ).getKeyCode( ) ) {
      searchHandler.search( searchBox.getInput( ) );
    }
  }
  
  @Override
  public void setUser( String user ) {
    this.userLink.setUser( user );
  }

  @Override
  public UserSettingView getUserSetting( ) {
    return this.settingPopup;
  }

  @Override
  public void setSearchHandler( SearchHandler handler ) {
    this.searchHandler = handler;
  }

  @Override
  public void setLogoTitle( String title, String subtitle ) {
    this.logoTitle.setText( title );
    this.logoSubtitle.setText( subtitle );
  }
  
}
