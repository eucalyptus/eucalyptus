package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class LogSwitch extends Composite implements HasClickHandlers {
  
  private static LogSwitchUiBinder uiBinder = GWT.create( LogSwitchUiBinder.class );
  interface LogSwitchUiBinder extends UiBinder<Widget, LogSwitch> {}
  
  interface SwitchStyle extends CssResource {
    String active( );
    String inactive( );
  }
  
  public LogSwitch( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @UiField
  SwitchStyle switchStyle;
  
  @UiField
  Anchor switchButton;
  
  private boolean checked = false;
  
  public boolean isClicked( ) {
    return this.checked;
  }
  
  @Override
  public HandlerRegistration addClickHandler( ClickHandler handler ) {
    return switchButton.addClickHandler( handler );
  }
  
  @UiHandler( "switchButton" )
  void handleClickEvent( ClickEvent e ) {
    checked = !checked;
    if ( checked ) {
      switchButton.removeStyleName( switchStyle.inactive( ) );
      switchButton.addStyleName( switchStyle.active( ) );
    } else {
      switchButton.removeStyleName( switchStyle.active( ) );
      switchButton.addStyleName( switchStyle.inactive( ) );
    }
  }
  
}
