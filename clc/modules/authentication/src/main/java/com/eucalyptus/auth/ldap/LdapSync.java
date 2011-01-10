package com.eucalyptus.auth.ldap;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.Users;
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
  
  private static final Logger LOG = Logger.getLogger( LdapSync.class );
  
  private static final String LDAP_SYNC_THREAD = "LDAP sync";
  
  private static final BasicAttributes WILDCARD_FILTER = new BasicAttributes( );
  
  private static final LdapIntegrationConfiguration DEFAULT_LIC = new LdapIntegrationConfiguration( );
  
  private static LdapIntegrationConfiguration lic = DEFAULT_LIC;
  private static boolean inSync = false;
  private static long timeTillNextSync;
  
  private static final ClockTickListener TIMER_LISTENER = new ClockTickListener( );
  
  private static class ClockTickListener implements EventListener {

    @Override
    public void advertiseEvent( Event event ) {      
    }

    @Override
    public void fireEvent( Event event ) {
      if ( event instanceof ClockTick ) {
        periodicSync( );
      }
    }
    
  }
  
  private static class DummyPrincipal implements Principal {

    private String name;
    
    public DummyPrincipal( String name ) {
      this.name = name;
    }
    
    @Override
    public String getName( ) {
      return this.name;
    }
    
  }
  
  public static synchronized void start( ) {
    if ( lic.isSyncEnabled( ) && lic.isAutoSync( ) ) {
      ListenerRegistry.getInstance( ).register( ClockTick.class, TIMER_LISTENER );
    }
    doForceSync( );
  }
  
  public static synchronized LdapIntegrationConfiguration getLic( ) {
    return lic;
  }
  
  public static synchronized void setLic( LdapIntegrationConfiguration config ) {
    lic = config;
    if ( Bootstrap.isFinished( ) ) {
      if ( lic.isSyncEnabled( ) && lic.isAutoSync( ) ) {
        ListenerRegistry.getInstance( ).register( ClockTick.class, TIMER_LISTENER );
      } else {
        ListenerRegistry.getInstance( ).deregister( ClockTick.class, TIMER_LISTENER );
      }
      doForceSync( );
    }
  }
  
  public static synchronized void forceSync( ) {
    doForceSync( );
  }
  
  public static synchronized boolean check( ) {
    if ( lic.isSyncEnabled( ) ) {
      LdapClient ldap = null;
      try {
        ldap = new LdapClient( lic );
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
    } else {
      return true;
    }
  }
  
  private static void doForceSync( ) {
    if ( lic.isSyncEnabled( ) ) {
      timeTillNextSync = lic.getSyncInterval( );
      startSync( );
    }    
  }
  
  private static synchronized void setSyncCompleted( ) {
    inSync = false;
  }
  
  private static synchronized void periodicSync( ) {
    if ( lic.isSyncEnabled( ) && lic.isAutoSync( ) ) {
      timeTillNextSync -= SystemClock.getRate( );
      if ( timeTillNextSync <= 0 ) {
        timeTillNextSync = lic.getSyncInterval( );
        startSync( );
      }
    } 
  }
  
  private static void startSync( ) {
    if ( !inSync ) {
      inSync = true;
      
      Threads.newThread( new Runnable( ) {
        
        @Override
        public void run( ) {
          LOG.debug( "Sync started" );
          sync( lic );
          setSyncCompleted( );
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
      ldap = new LdapClient( lic );
      if ( lic.hasAccountingGroups( ) ) {
        accountingGroups = loadLdapGroupType( ldap,
                                              lic.getAccountingGroupBaseDn( ),
                                              lic.getAccountingGroupIdAttribute( ),
                                              lic.getGroupsAttribute( ),
                                              lic.getGroupIdAttribute( ) );
      }
      groups = loadLdapGroupType( ldap,
                                  lic.getGroupBaseDn( ),
                                  lic.getGroupIdAttribute( ),
                                  lic.getUsersAttribute( ),
                                  lic.getUserIdAttribute( ) );
      users = loadLdapUsers( ldap, lic );
    } catch ( LdapException e ) {
      LOG.error( e, e );
      LOG.error( "Failed to sync with LDAP", e );
      return;
    } finally {
      if ( ldap != null ) {
        ldap.close( );
      }
    }
    rebuildLocalAuthDatabase( lic, accountingGroups, groups, users );
  }
  
  private static void rebuildLocalAuthDatabase( LdapIntegrationConfiguration lic, Map<String, Set<String>> accountingGroups,
                                                Map<String, Set<String>> groups, Map<String, Map<String, String>> users ) {
    try {
      Set<String> oldAccountSet = getLocalAccountSet( );
      if ( !lic.hasAccountingGroups( ) ) {
        accountingGroups = lic.getGroupsPartition( );
      }
      for ( Map.Entry<String, Set<String>> entry : accountingGroups.entrySet( ) ) {
        String accountName = entry.getKey( );
        Set<String> accountMembers = entry.getValue( );
        if ( oldAccountSet.contains( accountName ) ) {
          // Remove common elements from old account set
          oldAccountSet.remove( accountName );
          updateAccount( accountName, accountMembers, groups, users );
        } else {
          addNewAccount( accountName, accountMembers, groups, users );
        }
      }
      // Remaining accounts are obsolete
      removeObsoleteAccounts( oldAccountSet );
    } catch ( AuthException e ) {
      LOG.error( e, e );
      LOG.error( "Error in rebuilding local auth database", e );
    }
  }

  private static void addNewAccount( String accountName, Set<String> accountMembers, Map<String, Set<String>> groups, Map<String, Map<String, String>> users ) {
    try {
      Accounts.addAccount( accountName );
      Users.addAccountAdmin( accountName );
      for ( String user : getAccountUserSet( accountMembers, groups ) ) {
        try {
          Users.addUser( user, "/", true /* skipRegistration */, true /* enabled */, users.get( user ), true /* createKey */, false /* createPassword */, accountName );
        } catch ( AuthException e ) {
          LOG.error( e, e );
          LOG.warn( "Failed add new user " + user, e );
        }
      }
      for ( String group : accountMembers ) {
        Group dbGroup = null;
        try {
          dbGroup = Groups.addGroup( group, "/", accountName );
          for ( String user : groups.get( group ) ) {
            dbGroup.addMember( new DummyPrincipal( user ) );
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
      userSet.addAll( groups.get( member ) );
    }
    return userSet;
  }
  
  private static void updateAccount( String accountName, Set<String> accountMembers, Map<String, Set<String>> groups, Map<String, Map<String, String>> users ) {
    try {
      // Update users first
      Set<String> newUserSet = getAccountUserSet( accountMembers, groups );
      Set<String> oldUserSet = getLocalUserSet( accountName );
      for ( String user : newUserSet ) {
        if ( oldUserSet.contains( user ) ) {
          oldUserSet.remove( user );
          try {
            updateUser( accountName, user, users.get( user ) );
          } catch ( AuthException e ) {
            LOG.error( e, e );
            LOG.warn( "Failed to update user " + user + " in " + accountName, e );
          }
        } else {
          try {
            addNewUser( accountName, user, users.get( user ) );
          } catch ( AuthException e ) {
            LOG.error( e, e );
            LOG.warn( "Failed to add new user " + user + " in " + accountName, e );
          }
        }
      }
      removeObsoleteUsers( accountName, oldUserSet );
      // Now update groups
      Set<String> oldGroupSet = getLocalGroupSet( accountName );
      for ( String group : accountMembers ) {
        if ( oldGroupSet.contains( group ) ) {
          oldGroupSet.remove( group );
          updateGroup( accountName, group, groups.get( group ) );
        } else {
          addNewGroup( accountName, group, groups.get( group ) );
        }
      }
      removeObsoleteGroups( accountName, oldGroupSet );
    } catch ( AuthException e ) {
      LOG.error( e, e );
      LOG.error( "Failed to update account " + accountName, e );
    }
  }
  
  private static void removeObsoleteGroups( String accountName, Set<String> oldGroupSet ) {
    for ( String group : oldGroupSet ) {
      try {
        Groups.deleteGroup( group, accountName, true /* recursive */ );
      } catch ( AuthException e ) {
        LOG.error( e, e );
        LOG.warn( "Failed to delete group " + group + " in " + accountName, e );
      }
    }
  }

  private static void addNewGroup( String accountName, String group, Set<String> users ) {
    try {
      Group g = Groups.addGroup( group, "/", accountName );
      for ( String user : users ) {
        g.addMember( new DummyPrincipal( user ) );
      }
    } catch ( AuthException e ) {
      LOG.error( e, e );
      LOG.warn( "Failed to add new group " + group + " in " + accountName, e );
    }
  }

  private static void updateGroup( String accountName, String group, Set<String> users ) {
    try {
      // Get local user set of the group
      Set<String> localUserSet = Sets.newHashSet( );
      Group g = Groups.lookupGroupByName( group, accountName );
      for ( User u : g.getUsers( ) ) {
        localUserSet.add( u.getName( ) );
      }
      // Update group by adding new users and remove obsolete users
      for ( String user : users ) {
        if ( localUserSet.contains( user ) ) {
          localUserSet.remove( user );
        } else {
          g.addMember( new DummyPrincipal( user ) );
        }
      }
      for ( String user : localUserSet ) {
        g.removeMember( new DummyPrincipal( user ) );
      }
    } catch ( AuthException e ) {
      LOG.error( e, e );
      LOG.warn( "Failed to update group " + group + " in " + accountName, e );
    }
  }

  private static Set<String> getLocalGroupSet( String accountName ) throws AuthException {
    Set<String> groupSet = Sets.newHashSet( );
    for ( Group group : Accounts.listAllGroups( accountName ) ) {
      groupSet.add( group.getName( ) );
    }
    return groupSet;
  }

  private static void removeObsoleteUsers( String accountName, Set<String> oldUserSet ) {
    // We don't want to remove account admin when updating an account
    oldUserSet.remove( User.ACCOUNT_ADMIN );
    for ( String user : oldUserSet ) {
      try {
        Users.deleteUser( user, accountName, true /* forceDeleteAdmin */, true /* recursive */ );
      } catch ( AuthException e ) {
        LOG.error( e, e );
        LOG.warn( "Failed to delete user " + user + " in " + accountName );
      }
    }
  }

  private static void addNewUser( String accountName, String user, Map<String, String> info ) throws AuthException {
    Users.addUser( user, "/", true /* skipRegistration */, true /* enabled */, info, true /* createKey */, false /* createPassword */, accountName );
  }

  private static void updateUser( String accountName, String user, Map<String, String> map ) throws AuthException {
    Users.lookupUserByName( user, accountName ).setInfo( map );
  }

  private static Set<String> getLocalUserSet( String accountName ) throws AuthException {
    Set<String> userSet = Sets.newHashSet( );
    for ( User user : Accounts.listAllUsers( accountName ) ) {
      userSet.add( user.getName( ) );
    }
    return userSet;
  }
  
  private static void removeObsoleteAccounts( Set<String> oldAccountSet ) {
    // We don't want to remove system account
    oldAccountSet.remove( Account.SYSTEM_ACCOUNT );
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
    String id = ( String ) attrs.get( idAttrName ).get( );
    if ( LicParser.isEmpty( id ) ) {
      throw new NamingException( "Empty ID for " + attrs );
    }
    return id;
  }
  
  private static Set<String> getMembers( String idAttrName, String memberAttrName, Attributes attrs ) throws NamingException {
    Set<String> members = Sets.newHashSet( );
    NamingEnumeration names = attrs.get( memberAttrName ).getAll( );
    while ( names.hasMore( ) ) {
      members.add( parseMemberName( idAttrName, ( String ) names.next( ) ) );
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
  
  private static Map<String, Set<String>> loadLdapGroupType( LdapClient ldap, String baseDn, String idAttrName, String memberAttrName, String memberIdAttrName ) throws LdapException {
    NamingEnumeration<SearchResult> results = ldap.search( baseDn, 
                                                           WILDCARD_FILTER, 
                                                           new String[]{ idAttrName, memberAttrName } );
    try {
      Map<String, Set<String>> groupMap = Maps.newHashMap( );
      while ( results.hasMore( ) ) {
        Attributes attrs = results.next( ).getAttributes( );
        groupMap.put( getId( idAttrName, attrs ),
                      getMembers( memberIdAttrName, memberAttrName, attrs ) );
      }
      return groupMap;
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( "Error reading groups", e );
    }
  }
  
  private static Map<String, Map<String, String>> loadLdapUsers( LdapClient ldap, LdapIntegrationConfiguration lic ) throws LdapException {
    // Prepare the list of attributes to retrieve
    List<String> attrNames = Lists.newArrayList( );
    attrNames.addAll( lic.getUserInfoAttributes( ) );
    if ( !lic.getUserInfoAttributes( ).contains( lic.getUserIdAttribute( ) ) ) {
      attrNames.add( lic.getUserIdAttribute( ) );
    }
    // Retrieving from LDAP using a search
    NamingEnumeration<SearchResult> results = ldap.search( lic.getUserBaseDn( ),
                                                           WILDCARD_FILTER,
                                                           attrNames.toArray( new String[0] ) );
    try {
      Map<String, Map<String, String>> userMap = Maps.newHashMap( );
      while ( results.hasMore( ) ) {
        Attributes attrs = results.next( ).getAttributes( );
        Map<String, String> infoMap = Maps.newHashMap( );
        for ( String attr : lic.getUserInfoAttributes( ) ) {
          infoMap.put( attr, ( String ) attrs.get( attr ).get( ) );
        }
      }
      return userMap;
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( "Error reading users", e );
    }
  }

}
