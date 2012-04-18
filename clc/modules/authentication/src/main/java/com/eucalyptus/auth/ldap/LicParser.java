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
 * 
 * @author wenye
 *
 */
public class LicParser {
  
  private static final Logger LOG = Logger.getLogger( LicParser.class );
  
  private static final String COMMENT = "_comment";
  
  private static final String LDAP_URL_PREFIX = "ldap://";
  
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
    if ( isEmpty( url ) || !url.startsWith( LDAP_URL_PREFIX ) ) {
      throw new JSONException( "Invalid server url " + url );
    }
    return url;
  }
  
  private String validateAuthMethod( String method, boolean allowEmpty ) throws JSONException {
    if ( ( !allowEmpty && isEmpty( method) ) || ( !isEmpty( method ) && !LDAP_AUTH_METHODS.contains( method ) ) ) {
      throw new JSONException( "Unsupported LDAP authentication method " + ( method != null ? method : "null" ) );
    }
    return method;
  }
  
  private String validateNonEmpty( String value ) throws JSONException {
    if ( isEmpty( value ) ) {
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
    lic.setAccountingGroupIdAttribute( JsonUtils.getRequiredByType( String.class, accountingGroups, LicSpec.ID_ATTRIBUTE ).toLowerCase( ) );
    lic.setGroupsAttribute( JsonUtils.getRequiredByType( String.class, accountingGroups, LicSpec.GROUPS_ATTRIBUTE ) );
  }

  private void parseGroups( JSONObject licJson, LdapIntegrationConfiguration lic ) throws JSONException {
    JSONObject groups = JsonUtils.getRequiredByType( JSONObject.class, licJson, LicSpec.GROUPS );
    lic.setGroupBaseDn( validateNonEmpty( JsonUtils.getRequiredByType( String.class, groups, LicSpec.GROUP_BASE_DN ) ) );
    lic.setGroupsSelection( parseSelection( JsonUtils.getByType( JSONObject.class, groups, LicSpec.SELECTION ) ) );
    lic.setGroupIdAttribute( JsonUtils.getRequiredByType( String.class, groups, LicSpec.ID_ATTRIBUTE ).toLowerCase( ) );
    lic.setUsersAttribute( JsonUtils.getRequiredByType( String.class, groups, LicSpec.USERS_ATTRIBUTE ) );
  }
  
  private void parseUsers( JSONObject licJson, LdapIntegrationConfiguration lic ) throws JSONException {
    JSONObject users = JsonUtils.getRequiredByType( JSONObject.class, licJson, LicSpec.USERS );
    lic.setUserBaseDn( validateNonEmpty( JsonUtils.getRequiredByType( String.class, users, LicSpec.USER_BASE_DN ) ) );
    lic.setUsersSelection( parseSelection( JsonUtils.getByType( JSONObject.class, users, LicSpec.SELECTION ) ) );
    lic.setUserIdAttribute( JsonUtils.getRequiredByType( String.class, users, LicSpec.ID_ATTRIBUTE ).toLowerCase( ) );
    parseUserInfoMap( ( JSONObject ) JsonUtils.getByType( JSONObject.class, users, LicSpec.USER_INFO_ATTRIBUTES ), lic );
  }
  
  private void parseUserInfoMap( JSONObject map, LdapIntegrationConfiguration lic ) throws JSONException {
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
  
  public static boolean isEmpty( String value ) {
    return null == value || "".equals( value );
  }
  
}
