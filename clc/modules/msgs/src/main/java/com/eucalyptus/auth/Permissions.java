package com.eucalyptus.auth;

import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.PolicyEngine;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

public class Permissions {
  
  private static Logger LOG = Logger.getLogger( Permissions.class );
  
  private static PolicyEngine policyEngine;
  
  public static void setPolicyEngine( PolicyEngine engine ) {
    synchronized( Permissions.class ) {
      LOG.info( "Setting the policy engine to: " + engine.getClass( ) );
      policyEngine = engine;
    }
  }
  public static boolean isAuthorized( String vendor, String resourceType, String resourceName, Account resourceAccount, String action, User requestUser ) {
    Context context = null;
    try {
      context = Contexts.lookup( );
    } catch ( IllegalContextAccessException e ) {
      LOG.debug( "Not in a request context", e );      
    }
    try {
      // If we are not in a request context, e.g. the UI, use a dummy contract map.
      // TODO(wenye): we should consider how to handle this if we allow the EC2 operations in the UI.
      Map<Contract.Type, Contract> contracts = context != null ? context.getContracts( ) : new HashMap<Contract.Type, Contract>( );
      policyEngine.evaluateAuthorization( vendor + ":" + resourceType, resourceName, resourceAccount, action, requestUser, contracts );
      return true;
    } catch ( AuthException e ) {
      LOG.error( "Denied resource access to " + resourceType + ":" + resourceName + " of " + resourceAccount.getName( ) + " for " + requestUser.getName( ), e );
    } catch ( Throwable e ) {
      LOG.debug( "Exception in resource access to " + resourceType + ":" + resourceName + " of " + resourceAccount.getName( ) + " for " + requestUser.getName( ), e );      
    }
    return false;
  }
  
  public static boolean canAllocate( String vendor, String resourceType, String resourceName, String action, User requestUser, Long quantity ) {
    try {
      policyEngine.evaluateQuota( vendor + ":" + resourceType, resourceName, action, requestUser, quantity );
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
