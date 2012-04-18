package com.eucalyptus.webui.client.view;

import com.eucalyptus.webui.client.view.IconButton.Type;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class InputWithButton extends Composite implements HasClickHandlers, HasValueChangeHandlers<String> {
  
  private static InputWithButtonUiBinder uiBinder = GWT.create( InputWithButtonUiBinder.class );
  
  interface InputWithButtonUiBinder extends UiBinder<Widget, InputWithButton> {}
  
  @UiField
  TextBox input;
  
  @UiField
  IconButton button;
  
  public InputWithButton( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  public String getValue( ) {
    return this.input.getValue( );
  }
  
  public void setValue( String value ) {
    this.input.setValue( value );
  }
  
  public void setType( Type t ) {
    this.button.setType( t );
  }
  
  @Override
  public HandlerRegistration addClickHandler( ClickHandler handler ) {
    return this.button.addClickHandler( handler );
  }

  @Override
  public HandlerRegistration addValueChangeHandler( ValueChangeHandler<String> handler ) {
    return this.input.addValueChangeHandler( handler );
  }
  
}
