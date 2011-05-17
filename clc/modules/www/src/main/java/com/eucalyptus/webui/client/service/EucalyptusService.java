package com.eucalyptus.webui.client.service;

import java.util.ArrayList;
import java.util.HashMap;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("backend")
public interface EucalyptusService extends RemoteService {
  
  /**
   * User signs in by name and password.
   * 
   * @param fullname
   * @param password
   * @return
   * @throws EucalyptusServiceException
   */
  Session login( String fullname, String password ) throws EucalyptusServiceException;
  
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
   * Get category tree data.
   * 
   * @param session
   * @return
   * @throws EucalyptusServiceException
   */
  ArrayList<CategoryTag> getCategory( Session session ) throws EucalyptusServiceException;
 
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
   * @param query
   * @param range
   * @return
   * @throws EucalyptusServiceException
   */
  SearchResult lookupVmType( Session session, String query, SearchRange range ) throws EucalyptusServiceException;

  /**
   * Set VmType values.
   * 
   * @param session
   * @param result
   * @throws EucalyptusServiceException
   */
  void setVmType( Session session, SearchResultRow result ) throws EucalyptusServiceException;
  
}
