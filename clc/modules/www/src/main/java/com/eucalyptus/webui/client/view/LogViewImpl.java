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
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class LogViewImpl extends Composite implements LogView {
  
  private static LogViewImplUiBinder uiBinder = GWT.create( LogViewImplUiBinder.class );
  
  interface LogViewImplUiBinder extends UiBinder<Widget, LogViewImpl> {}
  
  public LogViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

}
