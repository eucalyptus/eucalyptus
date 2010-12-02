package com.eucalyptus.auth;

import java.net.SocketAddress;
import java.util.Date;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.PolicyEngine;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.component.ResourceAllocationException;
import com.eucalyptus.component.ResourceAllocator;
import com.eucalyptus.component.ResourceLease;
import com.eucalyptus.component.ResourceLookup;
import com.eucalyptus.component.ResourceLookupException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.util.HasName;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

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
   * @param <T> the type of resource being resolved
   * @param resourceName the canonical name of the resource (e.g., emi-ABCDEFGH for an Image)
   * @param lookup function provided by the service implementation to perform the lookup
   * @return instance of the resource refered to by resourceName
   * @throws AuthException
   * @throws ResourceLookupException
   */
  public static <T> T lookupPrivileged( String resourceName, String resourceAccountId, ResourceLookup<T> resolver ) throws AuthException, ResourceLookupException {
    T resource;
    try {
      resource = resolver.resolve( resourceName );
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
      throw new ResourceLookupException( "Failure occurred while trying to resolve a resource using: "
                                         + resolver.getClass( ).getCanonicalName( ), ex );
    }
    /** Can also use the returned resourceType to do policy evaluation **/
    @SuppressWarnings( "unchecked" )
    Class<T> resourceType = ( Class<T> ) resource.getClass( );
    policyEngine.evaluateAuthorization( resourceType, resourceName, resourceAccountId );
    return resource;
  }
  
  /**
   * @param <T> type of the resource
   * @param quantity number of resources to allocate
   * @param allocator service implementation which performs the allocation
   * @throws AuthException 
   * @throws ResourceAllocationException 
   */
  public static <T> void allocatePrivileged( Integer quantity, ResourceAllocator<T> allocator ) throws AuthException, ResourceAllocationException {
    /*
    try {
      NavigableSet<T> resources = allocator.allocate( quantity );
      final Date expires = !resources.isEmpty( )
        ? policyEngine.lookupExpiration( resources.first( ).getClass( ) )
        : new Date( 2012, 12, 12 );
      final NavigableSet<ResourceLease<T>> leases = Sets.newTreeSet( );
      Iterators.all( resources.iterator( ), new Predicate<T>( ) {
        
        @Override
        public boolean apply( final T r ) {
          
          return leases.add( new ResourceLease<T>( ) {
            {
              this.expiration = expires;
              this.resource = r;
            }
          } );
        }
      } );
      return ImmutableSortedSet.copyOf( leases );
    } catch ( ResourceAllocationException ex ) {
      LOG.error( ex, ex );
      throw ex;
    } catch ( AuthException ex ) {
      LOG.error( ex , ex );
      throw ex;
    }
    */
  }
  
}