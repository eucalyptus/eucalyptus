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
  
  private Map<String, WebSession> sessionsById = Maps.newHashMap( );
  private Map<String, WebSession> sessionsByUser = Maps.newHashMap( );
  
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
  public synchronized String newSession( String userId ) {
    String id = ServletUtils.genGUID( );
    long time = System.currentTimeMillis( );
    WebSession session = new WebSession( id, userId, time/*creationTime*/, time/*lastAccessTime*/ );
    sessionsById.put( id, session );
    sessionsByUser.put( userId, session );
    return id;
  }
  
  /**
   * Get a session by ID. Remove this session if expired.
   * 
   * @param id
   * @return the session, null if not exists or expired.
   */
  public synchronized WebSession getSessionById( String id ) {
    return getValidSession( sessionsById.get( id ) );
  }
  
  /**
   * Get a session by user ID. Remove the found session if expired.
   * 
   * @param userId
   * @return the session, null if not exists or expired
   */
  public synchronized WebSession getSessionByUser( String userId ) {
    return getValidSession( sessionsByUser.get( userId ) );
  }
  
  /**
   * Remove a session.
   * 
   * @param id
   */
  public synchronized void removeSession( String id ) {
    if ( id != null ) {
      removeSession( sessionsById.get( id ) );
    }
  }
  
  private void removeSession( WebSession session ) {
    if ( session != null ) {
      sessionsById.remove( session.getId( ) );
      sessionsByUser.remove( session.getUserId( ) );
    }
  }
  
  private WebSession getValidSession( WebSession session ) {
    if ( session != null ) {
      if ( System.currentTimeMillis( ) - session.getCreationTime( ) > AuthenticationProperties.WEBSESSION_LIFE_IN_MINUTES * 60 * 1000 ) {
        removeSession( session );
        return null;
      }
    }
    return session;
  }
  
}
