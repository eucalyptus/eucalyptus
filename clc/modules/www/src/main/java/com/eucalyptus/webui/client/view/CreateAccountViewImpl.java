package com.eucalyptus.webui.client.view;

import com.eucalyptus.webui.shared.InputChecker;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class CreateAccountViewImpl extends DialogBox implements CreateAccountView {
  
  private static CreateAccountViewImplUiBinder uiBinder = GWT.create( CreateAccountViewImplUiBinder.class );
  
  interface CreateAccountViewImplUiBinder extends UiBinder<Widget, CreateAccountViewImpl> {}
  
  @UiField
  Anchor ok;
  
  @UiField
  Anchor cancel;
  
  @UiField
  TextBox name;
  
  @UiField
  Label error;
  
  private Presenter presenter;
  
  public CreateAccountViewImpl( ) {
    setWidget( uiBinder.createAndBindUi( this ) );
    setGlassEnabled( true );
  }
  
  @UiHandler( "ok" )
  void handleOkClickEvent( ClickEvent event ) {
    String checkError = InputChecker.checkAccountName( name.getValue( ) );
    if ( checkError == null ) {
      hide( );
      this.presenter.doCreateAccount( name.getValue( ) );
    } else {
      error.setText( checkError );
    }
  }

  @UiHandler( "cancel" )
  void handleCancelClickEvent( ClickEvent event ) {
    hide( );
  }

  @Override
  public void display( String caption ) {
    error.setText( "" );
    name.setText( "" );
    setText( caption );
    center( );
    show( );
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }
  
}
