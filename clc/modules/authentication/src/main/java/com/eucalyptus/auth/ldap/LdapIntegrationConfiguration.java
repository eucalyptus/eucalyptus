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
  private String userAuthMethod;
  private String authPrincipal;
  private String authCredentials;
  private boolean useSsl;
  private boolean ignoreSslCertValidation = false;
  private String krb5Conf;
  
  // Sync configuration
  private boolean enableSync;
  private boolean autoSync;
  private long syncInterval;
  private boolean cleanDeletion;
  
  private boolean hasAccountingGroups;
  
  // Accounting groups
  private String accountingGroupBaseDn;
  private String accountingGroupIdAttribute;
  private String groupsAttribute;
  private Selection accountingGroupsSelection = new Selection( );
  
  // Or group partitions
  private Map<String, Set<String>> groupsPartition = Maps.newHashMap( );
  
  // Selected groups
  private String groupBaseDn;
  private String groupIdAttribute;
  private String usersAttribute;
  private Selection groupsSelection = new Selection( );
  
  // Selected users
  private String userBaseDn;
  private String userIdAttribute;
  private Map<String, String> userInfoAttributes = Maps.newHashMap( );
  private Selection usersSelection = new Selection( );
  
  public LdapIntegrationConfiguration( ) {
  }

  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "---Parsed LIC---\n" );
    sb.append( "ldap-service:\n" );
    sb.append( '\t' ).append( "server-url:" ).append( this.serverUrl ).append( '\n' );
    sb.append( '\t' ).append( "auth-method:" ).append( this.authMethod ).append( '\n' );
    sb.append( '\t' ).append( "user-auth-method:" ).append( this.userAuthMethod ).append( '\n' );
    sb.append( '\t' ).append( "auth-principal:" ).append( this.authPrincipal ).append( '\n' );
    sb.append( '\t' ).append( "auth-credentials:" ).append( this.authCredentials ).append( '\n' );
    sb.append( '\t' ).append( "use-ssl:" ).append( this.useSsl ).append( '\n' );
    sb.append( '\t' ).append( "ignore-ssl-cert-validation:" ).append( this.ignoreSslCertValidation ).append( '\n' );
    sb.append( '\t' ).append( "krb5-conf:" ).append( this.krb5Conf ).append( '\n' );
    sb.append( "sync:\n" );
    sb.append( '\t' ).append( "enable:" ).append( this.enableSync ).append( '\n' );
    sb.append( '\t' ).append( "auto:" ).append( this.autoSync ).append( '\n' );
    sb.append( '\t' ).append( "interval:" ).append( this.syncInterval ).append( '\n' );
    sb.append( '\t' ).append( "clean-deletion:" ).append( this.cleanDeletion ).append( '\n' );
    if ( this.hasAccountingGroups ) {
      sb.append( "accounting-groups:\n" );
      sb.append( '\t' ).append( "base-dn:" ).append( this.accountingGroupBaseDn ).append( '\n' );
      sb.append( '\t' ).append( "id-attribute:" ).append( this.accountingGroupIdAttribute ).append( '\n' );
      sb.append( '\t' ).append( "member-attribute:" ).append( this.groupsAttribute ).append( '\n' );
      sb.append( '\t' ).append( "selection:" ).append( this.accountingGroupsSelection ).append( '\n' );
    } else {
      sb.append( "groups-partition:\n" );
      sb.append( '\t' ).append( this.groupsPartition ).append( '\n' );
    }
    sb.append( "groups:\n" );
    sb.append( '\t' ).append( "base-dn:" ).append( this.groupBaseDn ).append( '\n' );
    sb.append( '\t' ).append( "id-attribute:" ).append( this.groupIdAttribute ).append( '\n' );
    sb.append( '\t' ).append( "member-attribute:" ).append( this.usersAttribute ).append( '\n' );
    sb.append( '\t' ).append( "selection:" ).append( this.groupsSelection ).append( '\n' );
    sb.append( "users:\n" );
    sb.append( '\t' ).append( "base-dn:" ).append( this.userBaseDn ).append( '\n' );
    sb.append( '\t' ).append( "id-attribute:" ).append( this.userIdAttribute ).append( '\n' );
    sb.append( '\t' ).append( "user-info-attributes:" ).append( this.userInfoAttributes ).append( '\n' );
    sb.append( '\t' ).append( "selection:" ).append( this.usersSelection ).append( '\n' );
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

  public void setGroupsPartition( Map<String, Set<String>> groupsPartition ) {
    this.groupsPartition = groupsPartition;
  }

  public Map<String, Set<String>> getGroupsPartition( ) {
    return groupsPartition;
  }

  public void setUserInfoAttributes( Map<String, String> userInfoAttributes ) {
    this.userInfoAttributes = userInfoAttributes;
  }

  public Map<String, String> getUserInfoAttributes( ) {
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

  public void setAccountingGroupsSelection( Selection accountingGroupsSelection ) {
    this.accountingGroupsSelection = accountingGroupsSelection;
  }

  public Selection getAccountingGroupsSelection( ) {
    return accountingGroupsSelection;
  }

  public void setGroupsSelection( Selection groupsSelection ) {
    this.groupsSelection = groupsSelection;
  }

  public Selection getGroupsSelection( ) {
    return groupsSelection;
  }

  public void setUsersSelection( Selection usersSeletion ) {
    this.usersSelection = usersSeletion;
  }

  public Selection getUsersSelection( ) {
    return usersSelection;
  }

  public void setIgnoreSslCertValidation( boolean ignoreSslCertValidation ) {
    this.ignoreSslCertValidation = ignoreSslCertValidation;
  }

  public boolean isIgnoreSslCertValidation( ) {
    return ignoreSslCertValidation;
  }

  public void setKrb5Conf( String krb5Conf ) {
    this.krb5Conf = krb5Conf;
  }

  public String getKrb5Conf( ) {
    return krb5Conf;
  }

  public void setUserAuthMethod( String userAuthMethod ) {
    this.userAuthMethod = userAuthMethod;
  }

  public String getUserAuthMethod( ) {
    return userAuthMethod;
  }
  
  public String getRealUserAuthMethod( ) {
    return ( userAuthMethod != null ? userAuthMethod : authMethod );
  }

  public void setCleanDeletion( boolean cleanDeletion ) {
    this.cleanDeletion = cleanDeletion;
  }

  public boolean isCleanDeletion( ) {
    return cleanDeletion;
  }

}
