/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

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
