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
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.service.GuideItem;
import com.eucalyptus.webui.client.view.ItemView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class GenericGuideActivity extends AbstractActivity {
  
  private ClientFactory clientFactory;
  private String snippet;
  
  public GenericGuideActivity( ClientFactory clientFactory, String snippet ) {
    this.clientFactory = clientFactory;
    this.snippet = snippet;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    ItemView view = clientFactory.createItemView( );
    container.setWidget( view );
    doLoadItems( view );
  }

  private void doLoadItems( final ItemView view ) {
    clientFactory.getBackendService( ).getGuide( clientFactory.getLocalSession( ).getSession( ), snippet, new AsyncCallback<ArrayList<GuideItem>>( ) {

      @Override
      public void onFailure( Throwable caught ) {       
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
      }

      @Override
      public void onSuccess( ArrayList<GuideItem> result ) {
        view.display( result );
      }
      
    } );
    
  }
  
}
