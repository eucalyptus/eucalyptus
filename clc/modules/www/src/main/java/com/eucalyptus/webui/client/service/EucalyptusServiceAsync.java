package com.eucalyptus.webui.client.service;

import java.util.ArrayList;
import java.util.HashMap;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface EucalyptusServiceAsync {
  
  void login( String fullname, String password, AsyncCallback<Session> callback );

  void logout( Session session, AsyncCallback<Void> callback );
  
  void getLoginUserProfile( Session session, AsyncCallback<LoginUserProfile> callback );
  
  void getSystemProperties( Session session, AsyncCallback<HashMap<String, String>> callback );
  
  void getCategory( Session session, AsyncCallback<ArrayList<CategoryTag>> callback );
  
  void lookupAccount( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void lookupConfiguration( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );
  
}
