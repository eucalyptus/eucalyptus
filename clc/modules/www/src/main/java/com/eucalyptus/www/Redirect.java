package com.eucalyptus.www;

import java.io.IOException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.handler.rewrite.Rule;
import com.eucalyptus.bootstrap.HttpServerBootstrapper;

public class Redirect extends Rule {
  public Redirect( ) {
    setTerminating( true );
  }
  
  @Override
  public String matchAndApply( String target, HttpServletRequest req, HttpServletResponse resp ) throws IOException {
    String urlStr = req.getRequestURL( ).toString( );
    URL url = new URL( urlStr );
    if ( !urlStr.startsWith( "https" ) ) {
      resp.sendRedirect( String.format( "https://%s:%d/%s", url.getHost( ), HttpServerBootstrapper.HTTPS_PORT,
                                        url.getPath( ) + ( ( req.getQueryString( ) != null ) ? "?" + req.getQueryString( ) : "" ) ).replaceAll("//","/") );
      return target;
    } else {
      return target;
    }
  }
  
}