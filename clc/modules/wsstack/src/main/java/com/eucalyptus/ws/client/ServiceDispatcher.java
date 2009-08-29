package com.eucalyptus.ws.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.mule.module.client.MuleClient;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.event.ComponentEvent;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.StartComponentEvent;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.client.pipeline.InternalClientPipeline;
import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public abstract class ServiceDispatcher {
  private static Logger LOG = Logger.getLogger( ServiceDispatcher.class );
  private static ConcurrentMap<String,ServiceDispatcher> proxies = new ConcurrentHashMap<String,ServiceDispatcher>(); 
  
  public static ServiceDispatcher lookupSingle( Component c ) throws NoSuchElementException {
    List<ServiceDispatcher> dispatcherList = lookupMany( c );
    if( dispatcherList.size( ) < 1 ) {
      LOG.error( "Failed to find service dispatcher for component=" + c );
      throw new NoSuchElementException("Failed to find service dispatcher for component=" + c);
    }
    return dispatcherList.get(0);
  }
  public static List<ServiceDispatcher> lookupMany( Component c ) {
    List<ServiceDispatcher> dispatcherList = Lists.newArrayList( );
    for( String key : proxies.keySet( ) ) {
      if( key.startsWith( c.name() )) {
        dispatcherList.add( proxies.get( key ) );
      }
    }
    return dispatcherList;
  }
  public static ServiceDispatcher lookup( Component c, String hostName ) {
    return proxies.get( c.getRegistryKey( hostName ) );
  }
  public static ServiceDispatcher lookup( String registryKey ) {
    return proxies.get( registryKey );
  }
  public static ServiceDispatcher register( String registryKey, ServiceDispatcher proxy ) {
    LOG.info( "Registering "+ registryKey + " as "  + proxy );
    return proxies.put( registryKey, proxy );
  }
  public static ServiceDispatcher deregister( String registryKey ) {
    LOG.info( "Deregistering "+ registryKey );
    return proxies.remove( registryKey );
  }
  public static Set<Map.Entry<String,ServiceDispatcher>> getEntries() {
    return proxies.entrySet( );
  }

  private Component     component;
  private String        name;
  private URI           address;
  private boolean isLocal = false;

  public ServiceDispatcher( Component component, String name, URI address, boolean isLocal ) {
    super( );
    this.component = component;
    this.name = name;
    this.address = address;
    this.isLocal = isLocal;
  }

  public abstract void dispatch( EucalyptusMessage msg );

  public abstract EucalyptusMessage send( EucalyptusMessage msg ) throws EucalyptusCloudException;
  @SuppressWarnings( "unchecked" )
  public <REPLY> REPLY send( EucalyptusMessage message, Class<REPLY> replyType ) throws EucalyptusCloudException {
    return (REPLY) this.send( message );
  }
  public Component getComponent( ) {
    return component;
  }
  public String getName( ) {
    return name;
  }
  public URI getAddress( ) {
    return address;
  }
  public boolean isLocal( ) {
    return isLocal;
  }
  protected MuleClient getMuleClient( ) throws Exception {
    return new MuleClient( );
  }

  protected NioClient getNioClient( ) throws Exception {
    return new NioClient( this.address.getHost( ), this.address.getPort( ), this.address.getPath( ), new InternalClientPipeline( new NioResponseHandler( ) ) );
  }
  @Override
  public String toString( ) {
    return LogUtil.dumpObject( this );
  }

  
  
}