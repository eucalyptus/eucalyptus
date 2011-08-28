package com.eucalyptus.context;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.Channels;
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
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;

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
      if ( Bootstrap.isFinished( ) ) {
        ServiceContextManager.restartSync( );
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
    } catch ( MuleException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to contruct session: " + ex.getMessage( ), ex );
    }
    MuleEvent muleEvent = new DefaultMuleEvent( muleMsg, endpoint, muleSession, false );
//    LOG.debug( "ServiceContext.dispatch(" + dest + ":" + msg.getClass( ).getCanonicalName( )/*, Exceptions.filterStackTrace( new RuntimeException( ), 3 )*/ );
    final Context ctx = msg instanceof BaseMessage
      ? Contexts.createWrapped( dest, ( BaseMessage ) msg )
      : null;
    try {
      dispatcherFactory.create( endpoint ).dispatch( muleEvent );
    } catch ( DispatchException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Error while dispatching message (" + msg + ")t o " + dest + " caused by: " + ex.getMessage( ), ex );
    } catch ( MuleException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to obtain service dispatcher reference: "
                                          + ex.getMessage( ), ex );
    } finally {
      Threads.lookup( Empyrean.class, ServiceContext.class ).submit( new Runnable( ) {
        @Override
        public void run( ) {
          try {
            TimeUnit.SECONDS.sleep( 60 );
            Contexts.clear( ctx );
          } catch ( InterruptedException ex ) {
            Thread.currentThread( ).interrupt( );
            return;
          }
        }
      } );
    }
  }
  
  public static <T> T send( String dest, Object msg ) throws ServiceDispatchException {
    dest = ServiceContextManager.mapEndpointToService( dest );
    MuleEvent context = RequestContext.getEvent( );
    Context ctx = null;
    if ( msg instanceof BaseMessage ) {
      ctx = Contexts.createWrapped( dest, ( BaseMessage ) msg );
    }
    try {
//      LOG.debug( "ServiceContext.send(" + dest + ":" + msg.getClass( ).getCanonicalName( )/*, Exceptions.filterStackTrace( new RuntimeException( ), 3 )*/ );
      MuleMessage reply = ServiceContextManager.getClient( ).sendDirect( dest, null, new DefaultMuleMessage( msg ) );
      
      if ( reply.getExceptionPayload( ) != null ) {
        throw Exceptions.trace( new ServiceDispatchException( reply.getExceptionPayload( ).getRootException( ).getMessage( ),
                                                                    reply.getExceptionPayload( ).getRootException( ) ) );
      } else {
        return ( T ) reply.getPayload( );
      }
    } catch ( Exception e ) {
      throw Exceptions.trace( new ServiceDispatchException( "Failed to send message " + msg.getClass( ).getSimpleName( ) + " to service " + dest
                                                                  + " because: " + e.getMessage( ), e ) );
    } finally {
      Contexts.clear( ctx );
      RequestContext.setEvent( context );
    }
  }
  
  @SuppressWarnings( "unchecked" )
  public static void response( BaseMessage responseMessage ) {
    EventRecord.here( ServiceContext.class, EventType.MSG_REPLY, responseMessage.getCorrelationId( ), responseMessage.getClass( ).getSimpleName( ) ).debug( );
    if ( responseMessage instanceof ExceptionResponseType ) {
      Logs.exhaust( ).trace( responseMessage );
    }
    String corrId = responseMessage.getCorrelationId( );
    try {
      Context context = Contexts.lookup( corrId );
      Channel channel = context.getChannel( );
      Channels.write( channel, responseMessage );
      Contexts.clear( context );
    } catch ( NoSuchContextException e ) {
      LOG.warn( "Received a reply for absent client:  No channel to write response message.", e );
      LOG.debug( responseMessage );
    }
  }
  
}
