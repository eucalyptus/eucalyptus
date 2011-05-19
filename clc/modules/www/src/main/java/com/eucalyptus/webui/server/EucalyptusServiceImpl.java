package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.webui.client.service.CategoryItem;
import com.eucalyptus.webui.client.service.CategoryTag;
import com.eucalyptus.webui.client.service.EucalyptusService;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.LoginUserProfile;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResult;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.service.Session;
import com.google.common.collect.Lists;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class EucalyptusServiceImpl extends RemoteServiceServlet implements EucalyptusService {

  private static final Logger LOG = Logger.getLogger( EucalyptusServiceImpl.class );
  
  private static final long serialVersionUID = 1L;
  
  private static User verifySession( Session session ) throws EucalyptusServiceException {
    WebSession ws = WebSessionManager.getInstance( ).getSession( session.getId( ) );
    if ( ws == null ) {
      throw new EucalyptusServiceException( EucalyptusServiceException.INVALID_SESSION );
    }
    return EuareWebBackend.getUser( ws.getUserName( ), ws.getAccountName( ) );
  }
  
  @Override
  public Session login( String fullname, String password ) throws EucalyptusServiceException {
    if ( fullname == null || password == null ) {
      throw new EucalyptusServiceException( "Empty user name or password" );
    }
    // Parse userId in the follow forms:
    // 1. "user" (meaning "user" in "eucalyptus" account)
    // 2. "user@account"
    String[] splits = fullname.split( "@", 2 );
    String userName = splits[0];
    String accountName = Account.SYSTEM_ACCOUNT;
    if ( splits.length > 1 ) {
      accountName = splits[1];
    }
    EuareWebBackend.checkPassword( EuareWebBackend.getUser( userName, accountName ), password );
    return new Session( WebSessionManager.getInstance( ).newSession( userName, accountName ) );
  }


  @Override
  public void logout( Session session ) throws EucalyptusServiceException {
    verifySession( session );
    WebSessionManager.getInstance( ).removeSession( session.getId( ) );
  }

  @Override
  public LoginUserProfile getLoginUserProfile( Session session ) throws EucalyptusServiceException {
    User user = verifySession( session );
    try {
      return new LoginUserProfile( user.getName( ), user.getAccount( ).getName( ) );
    } catch ( Exception e ) {
      throw new EucalyptusServiceException( "Failed to retrieve user profile" );
    }
  }

  @Override
  public HashMap<String, String> getSystemProperties( Session session ) throws EucalyptusServiceException {
    verifySession( session );
    return WebProperties.getProperties( );
  }

  @Override
  public ArrayList<CategoryTag> getCategory( Session session ) throws EucalyptusServiceException {
    verifySession( session );
    return Categories.getTags( );
  }
  
  @Override
  public SearchResult lookupConfiguration( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    verifySession( session );
    SearchResult result = new SearchResult( );
    result.setDescs( ConfigurationWebBackend.COMMON_FIELD_DESCS );
    result.addRow( ConfigurationWebBackend.getCloudConfiguration( ) );
    result.addRows( ConfigurationWebBackend.getClusterConfigurations( ) );
    result.addRows( ConfigurationWebBackend.getStorageConfiguration( ) );
    result.addRows( ConfigurationWebBackend.getWalrusConfiguration( ) );
    result.setTotalSize( result.length( ) );
    result.setRange( range );
    LOG.debug( "Configuration result: " + result );
    return result;
  }
  
  @Override
  public void setConfiguration( Session session, SearchResultRow config ) throws EucalyptusServiceException {
    verifySession( session );
    if ( config == null ) {
      throw new EucalyptusServiceException( "Empty config to save" );
    }
    String type = config.getField( ConfigurationWebBackend.TYPE_FIELD_INDEX );
    if ( type == null ) {
      throw new EucalyptusServiceException( "Empty configuration type" );
    }
    LOG.debug( "Set: " + config );
    if ( ConfigurationWebBackend.CLOUD_TYPE.equals( type ) ) {
      ConfigurationWebBackend.setCloudConfiguration( config );
    } else if ( ConfigurationWebBackend.CLUSTER_TYPE.equals( type ) ) {
      ConfigurationWebBackend.setClusterConfiguration( config );
    } else if ( ConfigurationWebBackend.STORAGE_TYPE.equals( type ) ) {
      ConfigurationWebBackend.setStorageConfiguration( config );
    } else if ( ConfigurationWebBackend.WALRUS_TYPE.equals( type ) ) {
      ConfigurationWebBackend.setWalrusConfiguration( config );
    } else {
      throw new EucalyptusServiceException( "Wrong configuration type: " + type );
    }
  }

  @Override
  public SearchResult lookupVmType( Session session, String query, SearchRange range ) throws EucalyptusServiceException {
    verifySession( session );
    SearchResult result = new SearchResult( );
    result.setDescs( VmTypeWebBackend.COMMON_FIELD_DESCS );
    result.addRows( VmTypeWebBackend.getVmTypes( ) );
    result.setTotalSize( result.length( ) );
    result.setRange( range );
    LOG.debug( "VmType result: " + result );
    return result;
  }

  @Override
  public void setVmType( Session session, SearchResultRow vmType ) throws EucalyptusServiceException {
    verifySession( session );
    if ( vmType == null ) {
      throw new EucalyptusServiceException( "Empty UI input for VmType" );
    }
    LOG.debug( "Set VmType: " + vmType );
    VmTypeWebBackend.setVmType( vmType );
  }

  
  @Override
  public SearchResult lookupAccount( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    verifySession( session );    
    List<SearchResultRow> searchResult = EuareWebBackend.searchAccounts( search );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.ACCOUNT_COMMON_FIELD_DESCS );
    result.setRows( SearchRangeUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupGroup( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    User user = verifySession( session );
    List<SearchResultRow> searchResult = EuareWebBackend.searchGroups( user, search );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.GROUP_COMMON_FIELD_DESCS );
    result.setRows( SearchRangeUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupUser( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    User user = verifySession( session );
    List<SearchResultRow> searchResult = EuareWebBackend.searchUsers( user, search );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.USER_COMMON_FIELD_DESCS );
    result.setRows( SearchRangeUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupPolicy( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    User user = verifySession( session );
    List<SearchResultRow> searchResult = EuareWebBackend.searchPolicies( user, search );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.POLICY_COMMON_FIELD_DESCS );
    result.setRows( SearchRangeUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupKey( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    User user = verifySession( session );
    List<SearchResultRow> searchResult = EuareWebBackend.searchKeys( user, search );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.KEY_COMMON_FIELD_DESCS );
    result.setRows( SearchRangeUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupCertificate( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    User user = verifySession( session );
    List<SearchResultRow> searchResult = EuareWebBackend.searchCerts( user, search );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.CERT_COMMON_FIELD_DESCS );
    result.setRows( SearchRangeUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupImage( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    verifySession( session );
    List<SearchResultRow> searchResult = ImageWebBackend.searchImages( search );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( ImageWebBackend.COMMON_FIELD_DESCS );
    result.setRows( SearchRangeUtil.getRange( searchResult, range ) );
    return result;
  }
  
}
