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
package com.eucalyptus.simplequeue;

import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.AwsServiceName;
import com.eucalyptus.component.annotation.ComponentDatabase;
import com.eucalyptus.component.annotation.DatabaseNamingStrategy;
import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.annotation.FaultLogPrefix;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.component.annotation.PublicService;
import com.eucalyptus.util.techpreview.TechPreview;

/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
@PublicService
@AwsServiceName( "sqs" )
@PolicyVendor( "sqs" )
@Partition( value = SimpleQueue.class, manyToOne = true )
@FaultLogPrefix( "services" )
@ComponentDatabase( namingStrategy = DatabaseNamingStrategy.Schema )
@Description( "Simple Queue API service" )
@TechPreview(enableByDefaultProperty = "enable.sqs.tech.preview")
public class SimpleQueue extends ComponentId {
  private static final long serialVersionUID = 1L;

  @Override
  public String getInternalNamespaceSuffix() {
    return "/SimpleQueue";
  }
  
}
