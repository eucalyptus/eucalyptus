package com.eucalyptus.ws.handlers.http;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.handler.ssl.SslHandler;

public class NioSslHandler extends SslHandler {

  public NioSslHandler( SSLEngine engine ) {
    super( engine );
  }


}
