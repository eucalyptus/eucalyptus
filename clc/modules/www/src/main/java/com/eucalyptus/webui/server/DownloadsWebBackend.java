package com.eucalyptus.webui.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import org.apache.commons.httpclient.HttpClient;
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
