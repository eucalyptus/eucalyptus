package com.eucalyptus.webui.client.session;

import com.eucalyptus.webui.client.service.Session;

public interface LocalSession {
  
  // Load session from disk
  Session getSession( );
  
  // Save session to disk
  void saveSession( Session session, boolean persistent );
  
  // Clear session
  void clearSession( );
  
}
