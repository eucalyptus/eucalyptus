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

package com.eucalyptus.webui.client;

import com.eucalyptus.webui.client.activity.ActionUtil;
import com.eucalyptus.webui.client.activity.WebAction;
import com.eucalyptus.webui.client.place.ConfirmSignupPlace;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.ResetPasswordPlace;
import com.eucalyptus.webui.shared.query.QueryType;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * Top level UI app. It sets up the top-level lifecycle activity manager,
 * which controls the state transition between main UI and login UI.
 * 
 *        success
 * Login ---------> Shell
 *        failure
 * Login <--------- Shell
 *         logout
 * Login <--------- Shell
 */
public class EucalyptusApp {
  
  private static final String CONFIRMATIONCODE = "confirmationcode";

  private final ClientFactory clientFactory;
  
  private ActivityManager lifecycleActivityManager;

  public EucalyptusApp( ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
  }
  
  public void start( final AcceptsOneWidget container ) {
    ActivityMapper activityMapper = new LifecycleActivityMapper( this.clientFactory );
    lifecycleActivityManager = new ActivityManager( activityMapper, this.clientFactory.getLifecycleEventBus( ) );
    lifecycleActivityManager.setDisplay( container );
    // First check special action activities
    checkAction( );
  }
  
  private void checkAction( ) {
    String token = History.getToken( );
    if ( token.startsWith( QueryType.confirm.name( ) + WebAction.ACTION_SEPARATOR ) ) {
      WebAction action = ActionUtil.parseAction( token );
      if ( action != null ) {
        this.clientFactory.getLifecyclePlaceController( ).goTo( new ConfirmSignupPlace( action.getValue( CONFIRMATIONCODE ) ) );
        return;
      }
    } else if ( token.startsWith( QueryType.reset.name( ) + WebAction.ACTION_SEPARATOR ) ) {
      WebAction action = ActionUtil.parseAction( token );
      if ( action != null ) {
        this.clientFactory.getLifecyclePlaceController( ).goTo( new ResetPasswordPlace( action.getValue( CONFIRMATIONCODE ) ) );
        return;
      }      
    }
    // Always login first 
    this.clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.DEFAULT_PROMPT ) );
  }
  
}
