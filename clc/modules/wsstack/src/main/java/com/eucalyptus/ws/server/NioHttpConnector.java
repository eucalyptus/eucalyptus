package com.eucalyptus.ws.server;

import org.apache.log4j.Logger;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.AbstractConnector;

public class NioHttpConnector extends AbstractConnector {

  private static Logger LOG      = Logger.getLogger( NioHttpConnector.class );

  public static String  PROTOCOL = "euca";
  private NioServer     server;

  public NioHttpConnector( ) {
    super.registerSupportedProtocol( "http" );
    super.registerSupportedProtocol( "https" );
  }

  public void doConnect( ) throws MuleException {
    this.server = new NioServer( 8773 );
  }

  public String getProtocol( ) {
    return PROTOCOL;
  }

  @Override
  public void doDisconnect( ) throws MuleException {
  }

  @Override
  public void doStart( ) throws MuleException {
    if ( this.server == null ) {
      this.doConnect( );
    }
    this.server.start( );
  }

  @Override
  public void doStop( ) throws MuleException {
  }

  @Override
  public void doDispose( ) {
  }

  @Override
  protected void doInitialise( ) throws InitialisationException {
  }
}
