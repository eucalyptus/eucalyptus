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

import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.SearchPlace;
import com.eucalyptus.webui.client.view.ActionResultView;
import com.eucalyptus.webui.client.view.ActionResultView.ResultType;
import com.google.common.base.Strings;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public abstract class AbstractActionActivity extends AbstractActivity implements ActionResultView.Presenter {
  
  protected interface KeyValue {
    public String getKey( );
    public String getValue( );
  }
  
  protected SearchPlace place;
  protected ClientFactory clientFactory;
  
  protected ActionResultView view;
  
  public AbstractActionActivity( SearchPlace place, ClientFactory clientFactory ) {
    this.place = place;
    this.clientFactory = clientFactory;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    view = clientFactory.getActionResultView( );
    view.display( ResultType.NONE, "", false );
    view.setPresenter( this );
    container.setWidget( view );
    doAction( place.getSearch( ) );
  }

  @Override
  public void onConfirmed( ) {
    // Nothing
  }
  
  protected static KeyValue parseKeyValue( String keyValue ) {
    if ( Strings.isNullOrEmpty( keyValue ) ) {
      return null;
    }
    final String[] parts = keyValue.split( WebAction.KEY_VALUE_SEPARATOR, 2 );
    if ( parts.length < 2 || Strings.isNullOrEmpty( parts[0] ) || Strings.isNullOrEmpty( parts[1] ) ) {
      return null;
    }
    return new KeyValue( ) {
      @Override
      public String getKey( ) {
        return parts[0];
      }
      @Override
      public String getValue( ) {
        return parts[1];
      }
    };
  }
  
  protected abstract void doAction( String action );
  
}
