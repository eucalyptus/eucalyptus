package com.eucalyptus.auth;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.PolicyEngine;
import com.eucalyptus.component.ResourceAllocationException;
import com.eucalyptus.component.ResourceAllocate;
import com.eucalyptus.component.ResourceLookup;
import com.eucalyptus.component.ResourceLookupException;
import com.eucalyptus.component.ResourceOwnerLookup;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;

/**
 * The service interface for doing policy based access control. There are two APIs:
 * 
 * lookupPrivileged: perform a restricted resource lookup (based on authorizations)
 * allocatePrivileged: perform a restricted resource allocation (based on quotas)
 * 
 * @author wenye
 *
 */
public class Authorizations {
  
  private static Logger LOG = Logger.getLogger( Authorizations.class );
   
  private static PolicyEngine policyEngine;
  
  public static void setPolicyEngine( PolicyEngine engine ) {
    synchronized( Authorizations.class ) {
      LOG.info( "Setting the policy engine to: " + engine.getClass( ) );
      policyEngine = engine;
    }
  }
  

  /**
   * Check permissions and resolve resource.
   * 
   * @param <T> The type of the resource object.
   * @param resourceType The class type of the resource object.
   * @param resourceName The optional resource name for allocation, e.g. the bucket to allocate object.
   * @param ownerResolver Resolver to get the owner of the resource.
   * @param resolver The actual resource resolution code to call.
   * @return The resource object.
   * @throws AuthException if permission to access the resource is denied.
   * @throws ResourceLookupException for any error in resource lookup.
   */
  public static <T> T lookupPrivileged( Class<T> resourceType, String resourceName, ResourceOwnerLookup<T> ownerResolver, ResourceLookup<T> resolver ) throws AuthException, ResourceLookupException {
    T resource;
    try {
      resource = resolver.resolve( resourceName );
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
      throw new ResourceLookupException( "Failure occurred while trying to resolve a resource using: "
                                         + resolver.getClass( ).getCanonicalName( ), ex );
    }
    Context context = Contexts.lookup( );
    String accountId = ownerResolver == null ? null : ownerResolver.getOwningAccountId( resource );
    context.getContracts( ).putAll(
        policyEngine.evaluateAuthorization( resourceType, resourceName, accountId, context.getRequest( ), context.getUser( ) ) );
    return resource;
  }
  

  /**
   * Check quota and allocate resource.
   * 
   * @param <T> The type of the resource object.
   * @param resourceType The class type of the resource object.
   * @param resourceName The optional resource name for allocation, e.g. the bucket to allocate object.
   * @param quantity The amount of the resource to allocate.
   * @param allocator The actual allocation code to call.
   * @throws AuthException if quota's exceeded.
   * @throws ResourceAllocationException for any resource allocation error.
   */
  public static <T> void allocatePrivileged( Class<T> resourceType, String resourceName, Integer quantity, ResourceAllocate<T> allocator ) throws AuthException, ResourceAllocationException {
    try {
      Context context = Contexts.lookup( );
      policyEngine.evaluateQuota( resourceType, resourceName, quantity, context.getRequest( ), context.getUser( ) );
      if ( allocator != null ) {
        allocator.allocate( );
      }
    } catch ( ResourceAllocationException ex ) {
      LOG.error( ex, ex );
      throw ex;
    } catch ( AuthException ex ) {
      LOG.error( ex , ex );
      throw ex;
    }
  }
  
}