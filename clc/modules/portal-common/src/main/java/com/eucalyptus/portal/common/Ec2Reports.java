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
package com.eucalyptus.portal.common;

import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.AwsServiceName;
import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.component.annotation.PublicService;
import com.eucalyptus.portal.common.policy.Ec2ReportsPolicySpec;

@PublicService
@AwsServiceName( "ec2reports" )
@PolicyVendor(Ec2ReportsPolicySpec.VENDOR_EC2REPORTS)
@Partition( value = Ec2Reports.class, manyToOne = true )
@Description( "Ec2 reports service" )
public class Ec2Reports extends ComponentId {
  private static final long serialVersionUID = 1L;
}
