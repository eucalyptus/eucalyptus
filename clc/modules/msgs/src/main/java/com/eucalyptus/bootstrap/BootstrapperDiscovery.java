package com.eucalyptus.bootstrap;

import static com.eucalyptus.system.Ats.From;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventType;
import com.google.common.collect.Lists;
import com.eucalyptus.records.EventRecord;

public class BootstrapperDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( BootstrapperDiscovery.class );
  private static List<Class>         bootstrappers = Lists.newArrayList( );

  public BootstrapperDiscovery() {}
  
  @Override
  public boolean processClass( Class candidate ) throws Throwable {
    String bc = candidate.getCanonicalName( );
    Class bootstrapper = this.getBootstrapper( candidate );
    if ( !From( candidate ).has( RunDuring.class ) ) {
      throw BootstrapException.throwFatal( "Bootstrap class does not specify execution stage (RunDuring.value=Bootstrap.Stage): " + bc );
    } else if ( !From( candidate ).has( Provides.class ) ) {
      throw BootstrapException.throwFatal( "Bootstrap class does not specify provided component (Provides.value=Component): " + bc );
    } //TODO: maybe more checks at pre-load time for bootstrappers.
    this.bootstrappers.add( bootstrapper );
    return true;
  }

  @SuppressWarnings( "unchecked" )
  public static List<Bootstrapper> getBootstrappers( ) {
    List<Bootstrapper> ret = Lists.newArrayList( );
    for ( Class c : bootstrappers ) {
      if ( c.equals( SystemBootstrapper.class ) ) continue;
      try {
        EventRecord.here( BootstrapperDiscovery.class, EventType.BOOTSTRAPPER_INIT,"<init>()V", c.getCanonicalName( ) ).info( );
        try {
          ret.add( ( Bootstrapper ) c.newInstance( ) );
        } catch ( Exception e ) {
          EventRecord.here( BootstrapperDiscovery.class, EventType.BOOTSTRAPPER_INIT,"getInstance()L", c.getCanonicalName( ) ).info( );
          Method m = c.getDeclaredMethod( "getInstance", new Class[] {} );
          ret.add( ( Bootstrapper ) m.invoke( null, new Object[] {} ) );
        }
      } catch ( Exception e ) {
        throw BootstrapException.throwFatal( "Error in <init>()V and getInstance()L; in bootstrapper: " + c.getCanonicalName( ), e );
      }
    }
    return ret;
  }

  @SuppressWarnings( "unchecked" )
  private Class getBootstrapper( Class candidate ) throws Exception {
    if ( Modifier.isAbstract( candidate.getModifiers( ) ) ) throw new InstantiationException( candidate.getName( ) + " is abstract." );
    if ( !Bootstrapper.class.isAssignableFrom( candidate ) ) throw new InstantiationException( candidate + " does not conform to " + Bootstrapper.class );
    LOG.debug( "Candidate bootstrapper: " + candidate.getName( ) );
    if ( !Modifier.isPublic( candidate.getDeclaredConstructor( new Class[] {} ).getModifiers( ) ) ) {
      Method factory = candidate.getDeclaredMethod( "getInstance", new Class[] {} );
      if ( !Modifier.isStatic( factory.getModifiers( ) ) || !Modifier.isPublic( factory.getModifiers( ) ) ) {
        throw new InstantiationException( candidate.getCanonicalName( ) + " does not declare public <init>()V or public static getInstance()L;" );
      }
    }
    LOG.debug( "Found bootstrapper: " + candidate.getName( ) );
    return candidate;
  }

  @Override
  public Double getPriority( ) {
    return 0.0d;
  }

}
