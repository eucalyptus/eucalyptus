package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.HasKeyPressHandlers;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class SearchBox extends Composite implements HasKeyPressHandlers {
  
  private static SearchBoxUiBinder uiBinder = GWT.create( SearchBoxUiBinder.class );
  
  interface SearchBoxUiBinder extends UiBinder<Widget, SearchBox> {}
  
  @UiField
  TextBox textInput;
  
  public SearchBox( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @Override
  public HandlerRegistration addKeyPressHandler( KeyPressHandler handler ) {
    return textInput.addKeyPressHandler( handler );
  }
  
  public String getInput( ) {
    return this.textInput.getText( );
  }

  public void clearInput( ) {
    this.textInput.setText( "" );
  }
  
}
