package com.eucalyptus.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.ldap.LdapException;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;

/**
 * An extremely simple and adhoc cache for LDAP, implemented at LdapAuthProvider layer, caching User and Group entities, just to optimize the most common use
 * cases.
 * 
 * @author wenye
 */
public class LdapCache {
  private static Logger                     LOG                = Logger.getLogger( LdapCache.class );
  // Max number of user entries
  private static final int                  MAX_CACHE_SIZE     = 5000;
  
  private ConcurrentHashMap<String, User>   userCache          = new ConcurrentHashMap<String, User>( );
  private ConcurrentHashMap<String, String> userCertMap        = new ConcurrentHashMap<String, String>( );
  private ConcurrentHashMap<String, String> userRevokedCertMap = new ConcurrentHashMap<String, String>( );
  private ConcurrentHashMap<String, String> userQueryIdMap     = new ConcurrentHashMap<String, String>( );
  
  private ConcurrentHashMap<String, Group>  groupCache         = new ConcurrentHashMap<String, Group>( );
  
  private static LdapCache                  instance           = null;
  
  public static LdapCache getInstance( ) {
    if ( instance == null ) {
      instance = new LdapCache( );
    }
    return instance;
  }
  
  public synchronized boolean isGroupCacheInvalidated( ) {
    return groupCache.size( ) == 0;
  }
  
  public synchronized void reloadGroups( List<Group> groups ) {
    LOG.debug( "Reload groups : " );
    groupCache.clear( );
    for ( Group group : groups ) {
      groupCache.put( group.getName( ), group );
      LOG.debug( "Load group cache " + group );
    }
  }
  
  public synchronized void clearGroups( ) {
    groupCache.clear( );
    LOG.debug( "Group cache cleared" );
  }
  
  public synchronized Group getGroup( String name ) {
    return groupCache.get( name );
  }
  
  public synchronized List<Group> getGroups( List<String> eucaGroupIds ) {
    List<Group> results = new ArrayList<Group>( );
    if ( eucaGroupIds == null ) {
      results.addAll( groupCache.values( ) );
    } else {
      for ( String id : eucaGroupIds ) {
        int colon = id.indexOf( EucaLdapHelper.EUCA_GROUP_ID_SEPARATOR );
        if ( colon < 1 || colon > id.length( ) - 2 ) {
          LOG.error( "Invalid eucaGroupId string: " + id );
          continue;
        }
        String name = id.substring( 0, colon );
        String timestamp = id.substring( colon + 1 );
        Group result = groupCache.get( name );
        if ( result != null && timestamp.equals( ( ( LdapWrappedGroup ) result ).getTimestamp( ) ) ) {
          results.add( result );
        }
      }
    }
    return results;
  }
  
  public void addUser( User user ) {
    LOG.debug( "Adding user " + user );
    if ( userCache.size( ) >= MAX_CACHE_SIZE ) {
      LOG.debug( "Cache size exceeded." );
      return;
    }
    userCache.put( user.getName( ), user );
    userQueryIdMap.put( user.getQueryId( ), user.getName( ) );
    for ( X509Cert cert : ( ( LdapWrappedUser ) user ).getCertificates( ) ) {
      if ( cert.isRevoked( ) ) {
        userRevokedCertMap.put( cert.getPemCertificate( ), user.getName( ) );
      } else {
        userCertMap.put( cert.getPemCertificate( ), user.getName( ) );
      }
    }
  }
  
  public void removeUser( String name ) {
    LOG.debug( "Removing user " + name );
    User user = userCache.remove( name );
    if ( user != null ) {
      userQueryIdMap.remove( user.getQueryId( ) );
      for ( X509Cert cert : ( ( LdapWrappedUser ) user ).getCertificates( ) ) {
        if ( cert.isRevoked( ) ) {
          userRevokedCertMap.remove( cert.getPemCertificate( ) );
        } else {
          userCertMap.remove( cert.getPemCertificate( ) );
        }
      }
    }
  }
  
  public User getUserByName( String name ) {
    return name == null ? null : userCache.get( name );
  }
  
  public User getUserByQueryId( String queryId ) {
    return getUserByName( userQueryIdMap.get( queryId ) );
  }
  
  public User getUserByCert( String cert ) {
    return getUserByName( userCertMap.get( cert ) );
  }
  
  public User getUserByRevokedCert( String cert ) {
    return getUserByName( userRevokedCertMap.get( cert ) );
  }
}
