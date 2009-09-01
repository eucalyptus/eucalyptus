package com.eucalyptus.ws.client;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.config.ComponentConfiguration;
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
    ListenerRegistry.getInstance( ).register( Component.walrus, me );
    ListenerRegistry.getInstance( ).register( Component.storage, me );
    ListenerRegistry.getInstance( ).register( Component.db, me );
    ListenerRegistry.getInstance( ).register( Component.dns, me );
    ListenerRegistry.getInstance( ).register( Component.jetty, me );
    ListenerRegistry.getInstance( ).register( Component.eucalyptus, me );
    ListenerRegistry.getInstance( ).register( Component.cluster, me );
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
    try {
      if ( event instanceof StartComponentEvent ) {
        StartComponentEvent e = ( StartComponentEvent ) event;
        ServiceDispatcher sd = null;
        if ( Component.db.equals( e.getComponent( ) ) ) {
          LOG.info( LogUtil.header( "Got information for the " + e.getComponent( ) + " " + LogUtil.dumpObject( e.getConfiguration( ) ) ) );
        }
        if ( e.isLocal( ) ) {
          Component c = e.getComponent( );
          URI uri = new URI( c.getLocalAddress( ) );
          sd = new LocalDispatcher( c, c.name( ), uri );
          if( Component.storage.equals( c ) ) {
            System.setProperty( "euca.storage.name", e.getConfiguration( ).getName( ) );
          }
          ServiceDispatcher.register( c.getRegistryKey( "localhost" ), sd );
        } else {
          ComponentConfiguration config = e.getConfiguration( );
          Component c = e.getComponent( );
          URI uri = new URI( c.makeUri( config.getHostName( ) ) );
          sd = new RemoteDispatcher( c, c.name( ), uri );
          ServiceDispatcher.register( c.getRegistryKey( config.getHostName( ) ), sd );
        }
        LOG.info( "Registering service dispatcher: " + LogUtil.dumpObject( sd ) );
      }
    } catch ( URISyntaxException e ) {
      LOG.warn( e, e );
    }
  }

}
