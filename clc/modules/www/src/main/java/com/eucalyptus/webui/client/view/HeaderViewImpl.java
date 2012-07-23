/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

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
  
  private UserSettingViewImpl settingPopup;
  
  private Presenter presenter;
    
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
      presenter.runManualSearch( searchBox.getInput( ) );
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
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }
  
}
