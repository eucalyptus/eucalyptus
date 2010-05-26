package com.eucalyptus.www;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.eucalyptus.bootstrap.HttpServerBootstrapper;

public class Redirect extends HttpServlet {

  @Override
  protected void service( HttpServletRequest arg0, HttpServletResponse arg1 ) throws ServletException, IOException {
    arg1.sendRedirect( arg0.getRequestURI( ).replaceAll( ""+HttpServerBootstrapper.HTTP_PORT, ""+HttpServerBootstrapper.HTTPS_PORT ).replaceAll( "http://", "https://" ) );
  }

}
