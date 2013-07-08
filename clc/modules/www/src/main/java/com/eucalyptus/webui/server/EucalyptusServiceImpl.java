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

package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.webui.client.service.QuickLinkTag;
import com.eucalyptus.webui.client.service.CloudInfo;
import com.eucalyptus.webui.client.service.DownloadInfo;
import com.eucalyptus.webui.client.service.EucalyptusService;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.GuideItem;
import com.eucalyptus.webui.client.service.LoginUserProfile;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResult;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.service.Session;
import com.eucalyptus.webui.shared.query.QueryParser;
import com.eucalyptus.webui.shared.query.QueryParsingException;
import com.eucalyptus.webui.shared.query.QueryType;
import com.eucalyptus.webui.shared.query.SearchQuery;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class EucalyptusServiceImpl extends RemoteServiceServlet implements EucalyptusService {

  private static final Logger LOG = Logger.getLogger( EucalyptusServiceImpl.class );
  
  private static final long serialVersionUID = 1L;

  private static final String NAME_SEPARATOR_PATTERN = ";";

  private static final Random RANDOM = new Random( );
  
  public static User verifySession( Session session ) throws EucalyptusServiceException {
    WebSession ws = WebSessionManager.getInstance( ).getSessionById( session.getId( ) );
    if ( ws == null ) {
      throw new EucalyptusServiceException( EucalyptusServiceException.INVALID_SESSION );
    }
    return EuareWebBackend.getUser( ws.getUserId( ) );
  }
  
  private static void invalidateSession( String userId ) {
	  WebSessionManager.getInstance( ).removeUserSessions( userId );
  }
    
  private static SearchQuery parseQuery( QueryType type, String query ) throws EucalyptusServiceException {
    SearchQuery sq = null;
    try {
      sq = QueryParser.get( ).parse( type.name( ), query );
    } catch ( QueryParsingException e ) {
      throw new EucalyptusServiceException( "Invalid query: " + e );
    }
    return sq;
  }

  private static void randomDelay( ) {
    try {
      Thread.sleep( 200 + RANDOM.nextInt( 800 ) );
    } catch ( Exception e ) { }
  }
  
  @Override
  public Session login( String accountName, String userName, String password ) throws EucalyptusServiceException {
    // Simple thwart to automatic login attack.
    randomDelay( );
    if ( Strings.isNullOrEmpty( accountName ) || Strings.isNullOrEmpty( userName ) || Strings.isNullOrEmpty( password ) ) {
      throw new EucalyptusServiceException( "Empty login or password" );
    }
    User user = EuareWebBackend.getUser( userName, accountName );
    try {
      EuareWebBackend.checkPassword( user, password );
    } catch ( EucalyptusServiceException e ) {
      // see LoginUserProfile.getLoginAction for expiry handling
      if ( !EucalyptusServiceException.EXPIRED_PASSWORD.equals( e.getMessage() ) ) throw e;
    }
    return new Session( WebSessionManager.getInstance( ).newSession( user.getUserId( ) ) );
  }


  @Override
  public void logout( Session session ) throws EucalyptusServiceException {
    verifySession( session );
    WebSessionManager.getInstance( ).removeSession( session.getId( ) );
  }

  @Override
  public LoginUserProfile getLoginUserProfile( Session session ) throws EucalyptusServiceException {
    User user = verifySession( session );
    return EuareWebBackend.getLoginUserProfile( user );
  }

  @Override
  public HashMap<String, String> getSystemProperties( Session session ) throws EucalyptusServiceException {
    verifySession( session );
    return WebProperties.getProperties( );
  }

  @Override
  public ArrayList<QuickLinkTag> getQuickLinks( Session session ) throws EucalyptusServiceException {
    User user = verifySession( session );
    return QuickLinks.getTags( user );
  }
  
  @Override
  public SearchResult lookupConfiguration( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    User user = verifySession( session );
    if ( !user.isSystemAdmin( ) ) {
      LOG.error( "Non system admin tries to query system configurations" );
      throw new EucalyptusServiceException( "Operation can not be authorized" );
    }
    SearchResult result = new SearchResult( );
    result.setDescs( ConfigurationWebBackend.COMMON_FIELD_DESCS );
    result.addRows( ConfigurationWebBackend.getCloudConfigurations( ) );
    result.addRows( ConfigurationWebBackend.getClusterConfigurations( ) );
    result.addRows( ConfigurationWebBackend.getStorageConfigurations( ) );
    result.addRows( ConfigurationWebBackend.getWalrusConfigurations( ) );
    result.addRows( ConfigurationWebBackend.getBrokerConfigurations( ) );
    result.addRows( ConfigurationWebBackend.getArbitratorConfigurations( ) );
    result.setTotalSize( result.length( ) );
    result.setRange( range );
    return result;
  }
  
  @Override
  public void setConfiguration( Session session, SearchResultRow config ) throws EucalyptusServiceException {
    User user = verifySession( session );
    if ( !user.isSystemAdmin( ) ) {
      LOG.error( "Non system admin tries to change system configurations" );
      throw new EucalyptusServiceException( "Operation can not be authorized" );
    }
    if ( config == null ) {
      throw new EucalyptusServiceException( "Empty config to save" );
    }
    String type = config.getField( ConfigurationWebBackend.TYPE_FIELD_INDEX );
    if ( type == null ) {
      throw new EucalyptusServiceException( "Empty configuration type" );
    }
    LOG.debug( "Set: " + config );
    if ( ConfigurationWebBackend.CLOUD_TYPE.equals( type ) ) {
      ConfigurationWebBackend.setCloudConfiguration( config );
    } else if ( ConfigurationWebBackend.CLUSTER_TYPE.equals( type ) ) {
      ConfigurationWebBackend.setClusterConfiguration( config );
    } else if ( ConfigurationWebBackend.STORAGE_TYPE.equals( type ) ) {
      ConfigurationWebBackend.setStorageConfiguration( config );
    } else if ( ConfigurationWebBackend.WALRUS_TYPE.equals( type ) ) {
      ConfigurationWebBackend.setWalrusConfiguration( config );
    } else if ( ConfigurationWebBackend.BROKER_TYPE.equals( type ) ) {
        ConfigurationWebBackend.setBrokerConfiguration( config );
    } else if ( ConfigurationWebBackend.ARBITRATOR_TYPE.equals( type ) ) {
        ConfigurationWebBackend.setArbitratorConfiguration( config );
    } else {
      throw new EucalyptusServiceException( "Wrong configuration type: " + type );
    }
  }

  @Override
  public SearchResult lookupVmType( Session session, String query, SearchRange range ) throws EucalyptusServiceException {
    verifySession( session );
    SearchResult result = new SearchResult( );
    result.setDescs( VmTypeWebBackend.COMMON_FIELD_DESCS );
    result.addRows( VmTypeWebBackend.getVmTypes( ) );
    result.setTotalSize( result.length( ) );
    result.setRange( range );
    return result;
  }

  @Override
  public void setVmType( Session session, SearchResultRow vmType ) throws EucalyptusServiceException {
    User user = verifySession( session );
    if ( !user.isSystemAdmin( ) ) {
      LOG.error( "Non system admin tries to change VM type definitions" );
      throw new EucalyptusServiceException( "Operation can not be authorized" );
    }
    if ( vmType == null ) {
      throw new EucalyptusServiceException( "Empty UI input for VmType" );
    }
    VmTypeWebBackend.setVmType( vmType );
  }

  
  @Override
  public SearchResult lookupAccount( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    User user = verifySession( session );
    SearchQuery searchQuery = parseQuery( QueryType.account, search );
    List<SearchResultRow> rows = EuareWebBackend.searchAccounts( user, searchQuery );
    SearchResult result = new SearchResult( rows.size( ), range );
    result.setDescs( EuareWebBackend.ACCOUNT_COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( rows, range ) );
    return result;
  }

  @Override
  public SearchResult lookupGroup( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    User user = verifySession( session );
    SearchQuery searchQuery = parseQuery( QueryType.group, search );
    List<SearchResultRow> searchResult = EuareWebBackend.searchGroups( user, searchQuery );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.GROUP_COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupUser( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    User user = verifySession( session );
    SearchQuery searchQuery = parseQuery( QueryType.user, search );
    List<SearchResultRow> searchResult = EuareWebBackend.searchUsers( user, searchQuery );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.USER_COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupPolicy( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    User user = verifySession( session );
    SearchQuery searchQuery = parseQuery( QueryType.policy, search );
    List<SearchResultRow> searchResult = EuareWebBackend.searchPolicies( user, searchQuery );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.POLICY_COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupKey( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    User user = verifySession( session );
    SearchQuery searchQuery = parseQuery( QueryType.key, search );
    List<SearchResultRow> searchResult = EuareWebBackend.searchKeys( user, searchQuery );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.KEY_COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupCertificate( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    User user = verifySession( session );
    SearchQuery searchQuery = parseQuery( QueryType.cert, search );
    List<SearchResultRow> searchResult = EuareWebBackend.searchCerts( user, searchQuery );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.CERT_COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupImage( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    User user = verifySession( session );
    List<SearchResultRow> searchResult = ImageWebBackend.searchImages( user, search );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( ImageWebBackend.COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public String createAccount( Session session, String accountName, String password ) throws EucalyptusServiceException {
    User user = verifySession( session );
    return EuareWebBackend.createAccount( user, accountName, password );
  }

  @Override
  public void deleteAccounts( Session session, ArrayList<String> ids ) throws EucalyptusServiceException {
    User user = verifySession( session );
    EuareWebBackend.deleteAccounts( user, ids );
  }

  @Override
  public void modifyAccount( Session session, ArrayList<String> values ) throws EucalyptusServiceException {
    User user = verifySession( session );
    EuareWebBackend.modifyAccount( user, values );
  }

  @Override
  public ArrayList<String> createUsers( Session session, String accountId, String names, String path ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    if ( Strings.isNullOrEmpty( names ) ) {
      throw new EucalyptusServiceException( "Invalid names for creating users: " + names );
    }
    ArrayList<String> users = Lists.newArrayList( );
    for ( String name : names.split( NAME_SEPARATOR_PATTERN ) ) {
      name = name.trim( );
      if ( !Strings.isNullOrEmpty( name ) ) {
        String created = EuareWebBackend.createUser( requestUser, accountId, name, path );
        if ( created != null ) {
          users.add( created );
        }
      }
    }
    return users;
  }

  @Override
  public ArrayList<String> createGroups( Session session, String accountId, String names, String path ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    if ( Strings.isNullOrEmpty( names ) ) {
      throw new EucalyptusServiceException( "Invalid names for creating groups: " + names );
    }
    ArrayList<String> groups = Lists.newArrayList( );
    for ( String name : names.split( NAME_SEPARATOR_PATTERN ) ) {
      name = name.trim( );
      if ( !Strings.isNullOrEmpty( name ) ) {
        String created = EuareWebBackend.createGroup( requestUser, accountId, name, path );
        if ( created != null ) {
          groups.add( created );
        }
      }
    }
    return groups;    
  }

  @Override
  public void deleteUsers( Session session, ArrayList<String> ids ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.deleteUsers( requestUser, ids );
  }

  @Override
  public void deleteGroups( Session session, ArrayList<String> ids ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.deleteGroups( requestUser, ids );
  }

  @Override
  public void addAccountPolicy( Session session, String accountId, String name, String document ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.addAccountPolicy( requestUser, accountId, name, document );
  }

  @Override
  public void addUserPolicy( Session session, String userId, String name, String document ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.addUserPolicy( requestUser, userId, name, document );
  }

  @Override
  public void addGroupPolicy( Session session, String groupId, String name, String document ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.addGroupPolicy( requestUser, groupId, name, document );
  }

  @Override
  public void deletePolicy( Session session, SearchResultRow policySerialized ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.deletePolicy( requestUser, policySerialized );
  }

  @Override
  public void deleteAccessKey( Session session, SearchResultRow keySerialized ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.deleteAccessKey( requestUser, keySerialized );
  }

  @Override
  public void deleteCertificate( Session session, SearchResultRow certSerialized ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.deleteCertificate( requestUser, certSerialized );
  }

  @Override
  public void addUsersToGroupsByName( Session session, String userNames, ArrayList<String> groupIds ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    if ( Strings.isNullOrEmpty( userNames ) || groupIds == null || groupIds.size( ) < 1 ) {
      throw new EucalyptusServiceException( "Empty names or empty group ids" );
    }
    for ( String groupId : groupIds ) {
      if ( !Strings.isNullOrEmpty( groupId ) ) {
        for ( String userName : userNames.split( NAME_SEPARATOR_PATTERN ) ) {
          userName = userName.trim( );
          if ( !Strings.isNullOrEmpty( userName ) ) {
            EuareWebBackend.addUserToGroupByName( requestUser, userName, groupId );
          }
        }
      }
    }
  }

  @Override
  public void addUsersToGroupsById( Session session, ArrayList<String> userIds, String groupNames ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    if ( Strings.isNullOrEmpty( groupNames ) || userIds == null || userIds.size( ) < 1 ) {
      throw new EucalyptusServiceException( "Empty names or empty group ids" );
    }
    for ( String userId : userIds ) {
      if ( !Strings.isNullOrEmpty( userId ) ) {
        for ( String groupName : groupNames.split( NAME_SEPARATOR_PATTERN ) ) {
          groupName = groupName.trim( );
          if ( !Strings.isNullOrEmpty( groupName ) ) {
            EuareWebBackend.addUserToGroupById( requestUser, userId, groupName );
          }
        }
      }
    }
  }

  @Override
  public void removeUsersFromGroupsByName( Session session, String userNames, ArrayList<String> groupIds ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    if ( Strings.isNullOrEmpty( userNames ) || groupIds == null || groupIds.size( ) < 1 ) {
      throw new EucalyptusServiceException( "Empty names or empty group ids" );
    }
    for ( String groupId : groupIds ) {
      if ( !Strings.isNullOrEmpty( groupId ) ) {
        for ( String userName : userNames.split( NAME_SEPARATOR_PATTERN ) ) {
          userName = userName.trim();
          if ( !Strings.isNullOrEmpty( userName ) ) {
            EuareWebBackend.removeUserFromGroupByName( requestUser, userName, groupId );
          }
        }
      }
    }
  }

  @Override
  public void removeUsersFromGroupsById( Session session, ArrayList<String> userIds, String groupNames ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    if ( Strings.isNullOrEmpty( groupNames ) || userIds == null || userIds.size( ) < 1 ) {
      throw new EucalyptusServiceException( "Empty names or empty group ids" );
    }
    for ( String userId : userIds ) {
      if ( !Strings.isNullOrEmpty( userId ) ) {
        for ( String groupName : groupNames.split( NAME_SEPARATOR_PATTERN ) ) {
          groupName = groupName.trim( );
          if ( !Strings.isNullOrEmpty( groupName ) ) {
            EuareWebBackend.removeUserFromGroupById( requestUser, userId, groupName );
          }
        }
      }
    }
  }

  @Override
  public void modifyUser( Session session, ArrayList<String> keys, ArrayList<String> values ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.modifyUser( requestUser, keys, values );
  }

  @Override
  public void modifyGroup( Session session, ArrayList<String> values ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.modifyGroup( requestUser, values );
  }

  @Override
  public void modifyAccessKey( Session session, ArrayList<String> values ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.modifyAccessKey( requestUser, values );
  }

  @Override
  public void modifyCertificate( Session session, ArrayList<String> values ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.modifyCertificate( requestUser, values );
  }

  @Override
  public void addAccessKey( Session session, String userId ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.addAccessKey( requestUser, userId );
  }

  @Override
  public void addCertificate( Session session, String userId, String pem ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    EuareWebBackend.addCertificate( requestUser, userId, pem );
  }

  @Override
  public void changePassword( Session session, String userId, String oldPass, String newPass, String email ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    User targetUser = EuareWebBackend.changeUserPasswordAndEmail( requestUser, userId, oldPass, newPass, email );
    //invalidateSession( targetUser );
  }

  @Override
  public void signupAccount( String accountName, String password, String email ) throws EucalyptusServiceException {
    // Simple thwart to automatic signup attack.
    randomDelay( );
    User admin = EuareWebBackend.signupAccount( accountName, password, email );
    if ( admin != null ) {
      EuareWebBackend.notifyAccountRegistration( admin, accountName, email, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
    }
  }


  @Override
  public ArrayList<String> approveAccounts( Session session, ArrayList<String> accountNames ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    return EuareWebBackend.processAccountSignups( requestUser, accountNames, true, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
  }

  @Override
  public ArrayList<String> rejectAccounts( Session session, ArrayList<String> accountNames ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    return EuareWebBackend.processAccountSignups( requestUser, accountNames, false, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
  }

  @Override
  public ArrayList<String> approveUsers( Session session, ArrayList<String> userIds ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    return EuareWebBackend.processUserSignups( requestUser, userIds, true, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
  }

  @Override
  public ArrayList<String> rejectUsers( Session session, ArrayList<String> userIds ) throws EucalyptusServiceException {
    User requestUser = verifySession( session );
    return EuareWebBackend.processUserSignups( requestUser, userIds, false, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
  }

  @Override
  public void signupUser( String userName, String accountName, String password, String email ) throws EucalyptusServiceException {
    randomDelay( );
    User user = EuareWebBackend.createUser( userName, accountName, password, email );
    if ( user != null ) {
      EuareWebBackend.notifyUserRegistration( user, accountName, email, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
    }
  }

  @Override
  public void confirmUser( String confirmationCode ) throws EucalyptusServiceException {
    EuareWebBackend.confirmUser( confirmationCode );
  }

  @Override
  public void requestPasswordRecovery( String userName, String accountName, String email ) throws EucalyptusServiceException {
    EuareWebBackend.requestPasswordRecovery( userName, accountName, email, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
  }

  @Override
  public void resetPassword( String confirmationCode, String password ) throws EucalyptusServiceException {
    User targetUser = EuareWebBackend.resetPassword( confirmationCode, password );
    invalidateSession( targetUser.getUserId( ) );
  }

  @Override
  public ArrayList<DownloadInfo> getToolDownloads( Session session ) throws EucalyptusServiceException {
    verifySession( session );
    String version;
    try {
      version = UriUtils.encodeQuery( WebProperties.getVersion( ), "UTF-8" );
    } catch ( Exception e ) {
      version = WebProperties.getVersion( );
    }
    return DownloadsWebBackend.getDownloads( WebProperties.getProperty( WebProperties.TOOL_DOWNLOAD_URL, WebProperties.TOOL_DOWNLOAD_URL_DEFAULT ) + version );
  }

  @Override
  public ArrayList<GuideItem> getGuide( Session session, String snippet ) throws EucalyptusServiceException {
    User user = verifySession( session );
    return StartGuideWebBackend.getGuide( user, snippet );
  }

  @Override
  public String getUserToken( Session session ) throws EucalyptusServiceException {
    User user = verifySession( session ); // request user
    return EuareWebBackend.getUserToken( user );
  }
    
}
