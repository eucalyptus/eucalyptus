package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class LoadingProgressViewImpl extends Composite implements LoadingProgressView {
  
  private static LoadProgressViewImplUiBinder uiBinder = GWT.create( LoadProgressViewImplUiBinder.class );
  
  interface LoadProgressViewImplUiBinder extends UiBinder<Widget, LoadingProgressViewImpl> {}
  
  @UiField
  ProgressBar progressBar;
  
  public LoadingProgressViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @Override
  public void setProgress( int percent ) {
    progressBar.setProgress( percent );
  }
  
}
