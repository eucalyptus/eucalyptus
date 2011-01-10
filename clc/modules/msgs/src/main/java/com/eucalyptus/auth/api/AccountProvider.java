package com.eucalyptus.auth.api;

import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;

public interface AccountProvider {

  /**
   * Add a new account. Add account admin user separately.
   * 
   * @param accountName The name of account
   * @return The added account entity.
   * @throws AuthException for any error.
   */
  public Account addAccount( String accountName ) throws AuthException;
  
  /**
   * Delete an account. The account must be already empty (no group or user).
   * 
   * @param accountName The name of the account to delete.
   * @param forceDeleteSystem If forcing to delete the system account.
   * @param recursive If delete the account recursively (by destroying all groups and users in the account)
   * @throws AuthException for any error.
   */
  public void deleteAccount( String accountName, boolean forceDeleteSystem, boolean recursive ) throws AuthException;
  
  /**
   * List all the accounts
   * 
   * @return all accounts in the system
   * @throws AuthException for any error
   */
  public List<Account> listAllAccounts( ) throws AuthException;
  
  /**
   * List all users of an account.
   * 
   * @param accountName The name of the account to list
   * @return all users of the account.
   * @throws AuthException for any error
   */
  public List<User> listAllUsers( String accountName ) throws AuthException;
  
  /**
   * List all groups of an account.
   * 
   * @param accountName The name of the account to list
   * @return all groups of the account.
   * @throws AuthException for any error
   */
  public List<Group> listAllGroups( String accountName ) throws AuthException;
  
}
