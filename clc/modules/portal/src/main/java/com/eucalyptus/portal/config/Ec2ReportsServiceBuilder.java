/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
