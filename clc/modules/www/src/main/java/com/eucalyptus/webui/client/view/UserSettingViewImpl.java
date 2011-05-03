package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class UserSettingViewImpl extends PopupPanel {
  
  private static UserSettingViewImplUiBinder uiBinder = GWT.create( UserSettingViewImplUiBinder.class );
  interface UserSettingViewImplUiBinder extends UiBinder<Widget, UserSettingViewImpl> {}
  
  public UserSettingViewImpl( ) {
    super( true );
    setWidget( uiBinder.createAndBindUi( this ) );
  }
  
  @UiHandler( "userLink" )
  void handleClickOnUserAnchor( ClickEvent e ) {
    this.hide( );
  }
  
}
