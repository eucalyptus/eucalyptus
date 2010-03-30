package com.eucalyptus.auth.group;

import java.util.List;
import com.eucalyptus.auth.User;

public interface GroupProvider {
  /**
   * Get a list of all the groups for which <tt>user</tt> is a member.
   * @param user
   * @return
   */
  public abstract List<Group> lookupUserGroups( User user );
  /**
   * Get the group named <tt>groupName</tt>
   * @param groupName
   * @return
   * @throws NoSuchGroupException
   */
  public abstract Group lookupGroup( String groupName ) throws NoSuchGroupException;
  
}
