package com.eucalyptus.system;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.SystemBootstrapper;

public class Threads {
  private static Logger LOG = Logger.getLogger( Threads.class );
  private static ThreadGroup          singletonGroup;
  static class EucalyptusThreadGroup extends ThreadGroup {
    EucalyptusThreadGroup( ) {
      super( "Eucalyptus" );
    }
  }
  public static ThreadGroup getThreadGroup( ) {
    synchronized ( SystemBootstrapper.class ) {
      if ( singletonGroup == null ) {
        singletonGroup = new EucalyptusThreadGroup( );
        LOG.info( "Creating Bootstrapper instance." );
      } else {
        LOG.info( "Returning Bootstrapper instance." );
      }
    }
    return singletonGroup;
  }

  public static Thread newThread( Runnable r, String name ) {
    return new Thread( getThreadGroup( ), r, name );
  }

  public static Thread newThread( Runnable r ) {
    return new Thread( getThreadGroup( ), r );
  }

}
