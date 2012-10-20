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
			String ratioStr = ((String) newValue).substring(0, ((String) newValue).length() - 1);
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
