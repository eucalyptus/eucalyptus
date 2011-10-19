package com.eucalyptus.webui.client.activity;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.service.DownloadInfo;
import com.eucalyptus.webui.client.view.DownloadView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class DownloadActivity extends AbstractActivity {
  
  private static final Logger LOG = Logger.getLogger( DownloadActivity.class.getName( ) );
  
  private ClientFactory clientFactory;
  
  public DownloadActivity( ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    DownloadView view = clientFactory.getDownloadView( );
    container.setWidget( view );
    doLoadImages( view );
    doLoadTools( view );
  }

  private void doLoadTools( final DownloadView view ) {
    clientFactory.getBackendService( ).getToolDownloads( clientFactory.getLocalSession( ).getSession( ), new AsyncCallback<ArrayList<DownloadInfo>>( ) {

      @Override
      public void onFailure( Throwable arg0 ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, arg0 );
        LOG.log( Level.INFO, "Failed to get image downloads" );
      }

      @Override
      public void onSuccess( ArrayList<DownloadInfo> downloads ) {
        view.displayToolDownloads( downloads );
      }
      
    } );
  }

  private void doLoadImages( final DownloadView view ) {
    clientFactory.getBackendService( ).getImageDownloads( clientFactory.getLocalSession( ).getSession( ), new AsyncCallback<ArrayList<DownloadInfo>>( ) {

      @Override
      public void onFailure( Throwable arg0 ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, arg0 );
        LOG.log( Level.INFO, "Failed to get image downloads" );
      }

      @Override
      public void onSuccess( ArrayList<DownloadInfo> downloads ) {
        view.displayImageDownloads( downloads );
      }
      
    } );
  }
  
}
