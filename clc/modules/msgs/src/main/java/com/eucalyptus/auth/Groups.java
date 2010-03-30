package com.eucalyptus.auth;

import java.util.List;
import org.apache.log4j.Logger;

public class Groups {
  private static Logger LOG = Logger.getLogger( Groups.class );
  private static GroupProvider groups;

  public static void setGroupProvider( GroupProvider provider ) {
    synchronized( Users.class ) {
      LOG.info( "Setting the group provider to: " + provider.getClass( ) );
      groups = provider;
    }
  }
  
  public static GroupProvider getGroupProvider() {
     return groups;
  }

  public static List<Group> lookupGroups( User user ) {
    return Groups.getGroupProvider( ).lookupUserGroups( user );
  }
  
  public static Group lookupGroup( String name ) throws NoSuchGroupException {
    return Groups.getGroupProvider( ).lookupGroup( name );
  }
  
}
