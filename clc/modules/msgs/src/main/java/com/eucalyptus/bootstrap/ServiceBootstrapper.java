package com.eucalyptus.bootstrap;

import java.util.List;

import org.apache.log4j.Logger;
import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextFactory;
import org.mule.config.ConfigResource;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;

import com.google.common.collect.Lists;

@Provides(resource=Resource.CloudService)
public class ServiceBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( ServiceBootstrapper.class );
  private MuleContext context;
  private MuleContextFactory contextFactory;
  private SpringXmlConfigurationBuilder builder;

  public ServiceBootstrapper( ) {
    super( );
    this.contextFactory = new DefaultMuleContextFactory( );
  }

  @Override
  public boolean load( Resource current ) throws Exception {
    List<ConfigResource> configs = Lists.newArrayList( );
    for( ResourceProvider r : current.getProviders( ) ) {
      LOG.info( "Preparing configuration for: " + r );
      configs.addAll( r.getConfigurations( ) );
    }
    for( ConfigResource cfg : configs ) {
      LOG.info( "-> Loaded cfg: " + cfg.getUrl( ) );
    }
    try {
      this.builder = new SpringXmlConfigurationBuilder( configs.toArray( new ConfigResource[]{} ) );
    } catch ( Exception e ) {
      LOG.fatal( "Failed to bootstrap services.", e );
      return false;
    }
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    try {
      this.context = this.contextFactory.createMuleContext( this.builder );
    } catch ( Exception e ) {
      LOG.fatal( "Failed to configure services.", e );
      return false;
    }
    try {
      this.context.start( );
    } catch ( Exception e ) {
      LOG.fatal( "Failed to start services.", e );
      return false;
    }
    return true;
  }

}
