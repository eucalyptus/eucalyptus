/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation;

import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.AwsServiceName;
import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.annotation.FaultLogPrefix;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.component.annotation.PolicyVendor;
import com.eucalyptus.component.annotation.PublicService;
import com.eucalyptus.util.techpreview.TechPreview;

@PublicService
@AwsServiceName( "cloudformation" )
@PolicyVendor( PolicySpec.VENDOR_CLOUDFORMATION )
@Partition( value = CloudFormation.class, manyToOne = true )
@FaultLogPrefix( "services" )
@TechPreview
@Description( "Cloudformation API service" )
public class CloudFormation extends ComponentId {
  private static final long serialVersionUID = 1L;

  @Override
  public String getInternalNamespaceSuffix() {
    return "/cloudformation";
  }
}
