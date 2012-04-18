package com.eucalyptus.ws.client;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.module.client.MuleClient;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.component.NoSuchServiceException;
import com.eucalyptus.component.Service;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.context.ServiceDispatchException;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.ws.EucalyptusRemoteFault;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public abstract class ServiceDispatcher implements Dispatcher {
  private static Logger                              LOG              = Logger.getLogger( ServiceDispatcher.class );
  private static ConcurrentMap<FullName, Dispatcher> proxies          = new ConcurrentHashMap<FullName, Dispatcher>( );
  private static ChannelPipelineFactory              internalPipeline = ComponentIds.lookup( Empyrean.class ).getClientPipeline( );
  
  public static Dispatcher lookupSingle( Component c ) throws NoSuchElementException {
    return lookupSingle( c.getComponentId( ).getClass( ) );
  }
  
  public static Dispatcher lookupSingle( Class<? extends ComponentId> c ) throws NoSuchElementException {
    try {
      ServiceConfiguration first = Topology.enabledServices( c ).iterator( ).next( );
      if ( !Component.State.ENABLED.equals( first.lookupState( ) ) ) {
        LOG.error( "Failed to find service dispatcher for component=" + c );
        throw new NoSuchElementException( "Failed to find ENABLED service for component=" + c.getName( ) + " existing services are: "
                                          + ServiceConfigurations.list( c ) );
      } else {
        return ServiceDispatcher.lookup( first );
      }
    } catch ( NoSuchElementException ex ) {
      LOG.error( "Failed to find service dispatcher for component=" + c, ex );
      throw new NoSuchElementException( "Failed to find service for component=" + c );
    }
  }
  
  public static Dispatcher lookup( ServiceConfiguration configuration ) {
    return configuration.isVmLocal( )
      ? new LocalDispatcher( configuration, configuration.getComponentId( ).getLocalEndpointUri( ) )
      : new RemoteDispatcher( configuration, ServiceUris.internal( configuration ) );
  }
  
  static abstract class AbstractDispatcher implements Dispatcher {
    private final ServiceConfiguration serviceConfiguration;
    private final URI                  address;
    
    AbstractDispatcher( ServiceConfiguration serviceConfiguration, URI address ) {
      this.serviceConfiguration = serviceConfiguration;
      this.address = address;
    }
    
    public final ComponentId getComponentId( ) {
      return serviceConfiguration.getComponentId( );
    }
    
    public final String getName( ) {
      return this.serviceConfiguration.getFullName( ).toString( );
    }
    
    public final URI getAddress( ) {
      return address;
    }
    
    protected final NioClient getNioClient( ) throws Exception {
      return new NioClient( this.address.getHost( ), this.address.getPort( ), this.address.getPath( ),
                            ComponentIds.lookup( Empyrean.class ).getClientPipeline( ) );
    }
    
    protected final ServiceConfiguration getServiceConfiguration( ) {
      return this.serviceConfiguration;
    }
    
    @Override
    public String toString( ) {
      return String.format( "Dispatcher:serviceConfiguration=%s:address=%s:isLocal()=%s", this.serviceConfiguration, this.address, this.isLocal( ) );
    }
  }
  
  static class RemoteDispatcher extends AbstractDispatcher {
    
    RemoteDispatcher( ServiceConfiguration serviceConfiguration, URI address ) {
      super( serviceConfiguration, address );
    }
    
    public void dispatch( BaseMessage msg ) {
      MuleEvent context = RequestContext.getEvent( );
      try {
        this.getNioClient( ).dispatch( msg );
      } catch ( Exception e ) {
        LOG.error( e );
      } finally {
        RequestContext.setEvent( context );
      }
    }
    
    public BaseMessage send( BaseMessage msg ) throws EucalyptusCloudException {
      MuleEvent context = RequestContext.getEvent( );
      try {
        return this.getNioClient( ).send( msg );
      } catch ( Exception e ) {
        LOG.error( e, e );
        Throwable rootCause = Exceptions.findCause( e, EucalyptusRemoteFault.class );
        if ( rootCause == null ) {
          throw new EucalyptusCloudException( e );
        } else if ( rootCause instanceof EucalyptusRemoteFault ) {
          EucalyptusRemoteFault remoteFault = ( EucalyptusRemoteFault ) rootCause;
          throw new EucalyptusCloudException( " " + remoteFault.getFaultString( ) );
        } else {
          throw new EucalyptusCloudException( msg.getClass( ).getSimpleName( ) + ": " + rootCause.getMessage( ), rootCause );
        }
      } finally {
        RequestContext.setEvent( context );
      }
    }
    
    @Override
    public boolean isLocal( ) {
      return false;
    }
    
  }
  
  static class LocalDispatcher extends AbstractDispatcher {
    private MuleClient muleClient;
    
    LocalDispatcher( ServiceConfiguration serviceConfiguration, URI address ) {
      super( serviceConfiguration, address );
    }
    
    @Override
    public void dispatch( BaseMessage msg ) {
      MuleEvent context = RequestContext.getEvent( );
      try {
        ServiceContext.dispatch( this.getServiceConfiguration( ).getComponentId( ).getLocalEndpointName( ), msg );
      } catch ( Exception e ) {
        LOG.error( e );
      } finally {
        RequestContext.setEvent( context );
      }
    }
    
    @Override
    public BaseMessage send( BaseMessage msg ) {
      try {
        return ServiceContext.send( this.getComponentId( ), msg );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex.getMessage( ), ex );
      }
    }
    
    @Override
    public boolean isLocal( ) {
      return true;
    }
    
  }
  
}
