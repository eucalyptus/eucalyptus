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

package com.eucalyptus.www;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mortbay.jetty.handler.rewrite.Rule;
import com.eucalyptus.bootstrap.HttpServerBootstrapper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;

public class Redirect extends Rule {
  
  private static final Logger LOG = Logger.getLogger( Redirect.class );
  
  public Redirect( ) {
    setTerminating( true );
  }
  
  @Override
  public String matchAndApply( String target, HttpServletRequest req, HttpServletResponse resp ) throws IOException {
    String redirectHost = null; // indicates if a redirect is necessary
    // First check if the request is for a CLC slave
    // If ture, redirect to the master CLC.
    if ( !Topology.isEnabledLocally( Eucalyptus.class ) ) {
      try {
        ServiceConfiguration clc = Topology.lookup( Eucalyptus.class );
        if ( clc == null ) {
          throw new RuntimeException( "Can not find enabled cloud controller" );
        }
        redirectHost = clc.getHostName( );
      } catch ( Exception e ) {
        LOG.error( "Failed to lookup service configuration for cloud controller", e );
        throw new IOException( "No available cloud web service" );
      }
    }
    // Then check if the request is plain HTTP
    // If true, redirect to the HTTPS service
    String urlStr = req.getRequestURL( ).toString( );
    if ( redirectHost == null && !urlStr.startsWith( "https" ) ) {
      redirectHost = ( new URL( urlStr ) ).getHost( );
    }
    if ( redirectHost != null ) {
      // A redirect is required
      String redirectUrl = getRedirectUrl( redirectHost, req );
      LOG.debug( "Redirecting request to " + redirectUrl );
      resp.sendRedirect( redirectUrl );
    }
    return target;
  }

  /**
   * Reconstruct the correct redirect URL.
   * 
   * @param redirectHost
   * @param req
   * @return
   * @throws MalformedURLException
   */
  private static String getRedirectUrl(String redirectHost, HttpServletRequest req) throws MalformedURLException {
    URL url = new URL( req.getRequestURL( ).toString( ) );
    return "https://" + String.format( "%s:%d/%s", redirectHost, HttpServerBootstrapper.HTTPS_PORT, 
        url.getPath( ) + ( ( req.getQueryString( ) != null ) ? "?" + req.getQueryString( ) : "" ) ).replaceAll("//","/");
  }
  
}
