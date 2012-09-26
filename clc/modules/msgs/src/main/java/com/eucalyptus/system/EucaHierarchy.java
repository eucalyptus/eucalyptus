package com.eucalyptus.system;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;

public class EucaHierarchy extends Hierarchy {

	private LoggerFactory defaultFactory;

	public EucaHierarchy(Logger root) {
		super(root);
		defaultFactory = new EucaLoggerFactory();
	}

	@Override
	public Logger getLogger(String name) {
		return getLogger(name, defaultFactory);
	}
}
