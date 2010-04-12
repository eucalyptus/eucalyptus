package com.eucalyptus.auth;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.GroupProvider;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.EntityWrapper;

public class Groups {
  private static Logger LOG = Logger.getLogger( Groups.class );
  private static GroupProvider groups;
  public static <T> EntityWrapper<T> getEntityWrapper( ) {
    return new EntityWrapper<T>( "eucalyptus_auth" );
  }

  public static void setGroupProvider( GroupProvider provider ) {
    synchronized( Users.class ) {
      LOG.info( "Setting the group provider to: " + provider.getClass( ) );
      groups = provider;
    }
  }
  
  public static GroupProvider getGroupProvider() {
     return groups;
  }

  public static List<Group> listAllGroups( ) {
    return Groups.getGroupProvider( ).listAllGroups();
  }

  public static List<Group> lookupGroups( User user ) {
    return Groups.getGroupProvider( ).lookupUserGroups( user );
  }
  
  public static Group lookupGroup( String name ) throws NoSuchGroupException {
    return Groups.getGroupProvider( ).lookupGroup( name );
  }
  public static Group addGroup( String name ) throws GroupExistsException {
    return Groups.getGroupProvider( ).addGroup( name );
  }

  public static void deleteGroup( String groupName ) throws NoSuchGroupException {
    Groups.getGroupProvider( ).deleteGroup( groupName );
  }
  
}
