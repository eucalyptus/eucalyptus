package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class DetailViewImpl extends Composite implements DetailView {
  
  private static DetailViewImplUiBinder uiBinder = GWT.create( DetailViewImplUiBinder.class );
  
  interface DetailViewImplUiBinder extends UiBinder<Widget, DetailViewImpl> {}
  
  private Presenter presenter;
  
  public DetailViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
  @UiHandler( "close" )
  void handleCloseEvent( ClickEvent e ) {
    closeSelf( );
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }
  
  private void closeSelf( ) {
    this.presenter.hideDetail( );
  }
}
