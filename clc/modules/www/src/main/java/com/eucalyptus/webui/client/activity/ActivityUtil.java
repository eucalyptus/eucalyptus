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

package com.eucalyptus.webui.client.activity;

import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.LogoutPlace;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.QuickLink;
import com.google.gwt.http.client.URL;

public class ActivityUtil {
  
  private static final Logger LOG = Logger.getLogger( ActivityUtil.class.getName( ) );
  
  /**
   * Update the highlighted quick link based on the current search.
   * 
   * @param clientFactory
   */
  public static void updateDirectorySelection( ClientFactory clientFactory ) {
    String currentSearch = getCurrentSearch( clientFactory );
    LOG.info( "Updating directory selection: current search is " + currentSearch );
    QuickLink link = clientFactory.getSessionData( ).lookupQuickLink( currentSearch );
    LOG.info( "Updating directory selection: found quick link " + ( link == null ? "NONE" : link.getName( ) ) );
    clientFactory.getShellView( ).getDirectoryView( ).changeSelection( link );
  }
  
  /**
   * Find out the current search using the history.
   * 
   * @param clientFactory
   * @return
   */
  public static String getCurrentSearch( ClientFactory clientFactory ) {
    return URL.decode( clientFactory.getMainHistorian( ).getToken( ) );
  }
  
  /**
   * Log out if session is invalid
   * @param clientFactory
   */
  public static void logoutForInvalidSession( ClientFactory clientFactory, Throwable exception ) {
    if ( EucalyptusServiceException.INVALID_SESSION.equals( exception.getMessage( ) ) ) {
      clientFactory.getLocalSession( ).clearSession( );
      clientFactory.getMainPlaceController( ).goTo( new LogoutPlace( ) );
    }
  }
  
}
