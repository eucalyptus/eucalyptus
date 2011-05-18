package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class ProgressBar extends Composite {
  
  private static ProgressBarUiBinder uiBinder = GWT.create( ProgressBarUiBinder.class );
  
  interface ProgressBarUiBinder extends UiBinder<Widget, ProgressBar> {}
  
  @UiField
  DivElement bar;
  
  @UiField
  DivElement text;
  
  public ProgressBar( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
  public void setProgress( int percent ) {
    String percentage = percent + "%";
    bar.setAttribute( "style", "width:" + percentage );
    text.setInnerText( percentage );
  }

}
