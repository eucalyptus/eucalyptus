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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import com.eucalyptus.webui.client.service.CategoryItem;
import com.eucalyptus.webui.client.service.CategoryTag;
import com.eucalyptus.webui.client.service.CloudInfo;
import com.eucalyptus.webui.client.service.DownloadInfo;
import com.eucalyptus.webui.client.service.GuideItem;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.Type;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.service.EucalyptusService;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.LoginUserProfile;
import com.eucalyptus.webui.client.service.SearchResult;
import com.eucalyptus.webui.client.service.Session;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.TableDisplay;
import com.eucalyptus.webui.shared.query.QueryType;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class EucalyptusServiceImpl extends RemoteServiceServlet implements EucalyptusService {

  private static final long serialVersionUID = 1L;

  @Override
  public Session login( String accountName, String userName, String password ) throws EucalyptusServiceException {
    return new Session( "FAKESESSIONID" );
  }

  @Override
  public LoginUserProfile getLoginUserProfile( Session session ) throws EucalyptusServiceException {
    return new LoginUserProfile( "1234", "admin", "eucalyptus", "123456", "user:id=1234", "key:userid=1234", null );
  }

  @Override
  public HashMap<String, String> getSystemProperties( Session session ) throws EucalyptusServiceException {
    HashMap<String, String> props = Maps.newHashMap( );
    props.put( "version", "Eucalyptus EEE 3.0" );
    props.put( "search-result-page-size", "5" );
    return props;
  }

  @Override
  public ArrayList<CategoryTag> getCategory( Session session ) throws EucalyptusServiceException {    
    return Categories.getTags( );
  }

  private static final List<SearchResultRow> DATA = Arrays.asList( new SearchResultRow( Arrays.asList( "test0", "0", "modify", "#start:", "test", "test1", "test2", "" ) ),
                                                                   new SearchResultRow( Arrays.asList( "test1", "1", "modify", "#start:", "test", "test1", "test2", "" ) ),
                                                                   new SearchResultRow( Arrays.asList( "test2", "2", "modify", "#start:", "test", "test1", "test2", "" ) ),
                                                                   new SearchResultRow( Arrays.asList( "test3", "3", "modify", "#start:", "test", "test1", "test2", "" ) ),
                                                                   new SearchResultRow( Arrays.asList( "test4", "4", "modify", "#start:", "test", "test1", "test2", "" ) ),
                                                                   new SearchResultRow( Arrays.asList( "test5", "5", "modify", "#start:", "test", "test1", "test2", "" ) ),
                                                                   new SearchResultRow( Arrays.asList( "test6", "6", "modify", "#start:", "test", "test1", "test2", "" ) ),
                                                                   new SearchResultRow( Arrays.asList( "test7", "7", "modify", "#start:", "test", "test1", "test2", "" ) ),
                                                                   new SearchResultRow( Arrays.asList( "test8", "8", "modify", "#start:", "test", "test1", "test2", "" ) ),
                                                                   new SearchResultRow( Arrays.asList( "test9", "9", "modify", "#start:", "test", "test1", "test2", "" ) ),
                                                                   new SearchResultRow( Arrays.asList( "testA", "A", "modify", "#start:", "test", "test1", "test2", "" ) )
                                                                 );
  private static final List<SearchResultFieldDesc> FIELDS = Arrays.asList( new SearchResultFieldDesc( "Name", true, "40%" ),
                                                                           new SearchResultFieldDesc( "Id", true, "60%", TableDisplay.MANDATORY, Type.TEXT, false, true ),
                                                                           new SearchResultFieldDesc( "Action", false, "0px", TableDisplay.NONE, Type.ACTION, false, false ),
                                                                           new SearchResultFieldDesc( "Link", false, "0px", TableDisplay.NONE, Type.LINK, false, false ),
                                                                           new SearchResultFieldDesc( "Access key", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ),
                                                                           new SearchResultFieldDesc( "Certificate", false, "0px", TableDisplay.NONE, Type.KEYVAL, true, false ),
                                                                           new SearchResultFieldDesc( "Another fancy key", false, "0px", TableDisplay.NONE, Type.KEYVAL, true, false ),
                                                                           new SearchResultFieldDesc( "", false, "0px", TableDisplay.NONE, Type.NEWKEYVAL, true, false )
                                                                         );
  @Override
  public SearchResult lookupAccount( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    System.out.println( "New search: " + range );
    
    final int sortField = range.getSortField( );
    if ( sortField >= 0 ) {
      final boolean ascending = range.isAscending( );
      Collections.sort( DATA, new Comparator<SearchResultRow>( ) {
        @Override
        public int compare( SearchResultRow r1, SearchResultRow r2 ) {
          if ( r1 == r2 ) {
            return 0;
          }
          // Compare the name columns.
          int diff = -1;
          if ( r1 != null ) {
            diff = ( r2 != null ) ? r1.getField( sortField ).compareTo( r2.getField( sortField ) ) : 1;
          }
          return ascending ? diff : -diff;
        }
      } );
    }
    int resultLength = Math.min( range.getLength( ), DATA.size( ) - range.getStart( ) );
    SearchResult result = new SearchResult( DATA.size( ), range );
    result.setDescs( FIELDS );
    result.setRows( DATA.subList( range.getStart( ), range.getStart( ) + resultLength ) );
    
    for ( SearchResultRow row : result.getRows( ) ) {
      System.out.println( "Row: " + row );
    }
    
    return result;
  }

  @Override
  public void logout( Session session ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public SearchResult lookupConfiguration( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setConfiguration( Session session, SearchResultRow config ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public SearchResult lookupVmType( Session session, String query, SearchRange range ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setVmType( Session session, SearchResultRow result ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public SearchResult lookupGroup( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SearchResult lookupUser( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SearchResult lookupPolicy( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SearchResult lookupKey( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SearchResult lookupCertificate( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SearchResult lookupImage( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String createAccount( Session session, String accountName ) throws EucalyptusServiceException {
    return "1234";
  }

  @Override
  public void deleteAccounts( Session session, ArrayList<String> ids ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void modifyAccount( Session session, ArrayList<String> values ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ArrayList<String> createUsers( Session session, String accountId, String names, String path ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ArrayList<String> createGroups( Session session, String accountId, String names, String path ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void deleteUsers( Session session, ArrayList<String> ids ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void deleteGroups( Session session, ArrayList<String> ids ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addAccountPolicy( Session session, String accountId, String name, String document ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addUserPolicy( Session session, String usertId, String name, String document ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addGroupPolicy( Session session, String groupId, String name, String document ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void deletePolicy( Session session, SearchResultRow policySerialized ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void deleteAccessKey( Session session, SearchResultRow keySerialized ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void deleteCertificate( Session session, SearchResultRow certSerialized ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addUsersToGroupsByName( Session session, String userNames, ArrayList<String> groupIds ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addUsersToGroupsById( Session session, ArrayList<String> userIds, String groupNames ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void removeUsersFromGroupsByName( Session session, String userNames, ArrayList<String> groupIds ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void removeUsersFromGroupsById( Session session, ArrayList<String> userIds, String groupNames ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void modifyUser( Session session, ArrayList<String> keys, ArrayList<String> values ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void modifyGroup( Session session, ArrayList<String> values ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void modifyAccessKey( Session session, ArrayList<String> values ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void modifyCertificate( Session session, ArrayList<String> values ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addAccessKey( Session session, String userId ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addCertificate( Session session, String userId, String pem ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void changePassword( Session session, String userId, String oldPass, String newPass, String email ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void signupAccount( String accountName, String password, String email ) throws EucalyptusServiceException {
    try {
      Thread.sleep( 2000 );
    } catch ( InterruptedException e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void signupUser( String userName, String accountName, String password, String email ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ArrayList<String> approveAccounts( Session session, ArrayList<String> accountNames ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ArrayList<String> rejectAccounts( Session session, ArrayList<String> accountNames ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ArrayList<String> approveUsers( Session session, ArrayList<String> userIds ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ArrayList<String> rejectUsers( Session session, ArrayList<String> userIds ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void confirmUser( String confirmationCode ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void requestPasswordRecovery( String userName, String accountName, String email ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void resetPassword( String confirmationCode, String password ) throws EucalyptusServiceException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public CloudInfo getCloudInfo( Session session, boolean setExternalHostPort ) throws EucalyptusServiceException {
    CloudInfo cloudInfo = new CloudInfo( );
    cloudInfo.setCloudId( "CLOUDID1234567890" );
    cloudInfo.setExternalHostPort( "127.0.0.1:8443" );
    cloudInfo.setInternalHostPort( "127.0.0.1:8443" );
    cloudInfo.setServicePath( "/Eucalyptus/services" );
    return cloudInfo;
  }

  @Override
  public ArrayList<DownloadInfo> getToolDownloads( Session session ) throws EucalyptusServiceException {
    return new ArrayList<DownloadInfo>( Arrays.asList( new DownloadInfo( "http://localhost", "euca-tools", "EUCA tools" ),
                                                          new DownloadInfo( "http://localhost", "boto", "BOTO" ),
                                                          new DownloadInfo( "http://localhost", "other", "Other stuff" ) ) );
  }

  @Override
  public ArrayList<GuideItem> getGuide( Session session, String snippet ) throws EucalyptusServiceException {
    return new ArrayList<GuideItem>( Arrays.asList( new GuideItem( "View and configure cloud service components",
                                                                   QueryBuilder.get( ).start( QueryType.config ).url( ),
                                                                   "cog" ),
                                                    new GuideItem( "View all the images you can access",
                                                                   QueryBuilder.get( ).start( QueryType.image ).url( ),
                                                                   "account" ),
                                                    new GuideItem( "View and configure virtual machine types",
                                                                   QueryBuilder.get( ).start( QueryType.vmtype ).url( ),
                                                                   "group" ),
                                                    new GuideItem( "View and configure virtual machine types",
                                                                   QueryBuilder.get( ).start( QueryType.vmtype ).url( ),
                                                                   "group" ),
                                                    new GuideItem( "View and configure virtual machine types",
                                                                   QueryBuilder.get( ).start( QueryType.vmtype ).url( ),
                                                                   "group" ),
                                                    new GuideItem( "View and configure virtual machine types",
                                                                   QueryBuilder.get( ).start( QueryType.vmtype ).url( ),
                                                                   "group" ),
                                                    new GuideItem( "Generate cloud resource usage report",
                                                                   QueryBuilder.get( ).start( QueryType.report ).url( ),
                                                                   "user" ) ) );
  }

}
