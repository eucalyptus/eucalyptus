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
   * Log out if 
   * @param clientFactory
   */
  public static void logoutForInvalidSession( ClientFactory clientFactory, Throwable exception ) {
    if ( EucalyptusServiceException.INVALID_SESSION.equals( exception.getMessage( ) ) ) {
      clientFactory.getLocalSession( ).clearSession( );
      clientFactory.getMainPlaceController( ).goTo( new LogoutPlace( ) );
    }
  }
  
}
