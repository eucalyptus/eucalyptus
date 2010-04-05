package com.eucalyptus.config;

import edu.ucsb.eucalyptus.msgs.RegisterComponentType;

public interface ServiceBuilder<T extends ComponentConfiguration> {
  public abstract Boolean isLocal( );
  public abstract T newInstance( );
  public abstract T newInstance( String name, String host, Integer port, RegisterComponentType request );
  public abstract void fireStart( ComponentConfiguration config ) throws ServiceRegistrationException;
  public abstract void fireStop( ComponentConfiguration config ) throws ServiceRegistrationException;
  /**
   * Do input validation on the parameters.
   * @param name
   * @param host
   * @param port
   * @return true if request accepted.
   * @throws ServiceRegistrationException
   */
  public abstract Boolean checkAdd( String name, String host, Integer port ) throws ServiceRegistrationException;
  public abstract Boolean checkRemove( String name ) throws ServiceRegistrationException;
}
