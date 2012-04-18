package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasWidgets.ForIsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public class ContentViewImpl extends Composite implements ContentView {
  
  private static ContentViewImplUiBinder uiBinder = GWT.create( ContentViewImplUiBinder.class );
  
  interface ContentViewImplUiBinder extends UiBinder<Widget, ContentViewImpl> {}
  
  @UiField
  LayoutPanel content;
  
  @UiField
  Label title;
  
  public ContentViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @Override
  public ForIsWidget getContentContainer( ) {
    return this.content;
  }

  @Override
  public void setContentTitle( String title ) {
    this.title.setText( title );
  }

}
