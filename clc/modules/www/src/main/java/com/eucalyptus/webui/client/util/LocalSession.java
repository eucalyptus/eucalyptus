package com.eucalyptus.webui.client.util;

public interface LocalSession {
  
  String loadSessionId( );
  
  void saveSessionId( String sessionId );
  
  void clearSessionId( );
  
}
