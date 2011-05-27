package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.TableDisplay;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.Type;
import com.eucalyptus.webui.shared.query.QueryType;
import com.eucalyptus.webui.shared.query.QueryValue;
import com.eucalyptus.webui.shared.query.SearchQuery;
import com.eucalyptus.webui.shared.query.SearchQuery.Matcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
  public static final String ACCOUNTID = "accountid";
  public static final String GROUPID = "groupid";
  public static final String USERID = "userid";
  public static final String OWNERID = "ownerid";
  
  public static final String ACTION_CHANGE = "modify";
    
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
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "25%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, "Name", true, "10%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PATH, "Path", true, "35%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNT, "Owner account", true, "30%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ARN, "ARN", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNTID, "Owner account", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( USERS, "Member users", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( POLICIES, "Policies", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> USER_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "25%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, "Name", true, "10%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PATH, "Path", true, "35%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNT, "Owner account", true, "15%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ENABLED, "Enabled", false, "15%", TableDisplay.MANDATORY, Type.BOOLEAN, true, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( REGISTRATION, "Registration status", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ARN, "ARN", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNTID, "Owner account", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( GROUPS, "Membership groups", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( POLICIES, "Policies", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PASSWORD, "Password", false, "0px", TableDisplay.NONE, Type.ACTION, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( EXPIRATION, "Password expires on", false, "0px", TableDisplay.NONE, Type.DATE, true, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( KEY, "Access keys", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( CERT, "X509 certificates", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> POLICY_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "25%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, "Name", true, "10%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( VERSION, "Version", false, "10%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNT, "Owner account", false, "10%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( GROUP, "Owner group", false, "10%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( USER, "Owner user", false, "35%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( OWNERID, "Owner", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( TEXT, "Policy text", false, "0px", TableDisplay.NONE, Type.ARTICLE, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> KEY_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "25%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACTIVE, "Active", false, "10%", TableDisplay.MANDATORY, Type.BOOLEAN, true, false ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNT, "Owner account", false, "10%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( USER, "Owner user", false, "55%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( CREATION, "Creation date", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( OWNERID, "Owner", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> CERT_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "25%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACTIVE, "Active", false, "10%", TableDisplay.MANDATORY, Type.BOOLEAN, true, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( REVOKED, "Revoked", false, "10%", TableDisplay.MANDATORY, Type.BOOLEAN, false, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNT, "Owner account", false, "10%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( USER, "Owner user", false, "45%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( CREATION, "Creation date", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( OWNERID, "Owner", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PEM, "PEM", false, "0px", TableDisplay.NONE, Type.ARTICLE, false, false ) );
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
  
  private static boolean accountMatchQuery( final Account account, SearchQuery query ) throws Exception {
    return query.match( NAME, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return account.getName( ) != null && account.getName( ).contains( value.getValue( ) );
      }
    } ) && query.match( ID, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return account.getAccountNumber( ) != null && account.getAccountNumber( ).equals( value.getValue( ) );
      }
    } );
  }
  
  public static List<SearchResultRow> searchAccounts( SearchQuery query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      // Optimization for a single account search
      if ( query.hasOnlySingle( ID ) ) {
        Account account = Accounts.lookupAccountById( query.getSingle( ID ).getValue( ) );
        results.add( serializeAccount( account ) );
      } else {
        for ( Account account : Accounts.listAllAccounts( ) ) {
          if ( accountMatchQuery( account, query ) ) {
            results.add( serializeAccount( account ) );
          }
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get accounts", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get accounts for query: " + query );
    }
    return results;
  }

  private static SearchResultRow serializeAccount( Account account ) throws Exception {
    SearchResultRow result = new SearchResultRow( );
    result.addField( account.getAccountNumber( ) );
    result.addField( account.getName( ) );
    // Search links for account fields: users, groups and policies
    result.addField( QueryBuilder.get( ).start( QueryType.user ).add( ACCOUNTID, account.getAccountNumber( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.group ).add( ACCOUNTID, account.getAccountNumber( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.policy ).add( ACCOUNTID, account.getAccountNumber( ) ).url( ) );
    return result;
  }
  
  private static List<Account> getAccounts( SearchQuery query ) throws Exception {
    List<Account> accounts = Lists.newArrayList( );
    for ( final Account account : Accounts.listAllAccounts( ) ) {
      if ( query.match( ACCOUNT, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return account.getName( ) != null && account.getName( ).contains( value.getValue( ) );
        }
      } ) && query.match( ACCOUNTID, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return account.getAccountNumber( ) != null && account.getAccountNumber( ).equals( value.getValue( ) );
        }        
      } ) ) {
        accounts.add( account );
      }
    }
    return accounts;
  }

  private static List<User> getUsers( Account account, SearchQuery query ) throws Exception {
    List<User> users = Lists.newArrayList( );
    for ( final User user : account.getUsers( ) ) {
      if ( query.match( USER, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return user.getName( ) != null && user.getName( ).contains( value.getValue( ) );
        }
      } ) && query.match( USERID, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return user.getUserId( ) != null && user.getUserId( ).equals( value.getValue( ) );
        }        
      } ) ) {
        users.add( user );
      }
    }
    return users;
  }
  
  private static List<Group> getGroups( Account account, SearchQuery query ) throws Exception {
    List<Group> groups = Lists.newArrayList( );
    for ( final Group group : account.getGroups( ) ) {
      if ( query.match( GROUP, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return group.getName( ) != null && group.getName( ).contains( value.getValue( ) );
        }
      } ) && query.match( GROUPID, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return group.getGroupId( ) != null && group.getGroupId( ).equals( value.getValue( ) );
        }        
      } ) ) {
        groups.add( group );
      }
    }
    return groups;
  }
  
  private static boolean groupMatchQuery( final Group group, SearchQuery query ) throws Exception {
    return query.match( NAME, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return group.getName( ) != null && group.getName( ).contains( value.getValue( ) );
      }
    } ) && query.match( ID, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return group.getGroupId( ) != null && group.getGroupId( ).equals( value.getValue( ) );
      }
    } ) && query.match( PATH, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return group.getPath( ) != null && group.getPath( ).contains( value.getValue( ) );
      }      
    } ) && query.match( USER, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        try {
          return group.hasUser( value.getValue( ) );
        } catch ( Exception e ) {
          LOG.error( e, e );
          return false;
        }
      }      
    } );
  }
  
  public static List<SearchResultRow> searchGroups( SearchQuery query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      if ( query.hasOnlySingle( ID ) ) {
        // Optimization for a single group search
        Group group = Accounts.lookupGroupById( query.getSingle( ID ).getValue( ) );
        Account account = group.getAccount( );
        results.add( serializeGroup( account, group ) );
      } else if ( query.hasOnlySingle( USERID ) ) {
        // Optimization for groups of a user
        User user = Accounts.lookupUserById( query.getSingle( USERID ).getValue( ) );
        Account account = user.getAccount( );
        for ( Group group : user.getGroups( ) ) {
          if ( !group.isUserGroup( ) ) {
            results.add( serializeGroup( account, group ) );
          }
        }        
      } else if ( query.hasOnlySingle( ACCOUNTID ) ) {
        // Optimization for groups of an account
        Account account = Accounts.lookupAccountById( query.getSingle( ACCOUNTID ).getValue( ) );
        for ( Group group : account.getGroups( ) ) {
          if ( !group.isUserGroup( ) ) {
            results.add( serializeGroup( account, group ) );
          }
        }
      } else {
        for ( Account account : getAccounts( query ) ) {
          for ( Group group : account.getGroups( ) ) {
            if ( !group.isUserGroup( ) && groupMatchQuery( group, query ) ) {
              results.add( serializeGroup( account, group ) );
            }
          }
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get groups", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get groups for query: " + query );
    }
    return results;    
  }

  private static SearchResultRow serializeGroup( Account account, Group group ) {
    SearchResultRow result = new SearchResultRow( );
    result.addField( group.getGroupId( ) );
    result.addField( group.getName( ) );
    result.addField( group.getPath( ) );
    result.addField( account.getName( ) );
    result.addField( ( new EuareResourceName( account.getName( ), PolicySpec.IAM_RESOURCE_GROUP, group.getPath( ), group.getName( ) ) ).toString( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.account ).add( ID, account.getAccountNumber( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.user ).add( GROUPID, group.getGroupId( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.policy ).add( GROUPID, group.getGroupId( ) ).url( ) );
    return result;
  }
  
  private static boolean userMatchQuery( final User user, SearchQuery query ) throws Exception {
    if ( !( query.match( NAME, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return user.getName( ) != null && user.getName( ).contains( value.getValue( ) );
      }
    } ) && query.match( ID, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return user.getUserId( ) != null && user.getUserId( ).equals( value.getValue( ) );
      }
    } ) && query.match( PATH, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return user.getPath( ) != null && user.getPath( ).contains( value.getValue( ) );
      }
    } ) && query.match( ENABLED, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return user.isEnabled( ) != null && ( user.isEnabled( ).booleanValue( ) == "true".equalsIgnoreCase( value.getValue( ) ) );
      }
    } ) && query.match( REGISTRATION, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return user.getRegistrationStatus( ) != null && user.getRegistrationStatus( ).name( ).equalsIgnoreCase( value.getValue( ) );
      }
    } ) ) ) {
      return false;
    }
    
    for ( final Map.Entry<String, String> entry : user.getInfo( ).entrySet( ) ) {
      if ( !query.match( entry.getKey( ), new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return entry.getValue( ) != null && entry.getValue( ).equalsIgnoreCase( value.getValue( ) );
        }
      } ) ) {
        return false;
      }
    }
    
    if ( query.has( GROUP ) ) {
      final Set<String> userGroups = Sets.newHashSet( );
      for ( Group g : user.getGroups( ) ) {
        userGroups.add( g.getName( ) );
      }
      if ( !query.match( GROUP, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          try {
            return userGroups.contains( value.getValue( ) );
          } catch ( Exception e ) {
            LOG.error( e, e );
            return false;
          }
        }      
      } ) ) {
        return false;
      }
    }
    
    return true;
  }
    
  public static List<SearchResultRow> searchUsers( SearchQuery query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      if ( query.hasOnlySingle( ID ) ) {
        // Optimization for a single user search
        User user = Accounts.lookupUserById( query.getSingle( ID ).getValue( ) );
        results.add( serializeUser( user.getAccount( ), user ) );
      } else if ( query.hasOnlySingle( GROUPID ) ) {
        // Optimization for users of a single group
        Group group = Accounts.lookupGroupById( query.getSingle( GROUPID ).getValue( ) );
        Account account = group.getAccount( );
        for ( User user : group.getUsers( ) ) {
          results.add( serializeUser( account, user ) );
        }
      } else if ( query.hasOnlySingle( ACCOUNTID ) ) {
        // Optimization for users of a single account
        Account account = Accounts.lookupAccountById( query.getSingle( ACCOUNTID ).getValue( ) );
        for ( User user : account.getUsers( ) ) {
          results.add( serializeUser( account, user ) );
        }        
      } else {
        for ( Account account : getAccounts( query ) ) {
          for ( User user : account.getUsers( ) ) {
            if ( userMatchQuery( user, query ) ) {
              results.add( serializeUser( account, user ) );
            }
          }
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get users", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get users for query: " + query );
    }
    return results;    
    
  }

  private static SearchResultRow serializeUser( Account account, User user ) throws Exception {
    SearchResultRow result = new SearchResultRow( );
    result.addField( user.getUserId( ) );
    result.addField( user.getName( ) );
    result.addField( user.getPath( ) );
    result.addField( account.getName( ) );
    result.addField( user.isEnabled( ).toString( ) );
    result.addField( user.getRegistrationStatus( ).name( ) );
    result.addField( ( new EuareResourceName( account.getName( ), PolicySpec.IAM_RESOURCE_USER, user.getPath( ), user.getName( ) ) ).toString( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.account ).add( ID, account.getAccountNumber( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.group ).add( USERID, user.getUserId( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.policy ).add( USERID, user.getUserId( ) ).url( ) );
    result.addField( ACTION_CHANGE );
    result.addField( user.getPasswordExpires( ).toString( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.key ).add( USERID, user.getUserId( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.cert ).add( USERID, user.getUserId( ) ).url( ) );
    // Now the info fields
    for ( Map.Entry<String, String> entry : user.getInfo( ).entrySet( ) ) {
      result.addExtraFieldDesc( new SearchResultFieldDesc( entry.getKey( ), entry.getKey( ), false, "0px", TableDisplay.NONE, Type.KEYVAL, true, false ) );
      result.addField( entry.getValue( ) );
    }
    result.addExtraFieldDesc( new SearchResultFieldDesc( "", "Type new info here", false, "0px", TableDisplay.NONE, Type.NEWKEYVAL, true, false ) );
    result.addField( "" );
    return result;
  }
  
  private static boolean policyMatchQuery( final Policy policy, SearchQuery query ) throws Exception {
    return query.match( NAME, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return policy.getName( ) != null && policy.getName( ).contains( value.getValue( ) );
      }
    } ) && query.match( ID, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return policy.getPolicyId( ) != null && policy.getPolicyId( ).equals( value.getValue( ) );
      }
    } ) && query.match( VERSION, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return policy.getVersion( ) != null && policy.getVersion( ).contains( value.getValue( ) );
      }
    } ) && query.match( TEXT, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return policy.getText( ) != null && policy.getText( ).contains( value.getValue( ) );
      }
    } );
  }
  
  public static List<SearchResultRow> searchPolicies( SearchQuery query ) throws EucalyptusServiceException {
    if ( ( query.has( USER ) || query.has( USERID ) ) && ( query.has( GROUP ) && query.has( GROUPID ) ) ) {
      throw new EucalyptusServiceException( "Invalid policy search: can not have both user and group conditions." );
    }
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      if ( query.hasOnlySingle( USERID ) ) {
        // Optimization for a single user's policies
        User user = Accounts.lookupUserById( query.getSingle( USERID ).getValue( ) );
        Account account = user.getAccount( );
        for ( Policy policy : user.getPolicies( ) ) {
          results.add( serializePolicy( policy, account, null, user ) );
        }
      } else if ( query.hasOnlySingle( GROUPID ) ) {
        // Optimization for a single group's policies
        Group group = Accounts.lookupGroupById( query.getSingle( GROUPID ).getValue( ) );
        Account account = group.getAccount( );
        for ( Policy policy : group.getPolicies( ) ) {
          results.add( serializePolicy( policy, account, group, null ) );
        }
      } else if ( query.hasOnlySingle( ACCOUNTID ) ) {
        // Optimization for a single account's policies
        Account account = Accounts.lookupAccountById( query.getSingle( ACCOUNTID ).getValue( ) );
        User admin = account.lookupUserByName( User.ACCOUNT_ADMIN );
        for ( Policy policy : admin.getPolicies( ) ) {
          results.add( serializePolicy( policy, account, null, null ) );
        }        
      } else {
        for ( Account account : getAccounts( query ) ) {
          if ( query.has( USER ) || query.has( USERID ) ) {
            for ( User user : getUsers( account, query ) ) {
              if ( user.isAccountAdmin( ) ) continue;
              for ( Policy policy : user.getPolicies( ) ) {
                if ( policyMatchQuery( policy, query ) ) {
                  results.add( serializePolicy( policy, account, null, user ) );
                }
              }
            }
          } else if ( query.has( GROUP ) || query.has( GROUPID ) ) {
            for ( Group group : getGroups( account, query ) ) {
              for ( Policy policy : group.getPolicies( ) ) {
                if ( policyMatchQuery( policy, query ) ) {
                  results.add( serializePolicy( policy, account, group, null ) );
                }
              }
            }          
          } else {
            User admin = account.lookupUserByName( User.ACCOUNT_ADMIN );
            for ( Policy policy : admin.getPolicies( ) ) {
              if ( policyMatchQuery( policy, query ) ) {
                results.add( serializePolicy( policy, account, null, null ) );
              }
            }          
          }
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get policies", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get policies for query: " + query );      
    }    
    return results;
  }

  private static SearchResultRow serializePolicy( Policy policy, Account account, Group group, User user ) throws Exception {
    SearchResultRow result = new SearchResultRow( );
    result.addField( policy.getPolicyId( ) );
    result.addField( policy.getName( ) );
    result.addField( policy.getVersion( ) );
    result.addField( account != null ? account.getName( ) : "" );
    result.addField( group != null ? group.getName( ) : "" );
    result.addField( user != null ? user.getName( ) : "" );
    if ( user != null ) {
      result.addField( QueryBuilder.get( ).start( QueryType.user ).add( ID, user.getUserId( ) ).url( ) );
    } else if ( group != null ) {
      result.addField( QueryBuilder.get( ).start( QueryType.group ).add( ID, group.getGroupId( ) ).url( ) );
    } else {
      result.addField( QueryBuilder.get( ).start( QueryType.account ).add( ID, account.getAccountNumber( ) ).url( ) );
    }
    result.addField( policy.getText( ) );
    return result;
  }

  private static boolean certMatchQuery( final Certificate cert, SearchQuery query ) {
    return query.match( ID, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return cert.getCertificateId( ) != null && cert.getCertificateId( ).equals( value.getValue( ) );
      }
    } ) && query.match( REVOKED, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        boolean val = "true".equalsIgnoreCase( value.getValue( ) );
        return cert.isRevoked( ) != null && ( cert.isRevoked( ).booleanValue( ) == val );
      }
    } ) && query.match( ACTIVE, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        boolean val = "true".equalsIgnoreCase( value.getValue( ) );
        return cert.isActive( ) != null && ( cert.isActive( ).booleanValue( ) == val );
      }
    } );
  }
  
  public static List<SearchResultRow> searchCerts( SearchQuery query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      if ( query.hasOnlySingle( USERID ) ) {
        // Optimization for a single user's certs
        User user = Accounts.lookupUserById( query.getSingle( USERID ).getValue( ) );
        Account account = user.getAccount( );
        for ( Certificate cert : user.getCertificates( ) ) {
          results.add( serializeCert( cert, account, user ) );
        }        
      } else {
        for ( Account account : getAccounts( query ) ) {
          for ( User user : account.getUsers( ) ) {
            for ( Certificate cert : user.getCertificates( ) ) {
              if ( certMatchQuery( cert, query ) ) {
                results.add( serializeCert( cert, account, user ) );
              }
            }
          }
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get certs", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get certs for query: " + query );      
    }
    return results;
  }

  private static SearchResultRow serializeCert( Certificate cert, Account account, User user ) throws Exception {
    SearchResultRow result = new SearchResultRow( );
    result.addField( cert.getCertificateId( ) );
    result.addField( cert.isActive( ).toString( ) );
    result.addField( cert.isRevoked( ).toString( ) );
    result.addField( account.getName( ) );
    result.addField( user.getName( ) );
    result.addField( cert.getCreateDate( ) == null ? "" : cert.getCreateDate( ).toString( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.user ).add( ID, user.getUserId( ) ).url( ) );
    result.addField( B64.url.decString( cert.getPem( ) ) );
    return result;
  }

  private static boolean keyMatchQuery( final AccessKey key, SearchQuery query ) {
    return query.match( ID, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return key.getAccessKey( ) != null && key.getAccessKey( ).equals( value.getValue( ) );
      }
    } ) && query.match( ACTIVE, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        boolean val = "true".equalsIgnoreCase( value.getValue( ) );
        return key.isActive( ) != null && ( key.isActive( ).booleanValue( ) == val );
      }
    } );
  }
  
  public static List<SearchResultRow> searchKeys( SearchQuery query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      if ( query.hasOnlySingle( USERID ) ) {
        // Optimization for a single user's keys
        User user = Accounts.lookupUserById( query.getSingle( USERID ).getValue( ) );
        Account account = user.getAccount( );
        for ( AccessKey key : user.getKeys( ) ) {
          results.add( serializeKey( key, account, user ) );
        }
      } else {
        for ( Account account : getAccounts( query ) ) {
          for ( User user : account.getUsers( ) ) {
            for ( AccessKey key : user.getKeys( ) ) {
              if ( keyMatchQuery( key, query ) ) {
                results.add( serializeKey( key, account, user ) );
              }
            }
          }
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get keys", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get keys for query " + query + ": " + e.getMessage( ) );      
    }    
    return results;    
  }

  private static SearchResultRow serializeKey( AccessKey key, Account account, User user ) throws Exception {
    SearchResultRow result = new SearchResultRow( );
    result.addField( key.getAccessKey( ) );
    result.addField( key.isActive( ).toString( ) );
    result.addField( account.getName( ) );
    result.addField( user.getName( ) );
    result.addField( key.getCreateDate( ) == null ? "" : key.getCreateDate( ).toString( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.user ).add( ID, user.getUserId( ) ).url( ) );
    return result;
  }

  public static String createAccount( String accountName ) throws EucalyptusServiceException {
    try {
      Account account = Accounts.addAccount( accountName );
      return account.getAccountNumber( );
    } catch ( Exception e ) {
      throw new EucalyptusServiceException( "Failed to create account " + accountName + ": " + e.getMessage( ) );
    }
  }

  public static void deleteAccounts( ArrayList<String> ids ) throws EucalyptusServiceException {
    boolean hasError = false;
    for ( String id : ids ) {
      try { 
        Account account = Accounts.lookupAccountById( id );
        Accounts.deleteAccount( account.getName( ), false, true );
      } catch ( Exception e ) {
        LOG.error( "Failed to delete account " + id, e );
        LOG.debug( e, e );
        hasError = true;
      }
    }
    if ( hasError ) {
      throw new EucalyptusServiceException( "Failed to delete some accounts" );
    }
  }

}
