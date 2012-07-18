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

package com.eucalyptus.webui.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.HttpServerBootstrapper;
import com.eucalyptus.webui.client.service.DownloadInfo;
import com.google.common.collect.Lists;

public class DownloadsWebBackend {

  private static final Logger LOG = Logger.getLogger( DownloadsWebBackend.class );
  
  private static final Integer TIMEOUT = 1000 * 10; // 10 seconds
  
  private static final int MAX = 50;
  
  public static ArrayList<DownloadInfo> getDownloads( String downloadsUrl ) {
    ArrayList<DownloadInfo> downloadsList = Lists.newArrayList( );

    HttpClient httpClient = new HttpClient( );

    //set User-Agent
    String clientVersion = (String)httpClient.getParams().getDefaults().getParameter(HttpMethodParams.USER_AGENT);
    String javaVersion   = System.getProperty("java.version");
    String osName        = System.getProperty("os.name");
    String osArch        = System.getProperty("os.arch");
    String eucaVersion   = System.getProperty("euca.version");
    String extraVersion  = System.getProperty("euca.extra_version");

    // Jakarta Commons-HttpClient/3.1 (java 1.6.0_24; Linux amd64) Eucalyptus/3.1.0-1.el6
    String userAgent = clientVersion + " (java " + javaVersion + "; " +
                       osName + " " + osArch + ") Eucalyptus/" + eucaVersion;
    if (extraVersion != null) {
        userAgent = userAgent + "-" + extraVersion;
    }

    httpClient.getParams().setParameter(HttpMethodParams.USER_AGENT, userAgent);

    //support for http proxy
    if ( HttpServerBootstrapper.httpProxyHost != null && (HttpServerBootstrapper.httpProxyHost.length( ) > 0 ) ) {
      String proxyHost = HttpServerBootstrapper.httpProxyHost;
      if ( HttpServerBootstrapper.httpProxyPort != null &&  ( HttpServerBootstrapper.httpProxyPort.length( ) > 0 ) ) {
        int proxyPort = Integer.parseInt( HttpServerBootstrapper.httpProxyPort );
        httpClient.getHostConfiguration( ).setProxy( proxyHost, proxyPort );
      } else {
        httpClient.getHostConfiguration( ).setProxyHost( new ProxyHost(proxyHost ) );
      }
    }
    GetMethod method = new GetMethod( downloadsUrl );
    method.getParams( ).setSoTimeout( TIMEOUT );

    try {
      httpClient.executeMethod( method );
      String str = "";
      InputStream in = method.getResponseBodyAsStream( );
      byte[] readBytes = new byte[1024];
      int bytesRead = -1;
      while ( ( bytesRead = in.read( readBytes ) ) > 0 ) {
        str += new String( readBytes, 0, bytesRead );
      }
      String entries[] = str.split( "[\\r\\n]+" );
      for ( int i = 0; i < entries.length; i++ ) {
        String entry[] = entries[i].split( "\\t" );
        if ( entry.length == 3 ) {
          downloadsList.add( new DownloadInfo( entry[0], entry[1], entry[2] ) );
        }
      }
    } catch ( MalformedURLException e ) {
      LOG.error( "Malformed URL exception: " + downloadsUrl, e );
      LOG.debug( e, e );
    } catch ( IOException e ) {
      LOG.error( "I/O exception", e );
      LOG.debug( e, e );
    } finally {
      method.releaseConnection( );
    }
    return downloadsList;
  }
  
}
