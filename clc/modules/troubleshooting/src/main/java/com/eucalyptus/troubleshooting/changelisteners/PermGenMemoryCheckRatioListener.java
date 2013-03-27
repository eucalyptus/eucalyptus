/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.troubleshooting.changelisteners;

import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.troubleshooting.checker.schedule.PermGenMemoryCheckScheduler;

public class PermGenMemoryCheckRatioListener implements PropertyChangeListener {
	/**
	 * @see com.eucalyptus.configurable.PropertyChangeListener#fireChange(com.eucalyptus.configurable.ConfigurableProperty,
	 *      java.lang.Object)
	 */
	@Override
	public void fireChange(ConfigurableProperty t, Object newValue)
			throws ConfigurablePropertyException {
		if (newValue == null) {
			throw new ConfigurablePropertyException("Invalid value " + newValue);
		} else if (!(newValue instanceof String)) {
			throw new ConfigurablePropertyException("Invalid value " + newValue);
		} else { 
			String ratioStr = (String) newValue;
			double ratio = -1.0;
			try {
				ratio = Double.parseDouble(ratioStr);
			} catch (Exception ex) {
				throw new ConfigurablePropertyException("Invalid value " + newValue);
			}
			if (ratio < 0 || ratio > 1) {
				throw new ConfigurablePropertyException("Invalid value " + newValue);
			}
		} 
		try {
			t.getField().set(null, t.getTypeParser().apply(newValue));
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
			throw new ConfigurablePropertyException(e1);
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
			throw new ConfigurablePropertyException(e1);
		}
		PermGenMemoryCheckScheduler.resetMXBeanMemoryCheck();
	}
}
