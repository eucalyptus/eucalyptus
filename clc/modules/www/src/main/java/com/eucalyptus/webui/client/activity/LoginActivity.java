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

import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.ApplyPlace;
import com.eucalyptus.webui.client.place.ApplyPlace.ApplyType;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.ShellPlace;
import com.eucalyptus.webui.client.service.Session;
import com.eucalyptus.webui.client.view.LoginView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * Login process. Always the first activity when the web app loads.
 */
public class LoginActivity extends AbstractActivity implements LoginView.Presenter {

  private static final Logger LOG = Logger.getLogger( LoginActivity.class.getName( ) );
                                                     
  private ClientFactory clientFactory;
  private LoginPlace place;
  
  public LoginActivity( LoginPlace place, ClientFactory clientFactory ) {
    this.place = place;
    this.clientFactory = clientFactory;
  }
  
  private void showLoginView( AcceptsOneWidget container ) {
    LoginView loginView = this.clientFactory.getLoginView( );
    loginView.setPresenter( this );
    loginView.setPrompt( place.getPrompt( ) );
    container.setWidget( loginView );
    loginView.clearPassword( );
    loginView.setFocus( );
  }

  /**
   * This is called when the user click on the submit button of the login screen.
   */
  @Override
  public void login( String accountName, String userName, String password, final boolean staySignedIn ) {
    this.clientFactory.getBackendService( ).login( accountName, userName, password, new AsyncCallback<Session>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        LOG.log( Level.WARNING, "Login failed: " + caught );
        clientFactory.getLoginView( ).setPrompt( LoginPlace.LOGIN_FAILURE_PROMPT );
      }

      @Override
      public void onSuccess( Session session ) {
        if ( session == null ) {
          LOG.log( Level.WARNING, "Login failed: empty session" );
          clientFactory.getLoginView( ).setPrompt( LoginPlace.LOGIN_FAILURE_PROMPT );
        } else {
          // Login success. Save the session (persistent if the user wants to stay signed in)
          clientFactory.getLocalSession( ).saveSession( session, staySignedIn );
          // Starting the shell
          clientFactory.getLifecyclePlaceController( ).goTo( new ShellPlace( session ) );
        }
      }
      
    } );
    
  }

  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    // Check stored session first. If empty, load login screen.
    Session session = this.clientFactory.getLocalSession( ).getSession( );
    if ( session == null ) {
      showLoginView( container );
    } else {
      this.clientFactory.getLifecyclePlaceController( ).goTo( new ShellPlace( session ) );
    }
  }

  @Override
  public void onAccountSignup( ) {
    this.clientFactory.getLifecyclePlaceController( ).goTo( new ApplyPlace( ApplyType.ACCOUNT ) );
  }

  @Override
  public void onUserSignup( ) {
    this.clientFactory.getLifecyclePlaceController( ).goTo( new ApplyPlace( ApplyType.USER ) );
  }

  @Override
  public void onRecoverPassword( ) {
    this.clientFactory.getLifecyclePlaceController( ).goTo( new ApplyPlace( ApplyType.PASSWORD_RESET ) );
  }

}
