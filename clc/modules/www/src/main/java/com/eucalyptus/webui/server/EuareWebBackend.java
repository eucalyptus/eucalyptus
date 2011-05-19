package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.User.RegistrationStatus;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.webui.client.service.Categories;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.QueryBuilder;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.TableDisplay;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.Type;
import com.google.common.collect.Lists;

public class EuareWebBackend {

  private static final Logger LOG = Logger.getLogger( EuareWebBackend.class );

  // Field names
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String PATH = "path";
  public static final String USERS = "users";
  public static final String GROUPS = "groups";
  public static final String POLICIES = "policies";
  public static final String ARN = "arn";
  public static final String ACCOUNT = "account";
  public static final String USER = "user";
  public static final String GROUP = "group";
  public static final String PASSWORD = "password";
  public static final String KEY = "key";
  public static final String CERT = "cert";
  public static final String VERSION = "version";
  public static final String TEXT = "text";
  public static final String OWNER = "owner";
  public static final String ENABLED = "enabled";
  public static final String REGISTRATION = "registration";
  public static final String EXPIRATION = "expiration";
  public static final String ACTIVE = "active";
  public static final String REVOKED = "revoked";
  public static final String CREATION = "creation";
  public static final String PEM = "pem";
  
  public static final String ACTION_CHANGE = "Change";
    
  public static final ArrayList<SearchResultFieldDesc> ACCOUNT_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    ACCOUNT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "20%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    ACCOUNT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, "Name", true, "80%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    ACCOUNT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( USERS, "Member users", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    ACCOUNT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( GROUPS, "Member groups", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    ACCOUNT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( POLICIES, "Policies", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> GROUP_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "15%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, "Name", true, "15%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PATH, "Path", true, "30%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNT, "Owner account", true, "40%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ARN, "ARN", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( linkfy( ACCOUNT ), "Owner account", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( USERS, "Member users", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( POLICIES, "Policies", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> USER_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    SearchResultFieldDesc desc;
    
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "15%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, "Name", true, "15%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PATH, "Path", true, "30%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNT, "Owner account", true, "15%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ENABLED, "Enabled", false, "25%", TableDisplay.MANDATORY, Type.BOOLEAN, true, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( REGISTRATION, "Registration status", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ARN, "ARN", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( linkfy( ACCOUNT ), "Owner account", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( GROUPS, "Membership groups", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( POLICIES, "Policies", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PASSWORD, "Password", false, "0px", TableDisplay.NONE, Type.ACTION, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( EXPIRATION, "Password expires on", false, "0px", TableDisplay.NONE, Type.DATE, true, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( KEY, "Access keys", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( CERT, "X509 certificates", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> POLICY_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "20%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, "Name", true, "20%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( VERSION, "Version", false, "60%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( OWNER, "Owner", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( TEXT, "Policy text", false, "0px", TableDisplay.NONE, Type.ARTICLE, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> KEY_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "20%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACTIVE, "Active", false, "80%", TableDisplay.MANDATORY, Type.BOOLEAN, true, false ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( CREATION, "Creation date", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( OWNER, "Owner", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> CERT_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "20%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACTIVE, "Active", false, "20%", TableDisplay.MANDATORY, Type.BOOLEAN, true, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( REVOKED, "Revoked", false, "60%", TableDisplay.MANDATORY, Type.BOOLEAN, false, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( CREATION, "Creation date", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( OWNER, "Owner", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PEM, "PEM", false, "0px", TableDisplay.NONE, Type.ARTICLE, false, false ) );
  }
  
  // Just to denote a link field for an otherwise text field.
  private static String linkfy( String name ) {
    return name + ":";
  }
  
  private static List<String> getRegistrationStatusList( ) {
    List<String> list = Lists.newArrayList( );
    for ( RegistrationStatus v : RegistrationStatus.values( ) ) {
      list.add( v.name( ) );
    }
    return list;
  }
  
  public static User getUser( String userName, String accountName ) throws EucalyptusServiceException {
    if ( userName == null || accountName == null ) {
      throw new EucalyptusServiceException( "Empty user name or account name" );
    }
    try {
      Account account = Accounts.lookupAccountByName( accountName );
      User user = account.lookupUserByName( userName );
      return user;
    } catch ( Exception e ) {
      LOG.error( "Failed to verify user " + userName + "@" + accountName );
      throw new EucalyptusServiceException( "Failed to verify user " + userName + "@" + accountName );
    }
  }
  
  public static void checkPassword( User user, String password ) throws EucalyptusServiceException {
    if ( !user.getPassword( ).equals( Crypto.generateHashedPassword( password ) ) ) {
      throw new EucalyptusServiceException( "Incorrect password" );
    }
  }
  
  public static List<SearchResultRow> searchAccounts( String query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      for ( Account account : Accounts.listAllAccounts( ) ) {
        results.add( serializeAccount( account ) );
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get accounts", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get accounts" );
    }
    return results;
  }

  private static SearchResultRow serializeAccount( Account account ) throws Exception {
    SearchResultRow result = new SearchResultRow( );
    result.addField( account.getAccountNumber( ) );
    result.addField( account.getName( ) );
    // Search links for account fields: users, groups and policies
    result.addField( QueryBuilder.get( ).start( Categories.USER ).and( ACCOUNT, account.getName( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( Categories.GROUP ).and( ACCOUNT, account.getName( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( Categories.POLICY ).and( ACCOUNT, account.getName( ) ).url( ) );
    return result;
  }

  public static List<SearchResultRow> searchGroups( User user, String query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      Account account = user.getAccount( );
      for ( Group group : account.getGroups( ) ) {
        results.add( serializeGroup( account.getName( ), group ) );
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get groups", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get groups" );
    }
    return results;    
  }

  private static SearchResultRow serializeGroup( String accountName, Group group ) {
    SearchResultRow result = new SearchResultRow( );
    result.addField( group.getGroupId( ) );
    result.addField( group.getName( ) );
    result.addField( group.getPath( ) );
    result.addField( accountName );
    result.addField( ( new EuareResourceName( accountName, PolicySpec.IAM_RESOURCE_GROUP, group.getPath( ), group.getName( ) ) ).toString( ) );
    result.addField( QueryBuilder.get( ).start( Categories.ACCOUNT ).and( NAME, accountName ).url( ) );
    result.addField( QueryBuilder.get( ).start( Categories.USER ).and( ACCOUNT, accountName ).and( GROUP, group.getName( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( Categories.POLICY ).and( ACCOUNT, accountName ).and( GROUP, group.getName( ) ).url( ) );
    return result;
  }
  
  public static List<SearchResultRow> searchUsers( User user, String query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      Account account = user.getAccount( );
      for ( User u : account.getUsers( ) ) {
        results.add( serializeUser( account.getName( ), u ) );
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get users", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get users" );
    }
    return results;    
    
  }

  private static SearchResultRow serializeUser( String accountName, User user ) throws Exception {
    SearchResultRow result = new SearchResultRow( );
    result.addField( user.getUserId( ) );
    result.addField( user.getName( ) );
    result.addField( user.getPath( ) );
    result.addField( accountName );
    result.addField( user.isEnabled( ).toString( ) );
    result.addField( user.getRegistrationStatus( ).name( ) );
    result.addField( ( new EuareResourceName( accountName, PolicySpec.IAM_RESOURCE_USER, user.getPath( ), user.getName( ) ) ).toString( ) );
    result.addField( QueryBuilder.get( ).start( Categories.ACCOUNT ).and( NAME, accountName ).url( ) );
    result.addField( QueryBuilder.get( ).start( Categories.GROUP ).and( ACCOUNT, accountName ).and( USER, user.getName( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( Categories.POLICY ).and( ACCOUNT, accountName ).and( USER, user.getName( ) ).url( ) );
    result.addField( ACTION_CHANGE );
    result.addField( user.getPasswordExpires( ).toString( ) );
    result.addField( QueryBuilder.get( ).start( Categories.KEY ).and( ACCOUNT, accountName ).and( USER, user.getName( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( Categories.CERTIFICATE ).and( ACCOUNT, accountName ).and( USER, user.getName( ) ).url( ) );
    // Now the info fields
    for ( Map.Entry<String, String> entry : user.getInfo( ).entrySet( ) ) {
      result.addExtraFieldDesc( new SearchResultFieldDesc( entry.getKey( ), entry.getKey( ), false, "0px", TableDisplay.NONE, Type.KEYVAL, true, false ) );
      result.addField( entry.getValue( ) );
    }
    result.addExtraFieldDesc( new SearchResultFieldDesc( "", "Type new info here", false, "0px", TableDisplay.NONE, Type.NEWKEYVAL, true, false ) );
    result.addField( "" );
    return result;
  }
  
  public static List<SearchResultRow> searchPolicies( User user, String query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      for ( Policy policy : user.getPolicies( ) ) {
        results.add( serializePolicy( user, policy ) );
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get policies", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get policies for user " + user.getName( ) );      
    }    
    return results;
  }

  private static SearchResultRow serializePolicy( User user, Policy policy ) throws Exception {
    SearchResultRow result = new SearchResultRow( );
    result.addField( policy.getPolicyId( ) );
    result.addField( policy.getName( ) );
    result.addField( policy.getVersion( ) );
    result.addField( QueryBuilder.get( ).start( Categories.USER ).and( ACCOUNT, user.getAccount( ).getName( ) ).and( NAME, user.getName( ) ).url( ) );
    result.addField( policy.getText( ) );
    return result;
  }

  public static List<SearchResultRow> searchCerts( User user, String query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      for ( Certificate cert : user.getCertificates( ) ) {
        results.add( serializeCert( user, cert ) );
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get certs", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get certs for user " + user.getName( ) );      
    }
    return results;
  }

  private static SearchResultRow serializeCert( User user, Certificate cert ) throws Exception {
    SearchResultRow result = new SearchResultRow( );
    result.addField( cert.getCertificateId( ) );
    result.addField( cert.isActive( ).toString( ) );
    result.addField( cert.isRevoked( ).toString( ) );
    result.addField( cert.getCreateDate( ).toString( ) );
    result.addField( QueryBuilder.get( ).start( Categories.USER ).and( ACCOUNT, user.getAccount( ).getName( ) ).and( NAME, user.getName( ) ).url( ) );
    result.addField( cert.getPem( ) );
    return result;
  }

  public static List<SearchResultRow> searchKeys( User user, String query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      for ( AccessKey key : user.getKeys( ) ) {
        results.add( serializeKey( user, key ) );
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get keys", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get keys for user " + user.getName( ) );      
    }    
    return results;    
  }

  private static SearchResultRow serializeKey( User user, AccessKey key ) throws Exception {
    SearchResultRow result = new SearchResultRow( );
    result.addField( key.getAccessKey( ) );
    result.addField( key.isActive( ).toString( ) );
    result.addField( key.getCreateDate( ).toString( ) );
    result.addField( QueryBuilder.get( ).start( Categories.USER ).and( ACCOUNT, user.getAccount( ).getName( ) ).and( NAME, user.getName( ) ).url( ) );
    return result;
  }

}
