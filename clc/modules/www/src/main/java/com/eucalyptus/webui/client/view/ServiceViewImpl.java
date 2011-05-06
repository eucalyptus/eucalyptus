package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ServiceViewImpl extends Composite implements ServiceView {
  
  private static ServiceViewImplUiBinder uiBinder = GWT.create( ServiceViewImplUiBinder.class );
  
  interface ServiceViewImplUiBinder extends UiBinder<Widget, ServiceViewImpl> {}
  
  public ServiceViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
}
