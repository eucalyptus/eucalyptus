package com.eucalyptus.webui.client.view;

import java.util.logging.Logger;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class FooterViewImpl extends Composite implements FooterView {
  
  private static Logger LOG = Logger.getLogger( "FooterViewImpl" );
  
  private static FooterViewImplUiBinder uiBinder = GWT.create( FooterViewImplUiBinder.class );
  interface FooterViewImplUiBinder extends UiBinder<Widget, FooterViewImpl> {}
  
  @UiField
  LogSwitch logSwitch;
  
  @UiField
  Label version;
  
  @UiField
  Image loadingIcon;
  
  @UiField
  Image errorIcon;
  
  @UiField
  Label status;
  
  private Presenter presenter;
  
  public FooterViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
    clearStatus( );
  }
  
  @UiHandler( "logSwitch" )
  void handleLogSwitchClickedEvent( ClickEvent e ) {
    if ( this.presenter != null ) {
      if ( this.logSwitch.isClicked( ) ) {
        this.presenter.onShowLogConsole( );
      } else {
        this.presenter.onHideLogConsole( );
      }
    }
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }

  @Override
  public void setVersion( String version ) {
    this.version.setText( version );
  }

  private void clearStatus( ) {
    this.loadingIcon.setVisible( false );
    this.errorIcon.setVisible( false );
    this.status.setText( "" );
  }
  
  @Override
  public void showStatus( StatusType type, String status, int clearDelay ) {
    switch ( type ) {
      case LOADING:
        this.loadingIcon.setVisible( true );
        this.errorIcon.setVisible( false );
        break;
      case ERROR:
        this.loadingIcon.setVisible( false );
        this.errorIcon.setVisible( true );
        break;
      default:
        this.loadingIcon.setVisible( false );
        this.errorIcon.setVisible( false );
        break;
    }
    this.status.setText( status );
    if ( clearDelay > 0 ) {
      Timer timer = new Timer( ) {
        @Override
        public void run( ) {
          clearStatus( );
        }
      };
      timer.schedule( clearDelay );
    }
  }
  
}
