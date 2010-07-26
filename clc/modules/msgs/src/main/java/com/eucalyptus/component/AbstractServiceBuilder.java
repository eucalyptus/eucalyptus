package com.eucalyptus.component;

import java.util.List;

import com.eucalyptus.component.event.StartComponentEvent;
import com.eucalyptus.component.event.StopComponentEvent;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.MultiDatabasePropertyEntry;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.configurable.SingletonDatabasePropertyEntry;
import com.eucalyptus.event.EventVetoedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.util.NetworkUtil;

public abstract class AbstractServiceBuilder<T extends ServiceConfiguration> implements ServiceBuilder<T> {

  @Override
  public Boolean checkRemove( String name ) throws ServiceRegistrationException {
    try {
      this.lookupByName( name );
      return true;
    } catch ( Exception e ) {
      return false;
    }
  }

  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    StartComponentEvent e = null;
    if ( config.isLocal( ) ) {
      e = StartComponentEvent.getLocal( config );
    } else {
      e = StartComponentEvent.getRemote( config );
    }
    try {
      ListenerRegistry.getInstance( ).fireEvent( config.getComponent( ), e );
    } catch ( EventVetoedException e1 ) {
      throw new ServiceRegistrationException( e1.getMessage( ), e1 );
    }
    
    List<ConfigurableProperty> props = PropertyDirectory.getPendingPropertyEntrySet(config.getComponent().name());
    for ( ConfigurableProperty prop : props ) {
      ConfigurableProperty addProp = null;
      if (prop instanceof SingletonDatabasePropertyEntry) {
    	  addProp = prop;
      } else if (prop instanceof MultiDatabasePropertyEntry) {
    	  addProp = ((MultiDatabasePropertyEntry) prop).getClone(config.getName());
      }
      if ( addProp != null ) {
  	    PropertyDirectory.addProperty(addProp);
      }
    }
  }

  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
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
    
    List<ConfigurableProperty> props = PropertyDirectory.getPropertyEntrySet(config.getComponent().name());
    for ( ConfigurableProperty prop : props ) {
      if(prop instanceof SingletonDatabasePropertyEntry) {   	 
      } else if ( prop instanceof MultiDatabasePropertyEntry) {
    	 ((MultiDatabasePropertyEntry) prop).setIdentifierValue(config.getName());
      }
      PropertyDirectory.removeProperty(prop);	
    }
  }

}
