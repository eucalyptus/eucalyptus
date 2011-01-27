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
  static {
    Velocity.init( );
  }
  
  public static class HupListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      if ( "123".equals( t.getValue( ) ) ) {
        System.exit( 123 );
      }
    }
  }
  
  private static AtomicMarkableReference<MuleContext>   context           = new AtomicMarkableReference<MuleContext>( null, true );
  private static AtomicBoolean                          ready             = new AtomicBoolean( false );
  private static ConcurrentNavigableMap<String, String> endpointToService = new ConcurrentSkipListMap<String, String>( );
  private static ConcurrentNavigableMap<String, String> serviceToEndpoint = new ConcurrentSkipListMap<String, String>( );
  private static VMMessageDispatcherFactory             dispatcherFactory = new VMMessageDispatcherFactory( );
  private static AtomicReference<MuleClient>            client            = new AtomicReference<MuleClient>( null );
  private static final BootstrapException               failEx            = new BootstrapException(
                                                                                                    "Attempt to use esb client before the service bus has been started." );
  private static final ThreadPool                       ctxMgmtThreadPool = Threads.lookup( ServiceContext.class.getSimpleName( ) ).limitTo( 1 );
  
  public static final restart( ) {
    ctxMgmtThreadPool.getExecutorService( ).submit( new Callable<MuleContext>( ) {
      @Override
      public MuleContext call( ) throws Exception {
        return null;
      }
    } );
    try {
      ServiceContext.shutdown( );
      ServiceContext.startup( );
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
    }
    
  }
  
  private static MuleClient getClient( ) throws MuleException {
    boolean[] bit = new boolean[1];
    MuleContext muleCtx = context.get( bit );  
    if ( context.getReference( ) == null ) {
      LOG.fatal( failEx, failEx );
      System.exit( 123 );
      throw failEx;
    } else if ( client.get( ) == null && client.compareAndSet( null, new MuleClient( context.getReference( ) ) ) ) {
      return client.get( );
    } else {
      return client.get( );
    }
  }
  
  public static void dispatch( String dest, Object msg ) throws ServiceInitializationException, ServiceDispatchException, ServiceStateException {
    if ( ( !dest.startsWith( "vm://" ) && !serviceToEndpoint.containsKey( dest ) ) || dest == null ) {
      dest = "vm://RequestQueue";
    } else if ( !dest.startsWith( "vm://" ) ) {
      dest = serviceToEndpoint.get( dest );
    }
//    try {
    MuleContext muleCtx;
    try {
      muleCtx = ServiceContext.getContext( );
    } catch ( ServiceInitializationException ex ) {
      LOG.debug( ex.getMessage( ) );
      throw ex;
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to obtain service context reference: " + ex.getMessage( ), ex );
    }
    OutboundEndpoint endpoint;
    try {
      endpoint = muleCtx.getRegistry( ).lookupEndpointFactory( ).getOutboundEndpoint( dest );
      if ( !endpoint.getConnector( ).isStarted( ) ) {
        endpoint.getConnector( ).start( );
      }
    } catch ( MuleException ex ) {
      LOG.error( ex , ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to obtain service endpoint reference: " + ex.getMessage( ), ex ); 
    }
    MuleMessage muleMsg = new DefaultMuleMessage( msg );
    MuleSession muleSession;
    try {
      muleSession = new DefaultMuleSession( muleMsg, ( ( AbstractConnector ) endpoint.getConnector( ) ).getSessionHandler( ),
                                                          ServiceContext.getContext( ) );
    } catch ( ServiceStateException ex ) {
      LOG.error( ex , ex );
      throw ex;
    } catch ( MuleException ex ) {
      LOG.error( ex , ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to contruct session: " + ex.getMessage( ), ex );       
    }
    MuleEvent muleEvent = new DefaultMuleEvent( muleMsg, endpoint, muleSession, false );
    LOG.debug( "ServiceContext.dispatch(" + dest + ":" + msg.getClass( ).getCanonicalName( ), Exceptions.filterStackTrace( new RuntimeException( ), 3 ) );
    try {
      dispatcherFactory.create( endpoint ).dispatch( muleEvent );
    } catch ( DispatchException ex ) {
      LOG.error( ex , ex );
      throw new ServiceDispatchException( "Error while dispatching message ("+msg+")t o " + dest + " caused by: " + ex.getMessage( ), ex ); 
    } catch ( MuleException ex ) {
      LOG.error( ex , ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to obtain service dispatcher reference: " + ex.getMessage( ), ex ); 
    }
  }
  
  public static <T> T send( String dest, Object msg ) throws EucalyptusCloudException {
    if ( ( dest.startsWith( "vm://" ) && !endpointToService.containsKey( dest ) ) || dest == null ) {
      throw new EucalyptusCloudException( "Failed to find destination: " + dest, new IllegalArgumentException( "No such endpoint: " + dest + " in endpoints="
                                                                                                               + endpointToService.entrySet( ) ) );
    }
    if ( dest.startsWith( "vm://" ) ) {
      dest = endpointToService.get( dest );
    }
    MuleEvent context = RequestContext.getEvent( );
    try {
      LOG.debug( "ServiceContext.send(" + dest + ":" + msg.getClass( ).getCanonicalName( ), Exceptions.filterStackTrace( new RuntimeException( ), 3 ) );
      MuleMessage reply = ServiceContext.getClient( ).sendDirect( dest, null, new DefaultMuleMessage( msg ) );
      
      if ( reply.getExceptionPayload( ) != null ) {
        EucalyptusCloudException ex = new EucalyptusCloudException( reply.getExceptionPayload( ).getRootException( ).getMessage( ),
                                                                    reply.getExceptionPayload( ).getRootException( ) );
        LOG.trace( ex, ex );
        throw ex;
      } else return ( T ) reply.getPayload( );
    } catch ( Throwable e ) {
      EucalyptusCloudException ex = new EucalyptusCloudException( "Failed to send message " + msg.getClass( ).getSimpleName( ) + " to service " + dest
                                                                  + " because of " + e.getMessage( ), e );
      LOG.trace( ex, ex );
      throw ex;
    } finally {
      RequestContext.setEvent( context );
    }
  }
  
  private static MuleContext createContext( ) throws ServiceInitializationException {
    MuleContextFactory contextFactory = new DefaultMuleContextFactory( );
    MuleContext muleCtx;
    try {
      muleCtx = contextFactory.createMuleContext( builder );
    } catch ( InitialisationException ex ) {
      LOG.error( ex , ex );
      throw new ServiceInitializationException( "Failed to initialize service context because of: " , ex );
    } catch ( ConfigurationException ex ) {
      LOG.error( ex , ex );
      throw new ServiceInitializationException( "Failed to initialize service context because of: " , ex );
    }
    return muleCtx;
  }
  
  public static void startContext( ) throws ServiceInitializationException {
    try {
      if ( !ServiceContext.getContext( ).isInitialised( ) ) {
        ServiceContext.getContext( ).initialise( );
      }
    } catch ( Throwable e ) {
      LOG.error( e, e );
      throw new ServiceInitializationException( "Failed to initialize service context: " + e.getMessage( ), e );
    }
    try {
      ServiceContext.getContext( ).start( );
      endpointToService.clear( );
      serviceToEndpoint.clear( );
      for ( Object o : ServiceContext.getContext( ).getRegistry( ).lookupServices( ) ) {
        Service s = ( Service ) o;
        for ( Object p : s.getInboundRouter( ).getEndpoints( ) ) {
          InboundEndpoint in = ( InboundEndpoint ) p;
          endpointToService.put( in.getEndpointURI( ).toString( ), s.getName( ) );
          serviceToEndpoint.put( s.getName( ), in.getEndpointURI( ).toString( ) );
        }
      }
    } catch ( Throwable e ) {
      LOG.error( e, e );
      throw new ServiceInitializationException( "Failed to start service context: " + e.getMessage( ), e );
    }
  }
  public static Integer SERVICE_CONTEXT_RELOAD_TIMEOUT = 10*1000;
  public static MuleContext getContext( ) throws ServiceInitializationException, ServiceStateException {
    boolean[] bit = new boolean[1];
    MuleContext ref = null;
    if( ( ref = context.get( bit ) ) == null && bit[0] ) {
      Integer i = 0;
      do {
        try {
          TimeUnit.MILLISECONDS.sleep( 500 );
          i += 500;
        } catch ( InterruptedException ex ) {
          LOG.error( ex , ex );
        }
      } while( i < SERVICE_CONTEXT_RELOAD_TIMEOUT && ( ref = context.get( bit ) ) == null && bit[0] );
      if( ref == null ) {
        throw new ServiceStateException( "Timed-out obtaining reference service context.  Waited for : " + SERVICE_CONTEXT_RELOAD_TIMEOUT + " milliseconds." );
      } else {
        return ref;
      }
    } else if( !bit[0] ) {
      throw new ServiceStateException( "Attempt to reference service context before it is ready." );
    } else {
      return ref;
    }
  }
  
  public static synchronized void shutdown( ) {
    MuleContext muleCtx = context.getReference( );
    if( muleCtx != null ) {
      context.compareAndSet( muleCtx, null, false, true );
      try {
        muleCtx.stop( );
        muleCtx.dispose( );
      } catch ( MuleException ex ) {
        LOG.error( ex , ex );
      } finally {
        context.compareAndSet( null, null, true, false );
      }
    } else {
      context.compareAndSet( null, null, false, false );
    }
  }
  
  static boolean loadContext( ) {
    List<ComponentId> components = ComponentIds.listEnabled( );
    LOG.info( "The following components have been identified as active: " );
    for ( ComponentId c : components ) {
      LOG.info( "-> " + c );
    }
    Set<ConfigResource> configs = ServiceContext.renderServiceConfigurations( components );
    for ( ConfigResource cfg : configs ) {
      LOG.info( "-> Rendered cfg: " + cfg.getResourceName( ) );
    }
    try {
      ServiceContext.builder = new SpringXmlConfigurationBuilder( configs.toArray( new ConfigResource[] {} ) );
    } catch ( Exception e ) {
      LOG.fatal( "Failed to bootstrap services.", e );
      return false;
    }
    return true;
  }
  
  private static Set<ConfigResource> renderServiceConfigurations( List<ComponentId> components ) {
    Set<ConfigResource> configs = Sets.newHashSet( );
    for ( ComponentId thisComponent : components ) {
      VelocityContext context = new VelocityContext( );
      context.put( "components", components );
      context.put( "thisComponent", thisComponent );
      LOG.info( "-> Rendering configuration for " + thisComponent.name( ) );
      String templateName = thisComponent.getServiceModel( );
      StringWriter out = new StringWriter( );
      try {
        Velocity.evaluate( context, out, thisComponent.getServiceModelFileName( ), thisComponent.getServiceModelAsReader( ) );
        ConfigResource configRsc = new ConfigResource( thisComponent.getServiceModelFileName( ), new ByteArrayInputStream( out.toString( ).getBytes( ) ) );
        configs.add( configRsc );
      } catch ( Throwable ex ) {
        LOG.error( "Failed to render service model configuration for: " + thisComponent + " because of: " + ex.getMessage( ), ex );
      }
    }
    return configs;
  }
  private static boolean firstTime = true; 
  public static synchronized boolean startup( ) {
    try {
      LOG.info( "Loading system bus." );
      loadContext( );
    } catch ( Exception e ) {
      LOG.fatal( "Failed to configure services.", e );
      return false;
    }
    if( firstTime ) {
      firstTime = false;
      context.compareAndSet( null, null, false, true );
      MuleContext muleCtx = createContext( );
      context.compareAndSet( null, muleCtx, true, true );
    }
    
    ServiceContext.context.compareAndSet( null, null, false, false );
    MuleContext muleCtx = createContext( );
    if ( !ServiceContext.context.compareAndSet( null, muleCtx, false, false ) ) {
      throw new ServiceInitializationException( "Service context initialized twice." );
    }
    try {
      startContext( );
    } catch ( Exception e ) {
      LOG.fatal( "Failed to start services.", e );
      return false;
    }
    return true;
  }
  
}
