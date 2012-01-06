package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class CloudRegistrationViewImpl extends Composite implements CloudRegistrationView {
  
  private static CloudRegistrationViewImplUiBinder uiBinder = GWT.create( CloudRegistrationViewImplUiBinder.class );
  
  interface CloudRegistrationViewImplUiBinder extends UiBinder<Widget, CloudRegistrationViewImpl> {}
  
  @UiField
  Label cloudUrl;
  
  @UiField
  Label cloudId;
    
  public CloudRegistrationViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @Override
  public void display( String cloudUrl, String cloudId ) {
    this.cloudUrl.setText( cloudUrl );
    this.cloudId.setText( cloudId );
  }
  
}
