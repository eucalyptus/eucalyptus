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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
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
