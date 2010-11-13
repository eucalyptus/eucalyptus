package com.eucalyptus.auth.api;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Group;

public interface GroupProvider {

  /**
   * Add a new group.
   * 
   * @param groupName The name of the new group.
   * @param path The path of the new group.
   * @param accountName The name of the group added to.
   * @return The newly created group.
   * @throws AuthException for any error.
   */
  public Group addGroup( String groupName, String path, String accountName ) throws AuthException;
  
  /**
   * Delete a group and its policies if recursive.
   * 
   * @param groupName The name of the group to be deleted.
   * @param accountName The name of the group's account.
   * @param recursive Whether to delete the group's attached resources, e.g. policies.
   * @throws AuthException for any error.
   */
  public void deleteGroup( String groupName, String accountName, boolean recursive ) throws AuthException;
  
  /**
   * Lookup a group by its name and account name.
   * 
   * @param groupName The name of the group
   * @param accountName The name of the account
   * @return the found group
   * @throws AuthException for any error
   */
  public Group lookupGroupByName( String groupName, String accountName ) throws AuthException;
  
  /**
   * Lookup a group by its ID.
   * 
   * @param groupId The ID of the group
   * @return the found group
   * @throws AuthException for any error
   */
  public Group lookupGroupById( String groupId ) throws AuthException;
  
}
