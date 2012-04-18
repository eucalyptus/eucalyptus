package com.eucalyptus.webui.client.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("backend")
public interface EucalyptusService extends RemoteService {
  
  Session login( String accountName, String userName, String password ) throws EucalyptusServiceException;
  
  /**
   * Logout current user.
   * 
   * @throws EucalyptusServiceException
   */
  void logout( Session session ) throws EucalyptusServiceException;
  
  /**
   * Get the login user profile
   * 
   * @param session
   * @return
   * @throws EucalyptusServiceException
   */
  LoginUserProfile getLoginUserProfile( Session session ) throws EucalyptusServiceException;
  
  /**
   * Get system properties.
   * 
   * @param session
   * @return
   * @throws EucalyptusServiceException
   */
  HashMap<String, String> getSystemProperties( Session session ) throws EucalyptusServiceException;
  
  /**
   * Get quicklinks tree data.
   * 
   * @param session
   * @return
   * @throws EucalyptusServiceException
   */
  ArrayList<QuickLinkTag> getQuickLinks( Session session ) throws EucalyptusServiceException;
 
  /**
   * Search system configurations.
   * 
   * @param session
   * @param search
   * @param range
   * @return
   * @throws EucalyptusServiceException
   */
  SearchResult lookupConfiguration( Session session, String search, SearchRange range ) throws EucalyptusServiceException;
  
  /**
   * Set system configurations.
   * 
   * @param session
   * @param config
   * @throws EucalyptusServiceException
   */
  void setConfiguration( Session session, SearchResultRow config ) throws EucalyptusServiceException;
  
  /**
   * Search accounts.
   * 
   * @param session
   * @param search
   * @param range
   * @return
   * @throws EucalyptusServiceException
   */
  SearchResult lookupAccount( Session session, String search, SearchRange range ) throws EucalyptusServiceException;

  /**
   * Search VM types.
   * 
   * @param session
   * @param search
   * @param range
   * @return
   * @throws EucalyptusServiceException
   */
  SearchResult lookupVmType( Session session, String search, SearchRange range ) throws EucalyptusServiceException;

  /**
   * Set VmType values.
   * 
   * @param session
   * @param result
   * @throws EucalyptusServiceException
   */
  void setVmType( Session session, SearchResultRow result ) throws EucalyptusServiceException;
  
  /**
   * Search user groups.
   * 
   * @param session
   * @param search
   * @param range
   * @return
   * @throws EucalyptusServiceException
   */
  SearchResult lookupGroup( Session session, String search, SearchRange range ) throws EucalyptusServiceException;
  
  /**
   * Search users.
   * 
   * @param session
   * @param search
   * @param range
   * @return
   * @throws EucalyptusServiceException
   */
  SearchResult lookupUser( Session session, String search, SearchRange range ) throws EucalyptusServiceException;
  
  /**
   * Search policies.
   * 
   * @param session
   * @param search
   * @param range
   * @return
   * @throws EucalyptusServiceException
   */
  SearchResult lookupPolicy( Session session, String search, SearchRange range ) throws EucalyptusServiceException;
  
  /**
   * Search access keys.
   * 
   * @param session
   * @param search
   * @param range
   * @return
   * @throws EucalyptusServiceException
   */
  SearchResult lookupKey( Session session, String search, SearchRange range ) throws EucalyptusServiceException;
  
  /**
   * Search X509 certificates.
   * 
   * @param session
   * @param search
   * @param range
   * @return
   * @throws EucalyptusServiceException
   */
  SearchResult lookupCertificate( Session session, String search, SearchRange range ) throws EucalyptusServiceException;
  
  /**
   * Search VM images.
   * 
   * @param session
   * @param search
   * @param range
   * @return
   * @throws EucalyptusServiceException
   */
  SearchResult lookupImage( Session session, String search, SearchRange range ) throws EucalyptusServiceException;
  
  /**
   * Create a new account.
   * 
   * @param session
   * @param accountName
   * @param adminPassword
   * @throws EucalyptusServiceException
   */
  String createAccount( Session session, String accountName, String adminPassword ) throws EucalyptusServiceException;
  
  /**
   * Delete accounts.
   * 
   * @param session
   * @param ids
   * @throws EucalyptusServiceException
   */
  void deleteAccounts( Session session, ArrayList<String> ids ) throws EucalyptusServiceException;
  
  /**
   * Modify account.
   * 
   * @param session
   * @param keys
   * @param values
   * @throws EucalyptusServiceException
   */
  void modifyAccount( Session session, ArrayList<String> values ) throws EucalyptusServiceException;
  
  /**
   * Create multiple users in the same account, with same path.
   * 
   * @param session
   * @param accountId
   * @param names User names separated by spaces.
   * @param path
   * @return the created user names.
   * @throws EucalyptusServiceException
   */
  ArrayList<String> createUsers( Session session, String accountId, String names, String path ) throws EucalyptusServiceException;
  
  /**
   * Create multiple groups in the same account, with same path.
   * 
   * @param session
   * @param accountId
   * @param names Group names separated by spaces.
   * @param path
   * @return the created group names.
   * @throws EucalyptusServiceException
   */
  ArrayList<String> createGroups( Session session, String accountId, String names, String path ) throws EucalyptusServiceException;
  
  /**
   * Delete a list of users.
   * 
   * @param session
   * @param ids
   * @throws EucalyptusServiceException
   */
  void deleteUsers( Session session, ArrayList<String> ids ) throws EucalyptusServiceException;
  
  /**
   * Delete a list of groups.
   * 
   * @param session
   * @param ids
   * @throws EucalyptusServiceException
   */
  void deleteGroups( Session session, ArrayList<String> ids ) throws EucalyptusServiceException;
  
  /**
   * Add policy to account.
   * 
   * @param session
   * @param accountId
   * @param name
   * @param document
   * @throws EucalyptusServiceException
   */
  void addAccountPolicy( Session session, String accountId, String name, String document ) throws EucalyptusServiceException;
  
  /**
   * Add policy to user.
   * 
   * @param session
   * @param usertId
   * @param name
   * @param document
   * @throws EucalyptusServiceException
   */
  void addUserPolicy( Session session, String usertId, String name, String document ) throws EucalyptusServiceException;
  
  /**
   * Add policy to group.
   * 
   * @param session
   * @param groupId
   * @param name
   * @param document
   * @throws EucalyptusServiceException
   */
  void addGroupPolicy( Session session, String groupId, String name, String document ) throws EucalyptusServiceException;
  
  /**
   * Delete a policy.
   * 
   * @param session
   * @param policySerialized
   * @throws EucalyptusServiceException
   */
  void deletePolicy( Session session, SearchResultRow policySerialized ) throws EucalyptusServiceException;
  
  /**
   * Delete an access key.
   * 
   * @param session
   * @param keySerialized
   * @throws EucalyptusServiceException
   */
  void deleteAccessKey( Session session, SearchResultRow keySerialized ) throws EucalyptusServiceException;
  
  /**
   * Delete certificate.
   * 
   * @param session
   * @param certSerialized
   * @throws EucalyptusServiceException
   */
  void deleteCertificate( Session session, SearchResultRow certSerialized ) throws EucalyptusServiceException;
 
  /**
   * Add users to groups using user names input
   * 
   * @param session
   * @param userNames
   * @param groupIds
   * @throws EucalyptusServiceException
   */
  void addUsersToGroupsByName( Session session, String userNames, ArrayList<String> groupIds ) throws EucalyptusServiceException;
  
  /**
   * Add users to groups using group names input.
   * 
   * @param session
   * @param userIds
   * @param groupNames
   * @throws EucalyptusServiceException
   */
  void addUsersToGroupsById( Session session, ArrayList<String> userIds, String groupNames ) throws EucalyptusServiceException;
  
  /**
   * Remove users from groups using user names input.
   * 
   * @param session
   * @param userNames
   * @param groupIds
   * @throws EucalyptusServiceException
   */
  void removeUsersFromGroupsByName( Session session, String userNames, ArrayList<String> groupIds ) throws EucalyptusServiceException;
  
  /**
   * Remove users from groups using group names input.
   * 
   * @param session
   * @param userIds
   * @param groupNames
   * @throws EucalyptusServiceException
   */
  void removeUsersFromGroupsById( Session session, ArrayList<String> userIds, String groupNames ) throws EucalyptusServiceException;
  
  /**
   * Modify user info.
   * 
   * @param session
   * @param keys
   * @param values
   * @throws EucalyptusServiceException
   */
  void modifyUser( Session session, ArrayList<String> keys, ArrayList<String> values ) throws EucalyptusServiceException;
  
  /**
   * Modify group info.
   * 
   * @param session
   * @param values
   * @throws EucalyptusServiceException
   */
  void modifyGroup( Session session, ArrayList<String> values ) throws EucalyptusServiceException;
  
  /**
   * Modify access key info.
   * 
   * @param session
   * @param values
   * @throws EucalyptusServiceException
   */
  void modifyAccessKey( Session session, ArrayList<String> values ) throws EucalyptusServiceException;
  
  /**
   * Modify certificate info.
   * 
   * @param session
   * @param values
   * @throws EucalyptusServiceException
   */
  void modifyCertificate( Session session, ArrayList<String> values ) throws EucalyptusServiceException;

  /**
   * Add an access key to a user.
   * 
   * @param session
   * @param userId
   * @throws EucalyptusServiceException
   */
  void addAccessKey( Session session, String userId ) throws EucalyptusServiceException;
  
  /**
   * Add a certificate to a user.
   * 
   * @param session
   * @param userId
   * @throws EucalyptusServiceException
   */
  void addCertificate( Session session, String userId, String pem ) throws EucalyptusServiceException;
  
  /**
   * Change user password and/or email.
   * 
   * @param session
   * @param userId
   * @param oldPass
   * @param newPass
   * @param email
   */
  void changePassword( Session session, String userId, String oldPass, String newPass, String email ) throws EucalyptusServiceException;
  
  /**
   * Sign up a new account by user.
   * 
   * @param accountName
   * @param password
   * @param email
   * @throws EucalyptusServiceException
   */
  void signupAccount( String accountName, String password, String email ) throws EucalyptusServiceException;
 
  /**
   * Sign up a new user in an account.
   * 
   * @param userName
   * @param accountName
   * @param password
   * @param email
   * @throws EucalyptusServiceException
   */
  void signupUser( String userName, String accountName, String password, String email ) throws EucalyptusServiceException;
  
  /**
   * Approve account signups.
   * 
   * @param session
   * @param accountNames
   * @return
   * @throws EucalyptusServiceException
   */
  ArrayList<String> approveAccounts( Session session, ArrayList<String> accountNames ) throws EucalyptusServiceException;
  
  /**
   * Reject account signups.
   * 
   * @param session
   * @param accountNames
   * @return
   * @throws EucalyptusServiceException
   */
  ArrayList<String> rejectAccounts( Session session, ArrayList<String> accountNames ) throws EucalyptusServiceException;
  
  /**
   * Approve user signups.
   * 
   * @param session
   * @param userIds
   * @return
   * @throws EucalyptusServiceException
   */
  ArrayList<String> approveUsers( Session session, ArrayList<String> userIds ) throws EucalyptusServiceException;
  
  /**
   * Reject user signups.
   * 
   * @param session
   * @param userIds
   * @return
   * @throws EucalyptusServiceException
   */
  ArrayList<String> rejectUsers( Session session, ArrayList<String> userIds ) throws EucalyptusServiceException;
  
  /**
   * Confirm a user for both account signup (confirm the admin) and user signup.
   * 
   * @param confirmationCode
   * @throws EucalyptusServiceException
   */
  void confirmUser( String confirmationCode ) throws EucalyptusServiceException;
  
  /**
   * Request a reset of password.
   * 
   * @param userName
   * @param accountName
   * @param email
   * @throws EucalyptusServiceException
   */
  void requestPasswordRecovery( String userName, String accountName, String email ) throws EucalyptusServiceException;
  
  /**
   * Reset the password based on the confirmation code.
   * 
   * @param confirmationCode
   * @param password
   * @throws EucalyptusServiceException
   */
  void resetPassword( String confirmationCode, String password ) throws EucalyptusServiceException;
  
  /**
   * Get cloud info for RightScale registration.
   * 
   * @param session
   * @param setExternalHostport
   * @return
   * @throws EucalyptusServiceException
   */
  public CloudInfo getCloudInfo( Session session, boolean setExternalHostPort ) throws EucalyptusServiceException;
  
  /**
   * Get the list of image downloads.
   * 
   * @param session
   * @return
   * @throws EucalyptusServiceException
   */
  public ArrayList<DownloadInfo> getImageDownloads( Session session ) throws EucalyptusServiceException;
  
  /**
   * Get the list of tool downloads.
   * 
   * @param session
   * @return
   * @throws EucalyptusServiceException
   */
  public ArrayList<DownloadInfo> getToolDownloads( Session session ) throws EucalyptusServiceException;
  
  /**
   * Get Start Guide snippet.
   * 
   * @param session
   * @param snippet
   * @return
   * @throws EucalyptusServiceException
   */
  public ArrayList<GuideItem> getGuide( Session session, String snippet ) throws EucalyptusServiceException;
  
  /**
   * Get user's security code.
   * 
   * @param session
   * @return
   * @throws EucalyptusServiceException
   */
  public String getUserToken( Session session ) throws EucalyptusServiceException;
  
}
