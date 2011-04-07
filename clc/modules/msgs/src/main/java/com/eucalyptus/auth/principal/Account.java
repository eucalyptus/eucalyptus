package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;

/**
 * The interface for the user account.
 * 
 * @author wenye
 * 
 *         FIXME:YE: We are missing the standard 12-digit amazon (not AWS) account identifier. We
 *         must be
 *         able to support the amazon 12-digit account IDs for existing users and future service
 *         integrations. I put some notes in Accounts and a test implementation using @PrePersist to
 *         generate the value in {@link AccountEntity#generateOnCommit()} and marked it as
 *         @Column(unique=true}.
 * 
 * @see {@link com.eucalyptus.auth.Accounts}
 * @see {@link com.eucalyptus.auth.entities.AccountEntity#generateOnCommit()}
 */
public interface Account extends /*HasId,*/ BasePrincipal, Serializable {
  public static final String NOBODY_ACCOUNT = "nobody";
  public static final Long NOBODY_ACCOUNT_ID = 1l;
  /**
   * <h2>NOTE:GRZE:</h2> there will <b>always</b> be an account named <tt>eucalyptus</tt>. The name is used
   * in a variety of ways as an input and identifier during system bootstrap. That is, not local
   * host bootstrap. So, this is before any other identifier information is created. To support any
   * simplifications to install, let alone unattended installs, this value MUST be hardcoded -- it
   * is the account which all system services use to bootstrap, including initial configuration.
   */
  public static final String SYSTEM_ACCOUNT = "eucalyptus";
  public static final Long SYSTEM_ACCOUNT_ID = 0l;

  public void setName( String name ) throws AuthException;
  
  public List<User> getUsers( ) throws AuthException;
  
  public List<Group> getGroups( ) throws AuthException;
  
  public User addUser( String userName, String path, boolean skipRegistration, boolean enabled, Map<String, String> info ) throws AuthException;
  public void deleteUser( String userName, boolean forceDeleteAdmin, boolean recursive ) throws AuthException;
  
  public Group addGroup( String groupName, String path ) throws AuthException;
  public void deleteGroup( String groupName, boolean recursive ) throws AuthException;
  
  public Group lookupGroupByName( String groupName ) throws AuthException;
  
  public User lookupUserByName( String userName ) throws AuthException;
  
  public List<Authorization> lookupAccountGlobalAuthorizations( String resourceType ) throws AuthException;
  public List<Authorization> lookupAccountGlobalQuotas( String resourceType ) throws AuthException;
  
  public String getAccountNumber( );
}
