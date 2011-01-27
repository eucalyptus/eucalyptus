package com.eucalyptus.context;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleSession;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.module.client.MuleClient;
import org.mule.transport.AbstractConnector;
import org.mule.transport.vm.VMMessageDispatcherFactory;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.util.Exceptions;

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
      if( Bootstrap.isFinished( ) ) {
        ServiceContextManager.restart( );
      }
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
