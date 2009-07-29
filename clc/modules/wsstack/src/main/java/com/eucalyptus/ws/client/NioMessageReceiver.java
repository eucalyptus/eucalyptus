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
//   try {
//     this.axisService = Axis2ServiceBuilder.getAxisService( this );
//   }
//   catch ( AxisFault axisFault ) {
//     throw new ConnectException( axisFault, this );
//   }
//   Iterator iter = this.axisService.getOperations();
//   while ( iter.hasNext() ) {
//     AxisOperation op = (AxisOperation)iter.next();
//     if ( EucalyptusProperties.getDisabledOperations().contains( op.getName().getLocalPart() ) )
//       this.disableOperation( op.getName().getLocalPart() );
//   }
 }

 public void doStart() {
//   try {
//     Axis2Connector connector = ( Axis2Connector ) this.getConnector();
//     this.axisService.setActive( true );
//     connector.getAxisConfig().deployService( this.axisService );
//     connector.addHttpPortListener( this.getEndpointURI().getAddress(), this.getEndpointURI().getPort() );
//   }
//   catch ( AxisFault axisFault ) {}
 }

 public void doStop() {}

 public void doDispose() {}

 public void doDisconnect() throws ConnectException {}

}

