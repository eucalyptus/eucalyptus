package com.eucalyptus.auth.api;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;

/**
 * Interface for user operations.
 * 
 * @author decker
 *
 */
public interface UserProvider {
  
  /**
   * Add a new user with basic information.
   * 
   * @param userName The name of the new user
   * @param path The path of the user.
   * @param skipRegistration Whether to skip registration.
   * @param enabled If the new user is enabled.
   * @param info The informational content of the user.
   * @param createKey Whether to generate a secret key.
   * @param createPassword Whether to generate a Web password.
   * @param groupNames The names of the groups the user should belong to.
   * @return The newly created user object.
   * @throws AuthException for any error, e.g. user already exists, groups can't be found, etc.
   */
  public User addUser( String userName, String path, boolean skipRegistration, boolean enabled, Map<String, String> info,
                       boolean createKey, boolean createPassword, String accountName ) throws AuthException;
  
  /**
   * Delete a user from an account, recursively removing it from all the groups
   * and its associated credentials.
   * 
   * @param userName The name of the user to remove.
   * @param accountName The name of the account that the user belongs to.
   * @param forceDeleteAdmin Whether to force delete account admin user.
   * @param recursive Whether to delete a user if he has resources (groups, policies, certs, keys) attached.
   * @throws AuthException for any error.
   */
  public void deleteUser( String userName, String accountName, boolean forceDeleteAdmin, boolean recursive ) throws AuthException;
  
  /**
   * Lookup an user by its name and its account.
   * 
   * @param userName The name of the user
   * @param accountName The name of the account of the user
   * @return The user found
   * @throws AuthException for any error
   */
  public User lookupUserByName( String userName, String accountName ) throws AuthException;
  
  /**
   * Lookup an enabled user by its access key ID. Only return the user if the access key is active.
   * 
   * @param keyId The ID of the access key.
   * @return The user owns the key.
   * @throws AuthException for any error.
   */
  public User lookupUserByAccessKeyId( String keyId ) throws AuthException;
  
  /**
   * Lookup an enabled user by its certificate. Only return the user if the certificate is active and not revoked.
   * 
   * @param cert The certificate of the user
   * @return The user owns the certificate
   * @throws AuthException for any error
   */
  public User lookupUserByCertificate( X509Certificate cert ) throws AuthException;
  
  /**
   * Lookup user by its unique ID.
   * 
   * @param userId
   * @return The user by this ID.
   * @throws AuthException for any error.
   */
  public User lookupUserById( String userId ) throws AuthException;
  
  /**
   * Check if two users share the same account.
   * 
   * @param userId1
   * @param userId2
   * @return true if they are in the same account, false otherwise
   */
  public boolean shareSameAccount( String userId1, String userId2 );
  
  /**
   * List all the users in the system.
   * 
   * @return the list of all users.
   * @throws AuthException for any error.
   */
  public List<User> listAllUsers( ) throws AuthException;
  
  /**
   * List all users in an account
   * @param accountName The name of the account
   * @return the list of users in the account
   * @throws AuthException for any error
   */
  public List<User> listAllUsers( String accountName ) throws AuthException;
  
}