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

package com.eucalyptus.webui.client.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface EucalyptusServiceAsync {
  
  void login( String accountName, String userName, String password, AsyncCallback<Session> callback );

  void logout( Session session, AsyncCallback<Void> callback );
  
  void getLoginUserProfile( Session session, AsyncCallback<LoginUserProfile> callback );
  
  void getSystemProperties( Session session, AsyncCallback<HashMap<String, String>> callback );
  
  void getQuickLinks( Session session, AsyncCallback<ArrayList<QuickLinkTag>> callback );
  
  void lookupAccount( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void lookupConfiguration( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void setConfiguration( Session session, SearchResultRow config, AsyncCallback<Void> callback );

  void lookupVmType( Session session, String query, SearchRange range, AsyncCallback<SearchResult> asyncCallback );

  void setVmType( Session session, SearchResultRow result, AsyncCallback<Void> asyncCallback );

  void lookupGroup( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void lookupUser( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void lookupPolicy( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void lookupKey( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void lookupCertificate( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void lookupImage( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void createAccount( Session session, String accountName, String adminPassword, AsyncCallback<String> callback );

  void deleteAccounts( Session session, ArrayList<String> ids, AsyncCallback<Void> callback );

  void modifyAccount( Session session, ArrayList<String> values, AsyncCallback<Void> callback );

  void createUsers( Session session, String accountId, String names, String path, AsyncCallback<ArrayList<String>> callback );

  void createGroups( Session session, String accountId, String names, String path, AsyncCallback<ArrayList<String>> callback );

  void deleteUsers( Session session, ArrayList<String> ids, AsyncCallback<Void> callback );

  void deleteGroups( Session session, ArrayList<String> ids, AsyncCallback<Void> callback );

  void deletePolicy( Session session, SearchResultRow policySerialized, AsyncCallback<Void> callback );

  void deleteAccessKey( Session session, SearchResultRow keySerialized, AsyncCallback<Void> callback );

  void deleteCertificate( Session session, SearchResultRow certSerialized, AsyncCallback<Void> callback );

  void addAccountPolicy( Session session, String accountId, String name, String document, AsyncCallback<Void> callback );

  void addUserPolicy( Session session, String usertId, String name, String document, AsyncCallback<Void> callback );

  void addGroupPolicy( Session session, String groupId, String name, String document, AsyncCallback<Void> callback );

  void addUsersToGroupsByName( Session session, String userNames, ArrayList<String> groupIds, AsyncCallback<Void> callback );

  void addUsersToGroupsById( Session session, ArrayList<String> userIds, String groupNames, AsyncCallback<Void> callback );

  void removeUsersFromGroupsByName( Session session, String userNames, ArrayList<String> groupIds, AsyncCallback<Void> callback );

  void removeUsersFromGroupsById( Session session, ArrayList<String> userIds, String groupNames, AsyncCallback<Void> callback );

  void modifyUser( Session session, ArrayList<String> keys, ArrayList<String> values, AsyncCallback<Void> callback );

  void modifyGroup( Session session, ArrayList<String> values, AsyncCallback<Void> callback );

  void modifyAccessKey( Session session, ArrayList<String> values, AsyncCallback<Void> callback );

  void modifyCertificate( Session session, ArrayList<String> values, AsyncCallback<Void> callback );

  void addAccessKey( Session session, String userId, AsyncCallback<Void> callback );

  void addCertificate( Session session, String userId, String pem, AsyncCallback<Void> callback );

  void changePassword( Session session, String userId, String oldPass, String newPass, String email, AsyncCallback<Void> callback );

  void signupAccount( String accountName, String password, String email, AsyncCallback<Void> callback );

  void approveAccounts( Session session, ArrayList<String> accountNames, AsyncCallback<ArrayList<String>> callback );

  void rejectAccounts( Session session, ArrayList<String> accountNames, AsyncCallback<ArrayList<String>> callback );

  void approveUsers( Session session, ArrayList<String> userIds, AsyncCallback<ArrayList<String>> callback );

  void rejectUsers( Session session, ArrayList<String> userIds, AsyncCallback<ArrayList<String>> callback );

  void signupUser( String userName, String accountName, String password, String email, AsyncCallback<Void> callback );

  void confirmUser( String confirmationCode, AsyncCallback<Void> callback );

  void requestPasswordRecovery( String userName, String accountName, String email, AsyncCallback<Void> callback );

  void resetPassword( String confirmationCode, String password, AsyncCallback<Void> callback );

  void getToolDownloads( Session session, AsyncCallback<ArrayList<DownloadInfo>> callback );

  void getGuide( Session session, String snippet, AsyncCallback<ArrayList<GuideItem>> callback );

  void getUserToken( Session session, AsyncCallback<String> callback );
  
}
