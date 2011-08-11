package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.webui.client.service.CategoryItem;
import com.eucalyptus.webui.client.service.CategoryTag;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.shared.query.QueryType;

public class Categories {
  
  private static final Logger LOG = Logger.getLogger( Categories.class );
  
  public static ArrayList<CategoryTag> getTags( User login ) throws EucalyptusServiceException {
    if ( login.isSystemAdmin( ) ) {
      return getSystemAdminTags( login );
    } else {
      return getUserTags( login );
    }
  }
  
  private static ArrayList<CategoryTag> getSystemAdminTags( User login ) throws EucalyptusServiceException {
    try {
      String accountId = login.getAccount( ).getAccountNumber( );
      String userId = login.getUserId( );
      return new ArrayList<CategoryTag>( Arrays.asList( 
              new CategoryTag( "System Management", 
                               new ArrayList<CategoryItem>( Arrays.asList(
                                              new CategoryItem( "Start", "Start guide", "home",
                                                                QueryBuilder.get( ).start( QueryType.start ).query( ) ),
                                              new CategoryItem( "Service Components", "Configuration of service components", "config",
                                                                QueryBuilder.get( ).start( QueryType.config ).query( ) ) ) ) ),
              new CategoryTag( "Identity Management",
                               new ArrayList<CategoryItem>( Arrays.asList(
                                              new CategoryItem( "All Accounts", "Accounts", "dollar", 
                                                                QueryBuilder.get( ).start( QueryType.account ).query( ) ),
                                              new CategoryItem( "Your Account's Groups", "User groups", "group",
                                                                QueryBuilder.get( ).start( QueryType.group ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                              new CategoryItem( "Your Account's Users", "Users", "user",
                                                                QueryBuilder.get( ).start( QueryType.user ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                              new CategoryItem( "Your Policies", "Policies", "lock",
                                                                QueryBuilder.get( ).start( QueryType.policy ).add( EuareWebBackend.USERID, userId ).query( ) ),
                                              new CategoryItem( "Your Keys", "Access keys", "key",
                                                                QueryBuilder.get( ).start( QueryType.key ).add( EuareWebBackend.USERID, userId ).query( ) ),
                                              new CategoryItem( "Your Certificates", "X509 certificates", "sun",
                                                                QueryBuilder.get( ).start( QueryType.cert ).add( EuareWebBackend.USERID, userId ).query( ) ) ) ) ),
              new CategoryTag( "Resource Management",
                               new ArrayList<CategoryItem>( Arrays.asList(
                                              new CategoryItem( "Images", "Virtual machine images (EMIs)", "image",
                                                                QueryBuilder.get( ).start( QueryType.image ).query( ) ),
                                              new CategoryItem( "VmTypes", "Virtual machine types", "type",
                                                                QueryBuilder.get( ).start( QueryType.vmtype ).query( ) ),
                                              new CategoryItem( "Usage Report", "Resource usage report", "report",
                                                                QueryBuilder.get( ).start( QueryType.report ).query( ) ) ) ) ) ) );
    } catch ( Exception e ) {
      LOG.error( "Failed to load user information", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to load user information for " + login );
    }    
  }

  private static ArrayList<CategoryTag> getUserTags( User login ) throws EucalyptusServiceException {
    try {
      String accountId = login.getAccount( ).getAccountNumber( );
      String userId = login.getUserId( );
      return new ArrayList<CategoryTag>( Arrays.asList( 
              new CategoryTag( "System Management", 
                               new ArrayList<CategoryItem>( Arrays.asList(
                                              new CategoryItem( "Start", "Start guide", "home",
                                                                QueryBuilder.get( ).start( QueryType.start ).query( ) ) ) ) ),
              new CategoryTag( "Identity Management",
                               new ArrayList<CategoryItem>( Arrays.asList(
                                              new CategoryItem( "All Accounts", "Accounts", "dollar", 
                                                                QueryBuilder.get( ).start( QueryType.account ).query( ) ),
                                              new CategoryItem( "Your Account's Groups", "User groups", "group",
                                                                QueryBuilder.get( ).start( QueryType.group ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                              new CategoryItem( "Your Account's Users", "Users", "user",
                                                                QueryBuilder.get( ).start( QueryType.user ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                              new CategoryItem( "Your Policies", "Policies", "lock",
                                                                QueryBuilder.get( ).start( QueryType.policy ).add( EuareWebBackend.USERID, userId ).query( ) ),
                                              new CategoryItem( "Your Keys", "Access keys", "key",
                                                                QueryBuilder.get( ).start( QueryType.key ).add( EuareWebBackend.USERID, userId ).query( ) ),
                                              new CategoryItem( "Your Certificates", "X509 certificates", "sun",
                                                                QueryBuilder.get( ).start( QueryType.cert ).add( EuareWebBackend.USERID, userId ).query( ) ) ) ) ),
              new CategoryTag( "Resource Management",
                               new ArrayList<CategoryItem>( Arrays.asList(
                                              new CategoryItem( "Images", "Virtual machine images (EMIs)", "image",
                                                                QueryBuilder.get( ).start( QueryType.image ).query( ) ) ) ) ) ) );
    } catch ( Exception e ) {
      LOG.error( "Failed to load user information", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to load user information for " + login );
    }    
  }

}
