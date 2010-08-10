package com.eucalyptus.ws.client;

import com.eucalyptus.component.Component;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.component.DispatcherFactory;
import com.eucalyptus.component.Service;

public class DefaultDispatcherFactory extends DispatcherFactory {
  static {
    DispatcherFactory.setFactory( new DefaultDispatcherFactory( ) );
  }

  @Override
  public void removeChild( Service service ) {
    ServiceDispatcher.deregister( service.getDispatcher( ).getName( ) );
  }
  
  @Override
  public Dispatcher buildChild( Component parent, Service service ) {
    Dispatcher d = null;
    if( service.getEndpoint( ).isLocal( ) ) {
      d = new LocalDispatcher( parent.getPeer( ), service.getName( ), service.getUri( ) );
    } else {
      d = new RemoteDispatcher( parent.getPeer( ), service.getName( ), service.getUri( ) );
    }
    ServiceDispatcher.register( service.getName( ), d );
    return d;
  }
  
}
