package com.eucalyptus.ws.client;

import java.net.URI;
import org.apache.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.module.client.MuleClient;
import com.eucalyptus.component.Component;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.context.ServiceDispatchException;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class LocalDispatcher extends ServiceDispatcher {
  private static Logger LOG = Logger.getLogger( LocalDispatcher.class );
  private MuleClient    muleClient;

  public LocalDispatcher( Component component, String name, URI address ) {
    super( component, name, address, true );
  }

  @Override
  public void dispatch( BaseMessage msg ) {
    MuleEvent context = RequestContext.getEvent( );
    try {
      ServiceContext.dispatch( this.getComponent( ).getComponentId( ).getLocalEndpointName( ), msg );
    } catch ( Exception e ) {
      LOG.error( e );
    } finally {
      RequestContext.setEvent( context );
    }
  }

  @Override
  public BaseMessage send( BaseMessage msg ) throws EucalyptusCloudException {
    try {
      return ServiceContext.send( this.getComponent( ).getComponentId( ).getLocalEndpointName( ), msg );
    } catch ( ServiceDispatchException ex ) {
      throw new EucalyptusCloudException( ex.getMessage( ), ex );
    }
  }

}
