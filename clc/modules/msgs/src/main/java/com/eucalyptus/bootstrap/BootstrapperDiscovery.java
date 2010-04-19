package com.eucalyptus.bootstrap;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.apache.log4j.Logger;
import com.google.common.collect.Lists;

public class BootstrapperDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( BootstrapperDiscovery.class );
  private static List<Class>         bootstrappers = Lists.newArrayList( );

  public BootstrapperDiscovery() {}
  
  @Override
  public boolean processsClass( Class candidate ) throws Throwable {
    Class bootstrapper = this.getBootstrapper( candidate );
    LOG.info( "---> Loading bootstrapper from entry: " + bootstrapper.getName( ) );
    this.bootstrappers.add( bootstrapper );
    return true;
  }

  @SuppressWarnings( "unchecked" )
  public static List<Bootstrapper> getBootstrappers( ) {
    List<Bootstrapper> ret = Lists.newArrayList( );
    for ( Class c : bootstrappers ) {
      if ( c.equals( SystemBootstrapper.class ) ) continue;
      try {
        LOG.debug( "-> Calling <init>()V on bootstrapper: " + c.getCanonicalName( ) );
        try {
          ret.add( ( Bootstrapper ) c.newInstance( ) );
        } catch ( Exception e ) {
          LOG.debug( "-> Calling getInstance()L; on bootstrapper: " + c.getCanonicalName( ) );
          Method m = c.getDeclaredMethod( "getInstance", new Class[] {} );
          ret.add( ( Bootstrapper ) m.invoke( null, new Object[] {} ) );
        }
      } catch ( Exception e ) {
        LOG.warn( "Error in <init>()V and getInstance()L; in bootstrapper: " + c.getCanonicalName( ) );
        //        LOG.warn( e.getMessage( ) );
        // LOG.debug( e, e );
      }
    }
    return ret;
  }

  @SuppressWarnings( "unchecked" )
  private Class getBootstrapper( Class candidate ) throws Exception {
    if ( Bootstrapper.class.equals( candidate ) ) throw new InstantiationException( Bootstrapper.class + " is abstract." );
    if ( !Bootstrapper.class.isAssignableFrom( candidate ) ) throw new InstantiationException( candidate + " does not conform to " + Bootstrapper.class );
    LOG.warn( "Candidate bootstrapper: " + candidate.getName( ) );
    if ( !Modifier.isPublic( candidate.getDeclaredConstructor( new Class[] {} ).getModifiers( ) ) ) {
      Method factory = candidate.getDeclaredMethod( "getInstance", new Class[] {} );
      if ( !Modifier.isStatic( factory.getModifiers( ) ) || !Modifier.isPublic( factory.getModifiers( ) ) ) {
        throw new InstantiationException( candidate.getCanonicalName( ) + " does not declare public <init>()V or public static getInstance()L;" );
      }
    }
    LOG.info( "Found bootstrapper: " + candidate.getName( ) );
    return candidate;
  }

  @Override
  public Double getPriority( ) {
    return 0.0d;
  }

}
