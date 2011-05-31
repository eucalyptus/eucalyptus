package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class ActionResultViewImpl extends Composite implements ActionResultView {
  
  private static ActionResultViewImplUiBinder uiBinder = GWT.create( ActionResultViewImplUiBinder.class );
  
  interface ActionResultViewImplUiBinder extends UiBinder<Widget, ActionResultViewImpl> {}
  
  public interface IconStyle extends CssResource {
    String red( );
    String green( );
    String none( );
  }
  
  @UiField
  IconStyle iconStyle;
  
  @UiField
  Button button;
  
  @UiField
  Label message;
  
  @UiField
  SpanElement icon;
  
  @UiField
  Image loading;
  
  private Presenter presenter;
  
  public ActionResultViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @UiHandler( "button" )
  void handleButtonClick( ClickEvent e ) {
    this.presenter.onConfirmed( );
  }
  
  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }

  @Override
  public void display( ResultType type, String message, boolean needsConfirmation ) {
    this.loading.setVisible( false );
    switch ( type ) {
      case ERROR:
        icon.setClassName( iconStyle.red( ) );
        break;
      case INFO:
        icon.setClassName( iconStyle.green( ) );
        break;
      default:
        icon.setClassName( iconStyle.none( ) );
        break;
    }
    this.message.setText( message );
    this.button.setVisible( needsConfirmation );
  }

  @Override
  public void loading( ) {
    this.loading.setVisible( true );
  }
  
}
