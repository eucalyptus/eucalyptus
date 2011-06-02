package com.eucalyptus.webui.client.activity;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.service.CloudInfo;
import com.eucalyptus.webui.client.session.SessionData;
import com.eucalyptus.webui.client.view.ConfirmationView;
import com.eucalyptus.webui.client.view.RightScaleView;
import com.google.common.base.Strings;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class RightScaleActivity extends AbstractActivity implements RightScaleView.Presenter, ConfirmationView.Presenter {

  private static final Logger LOG = Logger.getLogger( RightScaleActivity.class.getName( ) );
  
  public static final String REGISTER_CAPTION = "Register with RightScale";
  public static final String REGISTER_SUBJECT = "";
  
  private static final String RIGHTSCALE_REGISTRATION_BASE_URL_DEFAULT = "https://my.rightscale.com/cloud_registrations/new?callback_url=";
  
  private ClientFactory clientFactory;
  
  private CloudInfo cloudInfo = null;
  
  public RightScaleActivity( ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
  }
  
  @Override
  public void register( ) {
    ConfirmationView dialog = this.clientFactory.getConfirmationView( );
    dialog.setPresenter( this );
    dialog.display( REGISTER_CAPTION, REGISTER_SUBJECT, getPromptHtml( ) );
  }

  private String getPromptHtml( ) {
    String external = cloudInfo.getExternalHostPort( );
    String internal = cloudInfo.getInternalHostPort( );
    String text = "";
    if ( Strings.isNullOrEmpty( external )) {
      text = "<b>Warning:</b> Rightscale could not discover the external IP address of your cloud.  Hence, the pre-filled cloud URL <i>may</i> be incorrect. Check your firewall settings.</p> ";
    } else if ( !external.equals( internal ) ) {
      text = "<b>Warning:</b> The external cloud IP discovered by Rightscale (" + external + ") is different from the IP found by Eucalyptus (" + internal + ").  Hence, the pre-filled cloud URL <i>may</i> be incorrect.  Check your firewall settings.</p> ";
    }
    String pre = "<p>You are about to open a new window to Rightscale's Web site, on which you will be able to complete registraton.</p>";
    return pre + text;
  }
  
  private String getRegistrationUrl( ) {
    String external = cloudInfo.getExternalHostPort( );
    String internal = cloudInfo.getInternalHostPort( );
    String ip = external;
    if ( Strings.isNullOrEmpty( external )) {
      ip = internal;
    }    
    String callbackUrl = "https://" + ip + cloudInfo.getServicePath( );
    String rightscaleBaseUrl = clientFactory.getSessionData( ).getStringProperty( SessionData.RIGHTSCALE_REGISTRATION_BASE_URL, RIGHTSCALE_REGISTRATION_BASE_URL_DEFAULT );
    String rightscaleUrl = rightscaleBaseUrl
                           + URL.encodeQueryString( callbackUrl ) // URL.encode() wasn't quite right
                           + "&registration_version=1.0&retry=1&secret_token="
                           + URL.encodeQueryString( cloudInfo.getCloudId( ) );
    return rightscaleUrl;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    RightScaleView view = clientFactory.getRightScaleView( );
    view.setPresenter( this );
    container.setWidget( view );
    doLoad( view );
  }

  private void doLoad( final RightScaleView view ) {
    clientFactory.getBackendService( ).getCloudInfo( clientFactory.getLocalSession( ).getSession( ), false, new AsyncCallback<CloudInfo>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        LOG.log( Level.WARNING, "Failed to load cloud info", caught );
      }

      @Override
      public void onSuccess( CloudInfo cloudInfo ) {
        if ( cloudInfo != null ) {
          RightScaleActivity.this.cloudInfo = cloudInfo;
          String url = "https://" + cloudInfo.getInternalHostPort( ) + cloudInfo.getServicePath( );
          String id = cloudInfo.getCloudId( );
          view.display( url, id );
        }
      }
      
    } );
  }

  @Override
  public void confirm( String subject ) {
    String url = getRegistrationUrl( );
    if ( !Strings.isNullOrEmpty( url ) ) {
      Window.open ( url, "_blank", "" );
    }
  }

}
