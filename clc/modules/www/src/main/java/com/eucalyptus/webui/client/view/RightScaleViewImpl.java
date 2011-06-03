package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class RightScaleViewImpl extends Composite implements RightScaleView {
  
  private static RightScaleViewImplUiBinder uiBinder = GWT.create( RightScaleViewImplUiBinder.class );
  
  interface RightScaleViewImplUiBinder extends UiBinder<Widget, RightScaleViewImpl> {}
  
  @UiField
  Label cloudUrl;
  
  @UiField
  Label cloudId;
  
  private Presenter presenter;
  
  public RightScaleViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
  @UiHandler( "registerButton" )
  void handleClickRegisterButton( ClickEvent e ) {
    this.presenter.register( );
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }

  @Override
  public void display( String cloudUrl, String cloudId ) {
    this.cloudUrl.setText( cloudUrl );
    this.cloudId.setText( cloudId );
  }
  
}
