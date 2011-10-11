package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class ErrorSinkViewImpl extends Composite implements ErrorSinkView {
  
  private static ErrorSinkViewImplUiBinder uiBinder = GWT.create( ErrorSinkViewImplUiBinder.class );
  
  interface ErrorSinkViewImplUiBinder extends UiBinder<Widget, ErrorSinkViewImpl> {}

  @UiField
  SpanElement message;
  
  public ErrorSinkViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @Override
  public void setMessage( String message ) {
    this.message.setInnerText( message );
  }
  
}
