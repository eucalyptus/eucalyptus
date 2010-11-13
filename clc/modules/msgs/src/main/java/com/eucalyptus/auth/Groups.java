package com.eucalyptus.auth;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.GroupProvider;
import com.eucalyptus.auth.principal.Group;

public class Groups {
  private static Logger            LOG                    = Logger.getLogger( Groups.class );

  private static GroupProvider     groups;
  
  public static void setGroupProvider( GroupProvider provider ) {
    synchronized ( Groups.class ) {
      LOG.info( "Setting the group provider to: " + provider.getClass( ) );
      groups = provider;
    }
  }
  
  public static GroupProvider getGroupProvider( ) {
    return groups;
  }
  
  public static Group addGroup( String groupName, String path, String accountName ) throws AuthException {
    return Groups.getGroupProvider( ).addGroup( groupName, path, accountName );
  }
  
  public static void deleteGroup( String groupName, String accountName, boolean recursive ) throws AuthException {
    Groups.getGroupProvider( ).deleteGroup( groupName, accountName, recursive );
  }
  
  public static Group lookupGroupByName( String groupName, String accountName ) throws AuthException {
    return Groups.getGroupProvider( ).lookupGroupByName( groupName, accountName );
  }
  
  public static Group lookupGroupById( String groupId ) throws AuthException {
    return Groups.getGroupProvider( ).lookupGroupById( groupId );
  }
  
}
