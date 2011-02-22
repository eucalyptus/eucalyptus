package com.eucalyptus.auth.ldap;

import java.util.Map;
import java.util.Set;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * An in-memory cache of the parsed ldap integration configuration.
 * 
 * @author wenye
 *
 */
public class LdapIntegrationConfiguration {
  
  // LDAP service configuration
  private String serverUrl;
  private String authMethod;
  private String authPrincipal;
  private String authCredentials;
  private boolean useSsl;
  
  // Sync configuration
  private boolean enableSync;
  private boolean autoSync;
  private long syncInterval;
  
  private boolean hasAccountingGroups;
  
  // Accounting groups
  private String accountingGroupBaseDn;
  private SetFilter accountingGroups = new SetFilter( );
  private String accountingGroupIdAttribute;
  private String groupsAttribute;
  
  // Or group partitions
  private Map<String, Set<String>> groupsPartition = Maps.newHashMap( );
  
  // Selected groups
  private String groupBaseDn;
  private SetFilter groups = new SetFilter( );
  private String groupIdAttribute;
  private String usersAttribute;
  
  // Selected users
  private String userBaseDn;
  private SetFilter users = new SetFilter( );
  private String userIdAttribute;
  private String passwordAttribute;
  private Set<String> userInfoAttributes = Sets.newHashSet( );
  
  public LdapIntegrationConfiguration( ) {
  }

  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "---Parsed LIC---\n" );
    sb.append( "ldap-service:\n" );
    sb.append( '\t' ).append( "server-url:" ).append( this.serverUrl ).append( '\n' );
    sb.append( '\t' ).append( "auth-method:" ).append( this.authMethod ).append( '\n' );
    sb.append( '\t' ).append( "auth-principal:" ).append( this.authPrincipal ).append( '\n' );
    sb.append( '\t' ).append( "auth-credentials:" ).append( this.authCredentials ).append( '\n' );
    sb.append( '\t' ).append( "use-ssl:" ).append( this.useSsl ).append( '\n' );
    sb.append( "sync:\n" );
    sb.append( '\t' ).append( "enable:" ).append( this.enableSync ).append( '\n' );
    sb.append( '\t' ).append( "auto:" ).append( this.autoSync ).append( '\n' );
    sb.append( '\t' ).append( "interval:" ).append( this.syncInterval ).append( '\n' );
    if ( this.hasAccountingGroups ) {
      sb.append( "accounting-groups:\n" );
      sb.append( '\t' ).append( "base-dn:" ).append( this.accountingGroupBaseDn ).append( '\n' );
      sb.append( '\t' ).append( "id-attribute:" ).append( this.accountingGroupIdAttribute ).append( '\n' );
      sb.append( '\t' ).append( "member-attribute:" ).append( this.groupsAttribute ).append( '\n' );
      sb.append( '\t' ).append( "select:" ).append( this.accountingGroups ).append( '\n' );
    } else {
      sb.append( "groups-partition:\n" );
      sb.append( '\t' ).append( this.groupsPartition ).append( '\n' );
    }
    sb.append( "groups:\n" );
    sb.append( '\t' ).append( "base-dn:" ).append( this.groupBaseDn ).append( '\n' );
    sb.append( '\t' ).append( "id-attribute:" ).append( this.groupIdAttribute ).append( '\n' );
    sb.append( '\t' ).append( "member-attribute:" ).append( this.usersAttribute ).append( '\n' );
    sb.append( '\t' ).append( "select:" ).append( this.groups ).append( '\n' );
    sb.append( "users:\n" );
    sb.append( '\t' ).append( "base-dn:" ).append( this.userBaseDn ).append( '\n' );
    sb.append( '\t' ).append( "id-attribute:" ).append( this.userIdAttribute ).append( '\n' );
    sb.append( '\t' ).append( "password-attribute:" ).append( this.passwordAttribute ).append( '\n' );
    sb.append( '\t' ).append( "user-info-attributes:" ).append( this.userInfoAttributes ).append( '\n' );
    sb.append( '\t' ).append( "select:" ).append( this.users ).append( '\n' );
    return sb.toString( );
  }
  
  public void setServerUrl( String serverUrl ) {
    this.serverUrl = serverUrl;
  }

  public String getServerUrl( ) {
    return serverUrl;
  }

  public void setAuthMethod( String authMethod ) {
    this.authMethod = authMethod;
  }

  public String getAuthMethod( ) {
    return authMethod;
  }

  public void setAuthPrincipal( String authPrincipal ) {
    this.authPrincipal = authPrincipal;
  }

  public String getAuthPrincipal( ) {
    return authPrincipal;
  }

  public void setUserBaseDn( String userBaseDn ) {
    this.userBaseDn = userBaseDn;
  }

  public String getUserBaseDn( ) {
    return userBaseDn;
  }

  public void setGroupBaseDn( String groupBaseDn ) {
    this.groupBaseDn = groupBaseDn;
  }

  public String getGroupBaseDn( ) {
    return groupBaseDn;
  }

  public void setHasAccountingGroups( boolean hasAccountingGroups ) {
    this.hasAccountingGroups = hasAccountingGroups;
  }

  public boolean hasAccountingGroups( ) {
    return hasAccountingGroups;
  }

  public void setGroupsAttribute( String groupsAttribute ) {
    this.groupsAttribute = groupsAttribute;
  }

  public String getGroupsAttribute( ) {
    return groupsAttribute;
  }

  public void setPasswordAttribute( String passwordAttribute ) {
    this.passwordAttribute = passwordAttribute;
  }

  public String getPasswordAttribute( ) {
    return passwordAttribute;
  }

  public void setEnableSync( boolean enableSync ) {
    this.enableSync = enableSync;
  }

  public boolean isSyncEnabled( ) {
    return enableSync;
  }

  public void setAutoSync( boolean autoSync ) {
    this.autoSync = autoSync;
  }

  public boolean isAutoSync( ) {
    return autoSync;
  }

  public void setSyncInterval( long syncInterval ) {
    this.syncInterval = syncInterval;
  }

  public long getSyncInterval( ) {
    return syncInterval;
  }

  public void setAccountingGroups( SetFilter accountingGroups ) {
    this.accountingGroups = accountingGroups;
  }

  public SetFilter getAccountingGroups( ) {
    return accountingGroups;
  }

  public void setGroups( SetFilter groups ) {
    this.groups = groups;
  }

  public SetFilter getGroups( ) {
    return groups;
  }

  public void setGroupsPartition( Map<String, Set<String>> groupsPartition ) {
    this.groupsPartition = groupsPartition;
  }

  public Map<String, Set<String>> getGroupsPartition( ) {
    return groupsPartition;
  }

  public void setUsers( SetFilter users ) {
    this.users = users;
  }

  public SetFilter getUsers( ) {
    return users;
  }

  public void setUserInfoAttributes( Set<String> userInfoAttributes ) {
    this.userInfoAttributes = userInfoAttributes;
  }

  public Set<String> getUserInfoAttributes( ) {
    return userInfoAttributes;
  }

  public void setUsersAttribute( String usersAttribute ) {
    this.usersAttribute = usersAttribute;
  }

  public String getUsersAttribute( ) {
    return usersAttribute;
  }

  public void setAccountingGroupIdAttribute( String accountingGroupIdAttribute ) {
    this.accountingGroupIdAttribute = accountingGroupIdAttribute;
  }

  public String getAccountingGroupIdAttribute( ) {
    return accountingGroupIdAttribute;
  }

  public void setGroupIdAttribute( String groupIdAttribute ) {
    this.groupIdAttribute = groupIdAttribute;
  }

  public String getGroupIdAttribute( ) {
    return groupIdAttribute;
  }

  public void setUserIdAttribute( String userIdAttribute ) {
    this.userIdAttribute = userIdAttribute;
  }

  public String getUserIdAttribute( ) {
    return userIdAttribute;
  }

  public void setAuthCredentials( String authCredentials ) {
    this.authCredentials = authCredentials;
  }

  public String getAuthCredentials( ) {
    return authCredentials;
  }

  public void setUseSsl( boolean useSsl ) {
    this.useSsl = useSsl;
  }

  public boolean isUseSsl( ) {
    return useSsl;
  }

  public void setAccountingGroupBaseDn( String accountingGroupBaseDn ) {
    this.accountingGroupBaseDn = accountingGroupBaseDn;
  }

  public String getAccountingGroupBaseDn( ) {
    return accountingGroupBaseDn;
  }

}
