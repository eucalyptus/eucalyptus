package com.eucalyptus.system.log;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;

public class EucaLoggerFactory implements LoggerFactory {

	public EucaLoggerFactory() {
	}
	@Override
	public Logger makeNewLoggerInstance(String name) {
		return new EucaLogger(name);
	}

}
