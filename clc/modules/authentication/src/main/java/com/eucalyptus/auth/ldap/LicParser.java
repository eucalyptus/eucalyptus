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

package com.eucalyptus.auth.ldap;

import java.util.Set;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.LicParseException;
import com.eucalyptus.auth.json.JsonUtils;
import com.eucalyptus.auth.lic.LicSpec;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

/**
 * Parser of ldap integration configuration (LIC). LIC is in JSON format.
 */
public class LicParser {
  
  private static final Logger LOG = Logger.getLogger( LicParser.class );
  
  private static final String COMMENT = "_comment";
 
  // Support LDAP protocols 
  private static final String LDAP_URL_PREFIX = "ldap://";
  private static final String LDAPS_URL_PREFIX = "ldaps://";
  
  // Supported authentication methods
  public static final String LDAP_AUTH_METHOD_SIMPLE = "simple";
  public static final String LDAP_AUTH_METHOD_SASL_DIGEST_MD5 = "DIGEST-MD5";
  public static final String LDAP_AUTH_METHOD_SASL_GSSAPI = "GSSAPI";
  
  private static final Set<String> LDAP_AUTH_METHODS = Sets.newHashSet( );
  static {
    LDAP_AUTH_METHODS.add( LDAP_AUTH_METHOD_SIMPLE );
    LDAP_AUTH_METHODS.add( LDAP_AUTH_METHOD_SASL_DIGEST_MD5 );
    LDAP_AUTH_METHODS.add( LDAP_AUTH_METHOD_SASL_GSSAPI );
  }
  
  private static LicParser instance;
  
  public static LicParser getInstance( ) {
    if ( instance == null ) {
      instance = new LicParser( );
    }
    return instance;
  }
  
  public LicParser( ) {
  }
  
  public LdapIntegrationConfiguration parse( String licText ) throws LicParseException {
    if ( licText == null ) {
      throw new LicParseException( LicParseException.EMPTY_LIC );
    }
    try {
      JSONObject licJson = JSONObject.fromObject( licText );
      LdapIntegrationConfiguration lic = new LdapIntegrationConfiguration( );
      parseSyncConfig( licJson, lic );
      if ( lic.isSyncEnabled( ) ) {
        parseLdapService( licJson, lic );
        parseAccounts( licJson, lic );
        parseGroups( licJson, lic );
        parseUsers( licJson, lic );
      }
      return lic;
    } catch ( JSONException e ) {
      Debugging.logError( LOG, e, "Syntax error in input policy" );
      throw new LicParseException( LicParseException.SYNTAX_ERROR, e );
    }
  }

  private void parseLdapService( JSONObject licJson, LdapIntegrationConfiguration lic ) throws JSONException {
    JSONObject ldapServiceObj = JsonUtils.getRequiredByType( JSONObject.class, licJson, LicSpec.LDAP_SERVICE );
    lic.setServerUrl( validateServerUrl( JsonUtils.getRequiredByType( String.class, ldapServiceObj, LicSpec.SERVER_URL ) ) );
    // case sensitive
    lic.setAuthMethod( validateAuthMethod( JsonUtils.getRequiredByType( String.class, ldapServiceObj, LicSpec.AUTH_METHOD ), false ) );
    lic.setAuthPrincipal( validateNonEmpty( JsonUtils.getRequiredByType( String.class, ldapServiceObj, LicSpec.AUTH_PRINCIPAL ) ) );
    lic.setAuthCredentials( validateNonEmpty( JsonUtils.getRequiredByType( String.class, ldapServiceObj, LicSpec.AUTH_CREDENTIALS ) ) );
    lic.setUseSsl( "true".equalsIgnoreCase( JsonUtils.getRequiredByType( String.class, ldapServiceObj, LicSpec.USE_SSL ) ) );
    lic.setIgnoreSslCertValidation( "true".equalsIgnoreCase( JsonUtils.getByType( String.class, ldapServiceObj, LicSpec.IGNORE_SSL_CERT_VALIDATION ) ) );
    lic.setKrb5Conf( validateKrb5Conf( lic.getAuthMethod( ), JsonUtils.getByType( String.class, ldapServiceObj, LicSpec.KRB5_CONF ) ) );
    lic.setUserAuthMethod( validateAuthMethod( JsonUtils.getByType( String.class, ldapServiceObj, LicSpec.USER_AUTH_METHOD ), true ) );
}
  
  private String validateKrb5Conf( String authMethod, String krb5Conf ) throws JSONException {
    if ( LDAP_AUTH_METHOD_SASL_GSSAPI.equals( authMethod ) && Strings.isNullOrEmpty( krb5Conf ) ) {
      throw new JSONException( "krb5.conf must be specified for GSSAPI/KerberosV5" );
    }
    return krb5Conf;
  }

  private String validateServerUrl( String url ) throws JSONException {
    if ( Strings.isNullOrEmpty( url ) || !url.startsWith( LDAP_URL_PREFIX )  || !url.startsWith( LDAPS_URL_PREFIX ) ) {
      throw new JSONException( "Invalid server url " + url );
    }
    return url;
  }
  
  private String validateAuthMethod( String method, boolean allowEmpty ) throws JSONException {
    if ( ( !allowEmpty && Strings.isNullOrEmpty( method) ) || ( !Strings.isNullOrEmpty( method ) && !LDAP_AUTH_METHODS.contains( method ) ) ) {
      throw new JSONException( "Unsupported LDAP authentication method " + ( method != null ? method : "null" ) );
    }
    return method;
  }
  
  private String validateNonEmpty( String value ) throws JSONException {
    if ( Strings.isNullOrEmpty( value ) ) {
      throw new JSONException( "Empty value is not allowed for LIC element" );
    }
    return value;
  }
  
  private void parseAccounts( JSONObject licJson, LdapIntegrationConfiguration lic ) throws JSONException {
    String which = JsonUtils.checkBinaryOption( licJson, LicSpec.ACCOUNTING_GROUPS, LicSpec.GROUPS_PARTITION );
    if ( LicSpec.ACCOUNTING_GROUPS.equals( which ) ) {
      lic.setHasAccountingGroups( true );
      parseAccountingGroups( licJson, lic );
    } else {
      lic.setHasAccountingGroups( false );
      parseGroupsPartition( licJson, lic );
    }
  }
  
  private void parseGroupsPartition( JSONObject licJson, LdapIntegrationConfiguration lic )  throws JSONException {
    JSONObject groupsPartition = JsonUtils.getByType( JSONObject.class, licJson, LicSpec.GROUPS_PARTITION );
    for ( Object t : groupsPartition.keySet( ) ) {
      String partitionName = ( String ) t;
      if ( partitionName.equalsIgnoreCase( COMMENT ) ) {
        continue;
      }
      Set<String> groupSet = Sets.newHashSet( );
      groupSet.addAll( JsonUtils.getArrayByType( String.class, groupsPartition, partitionName ) );
      lic.getGroupsPartition( ).put( partitionName, groupSet );
    }
    if ( lic.getGroupsPartition( ).size( ) < 1 ) {
      throw new JSONException( "Expecting more than 1 group partition" );
    }
  }

  private void parseAccountingGroups( JSONObject licJson, LdapIntegrationConfiguration lic ) throws JSONException {
    JSONObject accountingGroups = JsonUtils.getByType( JSONObject.class, licJson, LicSpec.ACCOUNTING_GROUPS );
    lic.setAccountingGroupBaseDn( validateNonEmpty( JsonUtils.getRequiredByType( String.class, accountingGroups, LicSpec.ACCOUNTING_GROUP_BASE_DN ) ) );
    lic.setAccountingGroupsSelection( parseSelection( JsonUtils.getByType( JSONObject.class, accountingGroups, LicSpec.SELECTION ) ) );
    lic.setAccountingGroupIdAttribute( toLowerCaseIfNotNull( JsonUtils.getByType( String.class, accountingGroups, LicSpec.ID_ATTRIBUTE ) ) );
    lic.setGroupsAttribute( validateNonEmpty( JsonUtils.getRequiredByType( String.class, accountingGroups, LicSpec.GROUPS_ATTRIBUTE ) ) );
  }

  private void parseGroups( JSONObject licJson, LdapIntegrationConfiguration lic ) throws JSONException {
    JSONObject groups = JsonUtils.getRequiredByType( JSONObject.class, licJson, LicSpec.GROUPS );
    lic.setGroupBaseDn( validateNonEmpty( JsonUtils.getRequiredByType( String.class, groups, LicSpec.GROUP_BASE_DN ) ) );
    lic.setGroupsSelection( parseSelection( JsonUtils.getByType( JSONObject.class, groups, LicSpec.SELECTION ) ) );
    lic.setGroupIdAttribute( toLowerCaseIfNotNull( JsonUtils.getByType( String.class, groups, LicSpec.ID_ATTRIBUTE ) ) );
    lic.setUsersAttribute( validateNonEmpty( JsonUtils.getRequiredByType( String.class, groups, LicSpec.USERS_ATTRIBUTE ) ) );
  }
  
  private void parseUsers( JSONObject licJson, LdapIntegrationConfiguration lic ) throws JSONException {
    JSONObject users = JsonUtils.getRequiredByType( JSONObject.class, licJson, LicSpec.USERS );
    lic.setUserBaseDn( validateNonEmpty( JsonUtils.getRequiredByType( String.class, users, LicSpec.USER_BASE_DN ) ) );
    lic.setUsersSelection( parseSelection( JsonUtils.getByType( JSONObject.class, users, LicSpec.SELECTION ) ) );
    lic.setUserIdAttribute( toLowerCaseIfNotNull( JsonUtils.getByType( String.class, users, LicSpec.ID_ATTRIBUTE ) ) );
    lic.setUserSaslIdAttribute( toLowerCaseIfNotNull( JsonUtils.getByType( String.class, users, LicSpec.SASL_ID_ATTRIBUTE ) ) );
    parseUserInfoMap( ( JSONObject ) JsonUtils.getByType( JSONObject.class, users, LicSpec.USER_INFO_ATTRIBUTES ), lic );
  }
  
  private void parseUserInfoMap( JSONObject map, LdapIntegrationConfiguration lic ) throws JSONException {
    if ( map == null ) {
      return;
    }
    for ( Object m : map.keySet( ) ) {
      String attr = ( String ) m;
      if ( attr.equalsIgnoreCase( COMMENT ) ) {
        continue;
      }
      String name = JsonUtils.getByType( String.class, map, attr );
      lic.getUserInfoAttributes( ).put( attr, name );
    }
  }

  private Selection parseSelection( JSONObject obj ) throws JSONException {
    if ( obj == null ) {
      return null;
    }
    Selection selection = new Selection( );
    selection.setSearchFilter( JsonUtils.getRequiredByType( String.class, obj, LicSpec.FILTER ) );
    if ( selection.getSearchFilter( ) == null ) {
      throw new JSONException( "Empty search filter is not allowed" );
    }
    selection.getSelected( ).addAll( JsonUtils.getArrayByType( String.class, obj, LicSpec.SELECT ) );
    validateDnSet( selection.getSelected( ) );
    selection.getNotSelected( ).addAll( JsonUtils.getArrayByType( String.class, obj, LicSpec.NOT_SELECT ) );
    validateDnSet( selection.getSelected( ) );
    selection.getSelected( ).removeAll( selection.getNotSelected( ) );
    return selection;
  }
  
  private void validateDnSet( Set<String> selected ) throws JSONException {
    try {
      for ( String dn : selected ) {
        new LdapName( dn );
      }
    } catch ( InvalidNameException e ) {
      throw new JSONException( "Invalid DN name", e );
    }
  }

  private void parseSyncConfig( JSONObject licJson, LdapIntegrationConfiguration lic ) throws JSONException {
    JSONObject sync = JsonUtils.getRequiredByType( JSONObject.class, licJson, LicSpec.SYNC );
    lic.setEnableSync( "true".equalsIgnoreCase( JsonUtils.getRequiredByType( String.class, sync, LicSpec.ENABLE_SYNC ) ) );
    if ( lic.isSyncEnabled( ) ) {
      lic.setAutoSync( "true".equalsIgnoreCase( JsonUtils.getRequiredByType( String.class, sync, LicSpec.AUTO_SYNC ) ) );
      try {
        lic.setSyncInterval( Long.parseLong( JsonUtils.getRequiredByType( String.class, sync, LicSpec.SYNC_INTERVAL ) ) );
      } catch ( NumberFormatException e ) {
        throw new JSONException( "Invalid sync interval value" );
      }
      lic.setCleanDeletion( "true".equalsIgnoreCase( JsonUtils.getByType( String.class, sync, LicSpec.CLEAN_DELETION ) ) );
    }
  }
  
  private static String toLowerCaseIfNotNull( String value ) {
    if ( value != null ) {
      return value.toLowerCase( );
    }
    return value;
  }
  
}
