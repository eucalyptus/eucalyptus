package com.eucalyptus.webui.server;

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
  
  public static List<CategoryTag> getTags( User login ) throws EucalyptusServiceException {
    try {
      String accountId = login.getAccount( ).getAccountNumber( );
      String userId = login.getUserId( );
      return Arrays.asList( new CategoryTag( "System", 
                                             Arrays.asList( new CategoryItem( "Start", "Start guide", "home",
                                                                              QueryBuilder.get( ).start( QueryType.start ).query( ) ),
                                                            new CategoryItem( "Service Components", "Configuration of service components", "config",
                                                            		              QueryBuilder.get( ).start( QueryType.config ).query( ) ) ) ),
                            new CategoryTag( "Identity",
                                             Arrays.asList( new CategoryItem( "Account", "Accounts", "dollar", 
                                                                              QueryBuilder.get( ).start( QueryType.account ).query( ) ),
                                                            new CategoryItem( "Group", "User groups", "group",
                                                                              QueryBuilder.get( ).start( QueryType.group ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                                            new CategoryItem( "User", "Users", "user",
                                                                              QueryBuilder.get( ).start( QueryType.user ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                                            new CategoryItem( "Policy", "Policies", "lock",
                                                                              QueryBuilder.get( ).start( QueryType.policy ).add( EuareWebBackend.USERID, userId ).query( ) ),
                                                            new CategoryItem( "Key", "Access keys", "key",
                                                                              QueryBuilder.get( ).start( QueryType.key ).add( EuareWebBackend.USERID, userId ).query( ) ),
                                                            new CategoryItem( "Certificate", "X509 certificates", "sun",
                                                                              QueryBuilder.get( ).start( QueryType.cert ).add( EuareWebBackend.USERID, userId ).query( ) ) ) ),
                            new CategoryTag( "Resource",
                                             Arrays.asList( new CategoryItem( "Image", "Virtual machine images (EMIs)", "image",
                                                                              QueryBuilder.get( ).start( QueryType.image ).query( ) ),
                                                            new CategoryItem( "VmType", "Virtual machine types", "type",
                                                                              QueryBuilder.get( ).start( QueryType.vmtype ).query( ) ),
                                                            new CategoryItem( "Report", "Resource usage report", "report",
                                                                              QueryBuilder.get( ).start( QueryType.report ).query( ) ) ) ),
                            new CategoryTag( "Extras",
                                             Arrays.asList( new CategoryItem( "Downloads", "Extra downloads", "down",
                                                                              QueryBuilder.get( ).start( QueryType.downloads ).query( ) ),
                                                            new CategoryItem( "RightScale", "Register RightScale", "rightscale",
                                                                              QueryBuilder.get( ).start( QueryType.rightscale ).query( ) ) ) ) );
    } catch ( Exception e ) {
      LOG.error( "Failed to load user information", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to load user information for " + login );
    }
  }
  
}
