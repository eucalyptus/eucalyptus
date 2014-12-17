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
package com.eucalyptus.imaging;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;

@ConfigurableClass(root = "services.imaging", description = "Parameters controlling import tasks")
public class ImportTaskProperties {
  @ConfigurableField(displayName = "task_expiration_hours",
      description = "expiration hours of import volume/instance tasks",
      readonly = false,
      initial = "168",
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = NumberValidationListener.class)
  public static String IMPORT_TASK_EXPIRATION_HOURS = "168";

  @ConfigurableField(displayName = "task_timeout_minutes",
      description = "expiration time in minutes of import tasks",
      readonly = false, initial = "180",
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = NumberValidationListener.class)
  public static String IMPORT_TASK_TIMEOUT_MINUTES = "180";
  
  public static class NumberValidationListener implements
      PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try {
        Integer.parseInt(newValue);
      } catch (final NumberFormatException ex) {
        throw new ConfigurablePropertyException("Invalid number");
      }
    }
  }
}
