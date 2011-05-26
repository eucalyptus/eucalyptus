package com.eucalyptus.webui.client.view;

import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class CreateAccountViewImpl extends Composite implements CreateAccountView {
  
  private static CreateAccountViewImplUiBinder uiBinder = GWT.create( CreateAccountViewImplUiBinder.class );
  
  interface CreateAccountViewImplUiBinder extends UiBinder<Widget, CreateAccountViewImpl> {}
  
  @UiField
  Anchor ok;
  
  @UiField
  Anchor cancel;
  
  @UiField
  TextBox name;
  
  private Presenter presenter;
  
  public CreateAccountViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
  @UiHandler( "ok" )
  void handleOkClickEvent( ClickEvent event ) {
    if ( Strings.isNullOrEmpty( name.getValue( ) ) ) {
      this.presenter.doCreateAccount( name.getValue( ) );
    }
  }

  @UiHandler( "cancel" )
  void handleCancelClickEvent( ClickEvent event ) {
    this.presenter.cancel( );
  }

  @Override
  public void init( ) {
    name.setText( "" );
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }

}
