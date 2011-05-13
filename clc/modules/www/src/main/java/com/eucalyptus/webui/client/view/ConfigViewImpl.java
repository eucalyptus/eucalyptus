package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ConfigViewImpl extends Composite implements ConfigView {
  
  private static ConfigViewImplUiBinder uiBinder = GWT.create( ConfigViewImplUiBinder.class );
  
  interface ConfigViewImplUiBinder extends UiBinder<Widget, ConfigViewImpl> {}
  
  public ConfigViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
}
