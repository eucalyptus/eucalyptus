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
