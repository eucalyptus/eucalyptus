package com.eucalyptus.auth.api;

import java.util.List;
import com.eucalyptus.auth.GroupExistsException;
import com.eucalyptus.auth.NoSuchGroupException;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;

public interface GroupProvider {
  /**
   * Get a list of all the groups for which <tt>user</tt> is a member.
   * @param user
   * @return
   */
  public abstract List<Group> lookupUserGroups( User user );
  public abstract List<Group> listAllGroups( );
  /**
   * Get the group named <tt>groupName</tt>
   * @param groupName
   * @return
   * @throws NoSuchGroupException
   */
  public abstract Group lookupGroup( String groupName ) throws NoSuchGroupException;
  public abstract Group addGroup( String groupName ) throws GroupExistsException;
  public abstract void deleteGroup( String groupName ) throws NoSuchGroupException;
  
}
