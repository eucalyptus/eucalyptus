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
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.webui.client.service.QuickLink;
import com.eucalyptus.webui.client.service.QuickLinkTag;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.shared.query.QueryType;

public class QuickLinks {
  
  private static final Logger LOG = Logger.getLogger( QuickLinks.class );
  
  public static ArrayList<QuickLinkTag> getTags( User login ) throws EucalyptusServiceException {
    if ( login.isSystemAdmin( ) ) {
      return getSystemAdminTags( login );
    } else if ( login.isAccountAdmin() ) {
        return getAccountAdminTags( login );
    } else {
      return getUserTags( login );
    }
  }
  
  private static ArrayList<QuickLinkTag> getSystemAdminTags( User login ) throws EucalyptusServiceException {
    try {
      return new ArrayList<QuickLinkTag>( Arrays.asList(
              new QuickLinkTag( "System Management", 
                               new ArrayList<QuickLink>( Arrays.asList(
                                              new QuickLink( "Start", "Start guide", "home",
                                                                QueryBuilder.get( ).start( QueryType.start ).query( ) ),
                                              new QuickLink( "Service Components", "Configuration of service components", "config",
                                                                QueryBuilder.get( ).start( QueryType.config ).query( ) ) ) ) ),
              new QuickLinkTag( "Identity Management",
                               new ArrayList<QuickLink>( Arrays.asList(
                                              new QuickLink( "Accounts", "Accounts", "dollar", 
                                                                QueryBuilder.get( ).start( QueryType.account ).query( ) ),
                                              new QuickLink( "Groups", "User groups", "group",
                                                                QueryBuilder.get( ).start( QueryType.group ).query( ) ),
                                              new QuickLink( "Users", "Users", "user",
                                                                QueryBuilder.get( ).start( QueryType.user ).query( ) ),
                                              new QuickLink( "Policies", "Policies", "lock",
                                                                QueryBuilder.get( ).start( QueryType.policy ).query( ) ),
                                              new QuickLink( "Keys", "Access keys", "key",
                                                                QueryBuilder.get( ).start( QueryType.key ).query( ) ),
                                              new QuickLink( "Certificates", "X509 certificates", "sun",
                                                                QueryBuilder.get( ).start( QueryType.cert ).query( ) ) ) ) ),
              new QuickLinkTag( "Resource Management",
                               new ArrayList<QuickLink>( Arrays.asList(
                                              new QuickLink( "Images", "Virtual machine images (EMIs)", "image",
                                                                QueryBuilder.get( ).start( QueryType.image ).query( ) ),
                                              new QuickLink( "VmTypes", "Virtual machine types", "type",
                                                                QueryBuilder.get( ).start( QueryType.vmtype ).query( ) ),
                                              new QuickLink( "Usage Report", "Resource usage report", "report",
                                                                QueryBuilder.get( ).start( QueryType.report ).query( ) ) ) ) ) ) );
    } catch ( Exception e ) {
      LOG.error( "Failed to load user information", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to load user information for " + login );
    }    
  }

  private static ArrayList<QuickLinkTag> getAccountAdminTags( User login ) throws EucalyptusServiceException {
	    try {
	      String accountId = login.getAccount( ).getAccountNumber( );
	      String userId = login.getUserId( );
	      return new ArrayList<QuickLinkTag>( Arrays.asList( 
	              new QuickLinkTag( "System Management", 
	                               new ArrayList<QuickLink>( Arrays.asList(
	                                              new QuickLink( "Start", "Start guide", "home",
	                                                                QueryBuilder.get( ).start( QueryType.start ).query( ) ) ) ) ),
	              new QuickLinkTag( "Identity Management",
	                               new ArrayList<QuickLink>( Arrays.asList(
	                                              new QuickLink( "Accounts", "Accounts", "dollar", 
	                                                                QueryBuilder.get( ).start( QueryType.account ).query( ) ),
	                                              new QuickLink( "Groups", "User groups", "group",
	                                                                QueryBuilder.get( ).start( QueryType.group ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
	                                              new QuickLink( "Users", "Users", "user",
	                                                                QueryBuilder.get( ).start( QueryType.user ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
	                                              new QuickLink( "Policies", "Policies", "lock",
	                                                                QueryBuilder.get( ).start( QueryType.policy ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
	                                              new QuickLink( "Keys", "Access keys", "key",
	                                                                QueryBuilder.get( ).start( QueryType.key ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
	                                              new QuickLink( "Certificates", "X509 certificates", "sun",
	                                                                QueryBuilder.get( ).start( QueryType.cert ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ) ) ) ),
	              new QuickLinkTag( "Resource Management",
                                 new ArrayList<QuickLink>( Arrays.asList(
                                 new QuickLink( "Images", "Virtual machine images (EMIs)", "image",
                                                   QueryBuilder.get( ).start( QueryType.image ).query( ) )  ) ) ) ) );

	    } catch ( Exception e ) {
	      LOG.error( "Failed to load user information", e );
	      LOG.debug( e, e );
	      throw new EucalyptusServiceException( "Failed to load user information for " + login );
	    }    
	  }

  private static ArrayList<QuickLinkTag> getUserTags( User login ) throws EucalyptusServiceException {
    try {
      String accountId = login.getAccount( ).getAccountNumber( );
      String userId = login.getUserId( );
      return new ArrayList<QuickLinkTag>( Arrays.asList( 
              new QuickLinkTag( "System Management", 
                               new ArrayList<QuickLink>( Arrays.asList(
                                              new QuickLink( "Start", "Start guide", "home",
                                                                QueryBuilder.get( ).start( QueryType.start ).query( ) ) ) ) ),
              new QuickLinkTag( "Identity Management",
                               new ArrayList<QuickLink>( Arrays.asList(
                                              new QuickLink( "Accounts", "Accounts", "dollar", 
                                                                QueryBuilder.get( ).start( QueryType.account ).query( ) ),
                                              new QuickLink( "Groups", "User groups", "group",
                                                                QueryBuilder.get( ).start( QueryType.group ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                              new QuickLink( "Users", "Users", "user",
                                                                QueryBuilder.get( ).start( QueryType.user ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                              new QuickLink( "Policies", "Policies", "lock",
                                                                QueryBuilder.get( ).start( QueryType.policy ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                              new QuickLink( "Keys", "Access keys", "key",
                                                                QueryBuilder.get( ).start( QueryType.key ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                              new QuickLink( "Certificates", "X509 certificates", "sun",
                                                                QueryBuilder.get( ).start( QueryType.cert ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ) ) ) ),
              new QuickLinkTag( "Resource Management",
                               new ArrayList<QuickLink>( Arrays.asList(
                                              new QuickLink( "Images", "Virtual machine images (EMIs)", "image",
                                                                QueryBuilder.get( ).start( QueryType.image ).query( ) ) ) ) ) ) );
    } catch ( Exception e ) {
      LOG.error( "Failed to load user information", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to load user information for " + login );
    }    
  }

}
