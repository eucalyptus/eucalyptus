package com.eucalyptus.ws.client;

import java.net.URI;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.component.DispatcherFactory;
import com.eucalyptus.util.NetworkUtil;

public class DefaultDispatcherFactory extends DispatcherFactory {
  static {
    DispatcherFactory.setFactory( new DefaultDispatcherFactory( ) );
  }

  @Override
  public Dispatcher buildChild( Component parent, String hostName ) {
    Dispatcher d = null;
    String key = parent.getChildKey( hostName );
    if( parent.isSingleton( ) ) {
      d = new LocalDispatcher( parent.getDelegate( ), hostName, parent.getDelegate( ).getLocalUri( ) );
    } else if( NetworkUtil.testLocal( hostName ) ) {
      URI uri = parent.getConfiguration( ).getLocalUri( );
      d = new LocalDispatcher( parent.getDelegate( ), hostName, uri );
    } else {
      URI uri = parent.getLifecycle( ).getUri( hostName );
      d = new RemoteDispatcher( parent.getDelegate( ), hostName, uri );
    }
    ServiceDispatcher.register( parent.getChildKey( hostName ), d );
    return d;
  }
  
}
