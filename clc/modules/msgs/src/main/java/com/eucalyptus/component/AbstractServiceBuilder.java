package com.eucalyptus.component;

import java.util.List;
import org.apache.log4j.Logger;

import com.eucalyptus.component.event.DisableComponentEvent;
import com.eucalyptus.component.event.EnableComponentEvent;
import com.eucalyptus.component.event.LifecycleEvent;
import com.eucalyptus.component.event.LifecycleEvents;
import com.eucalyptus.component.event.StartComponentEvent;
import com.eucalyptus.component.event.StopComponentEvent;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.MultiDatabasePropertyEntry;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.configurable.SingletonDatabasePropertyEntry;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Internets;

public abstract class AbstractServiceBuilder<T extends ServiceConfiguration> implements ServiceBuilder<T> {
  private static Logger LOG = Logger.getLogger( AbstractServiceBuilder.class );
  public abstract T newInstance( String partition, String name, String host, Integer port );
  protected abstract T newInstance( );

  @Override
  public Boolean checkRemove( String partition, String name ) throws ServiceRegistrationException {
    try {
      this.lookupByName( name );
      return true;
    } catch ( ServiceRegistrationException e ) {
      throw e;
    } catch ( Throwable e ) {
      LOG.error( e, e );
      return false;
    }
  }

  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_START, config.getFullName( ).toString( ), config.toString( ) ).debug( );
    try {
      List<ConfigurableProperty> props = PropertyDirectory.getPendingPropertyEntrySet( config.getComponentId( ).name( ) );
      for ( ConfigurableProperty prop : props ) {
        ConfigurableProperty addProp = null;
        if ( prop instanceof SingletonDatabasePropertyEntry ) {
          addProp = prop;
        } else if ( prop instanceof MultiDatabasePropertyEntry ) {
          addProp = ( ( MultiDatabasePropertyEntry ) prop ).getClone( config.getName( ) );
        }
        PropertyDirectory.addProperty( addProp );
      }
    } catch ( Throwable ex ) {
      LOG.error( ex , ex );
    }

    try {
      ListenerRegistry.getInstance( ).fireEvent( config.getComponentId( ).getClass( ), LifecycleEvents.start( config ) );
    } catch ( EventFailedException e1 ) {
      LOG.error( e1, e1 );
      throw new ServiceRegistrationException( e1.getMessage( ), e1 );
    }
  }

  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_STOP, config.getFullName( ).toString( ), config.toString( ) ).debug( );
    try {
      ListenerRegistry.getInstance( ).fireEvent( config.getComponentId( ).getClass( ), LifecycleEvents.stop( config ) );
    } catch ( EventFailedException e1 ) {
      LOG.error( e1, e1 );
      throw new ServiceRegistrationException( e1.getMessage( ), e1 );
    }
    
    try {
      List<ConfigurableProperty> props = PropertyDirectory.getPropertyEntrySet( config.getComponentId( ).name( ) );
      for ( ConfigurableProperty prop : props ) {
        if ( prop instanceof SingletonDatabasePropertyEntry ) {
          //noop
        } else if ( prop instanceof MultiDatabasePropertyEntry ) {
          ( ( MultiDatabasePropertyEntry ) prop ).setIdentifierValue( config.getName( ) );
        }
        PropertyDirectory.removeProperty( prop );
      }
    } catch ( Throwable ex ) {
      LOG.error( ex , ex );
    }
  }

  /**
   * @see com.eucalyptus.component.ServiceBuilder#fireEnable(com.eucalyptus.component.ServiceConfiguration)
   * @param config
   * @throws ServiceRegistrationException
   */
  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_ENABLE, config.getFullName( ).toString( ), config.toString( ) ).debug( );
    try {
      ListenerRegistry.getInstance( ).fireEvent( config.getComponentId( ).getClass( ), LifecycleEvents.enable( config ) );
    } catch ( EventFailedException e1 ) {
      LOG.error( e1, e1 );
      throw new ServiceRegistrationException( e1.getMessage( ), e1 );
    }
  }

  /**
   * @see com.eucalyptus.component.ServiceBuilder#fireDisable(com.eucalyptus.component.ServiceConfiguration)
   * @param config
   * @throws ServiceRegistrationException
   */
  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_DISABLE, config.getFullName( ).toString( ), config.toString( ) ).debug( );
    try {
      ListenerRegistry.getInstance( ).fireEvent( config.getComponentId( ).getClass( ), LifecycleEvents.disable( config ) );
    } catch ( EventFailedException e1 ) {
      LOG.error( e1, e1 );
      throw new ServiceRegistrationException( e1.getMessage( ), e1 );
    }
  }

  /** ASAP:FIXME:GRZE **/
  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {}

  
}
