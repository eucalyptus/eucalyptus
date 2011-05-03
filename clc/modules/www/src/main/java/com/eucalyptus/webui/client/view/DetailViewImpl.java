package com.eucalyptus.webui.client.view;

import com.eucalyptus.webui.client.EucalyptusWebInterface;
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

public class DetailViewImpl extends Composite {
  
  private static DetailViewImplUiBinder uiBinder = GWT.create( DetailViewImplUiBinder.class );
  
  interface DetailViewImplUiBinder extends UiBinder<Widget, DetailViewImpl> {}
  
  public DetailViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
  @UiHandler( "close" )
  void handleCloseEvent( ClickEvent e ) {
    EucalyptusWebInterface.shell.closeDetail( );
  }
  
}
