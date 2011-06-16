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
