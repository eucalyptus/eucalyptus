/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

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
  
}
