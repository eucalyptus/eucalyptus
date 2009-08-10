package com.eucalyptus.ws.client;

import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mule.api.component.JavaComponent;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.ConnectException;

public class NioMessageReceiver extends AbstractMessageReceiver {

 private static Logger LOG = Logger.getLogger( NioMessageReceiver.class );

 @SuppressWarnings( "unchecked" )
 public NioMessageReceiver( Connector connector, Service service, InboundEndpoint endpoint ) throws CreateException {
   super( connector, service, endpoint );
   Class serviceClass = ( ( JavaComponent ) this.getService().getComponent() ).getObjectType();
 }

 public void doConnect() throws ConnectException {
 }

 public void doStart() {
 }

 public void doStop() {}

 public void doDispose() {}

 public void doDisconnect() throws ConnectException {}

}

