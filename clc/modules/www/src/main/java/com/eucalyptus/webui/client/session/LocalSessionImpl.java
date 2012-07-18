/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.webui.client.session;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.service.Session;
import com.google.gwt.user.client.Cookies;

public class LocalSessionImpl implements LocalSession {
  
  private static Logger LOG = Logger.getLogger( LocalSessionImpl.class.getName( ) );
  
  private static final String SESSION_COOKIE_NAME = "EUCASID";
  private static final long COOKIE_LIFE_IN_MS = 7 * 24 * 60 * 60 * 1000;// a week
  private static final boolean USE_SECURE_COOKIE = true;
  
  private Session session;
  
  public LocalSessionImpl( ) {
    this.session = null;
  }
  
  @Override
  public Session getSession( ) {
    if ( this.session == null ) {
      String sessionId = Cookies.getCookie( SESSION_COOKIE_NAME );
      if ( sessionId != null ) {
        this.session = new Session( sessionId );
      }
    }
    return this.session;
  }
  
  @Override
  public void saveSession( Session s, boolean persistent ) {
    if ( s == null ) {
      clearSession( );
    } else {
      this.session = s;
      if ( persistent ) {
        Date expiration = new Date( System.currentTimeMillis( ) + COOKIE_LIFE_IN_MS );
        Cookies.setCookie( SESSION_COOKIE_NAME, this.session.getId( ), expiration, null, null, USE_SECURE_COOKIE );
      } else {
        Cookies.setCookie( SESSION_COOKIE_NAME, this.session.getId( ), null, null, null, USE_SECURE_COOKIE );
      }
    }
  }

  @Override
  public void clearSession( ) {
    this.session = null;
    Cookies.removeCookie( SESSION_COOKIE_NAME );
  }

}
