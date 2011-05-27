package com.eucalyptus.webui.client.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface EucalyptusServiceAsync {
  
  void login( String fullname, String password, AsyncCallback<Session> callback );

  void logout( Session session, AsyncCallback<Void> callback );
  
  void getLoginUserProfile( Session session, AsyncCallback<LoginUserProfile> callback );
  
  void getSystemProperties( Session session, AsyncCallback<HashMap<String, String>> callback );
  
  void getCategory( Session session, AsyncCallback<List<CategoryTag>> callback );
  
  void lookupAccount( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void lookupConfiguration( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void setConfiguration( Session session, SearchResultRow config, AsyncCallback<Void> callback );

  void lookupVmType( Session session, String query, SearchRange range, AsyncCallback<SearchResult> asyncCallback );

  void setVmType( Session session, SearchResultRow result, AsyncCallback<Void> asyncCallback );

  void lookupGroup( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void lookupUser( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void lookupPolicy( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void lookupKey( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void lookupCertificate( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void lookupImage( Session session, String search, SearchRange range, AsyncCallback<SearchResult> callback );

  void createAccount( Session session, String accountName, AsyncCallback<String> callback );

  void deleteAccounts( Session session, ArrayList<String> ids, AsyncCallback<Void> callback );
  
}
