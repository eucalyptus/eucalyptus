package com.eucalyptus.ws.client;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.StartComponentEvent;
import com.eucalyptus.util.LogUtil;

public class ServiceEventListener implements EventListener {
  private static Logger LOG = Logger.getLogger( ServiceEventListener.class );

  public static void register( ) {
    ServiceEventListener me = new ServiceEventListener( );
    for( Component c : Component.values() ) {
      if( !c.isDummy( ) ) {
        ListenerRegistry.getInstance( ).register( c, me );        
      }
    }
    ListenerRegistry.getInstance( ).register( Component.eucalyptus, me );
    ListenerRegistry.getInstance( ).register( Component.walrus, me );
    ListenerRegistry.getInstance( ).register( Component.dns, me );
    ListenerRegistry.getInstance( ).register( Component.storage, me );
    ListenerRegistry.getInstance( ).register( Component.db, me );
    ListenerRegistry.getInstance( ).register( Component.cluster, me );
    ListenerRegistry.getInstance( ).register( Component.jetty, me );
    if( Component.eucalyptus.isLocal( ) ) {
      ListenerRegistry.getInstance( ).register( ClockTick.class, RemoteBootstrapperClient.getInstance( ) ); 
      ListenerRegistry.getInstance( ).register( Component.walrus, RemoteBootstrapperClient.getInstance( ) );
      ListenerRegistry.getInstance( ).register( Component.storage, RemoteBootstrapperClient.getInstance( ) );
    }
  }

  @Override
  public void advertiseEvent( Event event ) {
  }

  @Override
  public void fireEvent( Event event ) {
    if ( event instanceof StartComponentEvent ) {
      StartComponentEvent e = ( StartComponentEvent ) event;
      ServiceDispatcher sd = null;
      if ( Component.db.equals( e.getComponent( ) ) ) {
        LOG.info( LogUtil.header( "Got information for the " + e.getComponent( ) + " " + LogUtil.dumpObject( e.getConfiguration( ) ) ) );
      }
      com.eucalyptus.component.Component childComponent = Components.lookup( e.getComponent( ) ).getChild( e.getConfiguration( ).getHostName( ) );
      LOG.info( "Registering service dispatcher: " + LogUtil.dumpObject( sd ) );
    }
  }

  
  
}
