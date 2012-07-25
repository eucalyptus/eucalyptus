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
