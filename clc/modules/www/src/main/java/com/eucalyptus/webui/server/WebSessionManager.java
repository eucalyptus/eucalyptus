package com.eucalyptus.webui.server;

import java.util.Map;
import com.eucalyptus.auth.AuthenticationProperties;
import com.eucalyptus.auth.AuthenticationProperties.LicChangeListener;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.google.common.collect.Maps;

/**
 * Web session manager, maintaining a web session registrar.
 * 
 * @author Ye Wen (wenye@eucalyptus.com)
 *
 */
public class WebSessionManager {
  
  private static WebSessionManager instance = null;
  
  private Map<String, WebSession> sessions = Maps.newHashMap( );
  
  private WebSessionManager( ) {
    
  }
  
  public static synchronized WebSessionManager getInstance( ) {
    if ( instance == null ) {
      instance = new WebSessionManager( );
    }
    return instance;
  }
  
  /**
   * Create new web session record.
   * 
   * @param userName
   * @param accountName
   * @return the new session ID.
   */
  public synchronized String newSession( String userName, String accountName ) {
    String id = ServletUtils.genGUID( );
    long time = System.currentTimeMillis( );
    WebSession session = new WebSession( id, userName, accountName, time/*creationTime*/, time/*lastAccessTime*/ );
    sessions.put( id, session );
    return id;
  }
  
  /**
   * Get a session by ID. Remove this session if expired.
   * 
   * @param id
   * @return the session, null if not exists or expired.
   */
  public synchronized WebSession getSession( String id ) {
    WebSession session = sessions.get( id );
    if ( session != null ) {
      if ( System.currentTimeMillis( ) - session.getCreationTime( ) > AuthenticationProperties.WEBSESSION_LIFE_IN_MINUTES * 60 * 1000 ) {
        sessions.remove( id );
        session = null;
      }
    }
    return session;
  }
  
  /**
   * Get a session by user name and account name. Remove the found session if expired.
   * 
   * @param userName
   * @param accountName
   * @return
   */
  public synchronized WebSession getSession( String userName, String accountName ) {
	for ( WebSession session : sessions.values( ) ) {
	  if ( session != null && session.getUserName( ).equals( userName ) && session.getAccountName( ).equals( accountName ) ) {
	    if ( System.currentTimeMillis( ) - session.getCreationTime( ) > AuthenticationProperties.WEBSESSION_LIFE_IN_MINUTES * 60 * 1000 ) {
	      sessions.remove( session.getId( ) );
	      return null;
	    }
	    return session;
	  }
	}
	return null;
  }
  
  /**
   * Remove a session.
   * 
   * @param id
   */
  public synchronized void removeSession( String id ) {
    if ( id != null ) {
      sessions.remove( id );
    }
  }
  
}
