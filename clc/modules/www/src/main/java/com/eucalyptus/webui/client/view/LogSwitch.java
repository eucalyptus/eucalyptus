package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;

public class LogSwitch extends Composite implements HasClickHandlers {
  
  private static LogSwitchUiBinder uiBinder = GWT.create( LogSwitchUiBinder.class );
  interface LogSwitchUiBinder extends UiBinder<Widget, LogSwitch> {}
  
  interface SwitchStyle extends CssResource {
    String inactive( );
    String active( );
    String clickedInactive( );
    String clickedActive( );
  }
  
  public LogSwitch( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @UiField
  SwitchStyle switchStyle;
  
  @UiField
  Button switchButton;
  
  private boolean clicked = false;
  
  public boolean isClicked( ) {
    return this.clicked;
  }
  
  @Override
  public HandlerRegistration addClickHandler( ClickHandler handler ) {
    return switchButton.addClickHandler( handler );
  }
  
  @UiHandler( "switchButton" )
  void handleMouseOverEvent( MouseOverEvent e ) {
    clearAllStyle( );
    if ( clicked ) {
      switchButton.addStyleName( switchStyle.clickedActive( ) );
    } else {
      switchButton.addStyleName( switchStyle.active( ) );
    }
  }
  
  @UiHandler( "switchButton" )
  void handleMouseOutEvent( MouseOutEvent e ) {
    clearAllStyle( );
    if ( clicked ) {
      switchButton.addStyleName( switchStyle.clickedInactive( ) );
    } else {
      switchButton.addStyleName( switchStyle.inactive( ) );
    }    
  }
  
  @UiHandler( "switchButton" )
  void handleClickEvent( ClickEvent e ) {
    clearAllStyle( );
    clicked = !clicked;
    if ( clicked ) {
      switchButton.addStyleName( switchStyle.clickedActive( ) );
    } else {
      switchButton.addStyleName( switchStyle.active( ) );
    }
  }
  
  private void clearAllStyle( ) {
    switchButton.removeStyleName( switchStyle.inactive( ) );
    switchButton.removeStyleName( switchStyle.active( ) );
    switchButton.removeStyleName( switchStyle.clickedInactive( ) );
    switchButton.removeStyleName( switchStyle.clickedActive( ) );
  }
  
}
