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
package com.eucalyptus.portal.config;

import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.common.Tag;
import com.eucalyptus.portal.common.config.DeregisterTagType;
import com.eucalyptus.portal.common.config.DescribeTagType;
import com.eucalyptus.portal.common.config.ModifyTagAttributeType;
import com.eucalyptus.portal.common.config.RegisterTagType;

/**
 *
 */
@ComponentPart( Tag.class )
@Handles( {
    DeregisterTagType.class,
    DescribeTagType.class,
    ModifyTagAttributeType.class,
    RegisterTagType.class,
} )
public class TagServiceBuilder extends AbstractServiceBuilder<TagServiceConfiguration> {
  @Override
  public TagServiceConfiguration newInstance( ) {
    return new TagServiceConfiguration( );
  }

  @Override
  public TagServiceConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new TagServiceConfiguration( partition, name, host, port );
  }

  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( Tag.class );
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
