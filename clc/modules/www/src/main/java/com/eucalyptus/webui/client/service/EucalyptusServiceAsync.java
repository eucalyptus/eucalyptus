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

  void modifyAccount( Session session, ArrayList<String> values, AsyncCallback<Void> callback );

  void createUsers( Session session, String accountId, String names, String path, AsyncCallback<ArrayList<String>> callback );

  void createGroups( Session session, String accountId, String names, String path, AsyncCallback<ArrayList<String>> callback );

  void deleteUsers( Session session, ArrayList<String> ids, AsyncCallback<Void> callback );

  void deleteGroups( Session session, ArrayList<String> ids, AsyncCallback<Void> callback );

  void deletePolicy( Session session, SearchResultRow policySerialized, AsyncCallback<Void> callback );

  void deleteAccessKey( Session session, SearchResultRow keySerialized, AsyncCallback<Void> callback );

  void deleteCertificate( Session session, SearchResultRow certSerialized, AsyncCallback<Void> callback );

  void addAccountPolicy( Session session, String accountId, String name, String document, AsyncCallback<Void> callback );

  void addUserPolicy( Session session, String usertId, String name, String document, AsyncCallback<Void> callback );

  void addGroupPolicy( Session session, String groupId, String name, String document, AsyncCallback<Void> callback );

  void addUsersToGroupsByName( Session session, String userNames, ArrayList<String> groupIds, AsyncCallback<Void> callback );

  void addUsersToGroupsById( Session session, ArrayList<String> userIds, String groupNames, AsyncCallback<Void> callback );

  void removeUsersFromGroupsByName( Session session, String userNames, ArrayList<String> groupIds, AsyncCallback<Void> callback );

  void removeUsersFromGroupsById( Session session, ArrayList<String> userIds, String groupNames, AsyncCallback<Void> callback );

  void modifyUser( Session session, ArrayList<String> keys, ArrayList<String> values, AsyncCallback<Void> callback );

  void modifyGroup( Session session, ArrayList<String> values, AsyncCallback<Void> callback );

  void modifyAccessKey( Session session, ArrayList<String> values, AsyncCallback<Void> callback );

  void modifyCertificate( Session session, ArrayList<String> values, AsyncCallback<Void> callback );

  void addAccessKey( Session session, String userId, AsyncCallback<Void> callback );

  void addCertificate( Session session, String userId, String pem, AsyncCallback<Void> callback );
  
}
