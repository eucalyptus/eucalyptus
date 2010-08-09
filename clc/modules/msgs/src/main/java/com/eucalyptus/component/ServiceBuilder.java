package com.eucalyptus.component;

import java.net.URI;
import java.util.List;


/**
 * Interface providing support for creating service configurations.<br/>
 * Used by configuration service as follows:
 * 1. checkAdd/checkRemove
 * 2. add/remove
 * 3. fireStart/fireStop
 * @author decker
 *
 * @param <T>
 */
public interface ServiceBuilder<T extends ServiceConfiguration> {
  public abstract Component getComponent();
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
  public abstract ServiceConfiguration remove( ServiceConfiguration config ) throws ServiceRegistrationException;
  public abstract ServiceConfiguration add( String name, String host, Integer port ) throws ServiceRegistrationException;
  /**
   * NOTE: This method does not necessarily return the cannonical copy of the service configuration.
   * @param uri
   * @return ServiceConfiguration
   * @throws ServiceRegistrationException
   */
  public abstract ServiceConfiguration toConfiguration( URI uri ) throws ServiceRegistrationException;
  public abstract void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException;
  public abstract void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException;
  public abstract List<T> list() throws ServiceRegistrationException;
  public abstract T lookupByName( String name ) throws ServiceRegistrationException;
  public abstract T lookupByHost( String name ) throws ServiceRegistrationException;
}
