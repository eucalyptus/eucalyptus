/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
 * Please contact Eucalyptus Systems, Inc., 6750 Navigator Way, Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.util.metrics;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;


@ConfigurableClass( root = "cloud", description = "Parameters controlling internal metrics collection")
public class MetricsConfiguration {
    @ConfigurableField(initial = "1000", description = "Size of the reporting data set that stores performance mertics dataset",
        changeListener=CollectionSizeChangeListener.class)
    public static volatile int METRICS_COLLECTION_SIZE = 1000;

    public static class CollectionSizeChangeListener implements PropertyChangeListener<String> {
      @Override
      public void fireChange(ConfigurableProperty t, String newValue)
          throws ConfigurablePropertyException {
        if(t.getValue()!=null && t.getValue().equals(newValue))
          return;
        try {
          int newSize = Integer.parseInt(newValue);
          if (newSize <= 0)
            throw new NumberFormatException();
          ThruputMetrics.changeSize(newSize);
        } catch (NumberFormatException ex) {
          throw new ConfigurablePropertyException(
              "The value must be number type and bigger than 0");
        }
      }
    }
}
