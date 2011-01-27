package com.eucalyptus.context;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleSession;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.Registry;
import org.mule.api.service.Service;
import org.mule.api.transport.DispatchException;
import org.mule.config.ConfigResource;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.client.MuleClient;
import org.mule.transport.AbstractConnector;
import org.mule.transport.vm.VMMessageDispatcherFactory;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Resource;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.system.Threads;
import com.eucalyptus.system.Threads.ThreadPool;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

@ConfigurableClass( root = "system", description = "Parameters having to do with the system's state.  Mostly read-only." )
public class ServiceContext {
  private static Logger                        LOG                      = Logger.getLogger( ServiceContext.class );
  private static SpringXmlConfigurationBuilder builder;
  @ConfigurableField( initial = "16", description = "Max queue length allowed per service stage.", changeListener = HupListener.class )
  public static Integer                        MAX_OUTSTANDING_MESSAGES = 16;
  @ConfigurableField( initial = "0", description = "Do a soft reset.", changeListener = HupListener.class )
  public static Integer                        HUP                      = 0;
  
  public static class HupListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      ServiceContextManager.restart( );
    }
  }
  
  private static final VMMessageDispatcherFactory  dispatcherFactory = new VMMessageDispatcherFactory( );
  private static final AtomicReference<MuleClient> client            = new AtomicReference<MuleClient>( null );
  private static final BootstrapException          failEx            = new BootstrapException(
                                                                                                    "Attempt to use esb client before the service bus has been started." );
  
  public static void dispatch( String dest, Object msg ) throws ServiceInitializationException, ServiceDispatchException, ServiceStateException {
    dest = ServiceContextManager.mapServiceToEndpoint( dest );
    MuleContext muleCtx;
    try {
      muleCtx = ServiceContextManager.getContext( );
    } catch ( ServiceInitializationException ex ) {
      LOG.debug( ex.getMessage( ) );
      throw ex;
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to obtain service context reference: "
                                          + ex.getMessage( ), ex );
    }
    OutboundEndpoint endpoint;
    try {
      endpoint = muleCtx.getRegistry( ).lookupEndpointFactory( ).getOutboundEndpoint( dest );
      if ( !endpoint.getConnector( ).isStarted( ) ) {
        endpoint.getConnector( ).start( );
      }
    } catch ( MuleException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to obtain service endpoint reference: "
                                          + ex.getMessage( ), ex );
    }
    MuleMessage muleMsg = new DefaultMuleMessage( msg );
    MuleSession muleSession;
    try {
      muleSession = new DefaultMuleSession( muleMsg, ( ( AbstractConnector ) endpoint.getConnector( ) ).getSessionHandler( ),
                                                          ServiceContextManager.getContext( ) );
    } catch ( ServiceStateException ex ) {
      LOG.error( ex, ex );
      throw ex;
    } catch ( MuleException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to contruct session: " + ex.getMessage( ), ex );
    }
    MuleEvent muleEvent = new DefaultMuleEvent( muleMsg, endpoint, muleSession, false );
    LOG.debug( "ServiceContext.dispatch(" + dest + ":" + msg.getClass( ).getCanonicalName( ), Exceptions.filterStackTrace( new RuntimeException( ), 3 ) );
    try {
      dispatcherFactory.create( endpoint ).dispatch( muleEvent );
    } catch ( DispatchException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Error while dispatching message (" + msg + ")t o " + dest + " caused by: " + ex.getMessage( ), ex );
    } catch ( MuleException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to obtain service dispatcher reference: "
                                          + ex.getMessage( ), ex );
    }
  }
  
  public static <T> T send( String dest, Object msg ) throws ServiceDispatchException {
    dest = ServiceContextManager.mapEndpointToService( dest );
    MuleEvent context = RequestContext.getEvent( );
    try {
      LOG.debug( "ServiceContext.send(" + dest + ":" + msg.getClass( ).getCanonicalName( ), Exceptions.filterStackTrace( new RuntimeException( ), 3 ) );
      MuleMessage reply = ServiceContextManager.getClient( ).sendDirect( dest, null, new DefaultMuleMessage( msg ) );
      
      if ( reply.getExceptionPayload( ) != null ) {
        throw Exceptions.trace( new ServiceDispatchException( reply.getExceptionPayload( ).getRootException( ).getMessage( ),
                                                                    reply.getExceptionPayload( ).getRootException( ) ) );
      } else {
        return ( T ) reply.getPayload( );
      }
    } catch ( Throwable e ) {
      throw Exceptions.trace( new ServiceDispatchException( "Failed to send message " + msg.getClass( ).getSimpleName( ) + " to service " + dest
                                                                  + " because of " + e.getMessage( ), e ) );
    } finally {
      RequestContext.setEvent( context );
    }
  }
  
}
