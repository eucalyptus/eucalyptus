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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class UserSettingViewImpl extends PopupPanel implements UserSettingView {
  
  private static UserSettingViewImplUiBinder uiBinder = GWT.create( UserSettingViewImplUiBinder.class );
  interface UserSettingViewImplUiBinder extends UiBinder<Widget, UserSettingViewImpl> {}
  
  @UiField
  UserLink userLink;
  
  private Presenter presenter;
  
  public UserSettingViewImpl( ) {
    super( true );
    setWidget( uiBinder.createAndBindUi( this ) );
  }
  
  @UiHandler( "userLink" )
  void handleClickOnUserAnchor( ClickEvent e ) {
    this.hide( );
  }
  
  @UiHandler( "profileLink" )
  void handleClickOnProfile( ClickEvent e ) {
    this.hide( );
    this.presenter.onShowProfile( );
  }
  
  @UiHandler( "keyLink" )
  void handleClickOnKey( ClickEvent e ) {
    this.hide( );
    this.presenter.onShowKey( );
  }
  
  @UiHandler( "passwordLink" )
  void handlClickOnPassword( ClickEvent e ) {
    this.hide( );
    this.presenter.onChangePassword( );
  }
  
  @UiHandler( "credLink" )
  void handleClickOnCredential( ClickEvent e ) {
    this.hide( );
    this.presenter.onDownloadCredential( );
  }
  
  @UiHandler( "logoutLink" )
  void handleClickOnLogout( ClickEvent e ) {
    this.hide( );
    this.presenter.logout( );
  }
  
  @Override
  public void setUser( String user ) {
    this.userLink.setUser( user );
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }
  
}
