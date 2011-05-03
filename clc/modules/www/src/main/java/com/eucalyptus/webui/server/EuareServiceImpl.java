package com.eucalyptus.webui.server;

import com.eucalyptus.webui.client.service.EuareService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class EuareServiceImpl extends RemoteServiceServlet implements EuareService {

  private static final long serialVersionUID = 1L;

  public String test( String message ) {
    if ( message != null ) {
      return "I got " + message;
    } else {
      return "Empty";
    }
  }
  
}
