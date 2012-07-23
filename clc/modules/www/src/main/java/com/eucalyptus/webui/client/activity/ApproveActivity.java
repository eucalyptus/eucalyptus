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
import java.util.Arrays;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.ApprovePlace;
import com.eucalyptus.webui.client.view.ActionResultView;
import com.eucalyptus.webui.client.view.ActionResultView.ResultType;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class ApproveActivity extends AbstractActionActivity implements ActionResultView.Presenter {
  
  public static final String ACCOUNT = "account";
  public static final String USERID = "userid";
  protected static final String APPROVAL_FAILURE_MESSAGE = "Failed to approve the account or user";
  protected static final String APPROVAL_SUCCESS_MESSAGE = "Successfully approved the account or user";
  
  public ApproveActivity( ApprovePlace place, ClientFactory clientFactory ) {
    super( place, clientFactory );
  }

  @Override
  protected void doAction( String action ) {
    KeyValue keyValue = parseKeyValue( action );
    if ( keyValue == null ) {
      this.view.display( ResultType.ERROR, "Invalid account or user to approve: " + action, false );
      return;
    }
    if ( ACCOUNT.equals( keyValue.getKey( ) ) ) {
      approveAccount( keyValue.getValue( ) );
    } else if ( USERID.equals( keyValue.getKey( ) ) ) {
      approveUser( keyValue.getValue( ) );
    } else {
      this.view.display( ResultType.ERROR, "Invalid account or user to approve: " + action, false );
    }
  }

  private void approveAccount( final String accountName ) {
    this.view.loading( );
    
    clientFactory.getBackendService( ).approveAccounts( clientFactory.getLocalSession( ).getSession( ), new ArrayList<String>( Arrays.asList( accountName ) ),  new AsyncCallback<ArrayList<String>>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getActionResultView( ).display( ResultType.ERROR, APPROVAL_FAILURE_MESSAGE + ": " + caught.getMessage( ), false );
      }

      @Override
      public void onSuccess( ArrayList<String> approved ) {
        if ( approved != null && approved.size( ) > 0 && approved.get( 0 ) != null && approved.get( 0 ).equals( accountName ) ) {
          clientFactory.getActionResultView( ).display( ResultType.INFO, APPROVAL_SUCCESS_MESSAGE, false );
        } else {
          clientFactory.getActionResultView( ).display( ResultType.ERROR, APPROVAL_FAILURE_MESSAGE, false );
        }
      }
      
    } );
  }

  private void approveUser( final String userId ) {
    this.view.loading( );
    
    clientFactory.getBackendService( ).approveUsers( clientFactory.getLocalSession( ).getSession( ), new ArrayList<String>( Arrays.asList( userId ) ),  new AsyncCallback<ArrayList<String>>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getActionResultView( ).display( ResultType.ERROR, APPROVAL_FAILURE_MESSAGE + ": " + caught.getMessage( ), false );
      }

      @Override
      public void onSuccess( ArrayList<String> approved ) {
        if ( approved != null && approved.size( ) > 0 && approved.get( 0 ) != null && approved.get( 0 ).equals( userId ) ) {
          clientFactory.getActionResultView( ).display( ResultType.INFO, APPROVAL_SUCCESS_MESSAGE, false );
        } else {
          clientFactory.getActionResultView( ).display( ResultType.ERROR, APPROVAL_FAILURE_MESSAGE, false );
        }
      }
      
    } );
  }
  
}
