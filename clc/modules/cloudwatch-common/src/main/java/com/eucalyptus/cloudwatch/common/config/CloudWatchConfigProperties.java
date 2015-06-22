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
package com.eucalyptus.cloudwatch.common.config;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;

@ConfigurableClass( root = "cloudwatch", description = "Parameters controlling cloud watch and reporting")
public class CloudWatchConfigProperties {
  @ConfigurableField(initial = "false", description = "Set this to true to stop cloud watch alarm evaluation and new alarm/metric data entry")
  public static volatile Boolean DISABLE_CLOUDWATCH_SERVICE = false;

  public static Boolean isDisabledCloudWatchService() {
    return DISABLE_CLOUDWATCH_SERVICE;
  }
}
