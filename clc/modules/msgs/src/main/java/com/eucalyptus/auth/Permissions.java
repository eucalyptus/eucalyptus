package com.eucalyptus.auth;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.PolicyEngine;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.util.EucalyptusCloudException;

public class Permissions {
  
  private static Logger LOG = Logger.getLogger( Permissions.class );
  
  private static PolicyEngine policyEngine;
  
  public static void setPolicyEngine( PolicyEngine engine ) {
    synchronized( Permissions.class ) {
      LOG.info( "Setting the policy engine to: " + engine.getClass( ) );
      policyEngine = engine;
    }
  }
  public static boolean isAuthorized( String resourceType, String resourceName, Account resourceAccount, String action, User requestUser ) {
    Context context = null;
    try {
      context = Contexts.lookup( );
      context.getContracts( ).putAll(
          policyEngine.evaluateAuthorization( resourceType, resourceName, resourceAccount, action, requestUser ) );
      return true;
    } catch ( IllegalContextAccessException e ) {
      LOG.debug( "Exception trying to identify the current request context requesting resource access to " + resourceType + ":" + resourceName + " of " + resourceAccount.getName( ) + " for " + requestUser.getName( ), e );      
    } catch ( AuthException e ) {
      LOG.error( "Denied resource access to " + resourceType + ":" + resourceName + " of " + resourceAccount.getName( ) + " for " + requestUser.getName( ), e );
    } catch ( Throwable e ) {
      LOG.debug( "Exception in resource access to " + resourceType + ":" + resourceName + " of " + resourceAccount.getName( ) + " for " + requestUser.getName( ), e );      
    }
    return false;
  }
  
  public static boolean canAllocate( String resourceType, String resourceName, String action, User requestUser, Integer quantity ) {
    try {
      policyEngine.evaluateQuota( resourceType, resourceName, action, requestUser, quantity );
      return true;
    } catch ( AuthException e ) {
      LOG.debug( "Denied resource allocation of " + resourceType + ":" + resourceName + " by " + quantity + " for " + requestUser.getName( ), e );
    }
    return false;
  }
  
  public static User getUserById( String userId ) throws EucalyptusCloudException {
    try {
      return Accounts.lookupUserById( userId );
    } catch ( Throwable t ) {
      throw new EucalyptusCloudException( t );
    }
  }
  
  public static Account getAccountByUserId( String userId ) throws EucalyptusCloudException {
    try {
      return Accounts.lookupUserById( userId ).getAccount( );
    } catch ( Throwable t ) {
      throw new EucalyptusCloudException( t );
    }
  }
  
  public static Account getUserAccount( User user ) throws EucalyptusCloudException {
    try {
      return user.getAccount( );
    } catch ( Throwable t ) {
      throw new EucalyptusCloudException( t );
    }
  }
  
}
