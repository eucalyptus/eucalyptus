package com.eucalyptus.bootstrap.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.troubleshooting.TestFaultTrigger;

public class TriggerFaultListener implements PropertyChangeListener {
	private static final Logger LOG = Logger.getLogger(TriggerFaultListener.class);
	/**
	 * @see 
 com.eucalyptus.configurable.PropertyChangeListener#fireChange(com.eucalyptus.configurable.ConfigurableProperty,
	 * java.lang.Object)
	 */
	@Override
	public void fireChange( ConfigurableProperty t, Object newValue ) throws
	ConfigurablePropertyException {
		if (newValue == null) {
			newValue = "";
		}
		LOG.info( "Triggering fault with params " + newValue);
		List<String> args = split((String) newValue);
		if (args == null) throw new
		ConfigurablePropertyException("Invalid params for fault trigger " +
				newValue);
		if (args.size() < 1) throw new
		ConfigurablePropertyException("No fault id specified for fault trigger "
				+ newValue);
		int faultId = -1;
		try {
			faultId = Integer.parseInt(args.get(0));
		} catch (Exception ex) {
			throw new
			ConfigurablePropertyException("Invalid params for fault trigger " +
					newValue);
		}
		// There should be an odd number of arguments. A fault id, and an even number of key value
		// pairs. If there are not, discard the last value
		if (args.size() % 2 == 0) {
			LOG.warn("Unmatched key/value pairs in fault trigger, ignoring last value "
					+ args.get(args.size() - 1));
			args.remove(args.size() - 1);
		}
		Properties varProps = new Properties();
		for (int i=1;i<args.size();i+=2) {
			varProps.setProperty(args.get(i), args.get(i+1));
		}
		TestFaultTrigger.triggerFault(faultId, varProps);
		LOG.info("Triggered fault with params " + newValue);
		throw new
		ConfigurablePropertyException("Fault triggered, value not persisted");
	}

	private List<String> split(String s) {
		ArrayList<String> retVal = new ArrayList<String>();
		if (s != null) {
			StringTokenizer stok = new StringTokenizer(s);
			while (stok.hasMoreTokens()) {
				retVal.add(stok.nextToken());
			}
		}
		return retVal;
	}
}
