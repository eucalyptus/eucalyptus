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
import com.google.gwt.user.client.ui.Widget;

public class DirectoryViewImpl extends Composite {
  
  private static DirectoryViewImplUiBinder uiBinder = GWT.create( DirectoryViewImplUiBinder.class );
  interface DirectoryViewImplUiBinder extends UiBinder<Widget, DirectoryViewImpl> {}
  
  @UiField
  CategoryTree tree;
  
  public DirectoryViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

}
