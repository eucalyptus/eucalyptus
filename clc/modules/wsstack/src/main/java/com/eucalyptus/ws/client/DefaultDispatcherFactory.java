package com.eucalyptus.ws.client;

import com.eucalyptus.component.Component;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.component.DispatcherFactory;
import com.eucalyptus.component.Service;

public class DefaultDispatcherFactory extends DispatcherFactory {
  @Override
  public Dispatcher buildChild( Component parent, Service service ) {
    Dispatcher d = null;
    if( parent.isSingleton( ) ) {
      d = new LocalDispatcher( parent.getPeer( ), service.getName( ), service.getUri( ) );
    } else if( service.getEndpoint( ).isLocal( ) ) {
      d = new LocalDispatcher( parent.getPeer( ), service.getName( ), service.getUri( ) );
    } else {
      d = new RemoteDispatcher( parent.getPeer( ), service.getName( ), service.getUri( ) );
    }
    ServiceDispatcher.register( service.getName( ), d );
    return d;
  }
  
}
