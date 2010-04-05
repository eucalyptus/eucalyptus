package com.eucalyptus.config;

import com.eucalyptus.event.EventVetoedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.StartComponentEvent;
import com.eucalyptus.event.StopComponentEvent;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.NetworkUtil;

public abstract class AbstractServiceBuilder<T extends ComponentConfiguration> implements ServiceBuilder<T> {
  
  @Override
  public Boolean checkAdd( String name, String host, Integer port ) throws ServiceRegistrationException {
    try {
      if ( !NetworkUtil.testGoodAddress( host ) ) {
        throw new EucalyptusCloudException( "Components cannot be registered using local, link-local, or multicast addresses." );
      } else if ( NetworkUtil.testLocal( host ) && !this.isLocal( ) ) {
        throw new EucalyptusCloudException( "You do not have a local " + this.newInstance( ).getClass( ).getSimpleName( ).replaceAll( "Configuration", "" )
                                            + " enabled (or it is not installed)." );
      }
    } catch ( EucalyptusCloudException e ) {
      throw new ServiceRegistrationException( e.getMessage( ), e );
    } catch ( Exception e ) {
      throw new ServiceRegistrationException( "Service registration failed: " + e.getMessage( ), e );
    }
    return true;
  }
  
  @Override
  public void fireStart( ComponentConfiguration config ) throws ServiceRegistrationException {
    StartComponentEvent e = null;
    if( config.isLocal( ) ) {
      e = StartComponentEvent.getLocal( config );      
    } else {
      e = StartComponentEvent.getRemote( config );
    }
    try {
      ListenerRegistry.getInstance( ).fireEvent( config.getComponent( ), e );
    } catch ( EventVetoedException e1 ) {
      throw new ServiceRegistrationException( e1.getMessage( ), e1 );
    }
  }

  /**
   * @see com.eucalyptus.config.ServiceBuilder#fireStop(com.eucalyptus.config.ComponentConfiguration)
   * @param config
   * @throws ServiceRegistrationException
   */
  @Override
  public void fireStop( ComponentConfiguration config ) throws ServiceRegistrationException {
    StopComponentEvent e = null;
    if ( NetworkUtil.testLocal( config.getHostName( ) ) ) {
      e = StopComponentEvent.getLocal( config );
    } else {
      e = StopComponentEvent.getRemote( config );
    }
    try {
      ListenerRegistry.getInstance( ).fireEvent( config.getComponent( ), e );
    } catch ( EventVetoedException e1 ) {
      throw new ServiceRegistrationException( e1.getMessage( ), e1 );
    }
  }

  
  
}
