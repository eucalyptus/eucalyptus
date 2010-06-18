package com.eucalyptus.auth;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.GroupProvider;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.google.common.collect.Lists;

public class Groups {
  private static Logger            LOG                    = Logger.getLogger( Groups.class );
  public static String             NAME_ALL               = "all";
  public static String             NAME_DEFAULT           = "default";
  public static Group              ALL                    = null;
  public static Group              DEFAULT                = null;
  public static final List<Group>  RESTRICTED_GROUPS      = Lists.newArrayList( );
  public static final List<String> NAME_RESTRICTED_GROUPS = Lists.newArrayList( NAME_ALL, NAME_DEFAULT );
  private static GroupProvider     groups;
  
  public static void setGroupProvider( GroupProvider provider ) {
    synchronized ( Users.class ) {
      LOG.info( "Setting the group provider to: " + provider.getClass( ) );
      groups = provider;
    }
  }
  
  public static GroupProvider getGroupProvider( ) {
    return groups;
  }
  
  public static List<Group> listAllGroups( ) {
    return Groups.getGroupProvider( ).listAllGroups( );
  }
  
  public static List<Group> lookupUserGroups( User user ) {
    return Groups.getGroupProvider( ).lookupUserGroups( user );
  }
  
  public static Group lookupGroup( String name ) throws NoSuchGroupException {
    return Groups.getGroupProvider( ).lookupGroup( name );
  }
  
  public static Group addGroup( String name ) throws GroupExistsException {
    EventRecord.here( Groups.class, EventClass.GROUP, EventType.GROUP_ADDED, name ).info();
    return Groups.getGroupProvider( ).addGroup( name );
  }
  
  public static void deleteGroup( String groupName ) throws NoSuchGroupException {
    EventRecord.here( Groups.class, EventClass.GROUP, EventType.GROUP_DELETED, groupName ).info();
    Groups.getGroupProvider( ).deleteGroup( groupName );
  }
  
  public static void checkNotRestricted( String groupName ) {
    if ( Groups.NAME_RESTRICTED_GROUPS.contains( groupName ) ) {
      throw new IllegalArgumentException( "The groups " + Groups.NAME_RESTRICTED_GROUPS + " cannot be deleted or changed." );
    }
  }
  
}
