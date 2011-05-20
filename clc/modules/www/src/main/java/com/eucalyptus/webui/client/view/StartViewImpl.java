package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class StartViewImpl extends Composite implements StartView {
  
  private static StartViewImplUiBinder uiBinder = GWT.create( StartViewImplUiBinder.class );
  
  interface StartViewImplUiBinder extends UiBinder<Widget, StartViewImpl> {}
  
  public StartViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
}
