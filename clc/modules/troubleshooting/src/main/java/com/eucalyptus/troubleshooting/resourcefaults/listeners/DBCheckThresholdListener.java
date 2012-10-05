package com.eucalyptus.troubleshooting.resourcefaults.listeners;

import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.troubleshooting.resourcefaults.schedulers.DBCheckScheduler;

public class DBCheckThresholdListener implements PropertyChangeListener {
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
		} else if (((String) newValue).endsWith("%")) { //percentage
			String percentageStr = ((String) newValue).substring(0, ((String) newValue).length() - 1);
			double percentage = -1.0;
			try {
				percentage = Double.parseDouble(percentageStr);
			} catch (Exception ex) {
				throw new ConfigurablePropertyException("Invalid value " + newValue);
			}
			if (percentage < 0 || percentage > 100) {
				throw new ConfigurablePropertyException("Invalid value " + newValue);
			}
		} else {
			int numConnections = -1;
			try {
				numConnections = Integer.parseInt((String) newValue);
			} catch (Exception ex) {
				throw new ConfigurablePropertyException("Invalid value " + newValue);
			}
			if (numConnections <= 0) {
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
		DBCheckScheduler.resetDBCheck();
	}
}
