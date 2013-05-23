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

import com.eucalyptus.webui.client.service.EucalyptusServiceAsync;
import com.eucalyptus.webui.client.session.LocalSession;
import com.eucalyptus.webui.client.session.SessionData;
import com.eucalyptus.webui.client.view.AccountView;
import com.eucalyptus.webui.client.view.ActionResultView;
import com.eucalyptus.webui.client.view.CertView;
import com.eucalyptus.webui.client.view.ConfirmationView;
import com.eucalyptus.webui.client.view.DownloadView;
import com.eucalyptus.webui.client.view.ErrorSinkView;
import com.eucalyptus.webui.client.view.GroupView;
import com.eucalyptus.webui.client.view.ImageView;
import com.eucalyptus.webui.client.view.InputView;
import com.eucalyptus.webui.client.view.ItemView;
import com.eucalyptus.webui.client.view.KeyView;
import com.eucalyptus.webui.client.view.LoadingAnimationView;
import com.eucalyptus.webui.client.view.LoadingProgressView;
import com.eucalyptus.webui.client.view.LoginView;
import com.eucalyptus.webui.client.view.ConfigView;
import com.eucalyptus.webui.client.view.PolicyView;
import com.eucalyptus.webui.client.view.ReportView;
import com.eucalyptus.webui.client.view.ShellView;
import com.eucalyptus.webui.client.view.StartView;
import com.eucalyptus.webui.client.view.UserView;
import com.eucalyptus.webui.client.view.VmTypeView;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.place.shared.PlaceHistoryHandler.Historian;

public interface ClientFactory {

  /**
   * @return the default place.
   */
  Place getDefaultPlace( );
  /**
   * @return the place for the error page.
   */
  Place getErrorPlace( );
  
  /**
   * @return the event bus for the main activities.
   */
	EventBus getMainEventBus( );
	/**
	 * @return the place controller for the main activities.
	 */
	PlaceController getMainPlaceController( );
	/**
	 * @return the activity manager for the main activities.
	 */
	ActivityManager getMainActivityManager( );
	/**
	 * @return the place history handler for the main activities.
	 */
	PlaceHistoryHandler getMainPlaceHistoryHandler( );
	/**
	 * @return the Historian for the main activities.
	 */
	Historian getMainHistorian( );
	
	/**
	 * @return the event bus for the lifecycle activities.
	 */
	EventBus getLifecycleEventBus( );
	/**
	 * @return the place controller for the lifecycle activities.
	 */
	PlaceController getLifecyclePlaceController( );
	
	/**
	 * @return the impl. of local session record, essentially the session ID.
	 */
	LocalSession getLocalSession( );
	
	/**
	 * @return the local session data.
	 */
	SessionData getSessionData( );
	
	/**
	 * @return the impl. of Euare service.
	 */
	EucalyptusServiceAsync getBackendService( );

	LoginView getLoginView( );
	
	LoadingProgressView getLoadingProgressView( );
	
  ShellView getShellView( );
  
  StartView getStartView( );
  
  ConfigView getConfigView( );
	
  LoadingAnimationView getLoadingAnimationView( );
  
  ErrorSinkView getErrorSinkView( );
  
  AccountView getAccountView( );
  
  VmTypeView getVmTypeView( );
  
  ReportView getReportView( );
  
  GroupView getGroupView( );

  UserView getUserView( );
  
  PolicyView getPolicyView( );
  
  KeyView getKeyView( );
  
  CertView getCertView( );
  
  ImageView getImageView( );
      
  ConfirmationView getConfirmationView( );
  
  InputView getInputView( );
  
  ActionResultView getActionResultView( );
  
  DownloadView getDownloadView( );
  
  ItemView createItemView( );
  
}
