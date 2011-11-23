package com.eucalyptus.component;

import com.eucalyptus.component.Faults.CheckException;


/**
 * Interface providing support for creating service configurations.<br/>
 * Used by configuration service as follows:
 * 1. checkAdd/checkRemove
 * 2. add/remove
 * 3. fireStart/fireStop
 * 4. fireEnable/fireDisable
 * 
 * @author decker
 * @param <T>
 */
public interface ServiceBuilder<T extends ServiceConfiguration> {
  public abstract ComponentId getComponentId();
  /**
   * Do input validation on the parameters.
   * @param partition TODO
   * @param name
   * @param host
   * @param port
   * @return true if request accepted.
   * @throws ServiceRegistrationException
   */
  public abstract Boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException;
  public abstract void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException;
  public abstract void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException;
  public abstract void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException;
  public abstract void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException;
  public abstract void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException, CheckException;
  public abstract T newInstance( String partition, String name, String host, Integer port );
  abstract T newInstance( );

}
