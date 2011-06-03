package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.webui.client.service.CategoryTag;
import com.eucalyptus.webui.client.service.CloudInfo;
import com.eucalyptus.webui.client.service.DownloadInfo;
import com.eucalyptus.webui.client.service.EucalyptusService;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.GuideItem;
import com.eucalyptus.webui.client.service.LoginUserProfile;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResult;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.service.Session;
import com.eucalyptus.webui.shared.query.QueryParser;
import com.eucalyptus.webui.shared.query.QueryParsingException;
import com.eucalyptus.webui.shared.query.QueryType;
import com.eucalyptus.webui.shared.query.SearchQuery;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import edu.ucsb.eucalyptus.admin.server.ServletUtils;

public class EucalyptusServiceImpl extends RemoteServiceServlet implements EucalyptusService {

  private static final Logger LOG = Logger.getLogger( EucalyptusServiceImpl.class );
  
  private static final long serialVersionUID = 1L;

  private static final String WHITESPACE_PATTERN = "\\s+";
    
  private static User verifySession( Session session ) throws EucalyptusServiceException {
    WebSession ws = WebSessionManager.getInstance( ).getSession( session.getId( ) );
    if ( ws == null ) {
      throw new EucalyptusServiceException( EucalyptusServiceException.INVALID_SESSION );
    }
    return EuareWebBackend.getUser( ws.getUserName( ), ws.getAccountName( ) );
  }
  
  private static SearchQuery parseQuery( QueryType type, String query ) throws EucalyptusServiceException {
    SearchQuery sq = null;
    try {
      sq = QueryParser.get( ).parse( type.name( ), query );
    } catch ( QueryParsingException e ) {
      throw new EucalyptusServiceException( "Invalid query: " + e );
    }
    return sq;
  }

  @Override
  public Session login( String fullName, String password ) throws EucalyptusServiceException {
    if ( fullName == null || password == null ) {
      throw new EucalyptusServiceException( "Empty user name or password" );
    }
    // Parse userId in the follow forms:
    // 1. "user@account"
    // 2. any of the parts is missing, using the default: "admin" for user and "eucalyptus" for account.
    //    So it could be "test" (test@eucalyptus) or "@test" (admin@test).
    String userName = User.ACCOUNT_ADMIN;
    String accountName = Account.SYSTEM_ACCOUNT;
    int at = fullName.indexOf( '@' );
    if ( at < 0 ) {
      userName = fullName;
    } else if ( at == 0 ) {
      accountName = fullName.substring( 1 );
    } else {
      userName = fullName.substring( 0, at );
      accountName = fullName.substring( at + 1 );
    }
    EuareWebBackend.checkPassword( EuareWebBackend.getUser( userName, accountName ), password );
    try { Thread.sleep( 500 ); } catch ( Exception e ) { } // Simple thwart to automatic login attack.
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
    return EuareWebBackend.getLoginUserProfile( user );
  }

  @Override
  public HashMap<String, String> getSystemProperties( Session session ) throws EucalyptusServiceException {
    verifySession( session );
    return WebProperties.getProperties( );
  }

  @Override
  public ArrayList<CategoryTag> getCategory( Session session ) throws EucalyptusServiceException {
    User user = verifySession( session );
    return Categories.getTags( user );
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
    SearchQuery searchQuery = parseQuery( QueryType.account, search );
    List<SearchResultRow> rows = EuareWebBackend.searchAccounts( searchQuery );
    SearchResult result = new SearchResult( rows.size( ), range );
    result.setDescs( EuareWebBackend.ACCOUNT_COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( rows, range ) );
    return result;
  }

  @Override
  public SearchResult lookupGroup( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    verifySession( session );
    SearchQuery searchQuery = parseQuery( QueryType.group, search );
    List<SearchResultRow> searchResult = EuareWebBackend.searchGroups( searchQuery );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.GROUP_COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupUser( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    verifySession( session );
    SearchQuery searchQuery = parseQuery( QueryType.user, search );
    List<SearchResultRow> searchResult = EuareWebBackend.searchUsers( searchQuery );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.USER_COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupPolicy( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    verifySession( session );
    SearchQuery searchQuery = parseQuery( QueryType.policy, search );
    List<SearchResultRow> searchResult = EuareWebBackend.searchPolicies( searchQuery );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.POLICY_COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupKey( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    verifySession( session );
    SearchQuery searchQuery = parseQuery( QueryType.key, search );
    List<SearchResultRow> searchResult = EuareWebBackend.searchKeys( searchQuery );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.KEY_COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupCertificate( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    verifySession( session );
    SearchQuery searchQuery = parseQuery( QueryType.cert, search );
    List<SearchResultRow> searchResult = EuareWebBackend.searchCerts( searchQuery );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( EuareWebBackend.CERT_COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public SearchResult lookupImage( Session session, String search, SearchRange range ) throws EucalyptusServiceException {
    verifySession( session );
    List<SearchResultRow> searchResult = ImageWebBackend.searchImages( search );
    SearchResult result = new SearchResult( searchResult.size( ), range );
    result.setDescs( ImageWebBackend.COMMON_FIELD_DESCS );
    result.setRows( SearchUtil.getRange( searchResult, range ) );
    return result;
  }

  @Override
  public String createAccount( Session session, String accountName ) throws EucalyptusServiceException {
    verifySession( session );
    return EuareWebBackend.createAccount( accountName );
  }

  @Override
  public void deleteAccounts( Session session, ArrayList<String> ids ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.deleteAccounts( ids );
  }

  @Override
  public void modifyAccount( Session session, ArrayList<String> values ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.modifyAccount( values );
  }

  @Override
  public ArrayList<String> createUsers( Session session, String accountId, String names, String path ) throws EucalyptusServiceException {
    verifySession( session );
    if ( Strings.isNullOrEmpty( names ) ) {
      throw new EucalyptusServiceException( "Invalid names for creating users: " + names );
    }
    ArrayList<String> users = Lists.newArrayList( );
    for ( String name : names.split( WHITESPACE_PATTERN ) ) {
      if ( !Strings.isNullOrEmpty( name ) ) {
        users.add( EuareWebBackend.createUser( accountId, name, path ) );
      }
    }
    return users;
  }

  @Override
  public ArrayList<String> createGroups( Session session, String accountId, String names, String path ) throws EucalyptusServiceException {
    verifySession( session );
    if ( Strings.isNullOrEmpty( names ) ) {
      throw new EucalyptusServiceException( "Invalid names for creating groups: " + names );
    }
    ArrayList<String> groups = Lists.newArrayList( );
    for ( String name : names.split( WHITESPACE_PATTERN ) ) {
      if ( !Strings.isNullOrEmpty( name ) ) {
        groups.add( EuareWebBackend.createGroup( accountId, name, path ) );
      }
    }
    return groups;    
  }

  @Override
  public void deleteUsers( Session session, ArrayList<String> ids ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.deleteUsers( ids );
  }

  @Override
  public void deleteGroups( Session session, ArrayList<String> ids ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.deleteGroups( ids );
  }

  @Override
  public void addAccountPolicy( Session session, String accountId, String name, String document ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.addAccountPolicy( accountId, name, document );
  }

  @Override
  public void addUserPolicy( Session session, String userId, String name, String document ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.addUserPolicy( userId, name, document );
  }

  @Override
  public void addGroupPolicy( Session session, String groupId, String name, String document ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.addGroupPolicy( groupId, name, document );
  }

  @Override
  public void deletePolicy( Session session, SearchResultRow policySerialized ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.deletePolicy( policySerialized );
  }

  @Override
  public void deleteAccessKey( Session session, SearchResultRow keySerialized ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.deleteAccessKey( keySerialized );
  }

  @Override
  public void deleteCertificate( Session session, SearchResultRow certSerialized ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.deleteCertificate( certSerialized );
  }

  @Override
  public void addUsersToGroupsByName( Session session, String userNames, ArrayList<String> groupIds ) throws EucalyptusServiceException {
    verifySession( session );
    if ( Strings.isNullOrEmpty( userNames ) || groupIds == null || groupIds.size( ) < 1 ) {
      throw new EucalyptusServiceException( "Empty names or empty group ids" );
    }
    for ( String groupId : groupIds ) {
      for ( String userName : userNames.split( WHITESPACE_PATTERN ) ) {
        if ( !Strings.isNullOrEmpty( userName ) && !Strings.isNullOrEmpty( groupId ) ) {
          EuareWebBackend.addUserToGroupByName( userName, groupId );
        }
      }
    }
  }

  @Override
  public void addUsersToGroupsById( Session session, ArrayList<String> userIds, String groupNames ) throws EucalyptusServiceException {
    verifySession( session );
    if ( Strings.isNullOrEmpty( groupNames ) || userIds == null || userIds.size( ) < 1 ) {
      throw new EucalyptusServiceException( "Empty names or empty group ids" );
    }
    for ( String userId : userIds ) {
      for ( String groupName : groupNames.split( WHITESPACE_PATTERN ) ) {
        EuareWebBackend.addUserToGroupById( userId, groupName );
      }
    }
  }

  @Override
  public void removeUsersFromGroupsByName( Session session, String userNames, ArrayList<String> groupIds ) throws EucalyptusServiceException {
    verifySession( session );
    if ( Strings.isNullOrEmpty( userNames ) || groupIds == null || groupIds.size( ) < 1 ) {
      throw new EucalyptusServiceException( "Empty names or empty group ids" );
    }
    for ( String groupId : groupIds ) {
      for ( String userName : userNames.split( WHITESPACE_PATTERN ) ) {
        if ( !Strings.isNullOrEmpty( userName ) && !Strings.isNullOrEmpty( groupId ) ) {
          EuareWebBackend.removeUserFromGroupByName( userName, groupId );
        }
      }
    }
  }

  @Override
  public void removeUsersFromGroupsById( Session session, ArrayList<String> userIds, String groupNames ) throws EucalyptusServiceException {
    verifySession( session );
    if ( Strings.isNullOrEmpty( groupNames ) || userIds == null || userIds.size( ) < 1 ) {
      throw new EucalyptusServiceException( "Empty names or empty group ids" );
    }
    for ( String userId : userIds ) {
      for ( String groupName : groupNames.split( WHITESPACE_PATTERN ) ) {
        EuareWebBackend.removeUserFromGroupById( userId, groupName );
      }
    }
  }

  @Override
  public void modifyUser( Session session, ArrayList<String> keys, ArrayList<String> values ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.modifyUser( keys, values );
  }

  @Override
  public void modifyGroup( Session session, ArrayList<String> values ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.modifyGroup( values );
  }

  @Override
  public void modifyAccessKey( Session session, ArrayList<String> values ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.modifyAccessKey( values );
  }

  @Override
  public void modifyCertificate( Session session, ArrayList<String> values ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.modifyCertificate( values );
  }

  @Override
  public void addAccessKey( Session session, String userId ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.addAccessKey( userId );
  }

  @Override
  public void addCertificate( Session session, String userId, String pem ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.addCertificate( userId, pem );
  }

  @Override
  public void changePassword( Session session, String userId, String oldPass, String newPass, String email ) throws EucalyptusServiceException {
    verifySession( session );
    EuareWebBackend.changeUserPassword( userId, oldPass, newPass, email );
  }

  @Override
  public void signupAccount( String accountName, String password, String email ) throws EucalyptusServiceException {
    User admin = EuareWebBackend.createAccount( accountName, password, email );
    EuareWebBackend.notifyAccountRegistration( admin, accountName, email, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
  }


  @Override
  public ArrayList<String> approveAccounts( Session session, ArrayList<String> accountNames ) throws EucalyptusServiceException {
    verifySession( session );
    return EuareWebBackend.processAccountSignups( accountNames, true, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
  }

  @Override
  public ArrayList<String> rejectAccounts( Session session, ArrayList<String> accountNames ) throws EucalyptusServiceException {
    verifySession( session );
    return EuareWebBackend.processAccountSignups( accountNames, false, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
  }

  @Override
  public ArrayList<String> approveUsers( Session session, ArrayList<String> userIds ) throws EucalyptusServiceException {
    verifySession( session );
    return EuareWebBackend.processUserSignups( userIds, true, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
  }

  @Override
  public ArrayList<String> rejectUsers( Session session, ArrayList<String> userIds ) throws EucalyptusServiceException {
    verifySession( session );
    return EuareWebBackend.processUserSignups( userIds, false, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
  }

  @Override
  public void signupUser( String userName, String accountName, String password, String email ) throws EucalyptusServiceException {
    User user = EuareWebBackend.createUser( userName, accountName, password, email );
    EuareWebBackend.notifyUserRegistration( user, accountName, email, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
  }

  @Override
  public void confirmUser( String confirmationCode ) throws EucalyptusServiceException {
    EuareWebBackend.confirmUser( confirmationCode );
  }

  @Override
  public void requestPasswordRecovery( String userName, String accountName, String email ) throws EucalyptusServiceException {
    EuareWebBackend.requestPasswordRecovery( userName, accountName, email, ServletUtils.getRequestUrl( getThreadLocalRequest( ) ) );
  }

  @Override
  public void resetPassword( String confirmationCode, String password ) throws EucalyptusServiceException {
    EuareWebBackend.resetPassword( confirmationCode, password );
  }

  @Override
  public CloudInfo getCloudInfo( Session session, boolean setExternalHostPort ) throws EucalyptusServiceException {
    verifySession( session );
    return ConfigurationWebBackend.getCloudInfo( setExternalHostPort );
  }

  @Override
  public ArrayList<DownloadInfo> getImageDownloads( Session session ) throws EucalyptusServiceException {
    verifySession( session );
    String version;
    try {
      version = UriUtils.encodeQuery( WebProperties.getVersion( ), "UTF-8" );
    } catch ( Exception e ) {
      version = WebProperties.getVersion( );
    }
    return DownloadsWebBackend.getDownloads( DownloadsWebBackend.IMAGE_DOWNLOAD_URL + version );
  }

  @Override
  public ArrayList<DownloadInfo> getToolDownloads( Session session ) throws EucalyptusServiceException {
    verifySession( session );
    String version;
    try {
      version = UriUtils.encodeQuery( WebProperties.getVersion( ), "UTF-8" );
    } catch ( Exception e ) {
      version = WebProperties.getVersion( );
    }
    return DownloadsWebBackend.getDownloads( DownloadsWebBackend.TOOL_DOWNLOAD_URL + version );
  }

  @Override
  public ArrayList<GuideItem> getGuide( Session session, String snippet ) throws EucalyptusServiceException {
    User user = verifySession( session );
    return StartGuideWebBackend.getGuide( user, snippet );
  }
    
}
