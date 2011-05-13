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
  
  LoginUserProfile getLoginUserProfile( Session session ) throws EucalyptusServiceException;
  
  HashMap<String, String> getSystemProperties( Session session ) throws EucalyptusServiceException;
  
  ArrayList<CategoryTag> getCategory( Session session ) throws EucalyptusServiceException;
 
  SearchResult lookupAccount( Session session, String search, SearchRange range ) throws EucalyptusServiceException;
  
}
