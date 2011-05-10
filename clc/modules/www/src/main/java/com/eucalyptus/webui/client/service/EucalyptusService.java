package com.eucalyptus.webui.client.service;

import java.util.ArrayList;
import java.util.HashMap;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("backend")
public interface EucalyptusService extends RemoteService {
  
  Session login( String fullname, String password ) throws EucalyptusServiceException;
  
  LoginUserProfile getLoginUserProfile( Session session ) throws EucalyptusServiceException;
  
  HashMap<String, String> getSystemProperties( Session session ) throws EucalyptusServiceException;
  
  ArrayList<CategoryTag> getCategory( Session session ) throws EucalyptusServiceException;
  
  SearchResult lookupServiceComponents( Session session ) throws EucalyptusServiceException;
  
}
