package com.eucalyptus.webui.client.util;

import com.google.gwt.user.client.Cookies;

public class LocalSessionImpl implements LocalSession {
  
  private static final String SESSION_COOKIE_NAME = "EUCASID";
  
  @Override
  public String loadSessionId( ) {
    return Cookies.getCookie( SESSION_COOKIE_NAME );
  }
  
  @Override
  public void saveSessionId( String sessionId ) {
    Cookies.setCookie( SESSION_COOKIE_NAME, sessionId, null, null, null, true );
  }

  @Override
  public void clearSessionId( ) {
    Cookies.removeCookie( SESSION_COOKIE_NAME );
  }
  
}
