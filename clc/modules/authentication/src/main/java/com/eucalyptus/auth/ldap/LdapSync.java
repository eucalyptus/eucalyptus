package com.eucalyptus.auth.ldap;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.SystemClock;
import com.eucalyptus.system.Threads;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Logic to perform LDAP sync.
 * 
 * @author wenye
 *
 */
public class LdapSync {
  
  private interface LdapEntryProcessor {
    
    public void processLdapEntry( String dn, Attributes attrs ) throws NamingException;
    
  }
  
  private static final Logger LOG = Logger.getLogger( LdapSync.class );
  
  private static final boolean VERBOSE = true;
  
  private static final String LDAP_SYNC_THREAD = "LDAP sync";
  
  private static final BasicAttributes WILDCARD_FILTER = new BasicAttributes( );
  
  private static final LdapIntegrationConfiguration DEFAULT_LIC = new LdapIntegrationConfiguration( );
  
  private static LdapIntegrationConfiguration lic = DEFAULT_LIC;
  private static boolean inSync = false;
  private static long timeTillNextSync;
  
  private static final ClockTickListener TIMER_LISTENER = new ClockTickListener( );
  
  private static class ClockTickListener implements EventListener<Event> {

    @Override
    public void fireEvent( Event event ) {
      if ( event instanceof ClockTick ) {
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
        // SASL requires user ID
        // TODO(wenye): possible other formats of user ID, like "u:..." or "dn:..."
        login = user.getName( );
      }
      if ( login == null ) {
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
  
  public static void sync( LdapIntegrationConfiguration lic ) {
    // Get users/groups from LDAP
    Map<String, Set<String>> accountingGroups = null;
    Map<String, Set<String>> groups = null;
    Map<String, Map<String, String>> users = null;
    LdapClient ldap = null;
    try {
      ldap = LdapClient.authenticateClient( lic );
      if ( lic.hasAccountingGroups( ) ) {
        accountingGroups = loadLdapGroupType( ldap,
                                              lic.getAccountingGroupBaseDn( ),
                                              lic.getAccountingGroupIdAttribute( ),
                                              lic.getGroupsAttribute( ),
                                              lic.getGroupIdAttribute( ),
                                              lic.getAccountingGroupsSelection( ) );
      }
      groups = loadLdapGroupType( ldap,
                                  lic.getGroupBaseDn( ),
                                  lic.getGroupIdAttribute( ),
                                  lic.getUsersAttribute( ),
                                  lic.getUserIdAttribute( ),
                                  lic.getGroupsSelection( ) );
      users = loadLdapUsers( ldap, lic );
    } catch ( Exception e ) {
      LOG.error( e, e );
      LOG.error( "Failed to sync with LDAP", e );
      return;
    } finally {
      if ( ldap != null ) {
        ldap.close( );
      }
    }
    if ( !lic.hasAccountingGroups( ) ) {
      accountingGroups = lic.getGroupsPartition( );
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
      account.addUser( User.ACCOUNT_ADMIN, "/", true/* skipRegistration */, true, null );
      for ( String user : getAccountUserSet( accountMembers, groups ) ) {
        try {
          LOG.debug( "Adding new user " + user );
          Map<String, String> info = users.get( user );
          if ( info == null ) {
            LOG.warn( "Empty user info for user " + user );
          }
          account.addUser( user, "/", true/* skipRegistration */, true/* enabled */, info );
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
    account.addUser( user, "/", true/* skipRegistration */, true/* enabled */, info );
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
    for ( String account : oldAccountSet ) {
      try {
        Accounts.deleteAccount( account, false /* forceDeleteSystem */, true /* recursive */ );
      } catch ( AuthException e ) {
        LOG.error( e, e );
        LOG.warn( "Failed to delete account " + account, e );
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
  
  private static String getId( String idAttrName, Attributes attrs ) throws NamingException {
    String id = getAttrWithNullCheck( attrs, idAttrName );
    if ( LicParser.isEmpty( id ) ) {
      throw new NamingException( "Empty ID for " + attrs );
    }
    return id;
  }
  
  private static Set<String> getMembers( String idAttrName, String memberAttrName, Attributes attrs ) throws NamingException {
    Set<String> members = Sets.newHashSet( );
    Attribute membersAttr = attrs.get( memberAttrName );
    if ( membersAttr != null ) {
      NamingEnumeration<?> names = membersAttr.getAll( );
      while ( names.hasMore( ) ) {
        members.add( parseMemberName( idAttrName, ( ( String ) names.next( ) ).toLowerCase( ) ).toLowerCase( ) );
      }
    }
    return members;
  }
  
  private static String parseMemberName( String idAttrName, String dn ) throws NamingException {
    if ( LicParser.isEmpty( dn ) ) {
      throw new NamingException( "Empty member name in accounting group" + dn );
    }
    dn = dn.trim( );
    Pattern pattern = Pattern.compile( idAttrName + "=([^,]+),.*");
    Matcher matcher = pattern.matcher( dn );
    if ( matcher.matches( ) ) {
      return matcher.group( 1 );
    } else if ( dn.contains( "=" ) ) {
      throw new NamingException( "Can not recognize member name " + dn );
    } else {
      return dn;
    }
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
        if ( !selection.getNotSelected( ).contains( res.getNameInNamespace( ) ) ) {
          processor.processLdapEntry( res.getNameInNamespace( ), res.getAttributes( ) );
        }
      }
      // Get one-off DNs
      for ( String dn : selection.getSelected( ) ) {
        Attributes attrs = null;
        try {
          attrs = ldap.getContext( ).getAttributes( dn, attrNames );
        } catch ( NamingException e ) {
          LOG.debug( "Failed to retrieve entry " + dn );
          LOG.error( e, e );
        }
        processor.processLdapEntry( dn, attrs );
      }
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( e );
    }
  }
  
  private static Map<String, Set<String>> loadLdapGroupType( LdapClient ldap, String baseDn, final String idAttrName, final String memberAttrName, final String memberIdAttrName, Selection selection ) throws LdapException {
    String[] attrNames = new String[]{ idAttrName, memberAttrName };
    final Map<String, Set<String>> groupMap = Maps.newHashMap( );
    retrieveSelection( ldap, baseDn, selection, attrNames, new LdapEntryProcessor( ) {

      @Override
      public void processLdapEntry( String dn, Attributes attrs ) throws NamingException {
        if ( VERBOSE ) {
          LOG.debug( "Retrieved group: " + dn + " -> " + attrs );
        }
        groupMap.put( getId( idAttrName, attrs ), getMembers( memberIdAttrName, memberAttrName, attrs ) );
        
      }
      
    } );
    return groupMap;
  }
  
  private static Map<String, Map<String, String>> loadLdapUsers( LdapClient ldap, final LdapIntegrationConfiguration lic ) throws LdapException {
    // Prepare the list of attributes to retrieve
    List<String> attrNames = Lists.newArrayList( );
    attrNames.addAll( lic.getUserInfoAttributes( ).keySet( ) );
    if ( !lic.getUserInfoAttributes( ).keySet( ).contains( lic.getUserIdAttribute( ) ) ) {
      attrNames.add( lic.getUserIdAttribute( ) );
    }
    // Retrieving from LDAP using a search
    final Map<String, Map<String, String>> userMap = Maps.newHashMap( );
    retrieveSelection( ldap, lic.getUserBaseDn( ), lic.getUsersSelection( ), attrNames.toArray( new String[0] ), new LdapEntryProcessor( ) {

      @Override
      public void processLdapEntry( String dn, Attributes attrs ) throws NamingException {
        if ( VERBOSE ) {
          LOG.debug( "Retrieved user: " + dn + " -> " + attrs );
        }
        Map<String, String> infoMap = Maps.newHashMap( );
        for ( String attrName : lic.getUserInfoAttributes( ).keySet( ) ) {
          String infoKey = lic.getUserInfoAttributes( ).get( attrName );
          String infoVal = getAttrWithNullCheck( attrs, attrName );
          if ( infoVal != null ) {
            infoMap.put( infoKey, infoVal );
          }
        }
        infoMap.put( User.DN, dn );
        userMap.put( getId( lic.getUserIdAttribute( ), attrs ).toLowerCase( ), infoMap );
      }
      
    } );
    return userMap;
  }

  private static String getAttrWithNullCheck( Attributes attrs, String attrName ) throws NamingException {
    Attribute attr = attrs.get( attrName );
    if ( attr != null ) {
      return ( ( String ) attr.get( ) ).toLowerCase( );
    }
    return null;
  }
  
}
