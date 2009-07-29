package com.eucalyptus.ws.server;

import org.apache.log4j.Logger;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.AbstractConnector;

public class NioHttpConnector extends AbstractConnector {

    private static Logger LOG = Logger.getLogger( NioHttpConnector.class );

    public static String PROTOCOL = "euca";
    private NioServer server;

    public NioHttpConnector() {
      super.registerSupportedProtocol( "http" );
      super.registerSupportedProtocol( "https" );
    }

    public void doConnect() throws MuleException
    {
      this.server = new NioServer( 19191 );
    }

    public String getProtocol()
    {
      return PROTOCOL;
    }

    public void doDisconnect() throws MuleException {}

    public void doStart() throws MuleException {
      if( this.server == null ) {
        this.doConnect( );
      }
      this.server.start( );
    }

    public void doStop() throws MuleException {}

    public void doDispose() {}

    @Override
    protected void doInitialise( ) throws InitialisationException {}
}
