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
import com.eucalyptus.configurable.PropertyChangeListeners;

@ConfigurableClass( root = "cloudwatch", description = "Parameters controlling cloud watch and reporting")
public class CloudWatchConfigProperties {
  @ConfigurableField(initial = "true", description = "Set this to false to stop cloud watch alarm evaluation and new alarm/metric data entry", changeListener = PropertyChangeListeners.IsBoolean.class)
  public static volatile Boolean ENABLE_CLOUDWATCH_SERVICE = true;

  @ConfigurableField(initial = "1000", description = "Size of the reporting data set that stores cloud watch queues performance info (debug only prop)")
  public static volatile int CLOUDWATCH_MONITORING_HISTORY_SIZE = 1000;

  public static Boolean isDisabledCloudWatchService() {
    return !ENABLE_CLOUDWATCH_SERVICE;
  }
}
