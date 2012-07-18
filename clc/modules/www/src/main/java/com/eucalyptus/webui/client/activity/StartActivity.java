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

import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.StartPlace;
import com.eucalyptus.webui.client.view.StartView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * Showing the start page, providing guides for first-time users,
 * and shortcuts for experienced users.
 */
public class StartActivity extends AbstractActivity {
  
  public static final String TITLE = "START GUIDE";
  
  public static final String SERVICE_SNIPPET = "service";
  public static final String IAM_SNIPPET = "iam";
  
  private static final Logger LOG = Logger.getLogger( StartActivity.class.getName( ) );
  
  private ClientFactory clientFactory;
  private StartPlace place;
  
  public StartActivity( StartPlace place, ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
    this.place = place;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    LOG.log( Level.INFO, "Start StartActivity" );
    this.clientFactory.getShellView( ).getContentView( ).setContentTitle( TITLE );
    StartView startView = this.clientFactory.getStartView( );
    container.setWidget( startView );
    loadSnippets( startView, eventBus );
    ActivityUtil.updateDirectorySelection( clientFactory );
  }
  
  private void loadSnippets( StartView view, EventBus eventBus ) {
	boolean isSystemAdmin = this.clientFactory.getSessionData( ).getLoginUser( ).isSystemAdmin( );
    if ( isSystemAdmin ) {
      new CloudRegistrationActivity( clientFactory ).start( view.getCloudRegSnippetDisplay( ), eventBus );
      new GenericGuideActivity( clientFactory, SERVICE_SNIPPET ).start( view.getServiceSnippetDisplay( ), eventBus );
    }
    new DownloadActivity( clientFactory ).start( view.getDownloadSnippetDisplay( ), eventBus );
    new GenericGuideActivity( clientFactory, IAM_SNIPPET ).start( view.getIamSnippetDisplay( ), eventBus );
  }
  
}
