package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;

public class UserLink extends Composite implements HasClickHandlers {
  
  private static UserLinkUiBinder uiBinder = GWT.create( UserLinkUiBinder.class ); 
  interface UserLinkUiBinder extends UiBinder<Widget, UserLink> {}
  
  @UiField
  Anchor userAnchor;
  
  public UserLink( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @Override
  public HandlerRegistration addClickHandler( ClickHandler handler ) {
    return userAnchor.addClickHandler( handler );
  }
  
  public void setUser( String user ) {
    this.userAnchor.setText( user );
  }
  
}
