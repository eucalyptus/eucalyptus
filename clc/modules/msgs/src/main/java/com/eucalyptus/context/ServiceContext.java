package com.eucalyptus.context;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.registry.Registry;
import org.mule.config.ConfigResource;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.client.MuleClient;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Resource;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.event.PassiveEventListener;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.Lists;

@ConfigurableClass( root = "system", description = "Parameters having to do with the system's state.  Mostly read-only." )
public class ServiceContext {
  private static Logger                        LOG                      = Logger.getLogger( ServiceContext.class );
  private static SpringXmlConfigurationBuilder builder;
  @ConfigurableField( initial = "16", description = "Max queue length allowed per service stage.", changeListener = HupListener.class )
  public static Integer                        MAX_OUTSTANDING_MESSAGES = 16;
  @ConfigurableField( initial = "0", description = "Do a soft reset.", changeListener = HupListener.class )
  public static Integer                        HUP                      = 0;
  
  public static class HupListener extends PassiveEventListener<ConfigurableProperty> {
    @Override
    public void firingEvent( ConfigurableProperty t ) {
      if ( "123".equals( t.getValue( ) ) ) {
        System.exit( 123 );
      }
    }
  }
  
  private static AtomicReference<MuleContext> context = new AtomicReference<MuleContext>( null );
  private static AtomicReference<MuleClient> client = new AtomicReference<MuleClient>( null );
  private static final BootstrapException failEx = new BootstrapException( "Attempt to use esb client before the service bus has been started." );
  private static MuleClient getClient( ) throws MuleException {
    if( context.get( ) == null ) {
      LOG.fatal( failEx, failEx );
      System.exit( 123 );
      throw failEx;
    } else if( client.get( ) == null && client.compareAndSet( null, new MuleClient( context.get( ) ) ) ) {
      return client.get( );
    } else {
      return client.get( );
    }
  }

  public static void dispatch( String dest, Object msg ) {
    MuleEvent context = RequestContext.getEvent( );
    try {
      ServiceContext.getClient( ).sendDirect( dest, null, msg, null );
    } catch ( MuleException e ) {
      LOG.error( e );
    } finally {
      RequestContext.setEvent( context );
    }
  }

  public static <T> T send( String dest, Object msg ) throws EucalyptusCloudException {
    MuleEvent context = RequestContext.getEvent( );
    try {
      MuleMessage reply = ServiceContext.getClient( ).sendDirect( dest, null, msg, null );

      if ( reply.getExceptionPayload( ) != null ) throw new EucalyptusCloudException( reply.getExceptionPayload( ).getRootException( ).getMessage( ), reply.getExceptionPayload( ).getRootException( ) );
      else return (T) reply.getPayload( );
    } catch ( MuleException e ) {
      LOG.error( e, e );
      throw new EucalyptusCloudException( e );
    } finally {
      RequestContext.setEvent( context );
    }
  }

  
  public static void buildContext( List<ConfigResource> configs ) {
    ServiceContext.builder = new SpringXmlConfigurationBuilder( configs.toArray( new ConfigResource[] {} ) );
  }
  
  public static void createContext( ) {
    MuleContextFactory contextFactory = new DefaultMuleContextFactory( );
    try {
      MuleContext context = contextFactory.createMuleContext( builder );
      if ( !ServiceContext.context.compareAndSet( null, context ) ) {
        throw new ServiceInitializationException( "Service context initialized twice." );
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new ServiceInitializationException( "Failed to build service context.", e );
    }
  }
  
  public static void startContext( ) {
    try {
      if ( !ServiceContext.getContext( ).isInitialised( ) ) {
        ServiceContext.getContext( ).initialise( );
      }
    } catch ( Throwable e ) {
      LOG.error( e, e );
      throw new ServiceInitializationException( "Failed to initialize service context.", e );
    }
    try {
      ServiceContext.getContext( ).start( );
    } catch ( Throwable e ) {
      LOG.error( e, e );
      throw new ServiceInitializationException( "Failed to start service context.", e );
    }
  }
  
  public static MuleContext getContext( ) {
    if ( ServiceContext.context.get( ) == null ) {
      throw new ServiceInitializationException( "Attempt to reference service context before it is ready." );
    } else {
      return context.get( );
    }
  }
  
  public static Registry getRegistry( ) {
    return ServiceContext.getContext( ).getRegistry( );
  }
  
  public static void stopContext( ) {
    try {
      ServiceContext.getContext( ).stop( );
      ServiceContext.getContext( ).dispose( );
    } catch ( Throwable e ) {
      LOG.debug( e, e );
    }
  }
  
  @Provides( Component.bootstrap )
  @RunDuring( Bootstrap.Stage.CloudServiceInit )
  public static class ServiceBootstrapper extends Bootstrapper {
    
    public ServiceBootstrapper( ) {}
    
    @Override
    public boolean load( Stage current ) throws Exception {
      List<ConfigResource> configs = Lists.newArrayList( );
      for ( com.eucalyptus.component.Component comp : Components.list( ) ) {
        if ( comp.isEnabled( ) ) {
          Resource rsc = comp.getConfiguration( ).getResource( );
          if( rsc != null ) {
            LOG.info( LogUtil.subheader( "Preparing configuration for: " + rsc ) );
            configs.addAll( rsc.getConfigurations( ) );
          }
        }
      }
      for ( ConfigResource cfg : configs ) {
        LOG.info( "-> Loaded cfg: " + cfg.getUrl( ) );
      }
      try {
        ServiceContext.buildContext( configs );
      } catch ( Exception e ) {
        LOG.fatal( "Failed to bootstrap services.", e );
        return false;
      }
      return true;
    }
    
    @Override
    public boolean start( ) throws Exception {
      try {
        LOG.info( "Starting up system bus." );
        ServiceContext.createContext( );
      } catch ( Exception e ) {
        LOG.fatal( "Failed to configure services.", e );
        return false;
      }
      try {
        ServiceContext.startContext( );
      } catch ( Exception e ) {
        LOG.fatal( "Failed to start services.", e );
        return false;
      }
      return true;
    }
    
    @Override
    public boolean stop( ) throws Exception {
      ServiceContext.stopContext( );
      return true;
    }
    
  }
  
}
