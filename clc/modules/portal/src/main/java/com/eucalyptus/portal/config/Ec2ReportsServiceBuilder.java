/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/

package com.eucalyptus.portal.config;

import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.common.Ec2Reports;
import com.eucalyptus.portal.common.config.DeregisterEc2ReportsType;
import com.eucalyptus.portal.common.config.DescribeEc2ReportsType;
import com.eucalyptus.portal.common.config.ModifyEc2ReportsAttributeType;
import com.eucalyptus.portal.common.config.RegisterEc2ReportsType;

@SuppressWarnings( "unused" )
@ComponentPart( Ec2Reports.class )
@Handles( {
        DeregisterEc2ReportsType.class,
        DescribeEc2ReportsType.class,
        ModifyEc2ReportsAttributeType.class,
        RegisterEc2ReportsType.class,
} )
public class Ec2ReportsServiceBuilder extends AbstractServiceBuilder<Ec2ReportsServiceConfiguration> {
  @Override
  public Ec2ReportsServiceConfiguration newInstance() {
    return new Ec2ReportsServiceConfiguration();
  }

  @Override
  public Ec2ReportsServiceConfiguration newInstance(String partition, String name, String host, Integer port) {
    return new Ec2ReportsServiceConfiguration(partition, name, host, port);
  }

  @Override
  public ComponentId getComponentId() {
    return ComponentIds.lookup( Ec2Reports.class );
  }

  @Override
  public void fireStart(ServiceConfiguration config) throws ServiceRegistrationException { }

  @Override
  public void fireStop(ServiceConfiguration config) throws ServiceRegistrationException { }

  @Override
  public void fireEnable(ServiceConfiguration config) throws ServiceRegistrationException { }

  @Override
  public void fireDisable(ServiceConfiguration config) throws ServiceRegistrationException { }

  @Override
  public void fireCheck(ServiceConfiguration config) throws ServiceRegistrationException, Faults.CheckException { }
}
