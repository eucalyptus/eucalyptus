/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.simplequeue.config;

import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.simplequeue.DeregisterSimpleQueueType;
import com.eucalyptus.simplequeue.DescribeSimpleQueueType;
import com.eucalyptus.simplequeue.ModifySimpleQueueAttributeType;
import com.eucalyptus.simplequeue.RegisterSimpleQueueType;
import com.eucalyptus.simplequeue.SimpleQueue;
import org.apache.log4j.Logger;

/**
 *
 */
@ComponentPart( SimpleQueue.class )
@Handles( {
    DeregisterSimpleQueueType.class,
    DescribeSimpleQueueType.class,
    ModifySimpleQueueAttributeType.class,
    RegisterSimpleQueueType.class,
} )
public class SimpleQueueServiceBuilder extends AbstractServiceBuilder<SimpleQueueConfiguration> {
  private static final Logger LOG = Logger.getLogger( SimpleQueueServiceBuilder.class );

  @Override
  public SimpleQueueConfiguration newInstance( ) {
    return new SimpleQueueConfiguration( );
  }

  @Override
  public SimpleQueueConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new SimpleQueueConfiguration( partition, name, host, port );
  }

  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( SimpleQueue.class );
  }

  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException { }

  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException { }

  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException { }

  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException { }

  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException { }

}
