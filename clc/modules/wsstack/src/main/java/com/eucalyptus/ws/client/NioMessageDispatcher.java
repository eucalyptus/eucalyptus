package com.eucalyptus.ws.client;

import java.security.GeneralSecurityException;

import org.apache.log4j.Logger;
import org.mule.DefaultMuleMessage;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;

import com.eucalyptus.ws.client.pipeline.ClusterClientPipeline;
import com.eucalyptus.ws.client.pipeline.InternalClientPipeline;
import com.eucalyptus.ws.client.pipeline.LogClientPipeline;
import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.wssecurity.InternalWsSecHandler;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public class NioMessageDispatcher extends AbstractMessageDispatcher {
  private static Logger LOG = Logger.getLogger( NioMessageDispatcher.class );
  private Client client;

  public NioMessageDispatcher( OutboundEndpoint outboundEndpoint ) {
    super( outboundEndpoint );
    String host = outboundEndpoint.getEndpointURI( ).getHost( );
    int port = outboundEndpoint.getEndpointURI( ).getPort( );
    String servicePath = outboundEndpoint.getEndpointURI( ).getPath( );
    try {
      Client nioClient = new NioClient( host, port, servicePath, new InternalClientPipeline( new NioResponseHandler( ) ) );
    } catch ( GeneralSecurityException e ) {
      LOG.error( e );
    }
  }

  @Override
  protected void doDispatch( final MuleEvent muleEvent ) throws Exception {
    client.dispatch( (EucalyptusMessage) muleEvent.getMessage( ).getPayload( ) );
  }

  @Override
  protected MuleMessage doSend( final MuleEvent muleEvent ) throws Exception {
    MuleMessage muleMsg = muleEvent.getMessage( );
    EucalyptusMessage request = ( EucalyptusMessage ) muleMsg.getPayload( );
    EucalyptusMessage response = client.send( request );
    return new DefaultMuleMessage( response );
  }

  @Override
  protected void doDispose() {}

  @Override
  protected void doConnect() throws Exception {}

  @Override
  protected void doDisconnect() throws Exception {}
}
