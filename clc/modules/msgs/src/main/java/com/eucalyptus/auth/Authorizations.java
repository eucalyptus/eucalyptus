package com.eucalyptus.auth;

import java.net.SocketAddress;
import java.util.Date;
import java.util.NavigableSet;
import java.util.Set;
import org.apache.log4j.Logger;
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

public class Authorizations {
  private static Logger LOG = Logger.getLogger( Authorizations.class );
      
  /**
   * @param <T> the type of resource being resolved
   * @param resourceName the canonical name of the resource (e.g., emi-ABCDEFGH for an Image)
   * @param lookup function provided by the service implementation to perform the lookup
   * @return instance of the resource refered to by resourceName
   * @throws AuthorizationException
   * @throws ResourceLookupException
   */
  public static <T extends HasName<T>> T lookupPrivileged( String resourceName, ResourceLookup<T> resolver ) throws AuthorizationException, ResourceLookupException {
    /** Check the policy; see implementation example for context lookup, etc. **/
    PolicyEngine.evaluate( resourceName );
    /** If policy allows, perform resource lookup **/
    T resource;
    try {
      resource = resolver.resolve( resourceName );
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
      throw new ResourceLookupException( "Failure occurred while trying to resolve a resource using: "
                                         + resolver.getClass( ).getCanonicalName( ), ex );
    }
    /** The following should be a true statement **/
    if ( !resourceName.equals( resource.getName( ) ) ) {
      throw new ResourceLookupException( "Broken resolver implementation, returned resource "
                                         + resource.getClass( ).getSimpleName( )
                                         + " which doesn't match requested resource name "
                                         + resourceName + ": "
                                         + resolver.getClass( ).getCanonicalName( ) );
    }
    /** Can also use the returned resourceType to do policy evaluation **/
    Class<T> resourceType = ( Class<T> ) resource.getClass( );
    PolicyEngine.evaluate( resourceName, resourceType );
    return resource;
  }
  
  
  
  
  
  /**
   * @param <T> type of the resource
   * @param quantity number of resources to allocate
   * @param allocator service implementation which performs the allocation
   * @return immutable list of the resources which were allocated
   * @throws AuthorizationException 
   * @throws ResourceAllocationException 
   */
  public static <T extends HasName<T>> Set<ResourceLease<T>> allocatePrivileged( Integer quantity, ResourceAllocator<T> allocator ) throws AuthorizationException, ResourceAllocationException {
    try {
      NavigableSet<T> resources = allocator.allocate( quantity );
      final Date expires = !resources.isEmpty( )
        ? PolicyEngine.lookupExpiration( resources.first( ).getClass( ) )
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
    } catch ( AuthorizationException ex ) {
      LOG.error( ex , ex );
      throw ex;
    }
  }
  
  
  /**
   * Dummy implementation for illustrative purposes
   */
  public static class PolicyEngine {
    
    public static void evaluate( String resourceName ) throws AuthorizationException {
      try {
        Context requestContext = Contexts.lookup( );
        User user = requestContext.getUser( );
        BaseMessage currentRequest = requestContext.getRequest( );
        SocketAddress remoteAddress = requestContext.getChannel( ).getRemoteAddress( );
        boolean authorized = true;
        if ( !authorized ) {
          throw new AuthorizationException( "Authorization failed for some specific reason" );
        }
      } catch ( AuthorizationException ex ) {//throw by the policy engine implementation 
        throw ex;
      } catch ( IllegalContextAccessException ex ) {//this would happen if Contexts.lookup() is invoked outside of mule.
        throw new AuthorizationException( "Cannot invoke Authorizations.resolve without a corresponding service context available.", ex );
      } catch ( Throwable ex ) {
        throw new AuthorizationException( "An error occurred while trying to evaluate a policy" );
      }
    }
    
    public static <T> void evaluate( String resourceName, Class<T> resourceType ) throws AuthorizationException {
      /** maybe have an optimization that makes use of resourceType **/
      evaluate( resourceName );
    }
    
    public static <T> Date lookupExpiration( Class<T> resourceType ) throws AuthorizationException {
      return new Date( );
    }
  }
  
}