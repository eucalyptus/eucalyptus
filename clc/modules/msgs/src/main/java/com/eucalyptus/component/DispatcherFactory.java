package com.eucalyptus.component;

import javassist.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;

public abstract class DispatcherFactory {
  private static Logger            LOG = Logger.getLogger( DispatcherFactory.class );
  private static DispatcherFactory factory;
  
  public static void setFactory( DispatcherFactory factory ) {
    synchronized ( DispatcherFactory.class ) {
      LOG.info( "Setting the dispatcher factory to: " + factory.getClass( ).getCanonicalName( ) );
      DispatcherFactory.factory = factory;
    }
  }
  
  public static Dispatcher build( Component parent, Service service ) {
    return DispatcherFactory.factory.buildChild( parent, service );
  }
  
  public abstract Dispatcher buildChild( Component parent, Service service );
  
  public static class DispatcherFactoryDiscovery extends ServiceJarDiscovery {

    @Override
    public Double getPriority( ) {
      return 0.3;
    }

    @Override
    public boolean processsClass( Class candidate ) throws Throwable {
      if( DispatcherFactory.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) ) && !Modifier.isInterface( candidate.getModifiers( ) ) ) {
        DispatcherFactory.setFactory( ( DispatcherFactory ) candidate.newInstance( ) );
        return true;
      } else {
        return false;
      }
    }
    
  }
}
