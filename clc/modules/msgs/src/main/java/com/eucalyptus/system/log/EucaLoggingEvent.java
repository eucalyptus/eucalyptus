package com.eucalyptus.system.log;

import java.util.Map;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public class EucaLoggingEvent extends LoggingEvent {

	private static final long serialVersionUID = -2006882771771941367L;
	private Long threadId = null;
	
	public EucaLoggingEvent(String fqnOfCategoryClass,
			Category logger, Priority level, Object message, Throwable throwable) {
		super(fqnOfCategoryClass, logger, level, message, throwable);
	}

	public EucaLoggingEvent(String fqnOfCategoryClass,
			Category logger, long timeStamp, Priority level, Object message,
			Throwable throwable) {
		super(fqnOfCategoryClass, logger, timeStamp, level, message, throwable);
	}

	public EucaLoggingEvent(String fqnOfCategoryClass,
			Category logger, long timeStamp, Level level, Object message,
			String threadName, ThrowableInformation throwable, String ndc,
			LocationInfo info, Map properties) {
		super(fqnOfCategoryClass, logger, timeStamp, level, message,
				threadName, throwable, ndc, info, properties);
	}
	
	public long getThreadId() {
		if (threadId == null) {
			threadId = Thread.currentThread().getId();
		}
		return threadId;
	}
}
