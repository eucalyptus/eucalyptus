package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class LoadingAnimationViewImpl extends Composite implements LoadingAnimationView {
  
  private static LoadingAnimationViewImplUiBinder uiBinder = GWT.create( LoadingAnimationViewImplUiBinder.class );
  
  interface LoadingAnimationViewImplUiBinder extends UiBinder<Widget, LoadingAnimationViewImpl> {}
  
  public LoadingAnimationViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
}
