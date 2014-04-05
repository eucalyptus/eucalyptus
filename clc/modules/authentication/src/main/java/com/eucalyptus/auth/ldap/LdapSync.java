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

import java.util.Map;
import java.util.Set;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.checker.ValueCheckerFactory;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.SystemClock;
import com.eucalyptus.system.Threads;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Logic to perform LDAP sync.
 */
public class LdapSync {
  
  private interface LdapEntryProcessor {
    
    public void processLdapEntry( String dn, Attributes attrs ) throws NamingException;
    
  }
  
  private static final Logger LOG = Logger.getLogger( LdapSync.class );
  
  private static final boolean VERBOSE = true;
  
  private static final String LDAP_SYNC_THREAD = "LDAP sync";

  private static final LdapIntegrationConfiguration DEFAULT_LIC = new LdapIntegrationConfiguration( );
  
  private static LdapIntegrationConfiguration lic = DEFAULT_LIC;
  private static boolean inSync = false;
  private static long timeTillNextSync;
  
  private static final ClockTickListener TIMER_LISTENER = new ClockTickListener( );
  
  private static class ClockTickListener implements EventListener<Event> {

    @Override
    public void fireEvent( Event event ) {
      if ( Bootstrap.isOperational( ) && Hosts.isCoordinator( ) && event instanceof ClockTick ) {
        periodicSync( );
      }
    }
    
  }
  
  public static synchronized boolean inSync( ) {
    return inSync;
  }
  
  public static synchronized void start( ) {
    if ( lic.isSyncEnabled( ) ) {
      if ( lic.isAutoSync( ) ) {
        ListenerRegistry.getInstance( ).register( ClockTick.class, TIMER_LISTENER );
      }
      startSync( );
    }
  }
  
  public static synchronized LdapIntegrationConfiguration getLic( ) {
    return lic;
  }
  
  public static synchronized void setLic( LdapIntegrationConfiguration config ) {
    LOG.debug( "A new LIC is being set: " + config );
    lic = config;
    if ( Bootstrap.isFinished( ) ) {
      if ( lic.isSyncEnabled( ) ) {
        if ( lic.isAutoSync( ) ) {
          ListenerRegistry.getInstance( ).register( ClockTick.class, TIMER_LISTENER );
        }
        startSync( );
      } else {
        ListenerRegistry.getInstance( ).deregister( ClockTick.class, TIMER_LISTENER );
      }
    }
  }
  
  public static synchronized void forceSync( ) {
    if ( lic.isSyncEnabled( ) ) {
      startSync( );
    }
  }
  
  public static synchronized boolean check( ) {
    if ( !lic.isSyncEnabled( ) ) {
      return true;
    }
    LdapClient ldap = null;
    try {
      ldap = LdapClient.authenticateClient( lic );
      return true;
    } catch ( LdapException e ) {
      LOG.error( e, e );
      LOG.warn( "Failed to connect to LDAP service", e );
      return false;
    } finally {
      if ( ldap != null ) {
        ldap.close( );
      }
    }
  }
  
  /**
   * Authenticate an LDAP user by his login and password.
   * 
   * @param login
   * @param password
   * @return
   * @throws LdapException
   */
  public static synchronized void authenticate( User user, String password ) throws LdapException {
    if ( !lic.isSyncEnabled( ) ) {
      throw new LdapException( "LDAP sync is not enabled" );
    }
    LdapClient ldap = null;
    try {
      // Get proper user LDAP principal based on authentication method
      String login = null;
      if ( LicParser.LDAP_AUTH_METHOD_SIMPLE.equals( lic.getRealUserAuthMethod( ) ) ) {
        // Simple requires full user DN
        login = user.getInfo( User.DN );
      } else {
        // SASL requires a different ID:
        // Pick the specified ID first. If not exist, pick the user name.
        // TODO(wenye): possible other formats of user ID, like "u:..." or "dn:..."
        login = user.getInfo( User.SASLID );
        if ( Strings.isNullOrEmpty( login ) ) {
          login = user.getName( );
        }
      }
      if ( Strings.isNullOrEmpty( login ) ) {
        throw new LdapException( "Invalid login user" );
      }
      ldap = LdapClient.authenticateUser( lic, login, password );
    } catch ( AuthException e ) {
      LOG.error( e, e );
      LOG.debug( "Failed to get auth information for user " + user );
      throw new LdapException( "Failed to get auth information", e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
      LOG.debug( "Failed to connect to LDAP service", e );
      throw e;
    } finally {
      if ( ldap != null ) {
        ldap.close( );
      }
    }
  }
  
  /**
   * @return true if LDAP sync is enabled.
   */
  public static synchronized boolean enabled( ) {
    return lic.isSyncEnabled( );
  }
  
  private static synchronized boolean getAndSetSync( boolean newValue ) {
    boolean old = inSync;
    inSync = newValue;
    return old;
  }
  
  private static synchronized void periodicSync( ) {
    if ( lic.isSyncEnabled( ) && lic.isAutoSync( ) ) {
      timeTillNextSync -= SystemClock.getRate( );
      if ( timeTillNextSync <= 0 ) {
        startSync( );
      }
    } 
  }
  
  private static void startSync( ) {
    LOG.debug( "A new sync initiated." );
    
    timeTillNextSync = lic.getSyncInterval( );
    if ( !getAndSetSync( true ) ) {
      
      Threads.newThread( new Runnable( ) {
        
        @Override
        public void run( ) {
          LOG.debug( "Sync started" );
          sync( lic );
          getAndSetSync( false );
          LOG.debug( "Sync ended" );
        }
        
      }, LDAP_SYNC_THREAD ).start( );
    }
  }
  
  public static void sync( final LdapIntegrationConfiguration lic ) {
    // Get users/groups from LDAP
    Map<String, Set<String>> accountingGroups = Maps.newHashMap( );
    Map<String, String> groupDnToId = Maps.newHashMap( );
    Map<String, Set<String>> groups = Maps.newHashMap( );
    Map<String, String> userDnToId = Maps.newHashMap( );
    Map<String, Map<String, String>> users = Maps.newHashMap( );
    LdapClient ldap = null;
    try {
      ldap = LdapClient.authenticateClient( lic );
      
      loadLdapUsers( ldap, lic, userDnToId, users );
      loadLdapGroups( ldap, lic, userDnToId, groupDnToId, groups );
      if ( lic.hasAccountingGroups( ) ) {
        loadLdapAccountingGroups( ldap, lic, groupDnToId, accountingGroups );
      } else {
        accountingGroups = lic.getGroupsPartition( );
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
      LOG.error( "Failed to sync with LDAP", e );
      return;
    } finally {
      if ( ldap != null ) {
        ldap.close( );
      }
    }
    if ( VERBOSE ) {
      LOG.debug( "Sync remote accounts: " + accountingGroups );
      LOG.debug( "Sync remote groups: " + groups );
      LOG.debug( "Sync remote users: " + users );
    }
    
    checkConflictingIdentities( accountingGroups, groups, users );
    rebuildLocalAuthDatabase( lic, accountingGroups, groups, users );
  }
  
  private static void checkConflictingIdentities( Map<String, Set<String>> accountingGroups, Map<String, Set<String>> groups, Map<String, Map<String, String>> users ) {
    if ( accountingGroups.containsKey( Account.SYSTEM_ACCOUNT ) ) {
      LOG.error( "Account " + Account.SYSTEM_ACCOUNT + " is reserved for Eucalyptus only. Sync will skip this account from LDAP." );
      accountingGroups.remove( Account.SYSTEM_ACCOUNT );
    }
    if ( users.containsKey( User.ACCOUNT_ADMIN ) ) {
      LOG.error( "User " + User.ACCOUNT_ADMIN + " is reserved for Eucalyptus only. Sync will skip this user from LDAP." );
      users.remove( User.ACCOUNT_ADMIN );
    }
    for ( String group : groups.keySet( ) ) {
      if ( group.startsWith( User.USER_GROUP_PREFIX ) ) {
        LOG.error( "Group name starting with " + User.USER_GROUP_PREFIX + " is reserved for Eucalyptus only. Sync will skip this group " + group );
        groups.remove( group );
      }
    }
  }

  private static void rebuildLocalAuthDatabase( LdapIntegrationConfiguration lic, Map<String, Set<String>> accountingGroups,
                                                Map<String, Set<String>> groups, Map<String, Map<String, String>> users ) {
    try {
      Set<String> oldAccountSet = getLocalAccountSet( );      
      for ( Map.Entry<String, Set<String>> entry : accountingGroups.entrySet( ) ) {
        String accountName = entry.getKey( );
        Set<String> accountMembers = entry.getValue( );
        if ( oldAccountSet.contains( accountName ) ) {
          // Remove common elements from old account set
          oldAccountSet.remove( accountName );
          updateAccount( lic, accountName, accountMembers, groups, users );
        } else {
          addNewAccount( accountName, accountMembers, groups, users );
        }
      }
      if ( lic.isCleanDeletion( ) ) {
        // Remaining accounts are obsolete
        removeObsoleteAccounts( oldAccountSet );
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
      LOG.error( "Error in rebuilding local auth database", e );
    }
  }

  private static void addNewAccount( String accountName, Set<String> accountMembers, Map<String, Set<String>> groups, Map<String, Map<String, String>> users ) {
    LOG.debug( "Adding new account " + accountName );
    try {
      Account account = Accounts.addAccount( accountName );
      account.addUser( User.ACCOUNT_ADMIN, "/", true, null );
      for ( String user : getAccountUserSet( accountMembers, groups ) ) {
        try {
          LOG.debug( "Adding new user " + user );
          Map<String, String> info = users.get( user );
          if ( info == null ) {
            LOG.warn( "Empty user info for user " + user );
          }
          account.addUser( user, "/", true/* enabled */, info );
        } catch ( AuthException e ) {
          LOG.error( e, e );
          LOG.warn( "Failed add new user " + user, e );
        }
      }
      for ( String group : accountMembers ) {
        Group dbGroup = null;
        try {
          LOG.debug( "Adding new group " + group );
          dbGroup = account.addGroup( group, "/" );
          Set<String> groupUsers = groups.get( group );
          if ( groupUsers == null ) {
            LOG.error( "Empty user set for group " + group );
          } else {
            for ( String user : groupUsers ) {
              LOG.debug( "Adding " + user + " to group " + group );
              dbGroup.addUserByName( user );
            }
          }
        } catch ( AuthException e ) {
          LOG.error( e, e );
          LOG.warn( "Failed to add new group " + group + " in " + accountName, e );
        }
      }
    } catch ( AuthException e ) {
      LOG.error( e, e );
      LOG.error( "Failed to add new account " + accountName, e );
    }
  }
  
  private static Set<String> getAccountUserSet( Set<String> members, Map<String, Set<String>> groups ) {
    Set<String> userSet = Sets.newHashSet( );
    for ( String member : members ) {
      Set<String> groupMembers = groups.get( member );
      if ( groupMembers == null ) {
        LOG.error( "Empty user set of group " + member );
      } else {
        userSet.addAll( groupMembers );
      }
    }
    return userSet;
  }
  
  private static void updateAccount( LdapIntegrationConfiguration lic, String accountName, Set<String> accountMembers, Map<String, Set<String>> groups, Map<String, Map<String, String>> users ) {
    LOG.debug( "Updating account " + accountName );
    Account account = null;
    try {
      account = Accounts.lookupAccountByName( accountName );
      // Update users first
      Set<String> newUserSet = getAccountUserSet( accountMembers, groups );
      Set<String> oldUserSet = getLocalUserSet( account );
      for ( String user : newUserSet ) {
        if ( oldUserSet.contains( user ) ) {
          oldUserSet.remove( user );
          try {
            updateUser( account, user, users.get( user ) );
          } catch ( AuthException e ) {
            LOG.error( e, e );
            LOG.warn( "Failed to update user " + user + " in " + accountName, e );
          }
        } else {
          try {
            addNewUser( account, user, users.get( user ) );
          } catch ( AuthException e ) {
            LOG.error( e, e );
            LOG.warn( "Failed to add new user " + user + " in " + accountName, e );
          }
        }
      }
      if ( lic.isCleanDeletion( ) ) {
        removeObsoleteUsers( account, oldUserSet );
      }
      // Now update groups
      Set<String> oldGroupSet = getLocalGroupSet( account );
      for ( String group : accountMembers ) {
        if ( oldGroupSet.contains( group ) ) {
          oldGroupSet.remove( group );
          updateGroup( account, group, groups.get( group ) );
        } else {
          addNewGroup( account, group, groups.get( group ) );
        }
      }
      if ( lic.isCleanDeletion( ) ) {
        removeObsoleteGroups( account, oldGroupSet );
      }
    } catch ( AuthException e ) {
      LOG.error( e, e );
      LOG.error( "Failed to update account " + accountName, e );
    }
  }
  
  private static void removeObsoleteGroups( Account account, Set<String> oldGroupSet ) {
    for ( String group : oldGroupSet ) {
      try {
        account.deleteGroup( group, true/* recursive */ );
      } catch ( AuthException e ) {
        LOG.error( e, e );
        LOG.warn( "Failed to delete group " + group + " in " + account.getName( ), e );
      }
    }
  }

  private static void addNewGroup( Account account, String group, Set<String> users ) {
    LOG.debug( "Adding new group " + group + " in account " + account.getName( ) );
    if ( users == null ) {
      LOG.error( "Empty new user set of group " + group );
      return;
    }
    try {
      Group g = account.addGroup( group, "/" );
      for ( String user : users ) {
        LOG.debug( "Adding " + user + " to " + group );
        g.addUserByName( user );
      }
    } catch ( AuthException e ) {
      LOG.error( e, e );
      LOG.warn( "Failed to add new group " + group + " in " + account.getName( ), e );
    }
  }

  private static void updateGroup( Account account, String group, Set<String> users ) {
    LOG.debug( "Updating group " + group + " in account " + account.getName( ) );
    if ( users == null ) {
      LOG.error( "Empty new user set of group " + group );
      return;
    }
    try {
      // Get local user set of the group
      Set<String> localUserSet = Sets.newHashSet( );
      Group g = account.lookupGroupByName( group );
      for ( User u : g.getUsers( ) ) {
        localUserSet.add( u.getName( ) );
      }
      // Update group by adding new users and remove obsolete users
      for ( String user : users ) {
        if ( localUserSet.contains( user ) ) {
          localUserSet.remove( user );
        } else {
          LOG.debug( "Adding " + user + " to " + g.getName( ) );
          g.addUserByName( user );
        }
      }
      for ( String user : localUserSet ) {
        LOG.debug( "Removing " + user + " from " + g.getName( ) );
        g.removeUserByName( user );
      }
    } catch ( AuthException e ) {
      LOG.error( e, e );
      LOG.warn( "Failed to update group " + group + " in " + account.getName( ), e );
    }
  }

  private static Set<String> getLocalGroupSet( Account account ) throws AuthException {
    Set<String> groupSet = Sets.newHashSet( );
    for ( Group group : account.getGroups( ) ) {
      groupSet.add( group.getName( ) );
    }
    return groupSet;
  }

  private static void removeObsoleteUsers( Account account, Set<String> oldUserSet ) {
    // We don't want to remove account admin when updating an account
    oldUserSet.remove( User.ACCOUNT_ADMIN );
    
    LOG.debug( "Removing obsolete users: " + oldUserSet + ", in account " + account.getName( ) );
    for ( String user : oldUserSet ) {
      try {
        account.deleteUser( user, true/* forceDeleteAdmin */, true /* recursive */ );
      } catch ( AuthException e ) {
        LOG.error( e, e );
        LOG.warn( "Failed to delete user " + user + " in " + account.getName( ) );
      }
    }
  }

  private static void addNewUser( Account account, String user, Map<String, String> info ) throws AuthException {
    LOG.debug( "Adding new user " + user + " in account " + account.getName( ) );
    if ( info == null ) {
      LOG.warn( "Empty user info for user " + user );
    }
    account.addUser( user, "/", true/* enabled */, info );
  }

  private static void updateUser( Account account, String user, Map<String, String> map ) throws AuthException {
    LOG.debug( "Updating user " + user + " in account " + account.getName( ) );
    if ( map == null ) {
      LOG.error( "Empty info map of user " + user );
    } else {
      account.lookupUserByName( user ).setInfo( map );
    }
  }

  private static Set<String> getLocalUserSet( Account account ) throws AuthException {
    Set<String> userSet = Sets.newHashSet( );
    for ( User user : account.getUsers( ) ) {
      userSet.add( user.getName( ) );
    }
    return userSet;
  }
  
  private static void removeObsoleteAccounts( Set<String> oldAccountSet ) {
    // We don't want to remove system account
    oldAccountSet.remove( Account.SYSTEM_ACCOUNT );

    LOG.debug( "Removing obsolete accounts: " + oldAccountSet );
    for ( final String account : oldAccountSet ) {
      try {
        Accounts.deleteAccount( account, false /* forceDeleteSystem */, true /* recursive */ );
      } catch ( final AuthException e ) {
        if ( !AuthException.DELETE_SYSTEM_ACCOUNT.equals( e.getMessage( ) ) ) {
          LOG.error( e, e );
          LOG.warn( "Failed to delete account " + account, e );
        }
      }
    }
  }
  
  private static Set<String> getLocalAccountSet( ) throws AuthException {
    Set<String> accountSet = Sets.newHashSet( );
    for ( Account account : Accounts.listAllAccounts( ) ) {
      accountSet.add( account.getName( ) );
    }
    return accountSet;
  }
  
  /**
   * Following RFC 2253
   * 
   * @param dn
   * @return the last RDN of a DN
   */
  private static String parseIdFromDn( String dn ) {
    if ( Strings.isNullOrEmpty( dn ) ) {
      return null;
    }
    try {
      LdapName ln = new LdapName( dn );
      if ( ln.size( ) > 0 ) {
        return ( String ) ln.getRdn( ln.size( ) - 1 ).getValue( );
      }
    } catch ( InvalidNameException e ) {
      LOG.error( e, e );
      LOG.warn( "Invalid DN " + dn, e );
    }
    return null;
  }
  
  /**
   * Get the ID of an LDAP/AD entity, accounting group or group or user.
   * 
   * If no id attribute name is specified, use the last RDN of the entity DN
   * Otherwise, use the specified id attribute value.
   * 
   * @param dn
   * @param idAttrName
   * @param attrs
   * @return a valid ID
   * @throws NamingException
   */
  private static String getId( String dn, String idAttrName, Attributes attrs ) throws NamingException {
    String id = null;
    // If the id-attribute is not specified, by default, use the last RDN from DN
    // Else use the value of the id-attribute
    if ( Strings.isNullOrEmpty( idAttrName ) ) {
      id = parseIdFromDn( dn );
    } else {
      id = getAttrWithNullCheck( attrs, idAttrName );
    }
    if ( Strings.isNullOrEmpty( id ) ) {
      throw new NamingException( "Empty ID for " + attrs );
    }
    return id.toLowerCase( );
  }

  private static Set<String> getMembers( String memberAttrName, Attributes attrs, final Map<String, String> dnToId ) throws NamingException {
    Set<String> members = Sets.newHashSet( );
    String memberItemType = lic.getMembersItemType();
    String memberId = null;
    String memberDn = null;
    Attribute membersAttr = attrs.get( memberAttrName );
    if ( membersAttr != null ) {
      NamingEnumeration<?> names = membersAttr.getAll( );
      while ( names.hasMore( ) ) {
        if ( "identity".equals( memberItemType ) ) {
          memberId = sanitizeUserGroupId( ( String ) names.next( ) );
        } else {
          memberDn = ( String ) names.next( );
          memberId = dnToId.get( memberDn.toLowerCase( ) );
        }
        if ( Strings.isNullOrEmpty( memberId ) ) {
          LOG.warn( "Can not map member DN " + memberDn + " to ID for " + attrs + ". Check corresponding selection section in your LIC." );
        } else {
          members.add( memberId.toLowerCase( ) );
        }
      }
    }
    return members;
  }

  private static void retrieveSelection( LdapClient ldap, String baseDn, Selection selection, String[] attrNames, LdapEntryProcessor processor ) throws LdapException {
    if ( VERBOSE ) {
      LOG.debug( "Search users by: baseDn=" + baseDn + ", attributes=" + attrNames + ", selection=" + selection );
    }
    try {
      // Search by filter first.
      NamingEnumeration<SearchResult> results = ldap.search( baseDn, selection.getSearchFilter( ), attrNames );
      while ( results.hasMore( ) ) {
        SearchResult res = results.next( );
        try {
          if ( !selection.getNotSelected( ).contains( res.getNameInNamespace( ) ) ) {
            processor.processLdapEntry( res.getNameInNamespace( ).toLowerCase( ), res.getAttributes( ) );
          }
        } catch ( NamingException e ) {
          LOG.debug( "Failed to retrieve entry " + res );
          LOG.error( e, e );
        }
      }
      // Get one-off DNs
      for ( String dn : selection.getSelected( ) ) {
        Attributes attrs = null;
        try {
          attrs = ldap.getContext( ).getAttributes( dn, attrNames );
          processor.processLdapEntry( dn.toLowerCase( ), attrs );
        } catch ( NamingException e ) {
          LOG.debug( "Failed to retrieve entry " + attrs );
          LOG.error( e, e );
        }
      }
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( e );
    }
  }
  
  private static void loadLdapAccountingGroups( LdapClient ldap, final LdapIntegrationConfiguration lic, final Map<String, String> groupDnToId, final Map<String, Set<String>> accountingGroups ) throws LdapException {
    if ( VERBOSE ) {
      LOG.debug( "Loading accounting groups from LDAP/AD" );
    }
    Set<String> attrNames = Sets.newHashSet( );
    attrNames.add( lic.getGroupsAttribute( ) );
    if ( !Strings.isNullOrEmpty( lic.getAccountingGroupIdAttribute( ) ) ) {
      attrNames.add( lic.getAccountingGroupIdAttribute( ) );
    }
    if ( VERBOSE ) {
      LOG.debug( "Attributes to load for accounting groups: " + attrNames );
    }
    retrieveSelection( ldap, lic.getAccountingGroupBaseDn( ), lic.getAccountingGroupsSelection( ), attrNames.toArray( new String[0] ), new LdapEntryProcessor( ) {

      @Override
      public void processLdapEntry( String dn, Attributes attrs ) throws NamingException {
        if ( VERBOSE ) {
          LOG.debug( "Retrieved accounting group: " + dn + " -> " + attrs );
        }
        accountingGroups.put( sanitizeAccountId( getId( dn, lic.getAccountingGroupIdAttribute( ), attrs ) ),
                              getMembers( lic.getGroupsAttribute( ), attrs, groupDnToId ) );
        
      }
      
    } );    
  }
  
  private static void loadLdapGroups( LdapClient ldap, final LdapIntegrationConfiguration lic, final Map<String, String> userDnToId, final Map<String, String> groupDnToId, final Map<String, Set<String>> groups ) throws LdapException {
    if ( VERBOSE ) {
      LOG.debug( "Loading groups from LDAP/AD" );
    }
    Set<String> attrNames = Sets.newHashSet( );
    attrNames.add( lic.getUsersAttribute( ) );
    if ( !Strings.isNullOrEmpty( lic.getGroupIdAttribute( ) ) ) {
      attrNames.add( lic.getGroupIdAttribute( ) );
    }
    if ( VERBOSE ) {
      LOG.debug( "Attributes to load for groups: " + attrNames );
    }
    retrieveSelection( ldap, lic.getGroupBaseDn( ), lic.getGroupsSelection( ), attrNames.toArray( new String[0] ), new LdapEntryProcessor( ) {

      @Override
      public void processLdapEntry( String dn, Attributes attrs ) throws NamingException {
        if ( VERBOSE ) {
          LOG.debug( "Retrieved group: " + dn + " -> " + attrs );
        }
        String id = sanitizeUserGroupId( getId( dn, lic.getGroupIdAttribute( ), attrs ) ); 
        groupDnToId.put( dn, id );
        groups.put( id, getMembers( lic.getUsersAttribute( ), attrs, userDnToId ) );
      }
      
    } );    
  }
  
  private static void loadLdapUsers( LdapClient ldap, final LdapIntegrationConfiguration lic, final Map<String, String> userDnToId, final Map<String, Map<String, String>> users ) throws LdapException {
    if ( VERBOSE ) {
      LOG.debug( "Loading users from LDAP/AD" );
    }
    // Prepare the list of attributes to retrieve
    Set<String> attrNames = Sets.newHashSet( );
    attrNames.addAll( lic.getUserInfoAttributes( ).keySet( ) );
    if ( !Strings.isNullOrEmpty( lic.getUserIdAttribute( ) ) ) {
      attrNames.add( lic.getUserIdAttribute( ) );
    }
    if ( !Strings.isNullOrEmpty( lic.getUserSaslIdAttribute( ) ) ) {
      attrNames.add( lic.getUserSaslIdAttribute( ) );
    }
    if ( VERBOSE ) {
      LOG.debug( "Attributes to load for users: " + attrNames );
    }
    // Retrieving from LDAP using a search
    retrieveSelection( ldap, lic.getUserBaseDn( ), lic.getUsersSelection( ), attrNames.toArray( new String[0] ), new LdapEntryProcessor( ) {

      @Override
      public void processLdapEntry( String dn, Attributes attrs ) throws NamingException {
        if ( VERBOSE ) {
          LOG.debug( "Retrieved user: " + dn + " -> " + attrs );
        }
        String id = sanitizeUserGroupId( getId( dn, lic.getUserIdAttribute( ), attrs ) );
        userDnToId.put( dn, id );
        Map<String, String> infoMap = Maps.newHashMap( );
        for ( String attrName : lic.getUserInfoAttributes( ).keySet( ) ) {
          String infoKey = lic.getUserInfoAttributes( ).get( attrName );
          String infoVal = getAttrWithNullCheck( attrs, attrName );
          if ( infoVal != null ) {
            infoMap.put( infoKey, infoVal );
          }
        }
        infoMap.put( User.DN, dn );
        if ( !Strings.isNullOrEmpty( lic.getUserSaslIdAttribute( ) ) ) {
          infoMap.put( User.SASLID, getAttrWithNullCheck( attrs, lic.getUserSaslIdAttribute( ) ) );
        }
        users.put( id, infoMap );
      }
      
    } );
  }

  private static String getAttrWithNullCheck( Attributes attrs, String attrName ) throws NamingException {
    Attribute attr = attrs.get( attrName );
    if ( attr != null ) {
      return ( ( String ) attr.get( ) ).toLowerCase( );
    }
    return null;
  }
    
  private static String sanitizeUserGroupId( String id ) {
    if ( id != null ) {
      return id.replaceAll( ValueCheckerFactory.INVALID_USERGROUPNAME_CHARSET_REGEX, "-" ).replaceAll( "-{2,}", "-" );
    }
    return id;
  }

  private static String sanitizeAccountId( String id ) {
    if ( id != null ) {
      return id.replaceAll( ValueCheckerFactory.INVALID_ACCOUNTNAME_CHARSET_REGEX, "-" ).replaceAll( "-{2,}", "-" );
    }
    return id;
  }

}
