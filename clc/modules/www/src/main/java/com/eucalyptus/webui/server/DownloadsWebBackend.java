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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.webui.server;

import com.eucalyptus.bootstrap.BillOfMaterials;
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
    String extraVersion  = BillOfMaterials.getExtraVersion();

    LOG.debug("Eucalyptus EXTRA VERSION: " + extraVersion);
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
