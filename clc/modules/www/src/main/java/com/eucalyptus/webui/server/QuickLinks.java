package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.webui.client.service.QuickLink;
import com.eucalyptus.webui.client.service.QuickLinkTag;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.shared.query.QueryType;

public class QuickLinks {
  
  private static final Logger LOG = Logger.getLogger( QuickLinks.class );
  
  public static ArrayList<QuickLinkTag> getTags( User login ) throws EucalyptusServiceException {
    if ( login.isSystemAdmin( ) ) {
      return getSystemAdminTags( login );
    } else if ( login.isAccountAdmin() ) {
        return getAccountAdminTags( login );
    } else {
      return getUserTags( login );
    }
  }
  
  private static ArrayList<QuickLinkTag> getSystemAdminTags( User login ) throws EucalyptusServiceException {
    try {
      String accountId = login.getAccount( ).getAccountNumber( );
      String userId = login.getUserId( );
      return new ArrayList<QuickLinkTag>( Arrays.asList( 
              new QuickLinkTag( "System Management", 
                               new ArrayList<QuickLink>( Arrays.asList(
                                              new QuickLink( "Start", "Start guide", "home",
                                                                QueryBuilder.get( ).start( QueryType.start ).query( ) ),
                                              new QuickLink( "Service Components", "Configuration of service components", "config",
                                                                QueryBuilder.get( ).start( QueryType.config ).query( ) ) ) ) ),
              new QuickLinkTag( "Identity Management",
                               new ArrayList<QuickLink>( Arrays.asList(
                                              new QuickLink( "Accounts", "Accounts", "dollar", 
                                                                QueryBuilder.get( ).start( QueryType.account ).query( ) ),
                                              new QuickLink( "Groups", "User groups", "group",
                                                                QueryBuilder.get( ).start( QueryType.group ).query( ) ),
                                              new QuickLink( "Users", "Users", "user",
                                                                QueryBuilder.get( ).start( QueryType.user ).query( ) ),
                                              new QuickLink( "Policies", "Policies", "lock",
                                                                QueryBuilder.get( ).start( QueryType.policy ).query( ) ),
                                              new QuickLink( "Keys", "Access keys", "key",
                                                                QueryBuilder.get( ).start( QueryType.key ).query( ) ),
                                              new QuickLink( "Certificates", "X509 certificates", "sun",
                                                                QueryBuilder.get( ).start( QueryType.cert ).query( ) ) ) ) ),
              new QuickLinkTag( "Resource Management",
                               new ArrayList<QuickLink>( Arrays.asList(
                                              new QuickLink( "Images", "Virtual machine images (EMIs)", "image",
                                                                QueryBuilder.get( ).start( QueryType.image ).query( ) ),
                                              new QuickLink( "VmTypes", "Virtual machine types", "type",
                                                                QueryBuilder.get( ).start( QueryType.vmtype ).query( ) ),
                                              new QuickLink( "Usage Report", "Resource usage report", "report",
                                                                QueryBuilder.get( ).start( QueryType.report ).query( ) ) ) ) ) ) );
    } catch ( Exception e ) {
      LOG.error( "Failed to load user information", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to load user information for " + login );
    }    
  }

  private static ArrayList<QuickLinkTag> getAccountAdminTags( User login ) throws EucalyptusServiceException {
	    try {
	      String accountId = login.getAccount( ).getAccountNumber( );
	      String userId = login.getUserId( );
	      return new ArrayList<QuickLinkTag>( Arrays.asList( 
	              new QuickLinkTag( "System Management", 
	                               new ArrayList<QuickLink>( Arrays.asList(
	                                              new QuickLink( "Start", "Start guide", "home",
	                                                                QueryBuilder.get( ).start( QueryType.start ).query( ) ) ) ) ),
	              new QuickLinkTag( "Identity Management",
	                               new ArrayList<QuickLink>( Arrays.asList(
	                                              new QuickLink( "Accounts", "Accounts", "dollar", 
	                                                                QueryBuilder.get( ).start( QueryType.account ).query( ) ),
	                                              new QuickLink( "Groups", "User groups", "group",
	                                                                QueryBuilder.get( ).start( QueryType.group ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
	                                              new QuickLink( "Users", "Users", "user",
	                                                                QueryBuilder.get( ).start( QueryType.user ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
	                                              new QuickLink( "Policies", "Policies", "lock",
	                                                                QueryBuilder.get( ).start( QueryType.policy ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
	                                              new QuickLink( "Keys", "Access keys", "key",
	                                                                QueryBuilder.get( ).start( QueryType.key ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
	                                              new QuickLink( "Certificates", "X509 certificates", "sun",
	                                                                QueryBuilder.get( ).start( QueryType.cert ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ) ) ) ),
	              new QuickLinkTag( "Resource Management",
                                 new ArrayList<QuickLink>( Arrays.asList(
                                 new QuickLink( "Images", "Virtual machine images (EMIs)", "image",
                                                   QueryBuilder.get( ).start( QueryType.image ).query( ) ),
                                 new QuickLink( "Usage Report", "Resource usage report", "report",
                                                   QueryBuilder.get( ).start( QueryType.report ).query( ) ) ) ) ) ) );

	    } catch ( Exception e ) {
	      LOG.error( "Failed to load user information", e );
	      LOG.debug( e, e );
	      throw new EucalyptusServiceException( "Failed to load user information for " + login );
	    }    
	  }

  private static ArrayList<QuickLinkTag> getUserTags( User login ) throws EucalyptusServiceException {
    try {
      String accountId = login.getAccount( ).getAccountNumber( );
      String userId = login.getUserId( );
      return new ArrayList<QuickLinkTag>( Arrays.asList( 
              new QuickLinkTag( "System Management", 
                               new ArrayList<QuickLink>( Arrays.asList(
                                              new QuickLink( "Start", "Start guide", "home",
                                                                QueryBuilder.get( ).start( QueryType.start ).query( ) ) ) ) ),
              new QuickLinkTag( "Identity Management",
                               new ArrayList<QuickLink>( Arrays.asList(
                                              new QuickLink( "Accounts", "Accounts", "dollar", 
                                                                QueryBuilder.get( ).start( QueryType.account ).query( ) ),
                                              new QuickLink( "Groups", "User groups", "group",
                                                                QueryBuilder.get( ).start( QueryType.group ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                              new QuickLink( "Users", "Users", "user",
                                                                QueryBuilder.get( ).start( QueryType.user ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                              new QuickLink( "Policies", "Policies", "lock",
                                                                QueryBuilder.get( ).start( QueryType.policy ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                              new QuickLink( "Keys", "Access keys", "key",
                                                                QueryBuilder.get( ).start( QueryType.key ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ),
                                              new QuickLink( "Certificates", "X509 certificates", "sun",
                                                                QueryBuilder.get( ).start( QueryType.cert ).add( EuareWebBackend.ACCOUNTID, accountId ).query( ) ) ) ) ),
              new QuickLinkTag( "Resource Management",
                               new ArrayList<QuickLink>( Arrays.asList(
                                              new QuickLink( "Images", "Virtual machine images (EMIs)", "image",
                                                                QueryBuilder.get( ).start( QueryType.image ).query( ) ) ) ) ) ) );
    } catch ( Exception e ) {
      LOG.error( "Failed to load user information", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to load user information for " + login );
    }    
  }

}
