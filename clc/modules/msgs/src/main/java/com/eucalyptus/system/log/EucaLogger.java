package com.eucalyptus.system.log;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;

public class EucaLogger extends Logger {

	protected EucaLogger(String name) {
		super(name);
	}

	@Override
	protected void forcedLog(String fqcn, Priority level, Object message,
			Throwable t) {
		// TODO Auto-generated method stub
	    callAppenders(new EucaLoggingEvent(fqcn, this, level, message, t));
	}
	
}
